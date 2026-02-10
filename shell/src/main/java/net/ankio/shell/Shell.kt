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
 * - 遵循"Never break userspace"：在可用前提下始终维持既有行为（root 优先，失败再走 Shizuku）。
 *
 * 行为说明：
 * - [exec] 将在协程内挂起执行命令；若 root 可用则使用 root，否则若 Shizuku 可用则使用 Shizuku，否则抛出异常。
 * - [checkPermission] 仅做可用性探测，不会申请权限。
 * - [close] 会释放 Root 相关资源；Shizuku 由其内部实现自行管理。
 *
 * 性能优化：
 * - 首次 exec 时探测一次 root/Shizuku 可用性，结果缓存到实例生命周期结束。
 * - 避免每次命令都 fork su 进程失败再回退，非 root 设备不再有无意义的进程创建开销。
 */
class Shell(packageName: String) : Closeable {
    private val rootExecutor = RootShell()
    private val shizukuExecutor = ShizukuShell(packageName)
    val TAG = "AnkioShell"

    /**
     * 缓存的执行模式（首次 exec 时探测确定，后续复用）。
     * - null: 尚未探测
     * - ROOT: root 可用
     * - SHIZUKU: Shizuku 可用
     */
    @Volatile
    private var resolvedMode: ExecMode? = null

    private enum class ExecMode { ROOT, SHIZUKU }

    /**
     * 以 root 身份执行命令。
     */
    private suspend fun rootExecCommand(command: String): String {
        Log.d(TAG, "root => $command")
        return rootExecutor.execute(command)
    }

    /**
     * 以 Shizuku 能力执行命令。
     */
    private suspend fun shizukuExecCommand(command: String): String {
        Log.d(TAG, "shizuku => $command")
        return shizukuExecutor.runAndGetOutput(command)
    }

    /**
     * 探测 root 能力是否可用。
     */
    private fun rootPermission(): Boolean = rootExecutor.openShell()

    /**
     * 探测 Shizuku 能力是否可用（仅检查支持与未被拒绝，不主动拉起授权）。
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
     */
    fun checkPermission(): Boolean {
        return rootPermission() || shizukuPermission()
    }

    // ======================== 独立模式调用（供 OcrTools 等需要强制指定模式的场景） ========================

    /** 检查 Root 权限是否可用 */
    fun hasRootPermission(): Boolean = rootPermission()

    /** 检查 Shizuku 权限是否可用（Shizuku 运行中且已授权） */
    fun hasShizukuPermission(): Boolean = shizukuPermission()

    /** 请求 Shizuku 权限授权 */
    fun requestShizukuPermission() = shizukuExecutor.requestPermission()

    /**
     * 强制以 Root 身份执行命令。
     * 调用前应先通过 [hasRootPermission] 确认可用，否则可能返回空字符串。
     */
    suspend fun execAsRoot(command: String): String = rootExecCommand(command)

    /**
     * 强制以 Shizuku 身份执行命令。
     * 调用前应先通过 [hasShizukuPermission] 确认可用，否则可能返回空字符串。
     */
    suspend fun execAsShizuku(command: String): String = shizukuExecCommand(command)

    /**
     * 释放底层资源。
     */
    override fun close() {
        rootExecutor.close()
        resolvedMode = null
    }

    /**
     * 探测并缓存执行模式（仅首次调用时实际探测）。
     * @return 可用的执行模式
     * @throws IllegalStateException root 与 Shizuku 均不可用时抛出
     */
    private fun resolveMode(): ExecMode {
        // 已缓存直接返回
        resolvedMode?.let { return it }

        // 首次探测：root 优先
        if (rootPermission()) {
            resolvedMode = ExecMode.ROOT
            return ExecMode.ROOT
        }

        // root 不可用，尝试 Shizuku
        if (!shizukuPermission()) {
            // Shizuku 权限未授予，尝试请求
            requestPermission()
        }

        // 无论请求结果如何，标记为 Shizuku 模式（执行时若不可用会抛异常）
        resolvedMode = ExecMode.SHIZUKU
        return ExecMode.SHIZUKU
    }

    /**
     * 执行命令（挂起）。
     * 首次调用时探测 root/Shizuku 可用性并缓存，后续直接使用已确定的执行方式。
     *
     * @param command 待执行的 shell 命令（可多行）。
     * @return 命令标准输出文本。
     * @throws IllegalStateException 当 root 与 Shizuku 均不可用时抛出。
     */
    suspend fun exec(command: String): String {
        return when (resolveMode()) {
            ExecMode.ROOT -> rootExecCommand(command)
            ExecMode.SHIZUKU -> shizukuExecCommand(command)
        }
    }
}
