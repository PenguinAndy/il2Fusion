#include <jni.h>
#include <android/log.h>
#include <atomic>

#include "dobby.h"

#define LOG_TAG "TextExtractTool"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {
std::atomic_bool g_initialized{false};

void install_hooks() {
    // TODO: Add Dobby hooks to intercept Unity text APIs and forward extracted data.
    LOGI("install_hooks: Dobby available, hooks not installed yet");
}
}

extern "C" JNIEXPORT void JNICALL
Java_com_tools_module_NativeBridge_init(JNIEnv *env, jclass /*clazz*/) {
    if (g_initialized.exchange(true)) {
        LOGI("Native init already completed");
        return;
    }

    LOGI("Native init start");

    install_hooks();

    // Keeping env visible for future use
    (void) env;
}

jint JNI_OnLoad(JavaVM *vm, void *) {
    LOGI("JNI_OnLoad");
    (void) vm;
    return JNI_VERSION_1_6;
}
