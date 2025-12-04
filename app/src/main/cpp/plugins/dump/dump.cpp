#include "dump.h"

#include <atomic>
#include <chrono>
#include <cinttypes>
#include <cstdlib>
#include <string>
#include <thread>

#include "../../il2CppDumper/hack.h"
#include "../../utils/log.h"
#include "../../utils/utils.h"

namespace dump_plugin {
namespace {

constexpr const char* kLibIl2cpp = "libil2cpp.so";

JavaVM* g_vm = nullptr;
jclass g_bridge_class = nullptr;
jmethodID g_on_dump_finished = nullptr;

std::atomic_bool g_dump_enabled{false};
std::atomic_bool g_dump_started{false};
std::string g_dump_dir;
std::string g_process_name = "unknown";

std::string GetBasePackage(const std::string& process_name) {
    if (process_name.empty()) return "unknown";
    const auto pos = process_name.find(':');
    if (pos != std::string::npos) {
        return process_name.substr(0, pos);
    }
    return process_name;
}

void NotifyDumpResult(bool success, const char* msg) {
    if (g_vm == nullptr || g_bridge_class == nullptr || g_on_dump_finished == nullptr) {
        return;
    }

    JNIEnv* env = nullptr;
    bool attached = false;
    if (g_vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        if (g_vm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
            return;
        }
        attached = true;
    }

    jstring jmsg = env->NewStringUTF(msg ? msg : "");
    env->CallStaticVoidMethod(g_bridge_class, g_on_dump_finished,
                              success ? JNI_TRUE : JNI_FALSE, jmsg);
    env->DeleteLocalRef(jmsg);

    if (attached) {
        g_vm->DetachCurrentThread();
    }
}

bool CopyDumpToSdcard(const std::string& pkg, std::string& out_path) {
    const std::string src = g_dump_dir + "/files/dump.cs";
    out_path = "/sdcard/Download/" + pkg + ".cs";
    const std::string cmd = "su -c 'cp " + src + " " + out_path + "'";
    const int ret = system(cmd.c_str());
    if (ret == 0) {
        LOGI("copied dump to %s", out_path.c_str());
        return true;
    }
    LOGE("copy dump failed ret=%d cmd=%s", ret, cmd.c_str());
    return false;
}

void TriggerDumpAsync() {
    if (!g_dump_enabled.load()) {
        return;
    }
    if (g_dump_started.exchange(true)) {
        return;
    }
    if (g_dump_dir.empty()) {
        LOGE("trigger_dump skipped: dump dir empty");
        g_dump_started.store(false);
        return;
    }

    std::thread([] {
        const uintptr_t base = hookutils::WaitForModule(kLibIl2cpp, std::chrono::seconds(30));
        if (base == 0) {
            LOGE("dump wait libil2cpp timeout");
            g_dump_started.store(false);
            NotifyDumpResult(false, "等待 libil2cpp 超时");
            return;
        }
        LOGI("dump begin, libil2cpp @ 0x%" PRIxPTR, base);
        const bool ok = hack_prepare(g_dump_dir.c_str(), nullptr, 0);
        g_dump_started.store(false);
        if (ok) {
            std::string dst;
            const std::string pkg = GetBasePackage(g_process_name);
            const bool copy_ok = CopyDumpToSdcard(pkg, dst);
            if (copy_ok) {
                std::string msg = "Dump 完成，已复制到 " + dst;
                NotifyDumpResult(true, msg.c_str());
            } else {
                NotifyDumpResult(true, "Dump 完成，复制到 Download 失败");
            }
        } else {
            NotifyDumpResult(false, "Dump 失败，未找到 libil2cpp");
        }
    }).detach();
}

}  // namespace

void SetProcess(const std::string& process_name) {
    g_process_name = process_name.empty() ? "unknown" : process_name;
}

void SetJavaCallbacks(JavaVM* vm, jclass bridge_class, jmethodID on_dump_finished) {
    g_vm = vm;
    g_bridge_class = bridge_class;
    g_on_dump_finished = on_dump_finished;
}

void StartDump(const std::string& data_dir) {
    if (data_dir.empty()) {
        LOGE("startDump skipped: dataDir is empty");
        g_dump_dir.clear();
        g_dump_enabled.store(false);
        return;
    }
    g_dump_dir = data_dir;
    g_dump_enabled.store(true);
    g_dump_started.store(false);
    TriggerDumpAsync();
}

}  // namespace dump_plugin
