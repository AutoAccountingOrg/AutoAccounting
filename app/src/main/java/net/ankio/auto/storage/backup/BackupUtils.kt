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
import net.ankio.auto.R
import net.ankio.auto.storage.Constants
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.activity.HomeActivity
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.utils.PrefManager

/**
 * 备份工具类 - 简化后的主入口
 * 负责协调各个备份组件的工作
 */
class BackupUtils(private val context: Context) {

    private val backupManager = BackupManager(context)
    private val restoreManager = RestoreManager(context)

    companion object {

        /**
         * 初始化权限请求
         * @param activity 主活动实例
         */
        fun initRequestPermission(activity: HomeActivity) {
            BackupPermissionManager.initRequestPermission(activity)
        }

        /**
         * 请求备份权限
         * @param activity 主活动实例
         */
        fun requestPermission(activity: HomeActivity) {
            BackupPermissionManager.requestBackupPermission(activity)
        }

        /**
         * 请求恢复文件
         * @param activity 主活动实例
         */
        fun requestRestore(activity: HomeActivity) {
            BackupPermissionManager.requestRestore(activity)
        }

        /**
         * 检查是否有访问权限
         * @param context 上下文
         * @return 是否有权限
         */
        fun hasAccessPermission(context: Context): Boolean {
            return BackupPermissionManager.hasAccessPermission(context)
        }

        /**
         * 自动备份
         * @param activity 主活动实例
         */
        suspend fun autoBackup(activity: HomeActivity) {
            if (!PrefManager.autoBackup) return

            if (!shouldBackup()) return

            ToastUtils.info(R.string.backup_loading)
            val backupUtils = BackupUtils(activity)

            BackupResultHandler.handleOperation(
                context = activity,
                operation = "自动备份",
                onSuccess = {
                    PrefManager.lastBackupTime = System.currentTimeMillis()
                    BackupResultHandler.showSuccess(R.string.backup_success)
                },
                onError = {
                    Logger.e("自动备份失败", it)
                    BackupResultHandler.showError(R.string.backup_error)
                }
            ) {
                if (PrefManager.useWebdav) {
                    backupUtils.backupManager.createWebDAVBackup(activity)
                } else {
                    backupUtils.backupManager.createLocalBackup()
                }
            }
        }

        /**
         * 检查是否需要备份
         */
        private fun shouldBackup(): Boolean {
            val lastBackupTime = PrefManager.lastBackupTime
            val currentTime = System.currentTimeMillis()
            return (currentTime - lastBackupTime) >= Constants.BACKUP_TIME
        }
    }


    /**
     * 本地备份
     */
    suspend fun putLocalBackup() {
        backupManager.createLocalBackup()
    }

    /**
     * WebDAV 备份
     */
    suspend fun putWebdavBackup(activity: HomeActivity) {
        backupManager.createWebDAVBackup(activity)
    }

    /**
     * 从本地恢复备份
     */
    suspend fun getLocalBackup(uri: android.net.Uri) {
        restoreManager.restoreFromLocal(uri)
    }

    /**
     * 从 WebDAV 获取备份
     */
    suspend fun getWebdavBackup(activity: HomeActivity) {
        // TODO: 实现 WebDAV 备份文件选择对话框
        // 目前被注释掉的 BackupSelectorDialog 需要重新实现
        Logger.i("WebDAV 备份文件选择功能待实现")
    }
}
