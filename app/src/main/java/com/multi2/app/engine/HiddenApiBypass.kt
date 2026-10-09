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

    fun apply(): Boolean {
        if (applied) return true
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.P) {
            applied = true
            return true
        }
        return runCatching {
            val forName = Class::class.java.getDeclaredMethod("forName", String::class.java)
            val getDeclaredMethod = Class::class.java.getDeclaredMethod(
                "getDeclaredMethod", String::class.java, arrayOf<Class<*>>()::class.java,
            )
            val vmRuntimeClass = forName.invoke(null, "dalvik.system.VMRuntime") as Class<*>
            val getRuntime = getDeclaredMethod.invoke(vmRuntimeClass, "getRuntime", emptyArray()) as java.lang.reflect.Method
            val setExemptions = getDeclaredMethod.invoke(
                vmRuntimeClass, "setHiddenApiExemptions", arrayOf(arrayOf<String>()::class.java),
            ) as java.lang.reflect.Method
            val runtime = getRuntime.invoke(null)
            setExemptions.invoke(runtime, arrayOf("L"))
            applied = true
            Log.i(TAG, "hidden api exemptions applied")
            true
        }.getOrElse {
            Log.w(TAG, "bypass failed: ${it.message}")
            false
        }
    }
}
