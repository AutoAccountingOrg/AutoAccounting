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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.http.RequestsUtils
import net.ankio.auto.storage.Logger
import net.ankio.auto.utils.PrefManager
import org.ezbook.server.tools.runCatchingExceptCancel
import okhttp3.Credentials
import java.io.File

/**
 * WebDAV 管理器 - 真正的Linus式简化
 * 只做一件事：上传/下载文件，不管UI，不管Toast
 */
class WebDAVManager {

    private val requestUtils = RequestsUtils()
    private val backupUrl: String

    init {
        // 直接计算最终URL，消除所有中间步骤
        val host = PrefManager.webdavHost.trim('/')
        val path = PrefManager.webdavPath.trim('/')
        val base = if (path.isNotEmpty()) "$host/$path" else host
        backupUrl = "$base/AutoAccounting"

        // 设置认证
        requestUtils.addHeader(
            "Authorization",
            Credentials.basic(PrefManager.webdavUser, PrefManager.webdavPassword)
        )
    }

    /**
     * 上传文件，上传成功后自动清理旧备份
     */
    suspend fun upload(file: File, filename: String): Boolean = withContext(Dispatchers.IO) {

        return@withContext runCatchingExceptCancel {
            Logger.d("备份路径: $backupUrl")
            requestUtils.mkcol(backupUrl).getOrNull()
            requestUtils.put("$backupUrl/$filename", file).getOrThrow()
            Logger.d("上传成功: $filename")
            // 上传成功后自动清理旧备份，保持最多10个文件
            cleanupOldBackups()
            true
        }.getOrElse {
            Logger.e("上传失败: ${it.message}", it)
            false
        }
    }

    /**
     * 下载文件，只管下载，不管其他
     */
    suspend fun download(filename: String, targetFile: File): Boolean =
        withContext(Dispatchers.IO) {

            return@withContext runCatchingExceptCancel {
                requestUtils.download("$backupUrl/$filename", targetFile).getOrThrow()
                Logger.d("下载成功: $filename")
                true
            }.getOrElse {
                Logger.e("下载失败: ${it.message}", it)
            false
        }
    }

    /**
     * 列举最近一个备份文件
     * @return 最新的备份文件名，没有则返回null
     */
    suspend fun listLatest(): String? = withContext(Dispatchers.IO) {

        return@withContext runCatchingExceptCancel {
            val files = requestUtils.dir(backupUrl).getOrThrow()
            files.filter { it.endsWith("." + BackupFileManager.SUFFIX) }
                .maxByOrNull { it }
        }.getOrElse {
            Logger.w("获取最新备份失败: ${it.message}")
            null
        }
    }

    /**
     * 清理旧备份，只保留最新的10个文件
     * Linus式简化：上传后自动清理，用户无感知
     */
    private suspend fun cleanupOldBackups() = withContext(Dispatchers.IO) {

        runCatchingExceptCancel {
            val files = requestUtils.dir(backupUrl).getOrThrow()
            val backupFiles = files.filter { it.endsWith(".pk") }
                .sortedDescending()
            if (backupFiles.size > 10) {
                val filesToDelete = backupFiles.drop(10)
                Logger.d("清理WebDAV备份: 删除${filesToDelete.size}个旧文件")
                var deletedCount = 0
                filesToDelete.forEach { filename -> if (deleteFile(filename)) deletedCount++ }
                Logger.d("清理完成: 成功删除${deletedCount}个文件")
            }
        }.getOrElse {
            Logger.w("清理旧备份失败: ${it.message}")
        }
    }

    /**
     * 删除单个文件
     * @return 是否删除成功
     */
    private suspend fun deleteFile(filename: String): Boolean {

        return runCatchingExceptCancel {
            requestUtils.delete("$backupUrl/$filename").getOrThrow()
            true
        }.getOrElse {
            Logger.w("删除文件失败: $filename - ${it.message}")
            false
        }
    }
}
