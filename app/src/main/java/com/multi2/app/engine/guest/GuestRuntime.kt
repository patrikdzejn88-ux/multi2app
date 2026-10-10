package com.multi2.app.engine.guest

import android.content.Context
import android.content.ContextWrapper
import android.content.ContentResolver
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.content.res.Resources
import android.database.MatrixCursor
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.multi2.app.engine.EnginePaths
import com.multi2.app.engine.HiddenApiBypass
import com.multi2.app.engine.VirtualDeviceInfo
import com.multi2.app.engine.VirtualDeviceStore
import dalvik.system.DexClassLoader
import java.io.File

/**
 * Guest runtime — loads a cloned app's code into a proxy process.
 *
 * M2 scope (real, working): DexClassLoader over sandbox apks, guest
 * Resources via AssetManager.addAssetPath, ContextWrapper with sandboxed
 * dirs, guest Application instantiation + attach + onCreate.
 * Guest Activity UI attach (per-API-level Activity.attach signatures)
 * lands in the next iteration.
 */
object GuestRuntime {

    private const val TAG = "GuestRuntime"

    data class GuestApp(
        val pkg: String,
        val classLoader: DexClassLoader,
        val resources: Resources,
        val application: android.app.Application?,
    )

    fun load(context: Context, pkg: String, spaceId: Int): GuestApp {
        HiddenApiBypass.apply()

        val apkDir = EnginePaths.apkDir(context, spaceId, pkg)
        val apks = apkDir.listFiles { f -> f.extension == "apk" }
            ?.sortedByDescending { it.name == "base.apk" }
            ?: error("apk not found for $pkg")
        require(apks.isNotEmpty()) { "apk not found for $pkg" }

        val dexOut = EnginePaths.dexOptDir(context, spaceId, pkg).apply { mkdirs() }
        val libDir = EnginePaths.libDir(context, spaceId, pkg)
        val abiLib = Build.SUPPORTED_ABIS.firstNotNullOfOrNull { abi ->
            File(libDir, abi).takeIf { it.exists() }
        }

        val classLoader = DexClassLoader(
            apks.joinToString(File.pathSeparator) { it.absolutePath },
            dexOut.absolutePath,
            abiLib?.absolutePath,
            context.classLoader,
        )

        val assetManager = AssetManager::class.java.getDeclaredConstructor().newInstance()
        val addAssetPath = AssetManager::class.java.getDeclaredMethod(
            "addAssetPath", String::class.java,
        )
        apks.forEach { addAssetPath.invoke(assetManager, it.absolutePath) }
        val hostRes = context.resources
        val resources = Resources(assetManager, hostRes.displayMetrics, hostRes.configuration)

        val application = runCatching {
            instantiateApplication(context, pkg, spaceId, classLoader, resources)
        }.getOrElse {
            Log.w(TAG, "guest Application not started for $pkg: ${it.message}")
            null
        }

        return GuestApp(pkg, classLoader, resources, application)
    }

    private fun instantiateApplication(
        context: Context,
        pkg: String,
        spaceId: Int,
        classLoader: DexClassLoader,
        resources: Resources,
    ): android.app.Application? {
        val pm = context.packageManager
        val info: ApplicationInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageArchiveInfo(
                EnginePaths.apkDir(context, spaceId, pkg).absolutePath + "/base.apk",
                PackageManager.PackageInfoFlags.of(0),
            )?.applicationInfo
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageArchiveInfo(
                EnginePaths.apkDir(context, spaceId, pkg).absolutePath + "/base.apk",
                0,
            )?.applicationInfo
        } ?: return null

        val appClassName = info.className
        val appClass: Class<*>? = appClassName?.let { name ->
            runCatching { classLoader.loadClass(name) }.getOrNull()
        }

        val guestContext = GuestContext(context, pkg, spaceId, classLoader, resources, info)
        val application: android.app.Application = if (appClass != null &&
            android.app.Application::class.java.isAssignableFrom(appClass)
        ) {
            appClass.getDeclaredConstructor().newInstance() as android.app.Application
        } else {
            android.app.Application()
        }

        // Application.attach(Context) — hidden API; requires HiddenApiBypass
        val attach = android.app.Application::class.java.getDeclaredMethod(
            "attach", Context::class.java,
        )
        attach.isAccessible = true
        attach.invoke(application, guestContext)
        application.onCreate()
        Log.i(TAG, "guest Application started: $pkg in space $spaceId")
        return application
    }
}

/** ContextWrapper redirecting the guest into its sandbox. */
class GuestContext(
    base: Context,
    private val guestPkg: String,
    private val spaceId: Int,
    private val guestClassLoader: ClassLoader,
    private val guestResources: Resources,
    private val guestAppInfo: ApplicationInfo,
) : ContextWrapper(base) {

    private val virtualDevice = VirtualDeviceStore.loadOrGenerate(base, spaceId, guestPkg)
    private val spoofedResolver = SpoofedContentResolver(base, base.contentResolver, virtualDevice)

    override fun getClassLoader(): ClassLoader = guestClassLoader
    override fun getResources(): Resources = guestResources
    override fun getAssets(): AssetManager = guestResources.assets
    override fun getPackageName(): String = guestPkg
    override fun getApplicationInfo(): ApplicationInfo = guestAppInfo
    override fun getContentResolver(): ContentResolver = spoofedResolver
    override fun getPackageResourcePath(): String =
        EnginePaths.apkDir(this, spaceId, guestPkg).absolutePath + "/base.apk"

    override fun getFilesDir() = EnginePaths.dataDir(this, spaceId, guestPkg).apply { mkdirs() }
    override fun getCacheDir() = EnginePaths.cacheDir(this, spaceId, guestPkg).apply { mkdirs() }
    override fun getDir(name: String, mode: Int): File =
        File(EnginePaths.dataDir(this, spaceId, guestPkg), name).apply { mkdirs() }
    override fun getDataDir() = EnginePaths.dataDir(this, spaceId, guestPkg).apply { mkdirs() }
}

/**
 * ContentResolver wrapper spoofing Settings.Secure android_id for guests.
 * Settings.Secure.getString resolves via query()/call() on the settings
 * provider — both are intercepted here. WiFi/BT MAC spoofing lands with
 * the system-service hook layer (WifiManager/BluetoothAdapter proxies).
 */
class SpoofedContentResolver(
    context: Context,
    private val wrapped: ContentResolver,
    private val virtualDevice: VirtualDeviceInfo,
) : ContentResolver(context) {

    private fun isAndroidIdUri(uri: android.net.Uri): Boolean =
        uri.authority == "settings" && uri.lastPathSegment == "android_id"

    override fun query(
        uri: android.net.Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): android.database.Cursor? {
        if (isAndroidIdUri(uri)) {
            return MatrixCursor(arrayOf("name", "value")).apply {
                addRow(arrayOf("android_id", virtualDevice.androidId))
            }
        }
        return wrapped.query(uri, projection, selection, selectionArgs, sortOrder)
    }

    override fun call(uri: android.net.Uri, method: String, arg: String?, extras: Bundle?): Bundle? {
        if (uri.authority == "settings" && "android_id" == arg) {
            return Bundle().apply { putString("value", virtualDevice.androidId) }
        }
        return wrapped.call(uri, method, arg, extras)
    }

    override fun getType(uri: android.net.Uri): String? = wrapped.getType(uri)
    override fun insert(uri: android.net.Uri, values: android.content.ContentValues?): android.net.Uri? =
        wrapped.insert(uri, values)
    override fun delete(uri: android.net.Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        wrapped.delete(uri, selection, selectionArgs)
    override fun update(
        uri: android.net.Uri,
        values: android.content.ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = wrapped.update(uri, values, selection, selectionArgs)
}
