//
// Created by Perfare on 2020/7/4.
//

#ifndef ZYGISK_IL2CPPDUMPER_HACK_H
#define ZYGISK_IL2CPPDUMPER_HACK_H

#include <cstddef>

// data/length 参数保留兼容签名，当前实现不再使用跨 ABI arm so 注入。
void hack_prepare(const char *game_data_dir, void *data, size_t length);

#endif //ZYGISK_IL2CPPDUMPER_HACK_H
