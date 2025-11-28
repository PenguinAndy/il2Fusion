package com.tools.module

/**
 * <pre>
 *     author: PenguinAndy
 *     time  : 2025/11/28 18:10
 *     desc  :
 * </pre>
 */
object NativeBridge {
    @JvmStatic external fun init()
    @JvmStatic external fun updateHookTargets(targets: LongArray)
}
