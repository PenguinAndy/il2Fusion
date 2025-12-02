#include "utils.h"

#include <cctype>
#include <cinttypes>
#include <cstdio>
#include <cstring>
#include <dlfcn.h>
#include <elf.h>
#include <thread>
#include <vector>

#include "dobby.h"

namespace textutils {

bool ShouldFilter(const std::string& text) {
    if (text.empty()) {
        return true;
    }

    bool hasDigit = false;
    bool hasLetter = false;
    bool hasPunct = false;
    bool hasSpace = false;

    for (unsigned char ch : text) {
        if (std::isdigit(ch)) {
            hasDigit = true;
        } else if (std::isalpha(ch)) {
            hasLetter = true;
        } else if (std::ispunct(ch)) {
            hasPunct = true;
        } else if (std::isspace(ch)) {
            hasSpace = true;
        } else {
            // 包含非 ASCII 可见字符，不过滤
            return false;
        }
    }

    // 只包含数字/英文/标点/空格的组合（含任意排列）均过滤
    return hasDigit || hasLetter || hasPunct || hasSpace;
}

namespace {

void AppendUtf8(std::string& out, uint32_t cp) {
    if (cp <= 0x7F) {
        out.push_back(static_cast<char>(cp));
    } else if (cp <= 0x7FF) {
        out.push_back(static_cast<char>(0xC0 | ((cp >> 6) & 0x1F)));
        out.push_back(static_cast<char>(0x80 | (cp & 0x3F)));
    } else if (cp <= 0xFFFF) {
        out.push_back(static_cast<char>(0xE0 | ((cp >> 12) & 0x0F)));
        out.push_back(static_cast<char>(0x80 | ((cp >> 6) & 0x3F)));
        out.push_back(static_cast<char>(0x80 | (cp & 0x3F)));
    } else if (cp <= 0x10FFFF) {
        out.push_back(static_cast<char>(0xF0 | ((cp >> 18) & 0x07)));
        out.push_back(static_cast<char>(0x80 | ((cp >> 12) & 0x3F)));
        out.push_back(static_cast<char>(0x80 | ((cp >> 6) & 0x3F)));
        out.push_back(static_cast<char>(0x80 | (cp & 0x3F)));
    } else {
        out.push_back('?');
    }
}

}  // namespace

std::string Utf16ToUtf8(const char16_t* data, int32_t len) {
    std::string out;
    out.reserve(static_cast<size_t>(len) * 3);

    for (int32_t i = 0; i < len; ++i) {
        uint32_t cp = data[i];

        // Handle surrogate pairs
        if (cp >= 0xD800 && cp <= 0xDBFF && (i + 1) < len) {
            const uint32_t low = data[i + 1];
            if (low >= 0xDC00 && low <= 0xDFFF) {
                cp = 0x10000 + (((cp - 0xD800) << 10) | (low - 0xDC00));
                ++i;
            } else {
                cp = '?';
            }
        } else if (cp >= 0xDC00 && cp <= 0xDFFF) {
            cp = '?';
        }

        AppendUtf8(out, cp);
    }

    return out;
}

std::string DescribeIl2CppString(const Il2CppString* str) {
    if (str == nullptr) {
        return "<null>";
    }

    const int32_t len = str->length;
    if (len <= 0 || len > 0x1000) {
        char buf[64];
        snprintf(buf, sizeof(buf), "<length=%d>", len);
        return std::string(buf);
    }

    return Utf16ToUtf8(str->chars, len);
}

}  // namespace textutils

namespace hookutils {

uintptr_t FindModuleBase(const char* name) {
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

uintptr_t WaitForModule(const char* name, std::chrono::milliseconds timeout) {
    const auto deadline = std::chrono::steady_clock::now() + timeout;
    while (std::chrono::steady_clock::now() < deadline) {
        const auto base = FindModuleBase(name);
        if (base != 0) {
            return base;
        }
        std::this_thread::sleep_for(std::chrono::milliseconds(100));
    }
    return 0;
}

std::string FindModulePath(const char* name) {
    FILE* fp = fopen("/proc/self/maps", "r");
    if (!fp) {
        return {};
    }

    char line[512];
    while (fgets(line, sizeof(line), fp)) {
        if (strstr(line, name) == nullptr) {
            continue;
        }
        char* path = strchr(line, '/');
        if (path != nullptr) {
            const size_t len = strlen(path);
            if (len > 0 && path[len - 1] == '\n') {
                path[len - 1] = '\0';
            }
            fclose(fp);
            return std::string(path);
        }
    }

    fclose(fp);
    return {};
}

uintptr_t FindExportInElf(const char* path, const char* symbol, uintptr_t base) {
    if (path == nullptr || symbol == nullptr || base == 0) {
        return 0;
    }

    FILE* fp = fopen(path, "rb");
    if (!fp) {
        return 0;
    }

    Elf64_Ehdr ehdr64{};
    if (fread(&ehdr64, 1, sizeof(ehdr64), fp) != sizeof(ehdr64)) {
        fclose(fp);
        return 0;
    }

    if (memcmp(ehdr64.e_ident, ELFMAG, SELFMAG) != 0) {
        fclose(fp);
        return 0;
    }

    const bool is64 = (ehdr64.e_ident[EI_CLASS] == ELFCLASS64);
    fseek(fp, 0, SEEK_SET);  // rewind for reuse

    if (is64) {
        if (ehdr64.e_shentsize != sizeof(Elf64_Shdr)) {
            fclose(fp);
            return 0;
        }
        std::vector<Elf64_Shdr> shdrs(ehdr64.e_shnum);
        fseek(fp, static_cast<long>(ehdr64.e_shoff), SEEK_SET);
        if (fread(shdrs.data(), sizeof(Elf64_Shdr), shdrs.size(), fp) != shdrs.size()) {
            fclose(fp);
            return 0;
        }

        for (const auto& shdr : shdrs) {
            if (shdr.sh_type != SHT_DYNSYM || shdr.sh_entsize != sizeof(Elf64_Sym)) {
                continue;
            }
            if (shdr.sh_link >= shdrs.size()) {
                continue;
            }
            const Elf64_Shdr& strhdr = shdrs[shdr.sh_link];
            std::vector<char> strtab(strhdr.sh_size);
            fseek(fp, static_cast<long>(strhdr.sh_offset), SEEK_SET);
            if (fread(strtab.data(), 1, strtab.size(), fp) != strtab.size()) {
                continue;
            }

            const size_t count = shdr.sh_size / sizeof(Elf64_Sym);
            std::vector<Elf64_Sym> syms(count);
            fseek(fp, static_cast<long>(shdr.sh_offset), SEEK_SET);
            if (fread(syms.data(), sizeof(Elf64_Sym), syms.size(), fp) != syms.size()) {
                continue;
            }

            for (const auto& sym : syms) {
                if (sym.st_name >= strtab.size()) {
                    continue;
                }
                const char* name = strtab.data() + sym.st_name;
                if (strcmp(name, symbol) == 0 && sym.st_value != 0) {
                    fclose(fp);
                    return base + static_cast<uintptr_t>(sym.st_value);
                }
            }
        }
    } else {
        Elf32_Ehdr ehdr32{};
        if (fread(&ehdr32, 1, sizeof(ehdr32), fp) != sizeof(ehdr32)) {
            fclose(fp);
            return 0;
        }
        if (ehdr32.e_shentsize != sizeof(Elf32_Shdr)) {
            fclose(fp);
            return 0;
        }
        std::vector<Elf32_Shdr> shdrs(ehdr32.e_shnum);
        fseek(fp, static_cast<long>(ehdr32.e_shoff), SEEK_SET);
        if (fread(shdrs.data(), sizeof(Elf32_Shdr), shdrs.size(), fp) != shdrs.size()) {
            fclose(fp);
            return 0;
        }

        for (const auto& shdr : shdrs) {
            if (shdr.sh_type != SHT_DYNSYM || shdr.sh_entsize != sizeof(Elf32_Sym)) {
                continue;
            }
            if (shdr.sh_link >= shdrs.size()) {
                continue;
            }
            const Elf32_Shdr& strhdr = shdrs[shdr.sh_link];
            std::vector<char> strtab(strhdr.sh_size);
            fseek(fp, static_cast<long>(strhdr.sh_offset), SEEK_SET);
            if (fread(strtab.data(), 1, strtab.size(), fp) != strtab.size()) {
                continue;
            }

            const size_t count = shdr.sh_size / sizeof(Elf32_Sym);
            std::vector<Elf32_Sym> syms(count);
            fseek(fp, static_cast<long>(shdr.sh_offset), SEEK_SET);
            if (fread(syms.data(), sizeof(Elf32_Sym), syms.size(), fp) != syms.size()) {
                continue;
            }

            for (const auto& sym : syms) {
                if (sym.st_name >= strtab.size()) {
                    continue;
                }
                const char* name = strtab.data() + sym.st_name;
                if (strcmp(name, symbol) == 0 && sym.st_value != 0) {
                    fclose(fp);
                    return base + static_cast<uintptr_t>(sym.st_value);
                }
            }
        }
    }

    fclose(fp);
    return 0;
}

void* GetSecondArg(DobbyRegisterContext* ctx) {
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

void SetSecondArg(DobbyRegisterContext* ctx, void* value) {
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

}  // namespace hookutils
