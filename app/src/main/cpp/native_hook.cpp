#include <jni.h>

#include <atomic>
#include <cstdint>
#include <string>
#include <vector>

#include "plugins/dump/dump.h"
#include "plugins/textExtractor/textExtractor.h"
#include "utils/log.h"

namespace {
std::atomic_bool g_initialized{false};
std::string g_process_name = "unknown";
}  // namespace

extern "C" JNIEXPORT void JNICALL
Java_com_tools_module_NativeBridge_init(JNIEnv* env, jclass /*clazz*/, jstring processName) {
    if (processName != nullptr) {
        const char* utf = env->GetStringUTFChars(processName, nullptr);
        if (utf != nullptr) {
            g_process_name.assign(utf);
            env->ReleaseStringUTFChars(processName, utf);
        }
    }

    dump_plugin::SetProcess(g_process_name);
    text_extractor::Init(g_process_name);

    if (g_initialized.exchange(true)) {
        LOGI("[%s] Native init already completed", g_process_name.c_str());
        return;
    }

    LOGI("[%s] Native init start", g_process_name.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_tools_module_NativeBridge_setConfig(JNIEnv* env, jclass /*clazz*/, jlongArray rvas) {
    if (rvas == nullptr) {
        return;
    }

    const jsize len = env->GetArrayLength(rvas);
    std::vector<uintptr_t> values;
    values.reserve(static_cast<size_t>(len));

    jlong* elems = env->GetLongArrayElements(rvas, nullptr);
    for (jsize i = 0; i < len; ++i) {
        values.push_back(static_cast<uintptr_t>(elems[i]));
    }
    env->ReleaseLongArrayElements(rvas, elems, JNI_ABORT);

    text_extractor::UpdateRvas(values);
}

extern "C" JNIEXPORT void JNICALL
Java_com_tools_module_NativeBridge_startDump(JNIEnv* env, jclass /*clazz*/, jstring dataDir) {
    const char* path = dataDir != nullptr ? env->GetStringUTFChars(dataDir, nullptr) : nullptr;
    std::string dir;
    if (path != nullptr) {
        dir.assign(path);
        env->ReleaseStringUTFChars(dataDir, path);
    }
    dump_plugin::StartDump(dir);
}

jint JNI_OnLoad(JavaVM* vm, void*) {
    LOGI("JNI_OnLoad");
    JNIEnv* env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_VERSION_1_6;
    }
    jclass cls = env->FindClass("com/tools/module/NativeBridge");
    if (cls != nullptr) {
        jclass global_cls = static_cast<jclass>(env->NewGlobalRef(cls));
        env->DeleteLocalRef(cls);
        jmethodID on_dump_finished = env->GetStaticMethodID(
                global_cls,
                "onDumpFinished",
                "(ZLjava/lang/String;)V");
        dump_plugin::SetJavaCallbacks(vm, global_cls, on_dump_finished);
    }
    return JNI_VERSION_1_6;
}
