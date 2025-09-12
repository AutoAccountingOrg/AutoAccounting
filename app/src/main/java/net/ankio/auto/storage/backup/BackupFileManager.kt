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
import kotlinx.coroutines.delay
import net.ankio.auto.http.RequestsUtils
import net.ankio.auto.storage.CacheManager
import net.ankio.auto.storage.Logger
import net.ankio.auto.storage.ZipUtils
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.utils.SystemUtils
import java.io.File
import java.io.FileOutputStream
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * 备份文件管理器，负责打包和解包备份数据
 */
class BackupFileManager(private val context: Context) {

    private val logger = KotlinLogging.logger(this::class.java.name)

    companion object {
        const val SUFFIX = "pk"
        const val SUPPORT_VERSION = 202
    }

    private var loading = runCatching {
        LoadingUtils(context)
    }.getOrNull()

    /**
     * 打包数据文件
     * @param filename 目标文件名
     */
    suspend fun packData(filename: String) = withContext(Dispatchers.IO) {
        try {
            loading?.show(context.getString(R.string.backup_preparing))
            val backupDir = prepareBackupDirectory()

            // 下载数据库文件
            loading?.setText(context.getString(R.string.backup_database))
            downloadDatabase(backupDir)

            // 备份配置文件
            loading?.setText(context.getString(R.string.backup_preferences))
            backupPreferences(backupDir)

            // 创建索引文件
            loading?.setText(context.getString(R.string.backup_creating_index))
            createIndexFile(backupDir)

            // 压缩所有文件
            loading?.setText(context.getString(R.string.backup_compressing))
            ZipUtils.zipAll(backupDir, filename, excludeRootDir = true)

            loading?.close()
        } catch (e: Exception) {
            loading?.close()
            throw e
        }
    }

    /**
     * 解压备份文件
     * @param file 备份文件
     */
    suspend fun unpackData(file: File) = withContext(Dispatchers.IO) {
        try {
            loading?.show(context.getString(R.string.restore_preparing))
            val backupDir = prepareBackupDirectory()

            // 解压文件
            loading?.setText(context.getString(R.string.restore_extracting))
            ZipUtils.unzip(file.absolutePath, backupDir.absolutePath) {
                logger.debug { "解压进度: $it" }
            }
            file.delete()

            // 验证备份文件
            loading?.setText(context.getString(R.string.restore_validating))
            validateBackup(backupDir)

            // 恢复数据库
            loading?.setText(context.getString(R.string.restore_database))
            restoreDatabase(backupDir)

            // 恢复配置文件
            loading?.setText(context.getString(R.string.restore_preferences))
            restorePreferences(backupDir)

            // 清空缓存，确保恢复的数据生效
            loading?.setText(context.getString(R.string.restore_clearing_cache))
            CacheManager.clear()

            // 准备重启应用
            loading?.setText(context.getString(R.string.restore_restarting))
            loading?.close()

            // 延迟后重启应用，确保UI有时间关闭
            delay(1000)
            SystemUtils.restart()
        } catch (e: Exception) {
            loading?.close()
            throw e
        }
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

        if (result.isFailure) {
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
        logger.debug { "备份文件信息: $backupInfo" }

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


    }

    /**
     * 恢复数据库
     */
    private suspend fun restoreDatabase(backupDir: File) {
        val dbFile = File(backupDir, "auto.db")
        val requestUtils = RequestsUtils()
        val result = requestUtils.upload("http://127.0.0.1:52045/db/import", dbFile)
        logger.debug { "数据库导入结果: $result" }
    }

    /**
     * 备份配置文件
     */
    private fun backupPreferences(backupDir: File) {
        try {
            // Android SharedPreferences 文件路径
            val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
            val settingsFile = File(prefsDir, "settings.xml")

            if (settingsFile.exists()) {
                val backupPrefsFile = File(backupDir, "settings.xml")
                settingsFile.copyTo(backupPrefsFile, overwrite = true)
                logger.debug { "配置文件备份完成" }
            } else {
                logger.warn { "配置文件不存在，跳过备份" }
            }
        } catch (e: Exception) {
            logger.warn { "配置文件备份失败: ${e.message}" }
        }
    }

    /**
     * 恢复配置文件
     */
    private fun restorePreferences(backupDir: File) {
        try {
            val backupPrefsFile = File(backupDir, "settings.xml")

            if (backupPrefsFile.exists()) {
                // Android SharedPreferences 文件路径
                val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
                if (!prefsDir.exists()) {
                    prefsDir.mkdirs()
                }

                val settingsFile = File(prefsDir, "settings.xml")
                backupPrefsFile.copyTo(settingsFile, overwrite = true)

                // 清理备份文件
                backupPrefsFile.delete()

                logger.debug { "配置文件恢复完成" }
            } else {
                logger.warn { "备份中无配置文件，跳过恢复" }
            }
        } catch (e: Exception) {
            logger.warn { "配置文件恢复失败: ${e.message}" }
        }
    }
}
