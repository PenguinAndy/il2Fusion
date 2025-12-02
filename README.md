TextExtractTool
================

基于 LSPosed + native hook（Dobby）/Il2CppDumper 的 Unity 文本截获与 dump 工具。

架构概览
--------
- LSPosed 模块入口：`com.tools.module.MainHook`（由 `META-INF/xposed_init` 指定），在目标进程的 `Application.attach` 后加载 `libnative_hook.so`，调用 JNI。
- 配置桥：通过 `ContentProvider` (`com.tools.textextracttool.provider/config`) 同步 RVA 列表与 dump 模式开关，插件界面写入 provider，注入进程查询 provider 后调用 JNI 下发。
- UI 插件：Compose 界面 `HookConfigScreen` 支持多行 RVA 输入（十六进制/十进制）、Il2CppDumper 模式开关，并可从 `.cs` dump 文件解析 `set_Text` 上方的 RVA 自动填充；提示一次只勾选一个应用。
- Native 框架：`native_hook.cpp` 负责等待 `libil2cpp.so`，按配置安装 Dobby hook 记录文本；在 dump 模式触发 Il2CppDumper (`il2cpp_dump.cpp`) 生成 `dump.cs`，并尝试 `su -c cp` 复制到 `/sdcard/Download/<pkg>.cs`，结果通过 Toast 提示。
- LSPosed 元数据：`META-INF/xposed/module.prop`、`META-INF/xposed_init`、`META-INF/native_init` 打包入 assets。

使用方法
--------
1) 设置模式与 RVA  
   - 打开插件 App，关闭 Il2CppDumper 开关即为“文本拦截”模式：输入/解析 `set_text` 的 RVA（十六进制可用 `0x` 前缀，或从 `.cs` dump 文件自动解析），点击“保存到本地”。  
   - 打开 Il2CppDumper 开关则仅执行 dump：启动目标 App 后生成 `/data/data/<pkg>/files/dump.cs` 并尝试复制到 `/sdcard/Download/<pkg>.cs`。  
2) LSPosed 勾选目标应用  
   - 在 LSPosed 里仅勾选一个目标应用启用本模块（多选会互相覆盖，日志有提示）。  
3) 启动目标应用验证  
   - 文本拦截模式：等待 `libil2cpp.so` 后安装 hook，记录传入字符串到本地数据库，logcat 标签 `[TextExtractTool]`/`DobbyStub`。  
   - Dumper 模式：等待 `libil2cpp.so` 后自动 dump，并 Toast 通知结果及复制状态。  
4) 构建  
   - `./gradlew :app:assembleDebug`（已启用 CMake，so 名称 `libnative_hook.so`）。  

关于 Dobby
----------
- 现在直接使用预编译的静态库 `app/src/main/cpp/libs/<abi>/libdobby.a` + 头文件 `dobby.h`，未做源码改动；CMake 以 IMPORTED STATIC 方式链接。
- 仓库附带 arm64/armeabi-v7a/x86/x86_64 的 `.a`，但 Gradle 默认 abiFilter 只包含 `arm64-v8a`。如需其它 ABI，补齐对应库并调整 `ndk.abiFilters`。

目录指引
--------
- `app/src/main/java/com/tools/module/`：LSPosed 入口、JNI 桥。  
- `app/src/main/java/com/tools/textextracttool/`：插件 UI、配置存取、dump 文件解析（填充 RVA）。  
- `app/src/main/cpp/`：native hook 逻辑、Il2CppDumper、Dobby。  
- `META-INF/`：LSPosed 模块声明与入口文件。  

注意事项
--------
- 只在单一目标进程中启用模块，避免多应用同时 hook。  
- RVA 需对应目标版本的 `libil2cpp.so`，否则 hook 失效或崩溃。  
- 若更换替换文本或增加多语言，请在 `native_hook.cpp` 中调整目标字符串生成逻辑。  
