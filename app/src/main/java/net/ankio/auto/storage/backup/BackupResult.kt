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

package net.ankio.auto.storage.backup

/**
 * 备份恢复操作结果 - Linus式极简设计
 *
 * 设计原则：
 * 1. 消除特殊情况 - 只有成功和失败两种状态，没有中间态
 * 2. 携带详细信息 - 失败时必须包含可读的错误信息
 * 3. 类型安全 - 使用 sealed class 确保编译时检查
 *
 * 使用示例：
 * ```
 * when (val result = backupManager.createLocalBackup()) {
 *     is BackupResult.Success -> ToastUtils.info("备份成功")
 *     is BackupResult.Failure -> ToastUtils.error(result.message)
 * }
 * ```
 */
sealed class BackupResult<out T> {
    /**
     * 成功结果，携带数据
     */
    data class Success<T>(val data: T) : BackupResult<T>()

    /**
     * 失败结果，携带错误信息
     *
     * @param message 用户可读的错误信息
     * @param throwable 原始异常（可选，用于日志记录）
     */
    data class Failure(
        val message: String,
        val throwable: Throwable? = null
    ) : BackupResult<Nothing>()

    /**
     * 判断是否成功
     */
    val isSuccess: Boolean
        get() = this is Success

    /**
     * 判断是否失败
     */
    val isFailure: Boolean
        get() = this is Failure

    /**
     * 获取数据，失败时返回 null
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Failure -> null
    }

    /**
     * 获取数据，失败时抛出异常
     */
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Failure -> throw throwable ?: RuntimeException(message)
    }
}

