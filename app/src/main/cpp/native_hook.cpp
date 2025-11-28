#include <android/log.h>
#include <jni.h>

#include <atomic>
#include <chrono>
#include <cinttypes>
#include <cstdio>
#include <cstring>
#include <dlfcn.h>
#include <mutex>
#include <string>
#include <thread>
#include <unordered_map>
#include <vector>

#include "dobby.h"

#define LOG_TAG "TextExtractTool"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {

constexpr const char* kLibIl2cpp = "libil2cpp.so";
constexpr const char* kTargetText = "Hook Test";

struct Il2CppString {
    void* klass;
    void* monitor;
    int32_t length;
    char16_t chars[1];
};

using Il2CppStringNewFn = Il2CppString* (*)(const char*);

struct HookRegistry {
    std::unordered_map<void*, uintptr_t> targets;  // address -> rva
};

std::atomic_bool g_initialized{false};
std::mutex g_hookMutex;
std::vector<uintptr_t> g_rvas;
std::vector<void*> g_installedTargets;
std::atomic<HookRegistry*> g_registry{nullptr};
uintptr_t g_il2cpp_base = 0;
Il2CppString* g_managedText = nullptr;
Il2CppStringNewFn g_stringNew = nullptr;

uintptr_t find_module_base(const char* name) {
    FILE* fp = fopen("/proc/self/maps", "r");
    if (!fp) {
        return 0;
    }

    char line[512];
    while (fgets(line, sizeof(line), fp)) {
        if (strstr(line, name) == nullptr) {
            continue;
        }

        uintptr_t start = 0;
        if (sscanf(line, "%" SCNxPTR "-%*lx", &start) == 1) {
            fclose(fp);
            return start;
        }
    }

    fclose(fp);
    return 0;
}

uintptr_t wait_for_module(const char* name, std::chrono::milliseconds timeout) {
    const auto deadline = std::chrono::steady_clock::now() + timeout;
    while (std::chrono::steady_clock::now() < deadline) {
        const auto base = find_module_base(name);
        if (base != 0) {
            return base;
        }
        std::this_thread::sleep_for(std::chrono::milliseconds(100));
    }
    return 0;
}

std::string narrow_from_utf16(const char16_t* data, int32_t len) {
    std::string out;
    out.reserve(len);
    for (int32_t i = 0; i < len; ++i) {
        const char16_t c = data[i];
        if (c < 0x80) {
            out.push_back(static_cast<char>(c));
        } else {
            out.push_back('?');
        }
    }
    return out;
}

std::string describe_il2cpp_string(Il2CppString* str) {
    if (str == nullptr) {
        return "<null>";
    }

    const int32_t len = str->length;
    if (len <= 0 || len > 0x1000) {
        char buf[64];
        snprintf(buf, sizeof(buf), "<length=%d>", len);
        return std::string(buf);
    }

    return narrow_from_utf16(str->chars, len);
}

void* get_second_arg(DobbyRegisterContext* ctx) {
#if defined(__aarch64__)
    return reinterpret_cast<void*>(ctx->general.regs.x1);
#elif defined(__arm__)
    return reinterpret_cast<void*>(ctx->general.regs.r1);
#elif defined(__x86_64__)
    return reinterpret_cast<void*>(ctx->general.regs.rsi);
#elif defined(__i386__)
    return nullptr;  // extend if needed
#else
    return nullptr;
#endif
}

void set_second_arg(DobbyRegisterContext* ctx, void* value) {
#if defined(__aarch64__)
    ctx->general.regs.x1 = reinterpret_cast<uint64_t>(value);
#elif defined(__arm__)
    ctx->general.regs.r1 = reinterpret_cast<uint32_t>(value);
#elif defined(__x86_64__)
    ctx->general.regs.rsi = reinterpret_cast<uint64_t>(value);
#elif defined(__i386__)
    (void) value;  // extend if needed
#else
    (void) value;
#endif
}

void setter_pre_handler(void* address, DobbyRegisterContext* ctx) {
    HookRegistry* registry = g_registry.load();
    uintptr_t rva = 0;
    if (registry) {
        auto it = registry->targets.find(address);
        if (it != registry->targets.end()) {
            rva = it->second;
        }
    }

    if (g_managedText == nullptr) {
        return;
    }

    void* arg = get_second_arg(ctx);
    const auto original = describe_il2cpp_string(reinterpret_cast<Il2CppString*>(arg));

    if (!original.empty() && original != kTargetText) {
        LOGI("[Setter] RVA 0x%" PRIxPTR " 原始内容: %s", rva, original.c_str());
    }

    set_second_arg(ctx, g_managedText);
}

bool prepare_il2cpp_factory() {
    void* handle = dlopen(kLibIl2cpp, RTLD_NOW | RTLD_NOLOAD);
    if (!handle) {
        handle = dlopen(nullptr, RTLD_NOW);
    }
    if (!handle) {
        LOGE("dlopen failed when resolving il2cpp_string_new");
        return false;
    }

    g_stringNew = reinterpret_cast<Il2CppStringNewFn>(
            dlsym(handle, "il2cpp_string_new"));

    if (!g_stringNew) {
        LOGE("无法找到 il2cpp_string_new");
        return false;
    }

    g_managedText = g_stringNew(kTargetText);
    if (!g_managedText) {
        LOGE("创建托管字符串失败");
        return false;
    }

    return true;
}

void clear_hooks_locked() {
    for (void* target : g_installedTargets) {
        const int ret = DobbyDestroy(target);
        if (ret != 0) {
            LOGE("销毁 hook @%p 失败, ret=%d", target, ret);
        }
    }
    g_installedTargets.clear();

    HookRegistry* old = g_registry.exchange(nullptr);
    delete old;
}

void install_hooks_locked() {
#if !defined(__arm__) && !defined(__aarch64__) && !defined(__x86_64__) && !defined(__i386__)
    LOGE("当前架构不支持文本拦截");
    return;
#endif

    if (g_il2cpp_base == 0 || g_managedText == nullptr) {
        LOGE("il2cpp 未准备好，跳过安装 hook");
        return;
    }

    if (g_rvas.empty()) {
        LOGI("未配置任何 RVA，跳过安装 hook");
        return;
    }

    clear_hooks_locked();

    HookRegistry* registry = new HookRegistry();

    for (uintptr_t rva : g_rvas) {
        void* target = reinterpret_cast<void*>(g_il2cpp_base + rva);
        const int ret = DobbyInstrument(target, setter_pre_handler);
        if (ret == 0) {
            g_installedTargets.push_back(target);
            registry->targets[target] = rva;
            LOGI("Hooked RVA 0x%" PRIxPTR " @ %p", rva, target);
        } else {
            LOGE("Hook RVA 0x%" PRIxPTR " 失败, ret=%d", rva, ret);
        }
    }

    HookRegistry* old = g_registry.exchange(registry);
    delete old;
}

void update_rvas(const std::vector<uintptr_t>& newRvas) {
    std::lock_guard<std::mutex> _lk(g_hookMutex);
    g_rvas = newRvas;
    LOGI("更新 RVA 列表，共 %zu 个", g_rvas.size());

    if (g_il2cpp_base != 0 && g_managedText != nullptr) {
        install_hooks_locked();
    }
}

void init_worker() {
    g_il2cpp_base = wait_for_module(kLibIl2cpp, std::chrono::seconds(10));
    if (g_il2cpp_base == 0) {
        LOGE("等待 %s 载入超时", kLibIl2cpp);
        return;
    }

    LOGI("%s loaded @ 0x%" PRIxPTR, kLibIl2cpp, g_il2cpp_base);

    if (!prepare_il2cpp_factory()) {
        return;
    }

    std::lock_guard<std::mutex> _lk(g_hookMutex);
    install_hooks_locked();
}

}  // namespace

extern "C" JNIEXPORT void JNICALL
Java_com_tools_module_NativeBridge_init(JNIEnv* env, jclass /*clazz*/) {
    if (g_initialized.exchange(true)) {
        LOGI("Native init already completed");
        return;
    }

    LOGI("Native init start");
    std::thread(init_worker).detach();

    (void) env;
}

extern "C" JNIEXPORT void JNICALL
Java_com_tools_module_NativeBridge_updateHookTargets(JNIEnv* env, jclass /*clazz*/, jlongArray rvas) {
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

    update_rvas(values);
}

jint JNI_OnLoad(JavaVM* vm, void*) {
    LOGI("JNI_OnLoad");
    (void) vm;
    return JNI_VERSION_1_6;
}
