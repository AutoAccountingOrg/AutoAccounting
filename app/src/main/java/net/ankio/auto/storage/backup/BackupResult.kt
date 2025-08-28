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

import android.content.Context
import net.ankio.auto.R
import net.ankio.auto.exceptions.RestoreBackupException
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.utils.ToastUtils

/**
 * 备份操作结果处理器
 */
object BackupResultHandler {

    /**
     * 处理备份操作结果
     * @param context 上下文
     * @param operation 操作类型描述
     * @param onSuccess 成功回调
     * @param onError 错误回调
     * @param block 要执行的操作
     */
    suspend inline fun <T> handleOperation(
        context: Context,
        operation: String,
        crossinline onSuccess: suspend (T) -> Unit,
        crossinline onError: suspend (Throwable) -> Unit,
        crossinline block: suspend () -> T
    ) {
        runCatching {
            block()
        }.onSuccess { result ->
            Logger.i("$operation 操作成功")
            onSuccess(result)
        }.onFailure { throwable ->
            Logger.e("$operation 操作失败", throwable)
            handleError(context, throwable)
            onError(throwable)
        }
    }

    /**
     * 处理备份操作结果（只有成功回调）
     * @param context 上下文
     * @param operation 操作类型描述
     * @param onSuccess 成功回调
     * @param block 要执行的操作
     */
    suspend inline fun <T> handleOperation(
        context: Context,
        operation: String,
        crossinline onSuccess: suspend (T) -> Unit,
        crossinline block: suspend () -> T
    ) {
        handleOperation(
            context = context,
            operation = operation,
            onSuccess = onSuccess,
            onError = {},
            block = block
        )
    }

    /**
     * 处理备份操作结果（无回调）
     * @param context 上下文
     * @param operation 操作类型描述
     * @param block 要执行的操作
     */
    suspend inline fun <T> handleOperation(
        context: Context,
        operation: String,
        crossinline block: suspend () -> T
    ) {
        handleOperation(
            context = context,
            operation = operation,
            onSuccess = {},
            onError = {},
            block = block
        )
    }

    /**
     * 处理错误信息
     * @param context 上下文
     * @param throwable 异常
     */
    fun handleError(context: Context, throwable: Throwable) {
        when (throwable) {
            is RestoreBackupException -> {
                ToastUtils.error(throwable.message ?: context.getString(R.string.backup_error))
            }

            else -> {
                ToastUtils.error(R.string.backup_error)
            }
        }
    }

    /**
     * 显示成功消息
     * @param messageResId 消息资源ID
     */
    fun showSuccess(messageResId: Int) {
        ToastUtils.info(messageResId)
    }

    /**
     * 显示错误消息
     * @param messageResId 消息资源ID
     */
    fun showError(messageResId: Int) {
        ToastUtils.error(messageResId)
    }
}
