/*
 * Copyright (C) 2025 ankio(ankio@ankio.net)
 * Licensed under the Apache License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-3.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package net.ankio.shell

import android.util.Log
import java.io.Closeable

/**
 * Shell 执行器（优先 root，其次 Shizuku）。
 *
 * 设计目标：
 * - 以最简单直观的方式执行 shell 命令，且不强迫调用方关心使用 root 还是 Shizuku。
 * - 遵循“Never break userspace”：在可用前提下始终维持既有行为（root 优先，失败再走 Shizuku）。
 *
 * 行为说明：
 * - [exec] 将在协程内挂起执行命令；若 root 可用则使用 root，否则若 Shizuku 可用则使用 Shizuku，否则抛出异常。
 * - [checkPermission] 仅做可用性探测，不会申请权限。
 * - [close] 会释放 Root 相关资源；Shizuku 由其内部实现自行管理。
 *
 * 使用示例：
 * ```kotlin
 * App.launch {
 *     Shell().use { shell ->
 *         // 建议在 IO 线程调用，避免阻塞主线程
 *         val output = shell.exec("id && getprop ro.build.version.release")
 *         Logger.d("命令输出: $output")
 *     }
 * }
 * ```
 *
 * 注意事项：
 * - root 执行需要 su 环境且已授权。
 * - Shizuku 执行需要 Shizuku 已安装且授予本应用权限。
 * - 该类不主动请求权限，只做能力检测与命令执行。
 * - 如果遇到联发科设备无法正常获取shizuku执行结果的，使用低版本shizuku（3.5.x）重试，https://github.com/RikkaApps/Shizuku/issues/1171
 */
class Shell(packageName: String) : Closeable {
    private val rootExecutor = RootShell()
    private val shizukuExecutor = ShizukuShell(packageName)
    val TAG = "AnkioShell"

    /**
     * 以 root 身份执行命令。
     * @param command 待执行的 shell 命令（可包含多行）。
     * @return 命令标准输出（具体合并/分离由 [RootShell] 内部实现决定）。
     * @throws Exception 由 [RootShell.execute] 透传。
     */
    private suspend fun rootExecCommand(command: String): String {
        Log.d(TAG, "root => $command")
        val data = rootExecutor.execute(command)
        // Log.d("命令执行结果",data)
        return data
    }

    /**
     * 以 Shizuku 能力执行命令。
     * @param command 待执行的 shell 命令（可包含多行）。
     * @return 命令标准输出（具体合并/分离由 [ShizukuShell] 内部实现决定）。
     * @throws Exception 由 [ShizukuShell.runAndGetOutput] 透传。
     */
    private suspend fun shizukuExecCommand(command: String): String {
        Log.d(TAG, "shizuku => $command")
        val data = shizukuExecutor.runAndGetOutput(command)
        // Log.d("命令执行结果",data)
        return data
    }

    /**
     * 探测 root 能力是否可用。
     * @return true 表示可使用 root 执行命令。
     */
    private fun rootPermission(): Boolean = rootExecutor.openShell()

    /**
     * 探测 Shizuku 能力是否可用（仅检查支持与未被拒绝，不主动拉起授权）。
     * @return true 表示可使用 Shizuku 执行命令。
     */
    private fun shizukuPermission(): Boolean =
        shizukuExecutor.isSupported && !shizukuExecutor.isPermissionDenied

    fun requestPermission() {
        if (!checkPermission()) {
            shizukuExecutor.requestPermission()
        }
    }

    /**
     * 快速检查是否具备任一执行能力（root 或 Shizuku）。
     * @return 至少一种可用返回 true。
     */
    fun checkPermission(): Boolean {
        return rootPermission() || shizukuPermission()
    }

    /**
     * 释放底层资源。
     * - 当前仅需释放 root 相关资源；Shizuku 由其实现自行管理。
     */
    override fun close() {
        rootExecutor.close()
    }

    /**
     * 执行命令（挂起）。
     * 优先尝试 root，其次尝试 Shizuku；若均不可用则抛出异常。
     *
     * 性能与线程：建议在 IO 线程调用；方法本身为挂起函数。
     *
     * @param command 待执行的 shell 命令（可多行）。
     * @return 命令标准输出文本。
     * @throws IllegalStateException 当 root 与 Shizuku 均不可用时抛出。
     */
    suspend fun exec(command: String): String {
        if (rootPermission()) return rootExecCommand(command)
        else if (!shizukuPermission()) {
            requestPermission()
            return shizukuExecCommand(command)
        } else if (shizukuPermission()) {
            return shizukuExecCommand(command)
        }
        error("no permission")
    }


}