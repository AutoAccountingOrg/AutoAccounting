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
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.ui.activity.HomeActivity
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.utils.PrefManager
import org.ezbook.server.constant.DefaultData
import androidx.core.net.toUri
import net.ankio.auto.storage.Logger

/**
 * 备份权限管理器，负责处理文件访问权限和文件选择
 */
object BackupPermissionManager {

    private var backupLauncher: ActivityResultLauncher<Uri?>? = null
    private var restoreLauncher: ActivityResultLauncher<Array<String>>? = null

    /**
     * 初始化权限请求启动器
     * @param activity 主活动实例
     */
    fun initRequestPermission(activity: HomeActivity) {
        initBackupLauncher(activity)
        initRestoreLauncher(activity)
    }

    /**
     * 初始化备份权限启动器
     */
    private fun initBackupLauncher(activity: HomeActivity) {
        backupLauncher = activity.registerForActivityResult(
            ActivityResultContracts.OpenDocumentTree()
        ) { uri ->
            uri?.let {
                grantPersistablePermission(activity, it)
                PrefManager.localBackupPath = it.toString()
            }
        }
    }

    /**
     * 初始化恢复权限启动器
     */
    private fun initRestoreLauncher(activity: HomeActivity) {
        restoreLauncher = activity.registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            uri?.let {
                handleRestoreFile(activity, it)
            }
        }
    }

    /**
     * 授予持久化权限
     */
    private fun grantPersistablePermission(activity: HomeActivity, uri: Uri) {
        val takeFlags =
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        activity.contentResolver.takePersistableUriPermission(uri, takeFlags)
    }

    /**
     * 处理恢复文件
     */
    private fun handleRestoreFile(activity: HomeActivity, uri: Uri) {
        // 检查文件扩展名
        val fileExtension = activity.contentResolver.getType(uri)
            ?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }

        if (!BackupFileManager.SUFFIX.equals(fileExtension, true)) {
            ToastUtils.info(R.string.backup_error)
            return
        }

        // 执行恢复操作
        activity.lifecycleScope.launch {
            val loadingUtils = LoadingUtils(activity)
            loadingUtils.show(R.string.restore_loading)

            val restoreManager = RestoreManager(activity)

            BackupResultHandler.handleOperation(
                context = activity,
                operation = "文件恢复",
                onSuccess = {
                    loadingUtils.close()
                },
                onError = {
                    loadingUtils.close()
                }
            ) {
                restoreManager.restoreFromLocal(uri)
            }
        }
    }

    /**
     * 请求备份权限
     * @param activity 主活动实例
     */
    fun requestBackupPermission(activity: HomeActivity) {
        runCatching {
            backupLauncher?.launch(null)
        }.onFailure {
            Logger.e("Failed to request backup permission", it)
        }
    }

    /**
     * 请求恢复文件
     * @param activity 主活动实例
     */
    fun requestRestore(activity: HomeActivity) {
        runCatching {
            restoreLauncher?.launch(arrayOf("*/*"))
        }.onFailure {
            Logger.e("Failed to request restore permission", it)
        }
    }

    /**
     * 检查是否有访问权限
     * @param context 上下文
     * @return 是否有权限
     */
    fun hasAccessPermission(context: Context): Boolean {
        val persistedUriPermissions = context.contentResolver.persistedUriPermissions
        val targetUri =
            PrefManager.localBackupPath.ifEmpty { DefaultData.LOCAL_BACKUP_PATH }.toUri()

        return persistedUriPermissions.any { permission ->
            permission.uri == targetUri &&
                    permission.isReadPermission &&
                    permission.isWritePermission
        }
    }
}
