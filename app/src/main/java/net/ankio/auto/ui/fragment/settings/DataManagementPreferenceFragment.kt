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

package net.ankio.auto.ui.fragment.settings

import android.content.Intent
import android.net.Uri
import android.text.InputType
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.preference.Preference
import androidx.preference.PreferenceDataStore
import net.ankio.auto.R
import net.ankio.auto.storage.Logger
import net.ankio.auto.storage.backup.BackupManager
import net.ankio.auto.storage.backup.BackupResult
import net.ankio.auto.storage.backup.RestoreManager
import net.ankio.auto.storage.backup.WebDAVManager
import net.ankio.auto.ui.activity.HomeActivity
import net.ankio.auto.ui.api.BasePreferenceFragment
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.dialog.EditorDialogBuilder
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.utils.CoroutineUtils.withIO
import net.ankio.auto.utils.CoroutineUtils.withMain
import net.ankio.auto.utils.PrefManager
import org.ezbook.server.constant.DefaultData

/**
 * 数据管理设置页面
 * 包含：自动备份、本地备份、WebDAV备份
 */
class DataManagementPreferenceFragment : BasePreferenceFragment() {

    private lateinit var backupManager: BackupManager
    private lateinit var restoreManager: RestoreManager
    private var isBackupTriggered = false

    // 文件选择器
    private val restoreFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            launch {
                val result = restoreManager.restoreFromLocal(it)
                when (result) {
                    is BackupResult.Success -> {
                        ToastUtils.info(getString(R.string.restore_success))
                    }

                    is BackupResult.Failure -> {
                        ToastUtils.error(result.message)
                    }
                }
            }
        }
    }

    // 文件夹选择器
    private val backupFolderLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            saveBackupPath(uri)
            updatePreferenceDisplays()
            if (isBackupTriggered) {
                isBackupTriggered = false
                launch {
                    val result = backupManager.createLocalBackup()
                    when (result) {
                        is BackupResult.Success -> {
                            ToastUtils.info(getString(R.string.backup_success))
                        }

                        is BackupResult.Failure -> {
                            ToastUtils.error(result.message)
                        }
                    }
                }
            }
        }
    }

    override fun getTitleRes(): Int = R.string.setting_title_data_management

    override fun getPreferencesRes(): Int = R.xml.settings_data_management

    override fun createDataStore(): PreferenceDataStore = DataManagementDataStore()

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        backupManager = BackupManager(requireContext())
        restoreManager = RestoreManager(requireContext())
    }

    /**
     * 设置自定义弹窗处理
     */
    override fun setupPreferences() {
        super.setupPreferences()

        // 备份相关
        setPreferenceClickListener("backupToLocal") { performLocalBackup() }
        setPreferenceClickListener("restoreFromLocal") { restoreFileLauncher.launch(arrayOf("*/*")) }
        setPreferenceClickListener("setting_local_backup_path") { backupFolderLauncher.launch(null) }
        setPreferenceClickListener("backupToWebdav") { performWebDAVBackup() }
        setPreferenceClickListener("restoreFromWebdav") { performWebDAVRestore() }

        // WebDAV配置
        setupWebDAVConfigPreferences()

        // 备份保留数量设置
        setPreferenceClickListener("setting_backup_keep_count") {
            showBackupKeepCountDialog()
        }

        // 更新偏好显示
        updatePreferenceDisplays()
    }

    override fun onResume() {
        super.onResume()
        updatePreferenceDisplays()
    }

    // ============ 备份功能 ============

    /**
     * 执行本地备份
     */
    private fun performLocalBackup() {
        if (PrefManager.localBackupPath.isEmpty() || !hasAccessPermission()) {
            isBackupTriggered = true
            backupFolderLauncher.launch(null)
        } else {
            launch {
                val result = backupManager.createLocalBackup()
                when (result) {
                    is BackupResult.Success -> {
                        ToastUtils.info(getString(R.string.backup_success))
                    }

                    is BackupResult.Failure -> {
                        ToastUtils.error(result.message)
                    }
                }
            }
        }
    }

    /**
     * 执行WebDAV备份
     */
    private fun performWebDAVBackup() {
        if (!isWebDAVConfigured()) {
            ToastUtils.error(getString(R.string.setting_webdav_config_incomplete))
            return
        }

        launch {
            val result = backupManager.createWebDAVBackup()
            when (result) {
                is BackupResult.Success -> {
                    ToastUtils.info(getString(R.string.backup_success))
                }

                is BackupResult.Failure -> {
                    ToastUtils.error(result.message)
                }
            }
        }
    }

    /**
     * 执行WebDAV恢复
     */
    private fun performWebDAVRestore() {
        if (!isWebDAVConfigured()) {
            ToastUtils.error(getString(R.string.setting_webdav_config_incomplete))
            return
        }

        launch {
            try {
                val webDAVManager = WebDAVManager()
                val listResult = webDAVManager.listLatest()

                when {
                    listResult.isFailure -> {
                        // 获取列表失败，显示详细错误信息
                        val exception = listResult.exceptionOrNull()
                        val errorMsg = when {
                            exception is net.ankio.auto.http.RequestsUtils.HttpException -> {
                                when (exception.code) {
                                    401, 403 -> getString(
                                        R.string.webdav_error_auth,
                                        exception.code
                                    )

                                    404 -> getString(
                                        R.string.webdav_error_path_not_found,
                                        exception.code
                                    )

                                    else -> getString(
                                        R.string.webdav_error_list_failed,
                                        exception.code,
                                        exception.message ?: ""
                                    )
                                }
                            }

                            exception?.message?.contains("connect", ignoreCase = true) == true ||
                                    exception?.message?.contains(
                                        "timeout",
                                        ignoreCase = true
                                    ) == true ->
                                getString(R.string.webdav_error_connect_restore)

                            else -> getString(
                                R.string.webdav_error_list_failed_simple,
                                exception?.message ?: getString(R.string.unknown_error)
                            )
                        }
                        ToastUtils.error(errorMsg)
                        Logger.e("获取WebDAV备份列表失败", exception)
                    }

                    listResult.getOrNull() != null -> {
                        // 找到备份文件，执行恢复
                        val latestBackup = listResult.getOrNull()!!
                        val activity = requireActivity() as HomeActivity
                        val result = restoreManager.restoreFromWebDAV(activity, latestBackup)
                        when (result) {
                            is BackupResult.Success -> {
                                ToastUtils.info(getString(R.string.restore_success))
                            }

                            is BackupResult.Failure -> {
                                ToastUtils.error(result.message)
                            }
                        }
                    }

                    else -> {
                        // 没有找到备份文件
                        ToastUtils.error(getString(R.string.backup_not_found))
                    }
                }
            } catch (e: Exception) {
                ToastUtils.error(
                    getString(
                        R.string.webdav_error_list_failed_simple,
                        e.message ?: getString(R.string.unknown_error)
                    )
                )
                Logger.e("WebDAV恢复失败", e)
            }
        }
    }

    /**
     * 设置WebDAV配置项
     */
    private fun setupWebDAVConfigPreferences() {
        val configs = mapOf(
            "setting_webdav_url" to Triple(
                R.string.setting_webdav_url,
                "https://dav.jianguoyun.com/dav/",
                false
            ),
            "setting_webdav_user" to Triple(R.string.setting_webdav_user, "", false),
            "setting_webdav_password" to Triple(R.string.setting_webdav_password, "", true)
        )

        configs.forEach { (key, config) ->
            setPreferenceClickListener(key) {
                showEditDialog(
                    title = getString(config.first),
                    currentValue = getWebDAVConfigValue(key),
                    hint = config.second,
                    isPassword = config.third
                ) { newValue ->
                    setWebDAVConfigValue(key, newValue)
                    updatePreferenceDisplays()
                }
            }
        }
    }

    /**
     * 显示编辑对话框
     */
    private fun showEditDialog(
        title: String,
        currentValue: String,
        hint: String,
        isPassword: Boolean = false,
        onConfirm: (String) -> Unit
    ) {
        BaseSheetDialog.create<EditorDialogBuilder>(requireContext())
            .setTitle(title)
            .setMessage(currentValue.ifEmpty { hint })
            .setInputType(
                if (isPassword) {
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                } else {
                    InputType.TYPE_CLASS_TEXT
                }
            )
            .setEditorPositiveButton(R.string.sure_msg) { result ->
                onConfirm(result)
            }
            .setNegativeButton(R.string.cancel_msg, null)
            .show()
    }

    /**
     * 显示备份保留数量设置对话框
     */
    private fun showBackupKeepCountDialog() {
        val currentValue = PrefManager.backupKeepCount.toString()
        BaseSheetDialog.create<EditorDialogBuilder>(requireContext())
            .setTitle(getString(R.string.setting_backup_keep_count))
            .setMessage(currentValue)
            .setInputType(InputType.TYPE_CLASS_NUMBER)
            .setEditorPositiveButton(R.string.sure_msg) { result ->
                try {
                    val count = result.toInt().coerceIn(1, 100) // 限制在1-100之间
                    PrefManager.backupKeepCount = count
                    updatePreferenceDisplays()
                    ToastUtils.info(getString(R.string.setting_backup_keep_count_updated, count))
                } catch (e: NumberFormatException) {
                    ToastUtils.error(getString(R.string.setting_backup_keep_count_invalid))
                }
            }
            .setNegativeButton(R.string.cancel_msg, null)
            .show()
    }

    /**
     * 保存备份路径
     */
    private fun saveBackupPath(uri: Uri) {
        PrefManager.localBackupPath = uri.toString()
        val takeFlags =
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        requireContext().contentResolver.takePersistableUriPermission(uri, takeFlags)
    }

    /**
     * 检查访问权限
     */
    private fun hasAccessPermission(): Boolean {
        val persistedUriPermissions = requireContext().contentResolver.persistedUriPermissions
        val targetUri =
            PrefManager.localBackupPath.ifEmpty { DefaultData.LOCAL_BACKUP_PATH }.toUri()
        return persistedUriPermissions.any { permission ->
            permission.uri == targetUri && permission.isReadPermission && permission.isWritePermission
        }
    }

    /**
     * 检查WebDAV是否已配置
     */
    private fun isWebDAVConfigured(): Boolean {
        return PrefManager.webdavUrl.isNotBlank() &&
                PrefManager.webdavUser.isNotBlank() &&
                PrefManager.webdavPassword.isNotBlank()
    }

    // ============ 显示更新 ============

    /**
     * 更新所有preference的显示
     */
    private fun updatePreferenceDisplays() {
        // 更新备份路径显示
        findPreference<Preference>("setting_local_backup_path")?.apply {
            summary = if (PrefManager.localBackupPath.isEmpty()) {
                getString(R.string.setting_local_backup_path_summary)
            } else {
                PrefManager.localBackupPath.toUri().lastPathSegment ?: PrefManager.localBackupPath
            }
        }

        // 更新备份保留数量显示
        findPreference<Preference>("setting_backup_keep_count")?.apply {
            summary =
                getString(R.string.setting_backup_keep_count_summary, PrefManager.backupKeepCount)
        }

        // 更新WebDAV配置显示
        findPreference<Preference>("setting_webdav_url")?.updateWebDAVConfigSummary(
            PrefManager.webdavUrl, R.string.setting_webdav_url_summary
        )
        findPreference<Preference>("setting_webdav_user")?.updateWebDAVConfigSummary(
            PrefManager.webdavUser, R.string.setting_webdav_user_summary
        )
        findPreference<Preference>("setting_webdav_password")?.apply {
            summary = if (PrefManager.webdavPassword.isNotEmpty()) "••••••••"
            else getString(R.string.setting_webdav_password_summary)
        }
    }

    /**
     * 更新WebDAV配置项的摘要
     */
    private fun Preference.updateWebDAVConfigSummary(value: String, defaultRes: Int) {
        summary = value.ifEmpty { context.getString(defaultRes) }
    }

    /**
     * 获取WebDAV配置值
     */
    private fun getWebDAVConfigValue(key: String): String = when (key) {
        "setting_webdav_url" -> PrefManager.webdavUrl
        "setting_webdav_user" -> PrefManager.webdavUser
        "setting_webdav_password" -> PrefManager.webdavPassword
        else -> ""
    }

    /**
     * 设置WebDAV配置值
     */
    private fun setWebDAVConfigValue(key: String, value: String) {
        when (key) {
            "setting_webdav_url" -> PrefManager.webdavUrl = value
            "setting_webdav_user" -> PrefManager.webdavUser = value
            "setting_webdav_password" -> PrefManager.webdavPassword = value
        }
    }

    // ============ 辅助方法 ============

    /**
     * 设置Preference点击监听器
     */
    private fun setPreferenceClickListener(key: String, action: () -> Unit) {
        findPreference<Preference>(key)?.setOnPreferenceClickListener {
            action()
            true
        }
    }

    /**
     * 数据管理设置数据存储类
     */
    class DataManagementDataStore : PreferenceDataStore() {
        override fun getBoolean(key: String?, defValue: Boolean): Boolean {
            return when (key) {
                "auto_backup" -> PrefManager.autoBackup
                "setting_use_webdav" -> PrefManager.useWebdav
                else -> defValue
            }
        }

        override fun putBoolean(key: String?, value: Boolean) {
            when (key) {
                "auto_backup" -> PrefManager.autoBackup = value
                "setting_use_webdav" -> PrefManager.useWebdav = value
            }
        }
    }
}

