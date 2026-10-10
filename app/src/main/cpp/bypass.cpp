#include <jni.h>
#include <android/log.h>
#include <dlfcn.h>
#include <cstdio>
#include <cstring>

#define LOG_TAG "Multi2Bypass"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

static char g_lastError[512] = "none";
static char g_level1Error[256] = "none";
static char g_level2Error[256] = "none";

static void composeError() {
    snprintf(g_lastError, sizeof(g_lastError), "jni[%s] dlsym[%s]", g_level1Error, g_level2Error);
}

/** libart.so moved into the ART APEX on Android 12+; plain names don't resolve. */
static void* openArtLibrary() {
    static const char* kPaths[] = {
            "/apex/com.android.art/lib64/libart.so",
            "/apex/com.android.art/lib/libart.so",
            "/system/lib64/libart.so",
            "/system/lib/libart.so",
            "libart.so",
            "libartd.so",
            nullptr,
    };
    for (int i = 0; kPaths[i] != nullptr; i++) {
        void* h = dlopen(kPaths[i], RTLD_NOW);
        if (h != nullptr) {
            LOGI("libart opened: %s", kPaths[i]);
            return h;
        }
    }
    // last resort: libart is already mapped into every app process —
    // recover its absolute path from /proc/self/maps and dlopen that.
    FILE* f = fopen("/proc/self/maps", "r");
    if (f != nullptr) {
        char line[512];
        while (fgets(line, sizeof(line), f) != nullptr) {
            if (strstr(line, "libart.so") == nullptr) continue;
            const char* slash = strrchr(line, '/');
            if (slash == nullptr) continue;
            char path[256];
            snprintf(path, sizeof(path), "%s", slash);
            path[strcspn(path, "\r\n")] = 0;
            void* h = dlopen(path, RTLD_NOW);
            if (h != nullptr) {
                LOGI("libart opened via maps: %s", path);
                fclose(f);
                return h;
            }
        }
        fclose(f);
    }
    return nullptr;
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
        snprintf(g_level1Error, sizeof(g_level1Error), "VMRuntime class not found");
    } else {
        jmethodID getRuntime = env->GetStaticMethodID(
                vmRuntimeClass, "getRuntime", "()Ldalvik/system/VMRuntime;");
        jmethodID setExemptions = env->GetMethodID(
                vmRuntimeClass, "setHiddenApiExemptions", "([Ljava/lang/String;)V");
        if (getRuntime == nullptr || setExemptions == nullptr) {
            env->ExceptionClear();
            snprintf(g_level1Error, sizeof(g_level1Error), "VMRuntime methods not found");
        } else {
            jobject runtime = env->CallStaticObjectMethod(vmRuntimeClass, getRuntime);
            if (env->ExceptionCheck() || runtime == nullptr) {
                if (env->ExceptionCheck()) { env->ExceptionDescribe(); env->ExceptionClear(); }
                snprintf(g_level1Error, sizeof(g_level1Error), "getRuntime failed");
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
                    const char* nameStr = exName ? env->GetStringUTFChars(exName, nullptr) : "?";
                    const char* msgStr = exMsg ? env->GetStringUTFChars(exMsg, nullptr) : "(null)";
                    snprintf(g_level1Error, sizeof(g_level1Error), "threw %s(%s)", nameStr, msgStr);
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
    void* art = openArtLibrary();
    if (art == nullptr) {
        snprintf(g_level2Error, sizeof(g_level2Error), "all libart paths failed");
        composeError();
        return JNI_FALSE;
    }

    // art::Runtime::instance_  (stable exported data symbol since API 26)
    void* instancePtr = dlsym(art, "_ZN3art7Runtime9instance_E");
    if (instancePtr == nullptr) {
        snprintf(g_level2Error, sizeof(g_level2Error), "Runtime::instance_ not found");
        composeError();
        dlclose(art);
        return JNI_FALSE;
    }
    void* runtime = *reinterpret_cast<void**>(instancePtr);
    if (runtime == nullptr) {
        snprintf(g_level2Error, sizeof(g_level2Error), "Runtime::instance_ is null");
        composeError();
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
        snprintf(g_level2Error, sizeof(g_level2Error), "SetHiddenApiEnforcementPolicy symbol not found");
        composeError();
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
