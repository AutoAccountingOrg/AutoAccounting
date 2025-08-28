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
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.BuildConfig
import net.ankio.auto.R
import net.ankio.auto.exceptions.RestoreBackupException
import net.ankio.auto.http.RequestsUtils
import net.ankio.auto.storage.Logger
import net.ankio.auto.storage.ZipUtils
import java.io.File
import java.io.FileOutputStream

/**
 * 备份文件管理器，负责打包和解包备份数据
 */
class BackupFileManager(private val context: Context) {

    companion object {
        const val SUFFIX = "pk"
        const val SUPPORT_VERSION = 202
    }

    /**
     * 打包数据文件
     * @param filename 目标文件名
     */
    suspend fun packData(filename: String) = withContext(Dispatchers.IO) {
        val backupDir = prepareBackupDirectory()

        // 下载数据库文件
        downloadDatabase(backupDir)

        // 创建索引文件
        createIndexFile(backupDir)

        // 压缩所有文件
        ZipUtils.zipAll(backupDir, filename)
    }

    /**
     * 解压备份文件
     * @param file 备份文件
     */
    suspend fun unpackData(file: File) = withContext(Dispatchers.IO) {
        val backupDir = prepareBackupDirectory()

        // 解压文件
        ZipUtils.unzip(file.absolutePath, backupDir.absolutePath) {
            Logger.i("Unzip progress: $it")
        }
        file.delete()

        // 验证备份文件
        validateBackup(backupDir)

        // 恢复数据库
        restoreDatabase(backupDir)
    }

    /**
     * 准备备份目录
     */
    private fun prepareBackupDirectory(): File {
        val backupDir = File(context.filesDir, "backup")
        if (backupDir.exists()) {
            backupDir.deleteRecursively()
        }
        backupDir.mkdirs()
        return backupDir
    }

    /**
     * 下载数据库文件
     */
    private suspend fun downloadDatabase(backupDir: File) {
        val requestUtils = RequestsUtils()
        val dbFile = File(backupDir, "auto.db")
        val result = requestUtils.download("http://127.0.0.1:52045/db/export", dbFile)

        Logger.i("Download database: $result")
        if (!result) {
            throw RestoreBackupException(context.getString(R.string.backup_error))
        }
    }

    /**
     * 创建索引文件
     */
    private fun createIndexFile(backupDir: File) {
        val indexData = mapOf(
            "version" to SUPPORT_VERSION,
            "versionName" to BuildConfig.VERSION_NAME,
            "packageName" to BuildConfig.APPLICATION_ID,
            "packageVersion" to BuildConfig.VERSION_CODE,
        )

        val json = Gson().toJson(indexData)
        val indexFile = File(backupDir, "auto.index")
        FileOutputStream(indexFile).use { outputStream ->
            outputStream.write(json.toByteArray())
        }
    }

    /**
     * 验证备份文件
     */
    private fun validateBackup(backupDir: File) {
        val indexFile = File(backupDir, "auto.index")
        val json = indexFile.readText()
        indexFile.delete()

        val backupInfo = Gson().fromJson(json, JsonObject::class.java)
        Logger.i("Backup Data: $backupInfo")

        // 检查版本兼容性
        val version = backupInfo.get("version").asInt
        if (version < SUPPORT_VERSION) {
            throw RestoreBackupException(
                context.getString(
                    R.string.unsupport_backup,
                    backupInfo["versionName"],
                )
            )
        }

        // 检查包名
        val packageName = backupInfo["packageName"].asString
        if (packageName != BuildConfig.APPLICATION_ID && !BuildConfig.DEBUG) {
            throw RestoreBackupException(context.getString(R.string.unspport_package_backup))
        }
    }

    /**
     * 恢复数据库
     */
    private suspend fun restoreDatabase(backupDir: File) {
        val dbFile = File(backupDir, "auto.db")
        val requestUtils = RequestsUtils()
        val result = requestUtils.upload("http://127.0.0.1:52045/db/import", dbFile)
        Logger.i("Upload result: $result")
    }
}
