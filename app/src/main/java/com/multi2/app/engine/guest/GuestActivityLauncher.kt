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

    /** Probe and invoke the API-level-correct Activity.attach signature. */
    private fun attachActivity(
        stub: Activity,
        activity: Activity,
        guest: GuestRuntime.GuestApp,
        activityClassName: String,
        spaceId: Int,
    ) {
        val instrumentation = Instrumentation()
        val token = android.os.Binder()
        val app = guest.application ?: stub.application
        val intent = Intent().setComponent(ComponentName(guest.pkg, activityClassName))
        val appInfo = ApplicationInfo().apply {
            packageName = guest.pkg
            sourceDir = EnginePaths.apkDir(stub, spaceId, guest.pkg).absolutePath + "/base.apk"
            publicSourceDir = sourceDir
            dataDir = EnginePaths.dataDir(stub, spaceId, guest.pkg).absolutePath
        }
        val info = ActivityInfo().apply {
            packageName = guest.pkg
            name = activityClassName
            applicationInfo = appInfo
        }
        val guestContext = GuestContext(
            stub, guest.pkg, spaceId, guest.classLoader, guest.resources, appInfo,
        )

        val activityThread = Class.forName("android.app.ActivityThread")
        val ncInstances = Class.forName("android.app.Activity\$NonConfigurationInstances")
        val voiceInteractor = Class.forName("com.android.internal.app.IVoiceInteractor")
        val configCallback = runCatching {
            Class.forName("android.view.ViewRootImpl\$ActivityConfigCallback")
        }.getOrNull()

        val signatures = buildList<Array<Class<*>>> {
            // API 31–35: 18 params (IBinder token, IBinder assistToken, …, callback, IBinder shareableToken)
            if (configCallback != null) {
                add(
                    arrayOf(
                        Context::class.java, activityThread, Instrumentation::class.java,
                        IBinder::class.java, IBinder::class.java,
                        android.app.Application::class.java, Intent::class.java, ActivityInfo::class.java,
                        CharSequence::class.java, Activity::class.java, String::class.java,
                        ncInstances, android.content.res.Configuration::class.java, String::class.java,
                        voiceInteractor, android.view.Window::class.java, configCallback, IBinder::class.java,
                    ),
                )
                // API 30: 17 params (no shareable token)
                add(
                    arrayOf(
                        Context::class.java, activityThread, Instrumentation::class.java,
                        IBinder::class.java, IBinder::class.java,
                        android.app.Application::class.java, Intent::class.java, ActivityInfo::class.java,
                        CharSequence::class.java, Activity::class.java, String::class.java,
                        ncInstances, android.content.res.Configuration::class.java, String::class.java,
                        voiceInteractor, android.view.Window::class.java, configCallback,
                    ),
                )
                // API 28–29: 16 params (int ident)
                add(
                    arrayOf(
                        Context::class.java, activityThread, Instrumentation::class.java,
                        IBinder::class.java, Int::class.javaPrimitiveType!!,
                        android.app.Application::class.java, Intent::class.java, ActivityInfo::class.java,
                        CharSequence::class.java, Activity::class.java, String::class.java,
                        ncInstances, android.content.res.Configuration::class.java, String::class.java,
                        voiceInteractor, android.view.Window::class.java, configCallback,
                    ),
                )
            }
            // API 26–27: 15 params (no config callback)
            add(
                arrayOf(
                    Context::class.java, activityThread, Instrumentation::class.java,
                    IBinder::class.java, Int::class.javaPrimitiveType!!,
                    android.app.Application::class.java, Intent::class.java, ActivityInfo::class.java,
                    CharSequence::class.java, Activity::class.java, String::class.java,
                    ncInstances, android.content.res.Configuration::class.java, String::class.java,
                    voiceInteractor, android.view.Window::class.java,
                ),
            )
        }

        var lastError: Throwable? = null
        for (sig in signatures) {
            try {
                val method = Activity::class.java.getDeclaredMethod("attach", *sig)
                method.isAccessible = true
                val args = buildArgs(sig.size, stub, instrumentation, token, app, intent, info, guestContext)
                method.invoke(activity, *args)
                return
            } catch (t: Throwable) {
                lastError = t
            }
        }
        throw IllegalStateException("Activity.attach failed on API ${Build.VERSION.SDK_INT}: ${lastError?.message}")
    }

    private fun buildArgs(
        count: Int,
        stub: Activity,
        instrumentation: Instrumentation,
        token: IBinder,
        app: android.app.Application,
        intent: Intent,
        info: ActivityInfo,
        guestContext: Context,
    ): Array<Any?> {
        val args = arrayOfNulls<Any?>(count)
        for (i in 0 until count) {
            args[i] = when (i) {
                0 -> guestContext
                1 -> null // ActivityThread
                2 -> instrumentation
                3 -> token
                4 -> if (count >= 17) token else 0 // API30+: IBinder assistToken; older: int ident
                5 -> app
                6 -> intent
                7 -> info
                8 -> info.name
                9 -> stub // parent
                10 -> "multi2" // embedded id
                11 -> null // NonConfigurationInstances
                12 -> stub.resources.configuration
                13 -> stub.packageName // referrer
                14 -> null // IVoiceInteractor
                15 -> null // Window — attach creates it
                else -> null // config callback / shareable token
            }
        }
        return args
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
