package com.multi2.app.engine.guest

import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.content.res.Resources
import android.os.Build
import android.util.Log
import com.multi2.app.engine.EnginePaths
import com.multi2.app.engine.HiddenApiBypass
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
        // Android 14+ DCL restriction: dex files must be read-only.
        // Also heals clones installed before the installer enforced this.
        apks.forEach { it.setReadOnly() }

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

    // NOTE: spoofing Settings.Secure (android_id) reads is NOT possible by
    // subclassing ContentResolver — every relevant method is final on modern
    // API. Real virtual engines do it with VM-level hooks (dexposed/epic) or
    // by swapping the settings IContentProvider binder. That hook layer is a
    // separate engine iteration; until then the virtual identity is
    // generated/stored/shown (Device Info) and used by our own components.
    override fun getClassLoader(): ClassLoader = guestClassLoader
    override fun getResources(): Resources = guestResources
    override fun getAssets(): AssetManager = guestResources.assets
    override fun getPackageName(): String = guestPkg
    override fun getApplicationInfo(): ApplicationInfo = guestAppInfo
    override fun getPackageResourcePath(): String =
        EnginePaths.apkDir(this, spaceId, guestPkg).absolutePath + "/base.apk"

    override fun getFilesDir() = EnginePaths.dataDir(this, spaceId, guestPkg).apply { mkdirs() }
    override fun getCacheDir() = EnginePaths.cacheDir(this, spaceId, guestPkg).apply { mkdirs() }
    override fun getDir(name: String, mode: Int): File =
        File(EnginePaths.dataDir(this, spaceId, guestPkg), name).apply { mkdirs() }
    override fun getDataDir() = EnginePaths.dataDir(this, spaceId, guestPkg).apply { mkdirs() }
}

/**
 * Virtual identity lives in [VirtualDeviceStore] (generated at install,
 * regenerated by GENERATE_ID, shown on Device Info). Guest-visible spoofing
 * of Settings.Secure / WifiInfo / BluetoothAdapter requires the planned
 * system-service + binder hook layer.
 */
