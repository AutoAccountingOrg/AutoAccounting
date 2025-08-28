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
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.ui.activity.HomeActivity
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.utils.SystemUtils
import java.io.File

/**
 * 恢复管理器，负责从备份文件恢复数据
 */
class RestoreManager(private val context: Context) {

    private val fileManager = BackupFileManager(context)
    private val webDAVManager = WebDAVManager(context)

    /**
     * 从本地文件恢复
     * @param uri 文件 URI
     */
    suspend fun restoreFromLocal(uri: Uri) = withContext(Dispatchers.IO) {
        BackupResultHandler.handleOperation(
            context = context,
            operation = "本地恢复",
            onSuccess = {
                BackupResultHandler.showSuccess(R.string.restore_success)
                // 延迟后重启应用
                withContext(Dispatchers.IO) {
                    delay(3000)
                    SystemUtils.restart()
                }
            }
        ) {
            val filename = "restore_${System.currentTimeMillis()}.${BackupFileManager.SUFFIX}"

            // 读取文件内容
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val file = File(context.cacheDir, filename)
                file.writeBytes(inputStream.readBytes())

                // 解包并恢复数据
                fileManager.unpackData(file)
            }
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
    ) = withContext(Dispatchers.IO) {
        val loadingUtils = LoadingUtils(activity)
        val targetFile = File(context.cacheDir, "auto_backup.${BackupFileManager.SUFFIX}")

        BackupResultHandler.handleOperation(
            context = context,
            operation = "WebDAV恢复",
            onSuccess = {
                withContext(Dispatchers.Main) {
                    loadingUtils.close()
                    BackupResultHandler.showSuccess(R.string.restore_success)
                }
                // 延迟后重启应用
                delay(3000)
                SystemUtils.restart()
            },
            onError = {
                withContext(Dispatchers.Main) {
                    loadingUtils.close()
                }
            }
        ) {
            // 下载文件
            val downloadSuccess = webDAVManager.downloadBackup(filename, targetFile, loadingUtils)

            if (downloadSuccess) {
                // 切换到恢复进度提示
                withContext(Dispatchers.Main) {
                    loadingUtils.setText(R.string.restore_loading)
                }

                // 解包并恢复数据
                fileManager.unpackData(targetFile)
            } else {
                throw RuntimeException("下载备份文件失败")
            }
        }
    }
}
