#ifndef IL2FUSION_DB_H
#define IL2FUSION_DB_H

#include <string>

namespace textdb {

// 初始化数据库，若不存在则创建；log_path 为 true 时打印路径/存在状态。
void Init(const std::string& process_name, bool log_path);

// 将文本写入数据库，已存在则跳过。
void InsertIfNeeded(const std::string& text);

}  // namespace textdb

#endif  // IL2FUSION_DB_H
