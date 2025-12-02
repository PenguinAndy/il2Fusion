package com.tools.module

/**
 * <pre>
 *     author: PenguinAndy
 *     time  : 2025/11/28 18:10
 *     desc  :
 * </pre>
 */
object NativeBridge {
    @JvmStatic external fun init(processName: String)
    @JvmStatic external fun setConfig(targets: LongArray)
    @JvmStatic external fun startDump(dataDir: String)
}
