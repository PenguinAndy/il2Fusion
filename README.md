il2Fusion
================

<p align="center">
  <img src="doc/imgs/il2FusionIcon.png" alt="il2Fusion icon" width="233" />
</p>

LSPosed（Java 层 hook）+ Dobby（Native 层 hook）+ ContentProvider 跨进程通信的架构，围绕 Il2Cpp Dump/工具链的开源项目。

English version: see [README_EN](https://github.com/PenguinAndy/il2Fusion/blob/master/doc/README_EN.md).

<p align="center">
  <img src="doc/imgs/Screenshot_01.png" alt="il2Fusion screenshot 1" width="260" />
  <img src="doc/imgs/Screenshot_02.png" alt="il2Fusion screenshot 2" width="260" />
  <img src="doc/imgs/Screenshot_03.png" alt="il2Fusion screenshot 3" width="260" />
</p>

## 功能特性
- Dump 模式优先：一键触发 Il2CppDumper 生成 `dump.cs`，自动尝试复制到 `/sdcard/Download/<pkg>.cs` 并 Toast 结果；通过 LSPosed+Provider 配置，缓解原版 Zygisk-Il2CppDumper 在配置同步和产物获取上的痛点。
- 文本拦截：在 `libil2cpp.so` 中按 RVA 安装 hook，记录传入的 `set_Text` 字符串到本地数据库，作为“民间汉化”尝试性的工具能力，欢迎扩展更多玩法。
- 配置同步：插件 App 写入 `ContentProvider (com.tools.il2fusion.provider/config)`，注入进程读取后下发到 native。
- 文件解析：可从 `.cs` dump 文件自动解析 `set_Text` 上方的 RVA，支持十六进制/十进制混输。

## 环境要求
- 已 Root 的设备，Magisk + LSPosed 环境。
- Android 12+（minSdk 31，targetSdk 35，compileSdk 36）。
- 默认 ABI：`arm64-v8a`（如需其他 ABI，请补齐 `app/src/main/cpp/libs/<abi>/libdobby.a` 并调整 `ndk.abiFilters`）。
- 已验证设备：Google Pixel 3 XL，Android 12（SP1A.210812.016.C2 / 8618562）。

## 快速开始
1) 构建模块：`./gradlew :app:assembleDebug`（包含 CMake，生成 `libnative_hook.so`）。  
2) 安装并在 LSPosed 勾选目标应用（仅一个）。  
3) 打开插件 App：  
   - 关闭 Dump 开关即为“文本拦截”模式，输入/解析 RVA 后“保存到本地”。  
   - 开启 Dump 开关进入 Dump 模式，启动目标应用后会自动生成 `dump.cs` 并尝试复制到 Download。  
4) 启动目标应用验证：  
   - 文本拦截模式：等待 `libil2cpp.so` 后安装 hook，logcat 标签 `[il2Fusion]`/`DobbyStub`，文本存储于 `/data/data/<pkg>/text.db`。  
   - Dump 模式：等待 `libil2cpp.so` 后自动 dump，并 Toast 提示复制结果。  

## 架构与目录
- LSPosed 入口：`app/src/main/java/com/tools/module/MainHook.kt`（`assets/xposed_init` 指定），在 `Application.attach` 后加载 `libnative_hook.so` 并调用 JNI。
- 配置与仓库：`app/src/main/java/com/tools/il2fusion/config/`，通过 `ContentProvider` 同步 Dump 开关与 RVA 列表。
- Compose UI：`app/src/main/java/com/tools/il2fusion/ui/`，包含三页 Tab 及文件解析逻辑。
- Native Hook：`app/src/main/cpp/native_hook.cpp` 等，等待 `libil2cpp.so`，使用 Dobby 挂钩并写入 SQLite。
- Il2CppDumper：`app/src/main/cpp/il2CppDumper/`，Dump 完成后尝试复制到 Download。
- 资源声明：`app/src/main/assets/xposed/`（模块描述、入口配置）。

## 第三方致谢
- [jmpews - Dobby](https://github.com/jmpews/Dobby)：轻量级跨平台 hook 框架。
- [Perfare - Zygisk-Il2CppDumper](https://github.com/Perfare/Zygisk-Il2CppDumper)：Il2CppDumper 实现参考。

## 贡献与反馈
- 欢迎提交 issue 与 feature 请求，描述清楚环境、目标 App 和期望行为。
- 也欢迎 PR 修复 bug、完善多 ABI 支持或改进 UI/交互。  

## 免责声明
- 本项目仅供学习、研究与安全测试之用，不得用于任何违法、侵权或商业牟利场景。
- 使用者需自行确保遵守所在地法律法规，对由此产生的全部后果自行承担。
