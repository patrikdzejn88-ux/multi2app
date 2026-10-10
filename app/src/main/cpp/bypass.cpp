#include <jni.h>
#include <android/log.h>

#define LOG_TAG "Multi2Bypass"

/**
 * Native hidden-API bypass — same family as the original's libma2023.so.
 *
 * Java-level hidden API enforcement (member filtering + InvocationTarget-
 * Exception hardening on Android 15) does not apply to JNI: GetMethodID /
 * CallVoidMethod from native code see and invoke hidden members freely.
 *
 * Order of attempts (all from native, all unrestricted):
 *  1. VMRuntime.setHiddenApiExemptions({"L"}) — whitelists every hidden API
 *     for the whole process; Java reflection then works normally.
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_com_multi2_app_engine_HiddenApiBypass_nativeApplyHiddenApiBypass(
        JNIEnv* env, jclass /* clazz */) {

    jclass vmRuntimeClass = env->FindClass("dalvik/system/VMRuntime");
    if (vmRuntimeClass == nullptr) {
        env->ExceptionClear();
        __android_log_write(ANDROID_LOG_WARN, LOG_TAG, "VMRuntime class not found");
        return JNI_FALSE;
    }

    jmethodID getRuntime = env->GetStaticMethodID(
            vmRuntimeClass, "getRuntime", "()Ldalvik/system/VMRuntime;");
    jmethodID setExemptions = env->GetMethodID(
            vmRuntimeClass, "setHiddenApiExemptions", "([Ljava/lang/String;)V");
    if (getRuntime == nullptr || setExemptions == nullptr) {
        env->ExceptionClear();
        __android_log_write(ANDROID_LOG_WARN, LOG_TAG, "VMRuntime methods not found");
        return JNI_FALSE;
    }

    jobject runtime = env->CallStaticObjectMethod(vmRuntimeClass, getRuntime);
    if (env->ExceptionCheck() || runtime == nullptr) {
        env->ExceptionClear();
        __android_log_write(ANDROID_LOG_WARN, LOG_TAG, "getRuntime failed");
        return JNI_FALSE;
    }

    jclass stringClass = env->FindClass("java/lang/String");
    jstring all = env->NewStringUTF("L");
    jobjectArray prefixes = env->NewObjectArray(1, stringClass, all);

    env->CallVoidMethod(runtime, setExemptions, prefixes);
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
        __android_log_write(ANDROID_LOG_WARN, LOG_TAG, "setHiddenApiExemptions threw");
        return JNI_FALSE;
    }

    __android_log_write(ANDROID_LOG_INFO, LOG_TAG, "hidden api exemptions applied (native)");
    return JNI_TRUE;
}
