il2Fusion
================

<p align="center">
  <img src="imgs/il2FusionIcon.png" alt="il2Fusion icon" width="180" />
</p>

LSPosed (Java-layer hook) + Dobby (native hook) + ContentProvider IPC, built around Il2Cpp dumping and tooling.

<p align="center">
  <img src="imgs/Screenshot_01.png" alt="il2Fusion screenshot 1" width="260" />
  <img src="imgs/Screenshot_02.png" alt="il2Fusion screenshot 2" width="260" />
  <img src="imgs/Screenshot_03.png" alt="il2Fusion screenshot 3" width="260" />
</p>

## Features
- Dump-first: one-tap Il2CppDumper to produce `dump.cs`, auto-copy to `/sdcard/Download/<pkg>.cs` with Toast feedback; LSPosed + Provider config eases the sync/output pain points seen in Zygisk-Il2CppDumper setups.
- Text logging (community localization experiment): hook `set_Text` by RVA in `libil2cpp.so`, log incoming strings into SQLite for later processing; feel free to extend new use cases.
- Config sync: plugin app writes to `ContentProvider (com.tools.il2fusion.provider/config)`, injected process reads and forwards to native.
- File parsing: parse `set_Text` RVAs from `.cs` dump files (hex/decimal mixed input supported).

## Requirements
- Rooted device with Magisk + LSPosed (enable only one target app at a time).
- Android 12+ (minSdk 31, targetSdk 35, compileSdk 36).
- Default ABI: `arm64-v8a`; for other ABIs, add `app/src/main/cpp/libs/<abi>/libdobby.a` and update `ndk.abiFilters`.
- Verified device: Google Pixel 3 XL, Android 12 (SP1A.210812.016.C2 / 8618562).

## Quick Start
1) Build: `./gradlew :app:assembleDebug` (CMake included, outputs `libnative_hook.so`).
2) Install and enable the module in LSPosed (select a single target app).
3) In the plugin app:
   - Text mode: leave Dump off, enter/parse RVAs, then "Save locally".
   - Dump mode: toggle Dump on; launch the target app to generate `dump.cs` and copy to Download.
4) Validate in target app:
   - Text mode: waits for `libil2cpp.so`, installs hooks, logcat tags `[il2Fusion]` / `DobbyStub`; text stored at `/data/data/<pkg>/text.db`.
   - Dump mode: waits for `libil2cpp.so`, runs dumper, shows Toast with copy result.

## Architecture & Layout
- LSPosed entry: `app/src/main/java/com/tools/module/MainHook.kt` (declared in `assets/xposed_init`), loads `libnative_hook.so` in `Application.attach`.
- Config layer: `app/src/main/java/com/tools/il2fusion/config/` uses `ContentProvider` to sync dump switch and RVA list.
- UI: `app/src/main/java/com/tools/il2fusion/ui/` (Compose tabs for Overview / Dump / Parse).
- Native hook: `app/src/main/cpp/native_hook.cpp`, waits for `libil2cpp.so`, installs Dobby hooks, writes to SQLite.
- Il2CppDumper: `app/src/main/cpp/il2CppDumper/`, copies results to Download when possible.
- Assets: `app/src/main/assets/xposed/` for LSPosed descriptors and entry declarations.

## Credits
- [jmpews - Dobby](https://github.com/jmpews/Dobby): lightweight cross-platform hook framework.
- [Perfare - Zygisk-Il2CppDumper](https://github.com/Perfare/Zygisk-Il2CppDumper): Il2CppDumper implementation reference.

## Contributing
- Issues and feature requests are welcome—please include environment, target app, and expected behavior.
- PRs to fix bugs, add multi-ABI support, or improve UI/UX are appreciated.

## Disclaimer
- For learning, research, and security testing only. Do not use for illegal, infringing, or commercial purposes.
- Users must comply with local laws and bear all responsibilities arising from usage.
