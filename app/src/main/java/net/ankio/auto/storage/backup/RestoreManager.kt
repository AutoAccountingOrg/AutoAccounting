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
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * 恢复管理器，负责从备份文件恢复数据
 */
class RestoreManager(private val context: Context) {

    private val logger = KotlinLogging.logger(this::class.java.name)

    private val fileManager = BackupFileManager(context)
    private val webDAVManager = WebDAVManager()

    /**
     * 从本地文件恢复
     * @param uri 文件 URI
     */
    suspend fun restoreFromLocal(uri: Uri) = withIO {
        try {
            val filename = "restore_${System.currentTimeMillis()}.${BackupFileManager.SUFFIX}"

            // 读取文件内容
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val file = File(context.cacheDir, filename)
                file.writeBytes(inputStream.readBytes())

                // 解包并恢复数据
                fileManager.unpackData(file)
            }

            logger.info { "本地恢复完成" }
            // BackupFileManager 已经处理了重启逻辑
            ToastUtils.info(R.string.restore_success)
        } catch (throwable: Throwable) {
            logger.error(throwable) { "本地恢复失败" }
            ToastUtils.error(R.string.backup_error)
            throw throwable
        }
    }

    /**
     * 从 WebDAV 恢复
     * @param activity 活动实例
     * @param filename 备份文件名
     */
    suspend fun restoreFromWebDAV(
        activity: HomeActivity,
        filename: String
    ) = withIO {

        var loadingUtils: LoadingUtils? = null
        withMain {
            loadingUtils = LoadingUtils(activity)
        }
        val targetFile = File(context.cacheDir, "auto_backup.${BackupFileManager.SUFFIX}")

        try {
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

            logger.info { "WebDAV恢复完成" }
            withMain {
                loadingUtils?.close()
                // BackupFileManager 已经处理了重启逻辑
                ToastUtils.info(R.string.restore_success)
            }
        } catch (throwable: Throwable) {
            logger.error(throwable) { "WebDAV恢复失败" }
            withMain {
                loadingUtils?.close()
                ToastUtils.error(R.string.backup_error)
            }
            throw throwable
        }
    }
}
