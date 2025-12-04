#ifndef IL2FUSION_PLUGINS_TEXT_EXTRACTOR_H
#define IL2FUSION_PLUGINS_TEXT_EXTRACTOR_H

#include <cstdint>
#include <string>
#include <vector>

namespace text_extractor {

// 设置进程名并异步启动文本拦截初始化（幂等）。
void Init(const std::string& process_name);

// 更新 RVA 列表；若 libil2cpp 已就绪会立即重新安装 hook。
void UpdateRvas(const std::vector<uintptr_t>& new_rvas);

}  // namespace text_extractor

#endif  // IL2FUSION_PLUGINS_TEXT_EXTRACTOR_H
