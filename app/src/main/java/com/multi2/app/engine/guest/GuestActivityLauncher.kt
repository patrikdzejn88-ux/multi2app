package com.multi2.app.engine.guest

import android.app.Activity
import android.app.Instrumentation
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import com.multi2.app.engine.EnginePaths

/**
 * Guest activity launcher — brings a cloned app's UI up inside a proxy stub.
 *
 * Technique (plugin-lite family):
 *  1. Resolve the guest launcher activity (host PackageManager for host
 *     clones; GET_ACTIVITIES first-exported fallback for file imports).
 *  2. Instantiate it via the guest DexClassLoader.
 *  3. Activity.attach(...) via hidden-API-bypassed reflection, probing the
 *     per-API-level signature variants (API 26–35).
 *  4. Drive onCreate → guest builds its DecorView → transplant into the stub.
 *  5. Forward stub lifecycle (resume/pause/destroy).
 */
object GuestActivityLauncher {

    private const val TAG = "GuestActivity"

    data class GuestActivityInstance(
        val activity: Activity,
        val decorView: View?,
    )

    fun launch(
        stub: Activity,
        guest: GuestRuntime.GuestApp,
        pkg: String,
        spaceId: Int,
    ): Result<GuestActivityInstance> = runCatching {
        val candidates = resolveLauncherCandidates(stub, pkg, spaceId)
        require(candidates.isNotEmpty()) { "no launcher activity for $pkg" }

        // Try candidates until one actually loads from the guest dex —
        // host PM may return an activity-alias name that has no class.
        var lastError: Throwable? = null
        for (activityClassName in candidates) {
            val loaded = runCatching {
                guest.classLoader.loadClass(activityClassName)
                    .getDeclaredConstructor().newInstance() as Activity
            }
            loaded.onFailure { lastError = it }
            val activity = loaded.getOrNull() ?: continue
            Log.i(TAG, "launching guest activity: $activityClassName")

            attachActivity(stub, activity, guest, activityClassName, spaceId)
            callOnCreate(activity)

            val decor = runCatching { activity.window?.decorView }.getOrNull()
            return@runCatching GuestActivityInstance(activity, decor)
        }
        throw IllegalStateException(
            "no loadable launcher activity for $pkg (tried ${candidates.size}): ${lastError?.message}",
        )
    }

    /**
     * Ordered launcher-activity candidates with activity-alias resolution:
     * PackageManager returns the alias component for apps like foodora/
     * onlinepizza/Play Store (com.android.vending.AssetBrowserActivity is an
     * alias); the real class is ActivityInfo.targetActivity.
     */
    private fun resolveLauncherCandidates(context: Context, pkg: String, spaceId: Int): List<String> {
        val out = LinkedHashSet<String>()

        // 1) host clone: launch component + its alias target
        runCatching {
            context.packageManager.getLaunchIntentForPackage(pkg)?.component?.let { comp ->
                out += comp.className
                resolveAliasTarget(context.packageManager, comp)?.let { out += it }
            }
        }

        // 2) archive activities: exported first; alias targets included
        val baseApk = EnginePaths.apkDir(context, spaceId, pkg).absolutePath + "/base.apk"
        val flags = PackageManager.GET_ACTIVITIES.toLong()
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageArchiveInfo(baseApk, PackageManager.PackageInfoFlags.of(flags))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageArchiveInfo(baseApk, flags.toInt())
        }
        val activities = info?.activities.orEmpty().toList()
        activities.filter { it.exported }.forEach { ai ->
            out += ai.name
            ai.targetActivity?.let { out += it }
        }
        activities.forEach { ai ->
            out += ai.name
            ai.targetActivity?.let { out += it }
        }
        return out.toList()
    }

    /** Resolve an activity-alias component to its real target class. */
    private fun resolveAliasTarget(pm: PackageManager, component: ComponentName): String? =
        runCatching {
            @Suppress("DEPRECATION")
            pm.getActivityInfo(component, 0).targetActivity
        }.getOrNull()

    /**
     * Activity.attach via signature discovery, not hardcoded variants.
     * The method is unique per runtime; arguments are mapped by parameter
     * TYPE, so every API level works — including API 35's 20-param variant
     * (…, MultiWindowModeController, boolean shouldDockBigOverlays).
     */
    private fun attachActivity(
        stub: Activity,
        activity: Activity,
        guest: GuestRuntime.GuestApp,
        activityClassName: String,
        spaceId: Int,
    ) {
        val pkg = guest.pkg
        val appInfo = ApplicationInfo().apply {
            packageName = pkg
            sourceDir = EnginePaths.apkDir(stub, spaceId, pkg).absolutePath + "/base.apk"
            publicSourceDir = sourceDir
            dataDir = EnginePaths.dataDir(stub, spaceId, pkg).absolutePath
        }
        // Real ActivityInfo (theme, softInput, launchMode...) — guests crash
        // without their own theme (AppCompat) right after attach.
        val info = resolveActivityInfo(stub, pkg, spaceId, activityClassName).apply {
            applicationInfo = appInfo
        }
        val guestContext = GuestContext(
            stub, pkg, spaceId, guest.classLoader, guest.resources, appInfo,
        )

        val instrumentation = Instrumentation()
        val token: IBinder = android.os.Binder()
        val app = guest.application ?: stub.application
        val intent = Intent().setComponent(ComponentName(pkg, activityClassName))
        val activityThread = runCatching {
            val threadClass = Class.forName("android.app.ActivityThread")
            com.multi2.app.engine.HiddenApiBypass
                .findHiddenMethod(threadClass, "currentActivityThread")
                ?.apply { isAccessible = true }
                ?.invoke(null)
        }.getOrNull()

        // re-assert exemptions on this thread's process before reflection
        com.multi2.app.engine.HiddenApiBypass.apply()
        val method = com.multi2.app.engine.HiddenApiBypass
            .findHiddenMethod(Activity::class.java, "attach")
            ?: throw IllegalStateException(
                "Activity.attach not found (bypass=${com.multi2.app.engine.HiddenApiBypass.applied}, " +
                    "err=${com.multi2.app.engine.HiddenApiBypass.lastError}, " +
                    "declared=${runCatching { Activity::class.java.declaredMethods.size }.getOrDefault(-1)})",
            )
        method.isAccessible = true

        val params = method.parameterTypes
        val args = arrayOfNulls<Any?>(params.size)
        var stringIndex = 0
        params.forEachIndexed { i, t ->
            args[i] = when {
                i == 0 && Context::class.java.isAssignableFrom(t) -> guestContext
                t.name == "android.app.ActivityThread" -> activityThread
                t == Instrumentation::class.java -> instrumentation
                t == IBinder::class.java -> token
                t == java.lang.Integer.TYPE -> 0
                t == java.lang.Boolean.TYPE -> false
                t == android.app.Application::class.java -> app
                t == Intent::class.java -> intent
                t == ActivityInfo::class.java -> info
                t == CharSequence::class.java -> info.name
                t == Activity::class.java -> stub
                t == String::class.java -> {
                    stringIndex++
                    // first String = embedded id, second = referrer
                    if (stringIndex == 1) "multi2" else stub.packageName
                }
                t == android.content.res.Configuration::class.java -> stub.resources.configuration
                // NonConfigurationInstances / IVoiceInteractor / Window /
                // ActivityConfigCallback / shareable token extras / unknown
                else -> null
            }
        }
        method.invoke(activity, *args)
    }

    /** Full ActivityInfo from the host PM (or archive), for real theme/flags. */
    private fun resolveActivityInfo(
        context: Context,
        pkg: String,
        spaceId: Int,
        className: String,
    ): ActivityInfo {
        runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getActivityInfo(ComponentName(pkg, className), 0)
        }.getOrNull()?.let { return it }

        val baseApk = EnginePaths.apkDir(context, spaceId, pkg).absolutePath + "/base.apk"
        val flags = PackageManager.GET_ACTIVITIES.toLong()
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageArchiveInfo(baseApk, PackageManager.PackageInfoFlags.of(flags))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageArchiveInfo(baseApk, flags.toInt())
        }
        return info?.activities?.firstOrNull { it.name == className }
            ?: ActivityInfo().apply {
                packageName = pkg
                name = className
            }
    }

    private fun callOnCreate(activity: Activity) {
        val method = Activity::class.java.getDeclaredMethod("onCreate", Bundle::class.java)
        method.isAccessible = true
        method.invoke(activity, null)
    }

    /** Forward lifecycle from the stub to the guest activity. */
    fun forwardLifecycle(activity: Activity, event: String) {
        runCatching {
            val method = when (event) {
                "resume" -> Activity::class.java.getDeclaredMethod("onResume")
                "pause" -> Activity::class.java.getDeclaredMethod("onPause")
                "destroy" -> Activity::class.java.getDeclaredMethod("onDestroy")
                else -> return
            }
            method.isAccessible = true
            method.invoke(activity)
        }.onFailure { Log.w(TAG, "forward $event failed: ${it.message}") }
    }
}
