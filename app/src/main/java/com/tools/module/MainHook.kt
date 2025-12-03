package com.tools.module

import android.content.Context
import com.tools.il2fusion.config.HookConfigStore
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_InitPackageResources
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * <pre>
 *     author: PenguinAndy
 *     time  : 2025/11/28 17:41
 *     desc  :
 * </pre>
 */
class MainHook: IXposedHookLoadPackage {

    private companion object {
        const val TAG = "[il2Fusion]"
        const val NATIVE_LIB = "native_hook"
        const val MODULE_PKG = "com.tools.il2fusion"
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // ignore self
        if (lpparam.packageName == MODULE_PKG) return

        XposedBridge.log("$TAG Inject to: ${lpparam.packageName}")

        val  cl = lpparam.classLoader

        // Hook Application.attach(Context)
        XposedHelpers.findAndHookMethod(
            "android.app.Application",
            cl,
            "attach",
            Context::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    // Keep context for future use (e.g. resource access)
                    val ctx = param.args[0] as? Context
                    if (ctx == null) {
                        XposedBridge.log("$TAG Application.attach -> ctx is null, skip init")
                        return
                    }
                    NativeBridge.setContext(ctx)
                    XposedBridge.log("$TAG Application.attach -> ctx=$ctx")

                    // 提示：LSPosed 勾选多个 App 可能导致互相覆盖
                    try {
                        val enabledApps = HookConfigStore.markHookedPackage(ctx, lpparam.packageName)
                        if (enabledApps.size > 1) {
                            XposedBridge.log("$TAG Warning: LSPosed 勾选了多个 App，hook 只针对当前进程。建议单次只勾选一个目标包")
                        }
                    } catch (e: Throwable) {
                        XposedBridge.log("$TAG markHookedPackage failed: $e")
                    }

                    // Load native so
                    try {
                        System.loadLibrary(NATIVE_LIB)
                        XposedBridge.log("$TAG $NATIVE_LIB loaded")
                    } catch (e: Throwable) {
                        XposedBridge.log("$TAG loadLibrary failed: $e")
                    }

                    // native initialize
                    try {
                        NativeBridge.init(lpparam.processName)
                        XposedBridge.log("$TAG Native init() call, process=${lpparam.processName}")
                    } catch (e: Throwable) {
                        XposedBridge.log("$TAG Native init() failed: $e")
                    }

                    val dumpMode = try {
                        HookConfigStore.loadDumpModeForHook(ctx)
                    } catch (e: Throwable) {
                        XposedBridge.log("$TAG loadDumpMode failed: $e")
                        false
                    }

                    if (dumpMode) {
                        // Dump 模式：启动 il2cpp dumper，不做文本拦截
                        try {
                            val dir = ctx.dataDir?.absolutePath ?: ""
                            NativeBridge.startDump(dir)
                            XposedBridge.log("$TAG startDump -> $dir")
                        } catch (e: Throwable) {
                            XposedBridge.log("$TAG startDump failed: $e")
                        }
                    } else {
                        // 文本拦截模式：下发 RVA 列表，开启文本日志
                        try {
                            val rvas = HookConfigStore.loadRvasForHook(ctx).toLongArray()
                            NativeBridge.setConfig(rvas)
                            XposedBridge.log("$TAG setConfig -> ${rvas.joinToString()}")
                            // 如果当前列表为空，延迟重试几次，避免主进程早于配置写入完成
                            if (rvas.isEmpty()) {
                                retryLoadRvas(ctx, 3, 500L)
                            }
                        } catch (e: Throwable) {
                            XposedBridge.log("$TAG setConfig failed: $e")
                        }
                    }
                }
            }
        )
    }

    private fun retryLoadRvas(ctx: Context, times: Int, delayMs: Long) {
        if (times <= 0) return
        Thread {
            repeat(times) { idx ->
                try {
                    Thread.sleep(delayMs)
                    val rvas = HookConfigStore.loadRvasForHook(ctx).toLongArray()
                    if (rvas.isNotEmpty()) {
                        NativeBridge.setConfig(rvas)
                        XposedBridge.log("$TAG retry#$idx setConfig -> ${rvas.joinToString()}")
                        return@Thread
                    }
                } catch (t: Throwable) {
                    XposedBridge.log("$TAG retryLoadRvas failed: $t")
                }
            }
        }.start()
    }
}
