#ifndef IL2FUSION_PLUGINS_DUMP_H
#define IL2FUSION_PLUGINS_DUMP_H

#include <jni.h>
#include <string>

namespace dump_plugin {

// 设置当前进程名，便于日志和产物命名。
void SetProcess(const std::string& process_name);

// 保存 Java 回调引用，用于完成 Toast 通知。
void SetJavaCallbacks(JavaVM* vm, jclass bridge_class, jmethodID on_dump_finished);

// 触发 Dump 流程，传入 dataDir（/data/data/<pkg>）。
void StartDump(const std::string& data_dir);

}  // namespace dump_plugin

#endif  // IL2FUSION_PLUGINS_DUMP_H
