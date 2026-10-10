package com.multi2.app.engine

import android.util.Log

/**
 * Hidden API bypass — layered, like the original's libma2023.so:
 *  1. NATIVE (bypass.cpp): JNI setHiddenApiExemptions, then direct
 *     art::Runtime::SetHiddenApiEnforcementPolicy(kDisabled) via dlsym on
 *     libart.so — unrestricted by Java filtering and by the hardened
 *     Java entry point on Android 15.
 *  2. Meta-reflection: get VMRuntime.setHiddenApiExemptions through
 *     reflected Class.getDeclaredMethod (works on API 28–34 stock).
 *  3. findHiddenMethod meta-lookup as the last-resort member resolver.
 * Works API 28–35+; no-op below 28.
 */
object HiddenApiBypass {

    private const val TAG = "HiddenApiBypass"

    @Volatile
    var applied: Boolean = false
        private set

    @Volatile
    var lastError: String? = null
        private set

    @Volatile
    private var nativeTried = false

    /** JNI bypass — not subject to Java hidden-API filtering (API 15-safe). */
    private external fun nativeApplyHiddenApiBypass(): Boolean
    private external fun nativeGetLastError(): String

    @Volatile
    private var nativeDetail: String = "not run"

    private fun applyNative(): Boolean {
        if (nativeTried) return false
        nativeTried = true
        return runCatching {
            System.loadLibrary("multi2bypass")
            val ok = nativeApplyHiddenApiBypass()
            nativeDetail = if (ok) "ok" else nativeGetLastError()
            ok
        }.getOrElse {
            nativeDetail = "load fail: ${it.javaClass.simpleName}: ${it.message}"
            Log.w(TAG, "native bypass unavailable: $nativeDetail")
            false
        }
    }

    fun apply(): Boolean {
        if (applied) return true
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.P) {
            applied = true
            return true
        }
        // 1) native first — Java-level enforcement does not see JNI calls
        val nativeOk = applyNative()
        if (nativeOk) {
            applied = true
            Log.i(TAG, "hidden api exemptions applied (native)")
            return true
        }
        // 2) meta-reflection fallback (works <= API 14-ish ROMs)
        var metaDetail = "ok"
        val metaOk = runCatching {
            val classArrayClass: Class<*> = emptyArray<Class<*>>().javaClass
            val stringArrayClass: Class<*> = emptyArray<String>().javaClass
            val forName = Class::class.java.getDeclaredMethod("forName", String::class.java)
            val getDeclaredMethod = Class::class.java.getDeclaredMethod(
                "getDeclaredMethod", String::class.java, classArrayClass,
            )
            val vmRuntimeClass = forName.invoke(null, "dalvik.system.VMRuntime") as Class<*>
            val getRuntime = getDeclaredMethod.invoke(
                vmRuntimeClass, "getRuntime", emptyArray<Class<*>>(),
            ) as java.lang.reflect.Method
            val setExemptions = getDeclaredMethod.invoke(
                vmRuntimeClass, "setHiddenApiExemptions", arrayOf(stringArrayClass),
            ) as java.lang.reflect.Method
            val runtime = getRuntime.invoke(null)
            setExemptions.invoke(runtime, arrayOf("L"))
            applied = true
            Log.i(TAG, "hidden api exemptions applied")
            true
        }.getOrElse {
            metaDetail = "${it.javaClass.simpleName}: ${it.message}"
            Log.w(TAG, "meta bypass failed: $metaDetail")
            false
        }
        lastError = "native[$nativeDetail] meta[$metaDetail]"
        return metaOk
    }

    /**
     * Meta-reflective lookup that returns hidden members even when runtime
     * exemptions failed: invoking getDeclaredMethods reflectively makes the
     * hidden-member filter see java.lang.Class as the caller, not our code.
     */
    fun findHiddenMethod(clazz: Class<*>, name: String): java.lang.reflect.Method? {
        // 1) normal lookup (works once exemptions are effective)
        runCatching {
            clazz.declaredMethods.firstOrNull { it.name == name }?.let { return it }
        }
        // 2) meta-reflective lookup (works even without exemptions)
        return runCatching {
            val gdm = Class::class.java.getDeclaredMethod("getDeclaredMethods")
            @Suppress("UNCHECKED_CAST")
            val all = gdm.invoke(clazz) as Array<java.lang.reflect.Method>
            all.firstOrNull { it.name == name }
        }.getOrNull()
    }
}
