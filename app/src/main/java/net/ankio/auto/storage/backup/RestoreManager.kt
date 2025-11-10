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
    private val webDAVManager = WebDAVManager()

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
                    message = "无法打开备份文件，请检查文件是否存在",
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
                        "无法打开备份文件，请检查文件权限"

                    e.message?.contains("unpackData") == true ||
                            e.message?.contains("解包") == true ->
                        "备份文件损坏或格式不正确：${e.message}"

                    e.message?.contains("readBytes") == true ->
                        "读取备份文件失败，请检查文件是否完整"

                    else -> "恢复失败：${e.message ?: "未知错误"}"
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
            // 下载文件
            withMain {
                loadingUtils?.setText(R.string.restore_webdav)
            }
            webDAVManager.download(filename, targetFile).onSuccess {
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
                    e.message?.contains("connect", ignoreCase = true) == true ->
                        "无法连接到WebDAV服务器，请检查网络连接"

                    e.message?.contains("auth", ignoreCase = true) == true ||
                            e.message?.contains("401", ignoreCase = true) == true ||
                            e.message?.contains("403", ignoreCase = true) == true ->
                        "WebDAV认证失败，请检查用户名和密码"

                    e.message?.contains("404", ignoreCase = true) == true ->
                        "备份文件不存在：$filename"

                    e.message?.contains("unpackData") == true ||
                            e.message?.contains("解包") == true ->
                        "备份文件损坏或格式不正确：${e.message}"

                    e.message?.contains("download") == true ->
                        "下载备份文件失败：${e.message}"

                    else -> "WebDAV恢复失败：${e.message ?: "未知错误"}"
                },
                throwable = e
            )
        }
    }
}
