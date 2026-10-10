package com.multi2.app.engine

import android.util.Log

/**
 * Hidden API bypass via meta-reflection (pure Java, no native).
 *
 * Technique (public, used by LSPosed's HiddenApiBypass and cloner engines):
 *  1. Class.getDeclaredMethod IS an SDK-visible member — reflection on it is legal.
 *  2. Through it we obtain VMRuntime.getRuntime().setHiddenApiExemptions(String[])
 *     without the enforcement engine seeing a "hidden reflection" call site.
 *  3. Exempting "L" whitelists every hidden API (needed for Activity/Application
 *     attach, Instrumentation, AssetManager.addAssetPath in the guest runtime).
 *
 * The original performs the equivalent inside libma2023.so (native).
 * Works API 28–35; no-op (returns true) below 28.
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

    private fun applyNative(): Boolean {
        if (nativeTried) return false
        nativeTried = true
        return runCatching {
            System.loadLibrary("multi2bypass")
            nativeApplyHiddenApiBypass()
        }.getOrElse {
            lastError = "native: ${it.javaClass.simpleName}: ${it.message}"
            Log.w(TAG, "native bypass unavailable: $lastError")
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
        if (applyNative()) {
            applied = true
            Log.i(TAG, "hidden api exemptions applied (native)")
            return true
        }
        // 2) meta-reflection fallback (works <= API 14-ish ROMs)
        return runCatching {
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
            lastError = "${it.javaClass.simpleName}: ${it.message}"
            Log.w(TAG, "bypass failed: $lastError")
            false
        }
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
