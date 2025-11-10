/*
 * Copyright (C) 2024 ankio(ankio@ankio.net)
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
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.activity.HomeActivity
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.utils.CoroutineUtils.withIO
import net.ankio.auto.utils.CoroutineUtils.withMain
import java.io.File

/**
 * 恢复管理器，负责从备份文件恢复数据
 */
class RestoreManager(private val context: Context) {

    private val fileManager = BackupFileManager(context)

    /**
     * WebDAV管理器 - 每次使用时创建新实例，确保读取最新配置
     *
     * 为什么不在构造函数中创建？
     * - 配置可能在运行时改变（用户在设置页面修改）
     * - 每次使用时创建新实例，自动读取最新的 PrefManager 配置
     * - WebDAVManager 是无状态的，创建成本低
     */
    private fun getWebDAVManager(): WebDAVManager = WebDAVManager()

    /**
     * 从本地文件恢复
     *
     * @param uri 文件 URI
     * @return BackupResult.Success 表示恢复成功，Failure 包含详细错误信息
     */
    suspend fun restoreFromLocal(uri: Uri): BackupResult<Unit> = withIO {
        return@withIO try {
            val filename = "restore_${System.currentTimeMillis()}.${BackupFileManager.SUFFIX}"

            // 读取文件内容
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withIO BackupResult.Failure(
                    message = context.getString(R.string.restore_error_open_file),
                    throwable = null
                )

            inputStream.use { stream ->
                val file = File(context.cacheDir, filename)
                file.writeBytes(stream.readBytes())

                // 解包并恢复数据
                fileManager.unpackData(file)
            }

            Logger.i("本地恢复完成")
            // BackupFileManager 已经处理了重启逻辑
            BackupResult.Success(Unit)

        } catch (e: Exception) {
            Logger.e("本地恢复失败", e)
            BackupResult.Failure(
                message = when {
                    e.message?.contains("无法打开") == true ->
                        context.getString(R.string.restore_error_open_file)

                    e.message?.contains("unpackData") == true ||
                            e.message?.contains("解包") == true ->
                        context.getString(R.string.restore_error_file_corrupted, e.message ?: "")

                    e.message?.contains("readBytes") == true ->
                        context.getString(R.string.restore_error_read_file)

                    else -> context.getString(
                        R.string.restore_error_unknown,
                        e.message ?: context.getString(R.string.unknown_error)
                    )
                },
                throwable = e
            )
        }
    }

    /**
     * 从 WebDAV 恢复
     *
     * @param activity 活动实例
     * @param filename 备份文件名
     * @return BackupResult.Success 表示恢复成功，Failure 包含详细错误信息
     */
    suspend fun restoreFromWebDAV(
        activity: HomeActivity,
        filename: String
    ): BackupResult<Unit> = withIO {

        var loadingUtils: LoadingUtils? = null
        withMain {
            loadingUtils = LoadingUtils(activity)
        }
        val targetFile = File(context.cacheDir, "auto_backup.${BackupFileManager.SUFFIX}")

        return@withIO try {
            // 下载文件（使用最新配置）
            withMain {
                loadingUtils?.setText(R.string.restore_webdav)
            }
            getWebDAVManager().download(filename, targetFile).onSuccess {
                withMain {
                    loadingUtils?.setText(R.string.restore_loading)
                }

                // 解包并恢复数据
                fileManager.unpackData(targetFile)
            }.getOrThrow()

            Logger.i("WebDAV恢复完成")
            withMain {
                loadingUtils?.close()
            }
            // BackupFileManager 已经处理了重启逻辑
            BackupResult.Success(Unit)

        } catch (e: Exception) {
            Logger.e("WebDAV恢复失败", e)
            withMain {
                loadingUtils?.close()
            }
            BackupResult.Failure(
                message = when {
                    // 检查 HTTP 异常的状态码
                    e is net.ankio.auto.http.RequestsUtils.HttpException -> {
                        when (e.code) {
                            401, 403 -> context.getString(R.string.webdav_error_auth, e.code)
                            404 -> context.getString(
                                R.string.webdav_error_file_not_found,
                                e.code,
                                filename
                            )

                            500, 502, 503, 504 -> context.getString(
                                R.string.webdav_error_server_error,
                                e.code
                            )

                            else -> context.getString(
                                R.string.webdav_error_request_failed,
                                e.code,
                                e.message ?: ""
                            )
                        }
                    }
                    // 网络连接错误
                    e.message?.contains("connect", ignoreCase = true) == true ||
                            e.message?.contains("timeout", ignoreCase = true) == true ||
                            e.message?.contains("unreachable", ignoreCase = true) == true ->
                        context.getString(R.string.webdav_error_connect_restore)
                    
                    // 认证相关错误（兼容旧代码）
                    e.message?.contains("auth", ignoreCase = true) == true ||
                            e.message?.contains("401", ignoreCase = true) == true ||
                            e.message?.contains("403", ignoreCase = true) == true ->
                        context.getString(R.string.webdav_error_auth_simple)
                    
                    // 文件不存在
                    e.message?.contains("404", ignoreCase = true) == true ->
                        context.getString(R.string.webdav_error_file_not_found, 404, filename)
                    
                    // 解包错误
                    e.message?.contains("unpackData") == true ||
                            e.message?.contains("解包") == true ->
                        context.getString(R.string.restore_error_file_corrupted, e.message ?: "")
                    
                    // 下载错误
                    e.message?.contains("download") == true ->
                        context.getString(R.string.webdav_error_download_failed, e.message ?: "")
                    
                    // 其他错误
                    else -> context.getString(
                        R.string.webdav_error_restore_failed,
                        e.message ?: context.getString(R.string.unknown_error)
                    )
                },
                throwable = e
            )
        }
    }
}
