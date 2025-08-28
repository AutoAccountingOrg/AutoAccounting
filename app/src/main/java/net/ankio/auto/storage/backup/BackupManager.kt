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
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.BuildConfig
import net.ankio.auto.R
import net.ankio.auto.exceptions.PermissionException
import net.ankio.auto.ui.activity.HomeActivity
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.utils.PrefManager
import java.io.File
import java.io.FileInputStream
import androidx.core.net.toUri

/**
 * 备份管理器，负责创建和管理备份
 */
class BackupManager(private val context: Context) {

    private val fileManager = BackupFileManager(context)
    private val webDAVManager = WebDAVManager(context)

    /**
     * 生成备份文件名
     */
    private fun generateBackupFilename(): String {
        return "backup_${BuildConfig.VERSION_NAME}(${BackupFileManager.SUPPORT_VERSION})_${System.currentTimeMillis()}.${BackupFileManager.SUFFIX}"
    }

    /**
     * 本地备份
     */
    suspend fun createLocalBackup() = withContext(Dispatchers.IO) {
        // 检查权限
        if (!BackupPermissionManager.hasAccessPermission(context)) {
            throw PermissionException("No Storage Permission.")
        }

        val filename = generateBackupFilename()
        val uri = PrefManager.localBackupPath.toUri()

        BackupResultHandler.handleOperation(
            context = context,
            operation = "本地备份",
            onSuccess = {
                BackupResultHandler.showSuccess(R.string.backup_success)
            }
        ) {
            // 创建备份文件
            val file = File(context.cacheDir, filename)
            fileManager.packData(file.absolutePath)

            // 保存到指定目录
            saveToLocalDirectory(uri, filename, file)
        }
    }

    /**
     * WebDAV 备份
     */
    suspend fun createWebDAVBackup(activity: HomeActivity) = withContext(Dispatchers.IO) {
        val filename = generateBackupFilename()
        val file = File(context.cacheDir, filename)
        val loadingUtils = LoadingUtils(activity)

        BackupResultHandler.handleOperation(
            context = context,
            operation = "WebDAV备份",
            onSuccess = {
                BackupResultHandler.showSuccess(R.string.backup_success)
            },
            onError = {
                withContext(Dispatchers.Main) {
                    loadingUtils.close()
                }
            }
        ) {
            // 显示打包进度
            withContext(Dispatchers.Main) {
                loadingUtils.show(R.string.backup_pack)
            }

            // 打包数据
            fileManager.packData(file.absolutePath)

            // 上传到 WebDAV
            webDAVManager.uploadBackup(file, filename, loadingUtils)

            withContext(Dispatchers.Main) {
                loadingUtils.close()
            }
        }
    }

    /**
     * 保存文件到本地目录
     */
    private fun saveToLocalDirectory(uri: Uri, filename: String, file: File) {
        val documentUri = DocumentsContract.buildDocumentUriUsingTree(
            uri,
            DocumentsContract.getTreeDocumentId(uri),
        )

        val newFileUri = DocumentsContract.createDocument(
            context.contentResolver,
            documentUri,
            "application/${BackupFileManager.SUFFIX}",
            filename,
        )

        newFileUri?.let { fileUri ->
            context.contentResolver.openOutputStream(fileUri)?.use { outputStream ->
                FileInputStream(file).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
    }
}
