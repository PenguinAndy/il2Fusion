package com.tools.module

import android.content.Context
import com.tools.textextracttool.BuildConfig
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
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
        const val TAG = "[TextExtractTool]"
        const val NATIVE_LIB = "native_hook"
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // ignore self
        if (lpparam.packageName == BuildConfig.APPLICATION_ID) return

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
                    val ctx = param.args[0] as Context
                    XposedBridge.log("$TAG Application.attach -> ctx=$ctx")

                    // Load native so
                    try {
                        System.loadLibrary(NATIVE_LIB)
                        XposedBridge.log("$TAG $NATIVE_LIB loaded")
                    } catch (e: Throwable) {
                        XposedBridge.log("$TAG loadLibrary failed: $e")
                    }

                    // native initialize
                    try {
                        NativeBridge.init()
                        XposedBridge.log("$TAG Native init() call")
                    } catch (e: Throwable) {
                        XposedBridge.log("$TAG Native init() failed: $e")
                    }

                }
            }
        )
    }
}
