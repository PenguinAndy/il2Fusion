#ifndef IL2FUSION_UTILS_H
#define IL2FUSION_UTILS_H

#include <chrono>
#include <cstdint>
#include <string>

#include "dobby.h"

namespace textutils {

struct Il2CppString {
    void* klass;
    void* monitor;
    int32_t length;
    char16_t chars[1];
};

bool ShouldFilter(const std::string& text);
std::string Utf16ToUtf8(const char16_t* data, int32_t len);
std::string DescribeIl2CppString(const Il2CppString* str);
}

namespace hookutils {
uintptr_t FindModuleBase(const char* name);
uintptr_t WaitForModule(const char* name, std::chrono::milliseconds timeout);
std::string FindModulePath(const char* name);
uintptr_t FindExportInElf(const char* path, const char* symbol, uintptr_t base);
void* GetSecondArg(DobbyRegisterContext* ctx);
void SetSecondArg(DobbyRegisterContext* ctx, void* value);
}

#endif  // IL2FUSION_UTILS_H
