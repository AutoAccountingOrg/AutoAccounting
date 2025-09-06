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
import android.provider.DocumentsContract
import androidx.core.net.toUri
import net.ankio.auto.App
import net.ankio.auto.BuildConfig
import net.ankio.auto.R
import net.ankio.auto.autoApp
import net.ankio.auto.exceptions.PermissionException
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.utils.CoroutineUtils
import net.ankio.auto.utils.CoroutineUtils.withIO
import net.ankio.auto.utils.CoroutineUtils.withMain
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.utils.Throttle
import java.io.File
import java.io.FileInputStream

/**
 * 备份管理器 - Linus式极简重构
 *
 * 设计原则：
 * 1. 消除过度抽象 - 直接处理备份逻辑，不绕弯子
 * 2. 统一错误处理 - 所有异常在这里统一处理
 * 3. 简化权限检查 - 权限问题直接引导用户解决
 * 4. 清晰的职责 - 只负责备份创建，不管UI交互
 *
 * 功能说明：
 * - 本地备份：检查权限 → 打包数据 → 保存到用户选择的目录
 * - WebDAV备份：打包数据 → 上传到WebDAV服务器
 * - 权限管理：自动检查和请求必要的存储权限
 */
class BackupManager(private val context: Context) {

    private val fileManager = BackupFileManager(context)
    private val webDAVManager = WebDAVManager()

    /**
     * 生成备份文件名 - 包含版本和时间戳
     */
    private fun generateBackupFilename(): String {
        return "backup_${BuildConfig.VERSION_NAME}(${BackupFileManager.SUPPORT_VERSION})_${System.currentTimeMillis()}.${BackupFileManager.SUFFIX}"
    }

    /**
     * 本地备份 - Linus式简化实现
     *
     * 设计原则：
     * 1. 快速失败 - 权限检查在最前面
     * 2. 清晰流程 - 检查权限 → 打包 → 保存，一目了然
     * 3. 统一异常 - 所有问题都抛出明确的异常
     */
    suspend fun createLocalBackup() = withIO {
        // 1. 权限检查 - 快速失败
        if (!hasValidBackupPath()) {
            throw PermissionException("请先选择备份保存路径")
        }

        // 2. 数据打包
        val filename = generateBackupFilename()
        val backupFile = File(context.cacheDir, filename)

        try {
            Logger.i("开始本地备份")

            // 打包数据
            fileManager.packData(backupFile.absolutePath)

            // 保存到用户指定目录
            saveToUserDirectory(backupFile, filename)

            // 清理临时文件
            backupFile.delete()

            Logger.i("本地备份完成")
            ToastUtils.info(R.string.backup_success)

        } catch (e: Exception) {
            // 清理临时文件
            backupFile.delete()
            Logger.e("本地备份失败", e)
            throw e
        }
    }

    /**
     * WebDAV备份 - 使用Context而不是Activity
     */
    suspend fun createWebDAVBackup() = withIO {
        val filename = generateBackupFilename()
        val backupFile = File(this@BackupManager.context.cacheDir, filename)
        var loading: LoadingUtils? = null
        withMain {
            loading = runCatching { LoadingUtils(context) }.getOrNull()
        }

        try {
            Logger.i("开始WebDAV备份")
            
            // 显示打包进度
            withMain {
                loading?.show(R.string.backup_pack)
            }

            // 打包数据
            fileManager.packData(backupFile.absolutePath)

            // 上传到WebDAV
            withMain {
                loading?.setText(R.string.backup_webdav)
            }

            webDAVManager.upload(backupFile, filename).getOrThrow()

            Logger.i("WebDAV备份完成")

        } catch (e: Exception) {
            Logger.e("WebDAV备份失败", e)
            ToastUtils.error(e.message ?: "")
        } finally {
            // 清理资源
            backupFile.delete()
            withMain {
                loading?.close()
            }
        }
    }

    /**
     * 检查是否有有效的备份路径
     */
    private fun hasValidBackupPath(): Boolean {
        val backupPath = PrefManager.localBackupPath
        if (backupPath.isEmpty()) return false

        return try {
            val uri = backupPath.toUri()
            val permissions = context.contentResolver.persistedUriPermissions
            permissions.any { permission ->
                permission.uri == uri &&
                        permission.isReadPermission &&
                        permission.isWritePermission
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 保存备份文件到用户选择的目录
     */
    private fun saveToUserDirectory(backupFile: File, filename: String) {
        val uri = PrefManager.localBackupPath.toUri()
        
        val documentUri = DocumentsContract.buildDocumentUriUsingTree(
            uri,
            DocumentsContract.getTreeDocumentId(uri)
        )

        val newFileUri = DocumentsContract.createDocument(
            context.contentResolver,
            documentUri,
            "application/${BackupFileManager.SUFFIX}",
            filename
        ) ?: throw RuntimeException("无法创建备份文件")

        // 写入文件内容
        context.contentResolver.openOutputStream(newFileUri)?.use { outputStream ->
            FileInputStream(backupFile).use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        } ?: throw RuntimeException("无法写入备份文件")
    }

    companion object {

        /**
         * 自动备份节流器 - Linus式极简优化
         * 5分钟内最多执行一次，防止频繁进入后台导致的重复备份
         * 使用持久化模式，确保App重启后节流状态保持
         */
        private val autoBackupThrottle = Throttle.asFunction(
            intervalMs = 5 * 60 * 1000L, // 5分钟节流间隔
            persistKey = "auto_backup" // 持久化键名
        ) {
            // 实际执行备份的代码块
            performAutoBackup()
        }

        /**
         * 自动备份入口 - 带节流控制
         * 这是外部调用的统一入口，内部使用节流器控制执行频率
         */
        fun autoBackup() {
            autoBackupThrottle()
        }

        /**
         * 实际执行自动备份的方法 - 核心逻辑
         * 由节流器控制调用频率，避免重复执行
         */
        private fun performAutoBackup() {
            if (!PrefManager.autoBackup) {
                Logger.d("自动备份已关闭")
                return
            }


            Logger.i("开始自动备份")

            // 在协程中执行备份操作
            App.launch {
                try {
                    val context = autoApp

                    if (PrefManager.useWebdav) {
                        BackupManager(context).createWebDAVBackup()
                    } else {
                        BackupManager(context).createLocalBackup()
                    }

                    // 更新最后备份时间
                    PrefManager.lastBackupTime = System.currentTimeMillis()
                    Logger.i("自动备份完成")

                } catch (e: Exception) {
                    Logger.e("自动备份失败", e)
                }
            }
        }


    }

}