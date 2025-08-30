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
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceDataStore
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.storage.Logger
import net.ankio.auto.storage.backup.BackupManager
import net.ankio.auto.storage.backup.RestoreManager
import net.ankio.auto.storage.backup.WebDAVManager
import net.ankio.auto.ui.activity.HomeActivity
import net.ankio.auto.ui.api.BasePreferenceFragment
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.dialog.EditorDialogBuilder
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.utils.PrefManager
import org.ezbook.server.constant.DefaultData

/**
 * 备份设置页面 - Linus式极简重构
 *
 * 重构原则：
 * 1. 消除重复代码 - 统一WebDAV配置检查
 * 2. 简化测试逻辑 - 去掉过度复杂的测试数据生成
 * 3. 合并相似方法 - 统一preference更新逻辑
 * 4. 减少嵌套层次 - 提取公共方法
 */
class BackupPreferenceFragment : BasePreferenceFragment() {

    private lateinit var backupManager: BackupManager
    private lateinit var restoreManager: RestoreManager
    private var isBackupTriggered = false

    // 文件选择器
    private val restoreFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { lifecycleScope.launch { restoreManager.restoreFromLocal(it) } } }

    // 文件夹选择器
    private val backupFolderLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            saveBackupPath(uri)
            updatePreferenceDisplays()
            if (isBackupTriggered) {
                isBackupTriggered = false
                lifecycleScope.launch { backupManager.createLocalBackup() }
            }
        }
    }

    override fun getTitleRes(): Int = R.string.setting_title_backup
    override fun getPreferencesRes(): Int = R.xml.settings_backup
    override fun createDataStore(): PreferenceDataStore = BackupPreferenceDataStore()

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        backupManager = BackupManager(requireContext())
        restoreManager = RestoreManager(requireContext())
    }

    override fun setupPreferences() {
        super.setupPreferences()

        // 本地备份操作
        setPreferenceClickListener("backupToLocal") { performLocalBackup() }
        setPreferenceClickListener("restoreFromLocal") { restoreFileLauncher.launch(arrayOf("*/*")) }
        setPreferenceClickListener("setting_local_backup_path") { backupFolderLauncher.launch(null) }

        // WebDAV操作
        setPreferenceClickListener("backupToWebdav") { performWebDAVBackup() }
        setPreferenceClickListener("restoreFromWebdav") { performWebDAVRestore() }

        // WebDAV配置
        setupWebDAVConfigPreferences()
        updatePreferenceDisplays()
    }

    /**
     * 设置preference点击监听器 - 消除重复代码
     */
    private fun setPreferenceClickListener(key: String, action: () -> Unit) {
        findPreference<Preference>(key)?.setOnPreferenceClickListener {
            action()
            true
        }
    }

    /**
     * 设置WebDAV配置相关的preference
     */
    private fun setupWebDAVConfigPreferences() {
        val configs = mapOf(
            "setting_webdav_host" to Triple(
                R.string.setting_webdav_url,
                "https://your-webdav-server.com",
                false
            ),
            "setting_webdav_user" to Triple(R.string.setting_webdav_user, "", false),
            "setting_webdav_password" to Triple(R.string.setting_webdav_password, "", true),
            "setting_webdav_path" to Triple(R.string.setting_webdav_path, "/AutoAccounting/", false)
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
     * 检查WebDAV配置是否完整 - 统一检查逻辑
     */
    private fun isWebDAVConfigured(): Boolean {
        return PrefManager.webdavHost.isNotBlank() &&
                PrefManager.webdavUser.isNotBlank() &&
                PrefManager.webdavPassword.isNotBlank()
    }

    /**
     * 执行本地备份
     */
    private fun performLocalBackup() {
        if (PrefManager.localBackupPath.isEmpty() || !hasAccessPermission()) {
            isBackupTriggered = true
            backupFolderLauncher.launch(null)
        } else {
            lifecycleScope.launch { backupManager.createLocalBackup() }
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

        lifecycleScope.launch {
            try {
                backupManager.createWebDAVBackup()
                ToastUtils.info(getString(R.string.backup_success))
            } catch (e: Exception) {
                ToastUtils.error(getString(R.string.backup_error))
                Logger.e("WebDAV备份失败", e)
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

        lifecycleScope.launch {
            try {
                val webDAVManager = WebDAVManager()
                val latestBackup = webDAVManager.listLatest()

                if (latestBackup != null) {
                    val activity = requireActivity() as HomeActivity
                    restoreManager.restoreFromWebDAV(activity, latestBackup)
                } else {
                    ToastUtils.error(getString(R.string.backup_not_found))
                }
            } catch (e: Exception) {
                ToastUtils.error(getString(R.string.backup_error))
                Logger.e("WebDAV恢复失败", e)
            }
        }
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
     * 统一的preference显示更新 - 合并重复逻辑
     */
    private fun updatePreferenceDisplays() {
        // 本地备份路径
        updatePreferenceDisplay(
            "setting_local_backup_path",
            PrefManager.localBackupPath, R.string.setting_local_backup_path_summary
        )

        // WebDAV配置显示
        updatePreferenceDisplay(
            "setting_webdav_host",
            PrefManager.webdavHost, R.string.setting_webdav_url_summary
        )
        updatePreferenceDisplay(
            "setting_webdav_user",
            PrefManager.webdavUser, R.string.setting_webdav_user_summary
        )
        updatePreferenceDisplay(
            "setting_webdav_path",
            PrefManager.webdavPath, R.string.setting_webdav_path_summary
        )

        // 密码特殊处理
        findPreference<Preference>("setting_webdav_password")?.summary =
            if (PrefManager.webdavPassword.isNotEmpty()) "••••••••"
            else getString(R.string.setting_webdav_password_summary)
    }

    /**
     * 更新单个preference显示
     */
    private fun updatePreferenceDisplay(key: String, value: String, defaultRes: Int) {
        findPreference<Preference>(key)?.summary = value.ifEmpty { getString(defaultRes) }
    }

    /**
     * 获取WebDAV配置值
     */
    private fun getWebDAVConfigValue(key: String): String = when (key) {
        "setting_webdav_host" -> PrefManager.webdavHost
        "setting_webdav_user" -> PrefManager.webdavUser
        "setting_webdav_password" -> PrefManager.webdavPassword
        "setting_webdav_path" -> PrefManager.webdavPath
        else -> ""
    }

    /**
     * 设置WebDAV配置值
     */
    private fun setWebDAVConfigValue(key: String, value: String) {
        when (key) {
            "setting_webdav_host" -> PrefManager.webdavHost = value
            "setting_webdav_user" -> PrefManager.webdavUser = value
            "setting_webdav_password" -> PrefManager.webdavPassword = value
            "setting_webdav_path" -> PrefManager.webdavPath = value
        }
    }

    /**
     * 显示编辑对话框 - 简化参数
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
            .setMessage(currentValue)
            .setInputType(
                if (isPassword) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                else InputType.TYPE_CLASS_TEXT
            )
            .setEditorPositiveButton(android.R.string.ok) { result -> onConfirm(result) }
            .setNegativeButton(android.R.string.cancel, null)
            .show(true)
    }

    /**
     * 简化的数据存储类 - 去掉冗余注释
     */
    inner class BackupPreferenceDataStore : PreferenceDataStore() {
        override fun getString(key: String?, defValue: String?): String = when (key) {
            "setting_webdav_host" -> PrefManager.webdavHost
            "setting_webdav_user" -> PrefManager.webdavUser
            "setting_webdav_password" -> PrefManager.webdavPassword
            "setting_webdav_path" -> PrefManager.webdavPath
            "setting_local_backup_path" -> PrefManager.localBackupPath
            else -> defValue ?: ""
        }

        override fun putString(key: String?, value: String?) {
            val safeValue = value ?: ""
            when (key) {
                "setting_webdav_host" -> PrefManager.webdavHost = safeValue
                "setting_webdav_user" -> PrefManager.webdavUser = safeValue
                "setting_webdav_password" -> PrefManager.webdavPassword = safeValue
                "setting_webdav_path" -> PrefManager.webdavPath = safeValue
                "setting_local_backup_path" -> PrefManager.localBackupPath = safeValue
            }
        }

        override fun getBoolean(key: String?, defValue: Boolean): Boolean = when (key) {
            "auto_backup" -> PrefManager.autoBackup
            "setting_use_webdav" -> PrefManager.useWebdav
            else -> defValue
        }

        override fun putBoolean(key: String?, value: Boolean) {
            when (key) {
                "auto_backup" -> PrefManager.autoBackup = value
                "setting_use_webdav" -> PrefManager.useWebdav = value
            }
        }
    }
}