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
        try {
            requestUtils.mkcol(backupUrl)
            val (code, _) = requestUtils.put("$backupUrl/$filename", file)
            val success = code in 200..299

            if (success) {
                Logger.i("上传成功: $filename")
                // 上传成功后自动清理旧备份，保持最多10个文件
                cleanupOldBackups()
            } else {
                Logger.e("上传失败: HTTP $code")
            }

            success
        } catch (e: Exception) {
            Logger.e("上传异常: ${e.message}", e)
            false
        }
    }

    /**
     * 下载文件，只管下载，不管其他
     */
    suspend fun download(filename: String, targetFile: File): Boolean =
        withContext(Dispatchers.IO) {
        try {
            requestUtils.download("$backupUrl/$filename", targetFile).also { success ->
                if (success) {
                    Logger.i("下载成功: $filename")
                } else {
                    Logger.e("下载失败: $filename")
                }
            }
        } catch (e: Exception) {
            Logger.e("下载异常: ${e.message}", e)
            false
        }
    }

    /**
     * 列举最近一个备份文件
     * @return 最新的备份文件名，没有则返回null
     */
    suspend fun listLatest(): String? = withContext(Dispatchers.IO) {
        try {
            val (code, files) = requestUtils.dir(backupUrl)
            if (code in 200..299) {
                files.filter { it.endsWith("." + BackupFileManager.SUFFIX) }
                    .maxByOrNull { it }
                // 移除频繁的日志记录，只在异常时记录
            } else {
                Logger.e("列举失败: HTTP $code")
                null
            }
        } catch (e: Exception) {
            Logger.e("列举异常: ${e.message}", e)
            null
        }
    }

    /**
     * 清理旧备份，只保留最新的10个文件
     * Linus式简化：上传后自动清理，用户无感知
     */
    private suspend fun cleanupOldBackups() = withContext(Dispatchers.IO) {
        try {
            val (code, files) = requestUtils.dir(backupUrl)
            if (code in 200..299) {
                val backupFiles = files.filter { it.endsWith(".pk") }
                    .sortedDescending() // 按文件名降序，最新的在前

                if (backupFiles.size > 10) {
                    val filesToDelete = backupFiles.drop(10) // 跳过前10个，删除其余的
                    Logger.i("清理WebDAV备份：删除${filesToDelete.size}个旧文件，保留${backupFiles.size - filesToDelete.size}个")

                    var deletedCount = 0
                    filesToDelete.forEach { filename ->
                        if (deleteFile(filename)) deletedCount++
                    }

                    Logger.i("清理完成：成功删除${deletedCount}个文件")
                }
                // 移除"无需清理"的日志，减少噪音
            }
        } catch (e: Exception) {
            Logger.e("清理备份异常: ${e.message}", e)
        }
    }

    /**
     * 删除单个文件
     * @return 是否删除成功
     */
    private suspend fun deleteFile(filename: String): Boolean {
        return try {
            val (code, _) = requestUtils.delete("$backupUrl/$filename")
            if (code in 200..299) {
                // 移除单个文件删除的成功日志，减少噪音
                true
            } else {
                Logger.e("删除失败: $filename (HTTP $code)")
                false
            }
        } catch (e: Exception) {
            Logger.e("删除文件异常: $filename - ${e.message}", e)
            false
        }
    }
}
