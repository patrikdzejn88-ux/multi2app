#include <jni.h>
#include <android/log.h>
#include <dlfcn.h>
#include <cstdio>
#include <cstring>

#define LOG_TAG "Multi2Bypass"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

static char g_lastError[512] = "none";

static void setError(const char* what, const char* detail) {
    snprintf(g_lastError, sizeof(g_lastError), "%s: %s", what, detail ? detail : "?");
}

/**
 * Native hidden-API bypass — same family as the original's libma2023.so.
 *
 * Level 1: VMRuntime.setHiddenApiExemptions({"L"}) via JNI
 *          (Java-level filtering does not apply to JNI).
 * Level 2: art::Runtime::SetHiddenApiEnforcementPolicy(kDisabled) resolved
 *          directly from libart.so via dlsym — the epic/whale technique for
 *          ROMs where the Java entry point itself is hardened/throws.
 */

extern "C" JNIEXPORT jboolean JNICALL
Java_com_multi2_app_engine_HiddenApiBypass_nativeApplyHiddenApiBypass(
        JNIEnv* env, jclass /* clazz */) {

    // ---- Level 1: JNI call of VMRuntime.setHiddenApiExemptions ----
    jclass vmRuntimeClass = env->FindClass("dalvik/system/VMRuntime");
    if (vmRuntimeClass == nullptr) {
        env->ExceptionClear();
        setError("jni", "VMRuntime class not found");
    } else {
        jmethodID getRuntime = env->GetStaticMethodID(
                vmRuntimeClass, "getRuntime", "()Ldalvik/system/VMRuntime;");
        jmethodID setExemptions = env->GetMethodID(
                vmRuntimeClass, "setHiddenApiExemptions", "([Ljava/lang/String;)V");
        if (getRuntime == nullptr || setExemptions == nullptr) {
            env->ExceptionClear();
            setError("jni", "VMRuntime methods not found");
        } else {
            jobject runtime = env->CallStaticObjectMethod(vmRuntimeClass, getRuntime);
            if (env->ExceptionCheck() || runtime == nullptr) {
                if (env->ExceptionCheck()) { env->ExceptionDescribe(); env->ExceptionClear(); }
                setError("jni", "getRuntime failed");
            } else {
                jclass stringClass = env->FindClass("java/lang/String");
                jstring all = env->NewStringUTF("L");
                jobjectArray prefixes = env->NewObjectArray(1, stringClass, all);
                env->CallVoidMethod(runtime, setExemptions, prefixes);
                if (env->ExceptionCheck()) {
                    // capture the real exception for telemetry
                    jthrowable ex = env->ExceptionOccurred();
                    env->ExceptionClear();
                    jclass exClass = env->GetObjectClass(ex);
                    jclass classClass = env->FindClass("java/lang/Class");
                    jmethodID getName = env->GetMethodID(classClass, "getName", "()Ljava/lang/String;");
                    auto exName = (jstring) env->CallObjectMethod(exClass, getName);
                    jclass throwableClass = env->FindClass("java/lang/Throwable");
                    jmethodID getMessage = env->GetMethodID(throwableClass, "getMessage", "()Ljava/lang/String;");
                    auto exMsg = (jstring) env->CallObjectMethod(ex, getMessage);
                    char buf[256];
                    const char* nameStr = exName ? env->GetStringUTFChars(exName, nullptr) : "?";
                    const char* msgStr = exMsg ? env->GetStringUTFChars(exMsg, nullptr) : "(null)";
                    snprintf(buf, sizeof(buf), "setExemptions threw %s(%s)", nameStr, msgStr);
                    setError("jni", buf);
                    if (exName) env->ReleaseStringUTFChars(exName, nameStr);
                    if (exMsg) env->ReleaseStringUTFChars(exMsg, msgStr);
                } else {
                    LOGI("level 1 ok: exemptions via JNI");
                    return JNI_TRUE;
                }
            }
        }
    }

    // ---- Level 2: direct enforcement-policy flip inside libart.so ----
    void* art = dlopen("libart.so", RTLD_NOW);
    if (art == nullptr) {
        setError("dlsym", "dlopen libart.so failed");
        return JNI_FALSE;
    }

    // art::Runtime::instance_  (stable exported data symbol since API 26)
    void* instancePtr = dlsym(art, "_ZN3art7Runtime9instance_E");
    if (instancePtr == nullptr) {
        setError("dlsym", "Runtime::instance_ not found");
        dlclose(art);
        return JNI_FALSE;
    }
    void* runtime = *reinterpret_cast<void**>(instancePtr);
    if (runtime == nullptr) {
        setError("dlsym", "Runtime::instance_ is null");
        dlclose(art);
        return JNI_FALSE;
    }

    // Candidate mangled names for SetHiddenApiEnforcementPolicy across
    // AOSP versions/OEM rebuilds. Policy enum: 0 = kDisabled.
    static const char* kPolicySymbols[] = {
            "_ZN3art7Runtime27SetHiddenApiEnforcementPolicyENS_9hiddenapi16EnforcementPolicyE",
            "_ZN3art7Runtime27SetHiddenApiEnforcementPolicyENS0_23HiddenApiEnforcementPolicyE",
            "_ZN3art7Runtime27SetHiddenApiEnforcementPolicyENS_16HiddenApiPolicyE",
            "_ZN3art7Runtime23SetHiddenApiEnforcementPolicyENS_9hiddenapi16EnforcementPolicyE",
            "_ZN3art7Runtime30SetHiddenApiEnforcementPolicyENS_9hiddenapi16EnforcementPolicyE",
    };
    using SetPolicyFn = void (*)(void* runtime, int policy);
    SetPolicyFn setPolicy = nullptr;
    for (const char* sym : kPolicySymbols) {
        void* p = dlsym(art, sym);
        if (p != nullptr) {
            setPolicy = reinterpret_cast<SetPolicyFn>(p);
            LOGI("policy symbol resolved: %s", sym);
            break;
        }
    }
    if (setPolicy == nullptr) {
        setError("dlsym", "SetHiddenApiEnforcementPolicy symbol not found");
        dlclose(art);
        return JNI_FALSE;
    }

    setPolicy(runtime, 0 /* kDisabled */);
    LOGI("level 2 ok: enforcement policy disabled via libart");
    dlclose(art);
    return JNI_TRUE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_multi2_app_engine_HiddenApiBypass_nativeGetLastError(
        JNIEnv* env, jclass /* clazz */) {
    return env->NewStringUTF(g_lastError);
}
