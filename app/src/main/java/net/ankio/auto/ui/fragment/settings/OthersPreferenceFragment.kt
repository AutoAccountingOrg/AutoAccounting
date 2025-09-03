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

import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceDataStore
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.http.api.DatabaseAPI
import net.ankio.auto.ui.api.BasePreferenceFragment
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.dialog.BottomSheetDialogBuilder
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.utils.CoroutineUtils.withIO
import net.ankio.auto.utils.SystemUtils

/**
 * 其他设置页面 - Linus式极简设计
 *
 * 设计原则：
 * 1. 单一职责 - 只负责系统级设置和更新配置
 * 2. 统一架构 - 继承BasePreferenceFragment保持一致性
 * 3. 简洁实现 - 消除冗余的布局操作
 * 4. 向后兼容 - 保持所有原有功能不变
 *
 * 功能说明：
 * - 系统设置（调试模式、错误报告）
 * - 更新设置（应用更新、规则更新、更新渠道）
 * - 危险操作（数据库清理）
 */
class OthersPreferenceFragment : BasePreferenceFragment() {

    override fun getTitleRes(): Int = R.string.setting_title_others

    override fun getPreferencesRes(): Int = R.xml.settings_others

    override fun createDataStore(): PreferenceDataStore = OthersPreferenceDataStore()

    /**
     * 设置自定义行为处理
     */
    override fun setupPreferences() {
        super.setupPreferences()

        // 清除数据库的特殊处理
        findPreference<androidx.preference.Preference>("clearDatabase")?.setOnPreferenceClickListener {
            showClearDatabaseDialog()
            true
        }

        // 设置更新渠道显示的摘要
        setupAppChannelPreference()
    }

    /**
     * 其他设置专用的数据存储类
     */
    class OthersPreferenceDataStore : PreferenceDataStore() {
        override fun getString(key: String?, defValue: String?): String {
            return when (key) {
                "appChannel" -> PrefManager.appChannel
                else -> defValue ?: ""
            }
        }

        override fun putString(key: String?, value: String?) {
            val safeValue = value ?: ""
            when (key) {

                "appChannel" -> PrefManager.appChannel = safeValue
            }
        }

        override fun getBoolean(key: String?, defValue: Boolean): Boolean {
            return when (key) {
                "debugMode" -> PrefManager.debugMode
                "sendErrorReport" -> PrefManager.sendErrorReport
                "autoCheckAppUpdate" -> PrefManager.autoCheckAppUpdate
                "autoCheckRuleUpdate" -> PrefManager.autoCheckRuleUpdate
                else -> defValue
            }
        }

        override fun putBoolean(key: String?, value: Boolean) {
            when (key) {
                "debugMode" -> PrefManager.debugMode = value
                "sendErrorReport" -> PrefManager.sendErrorReport = value
                "autoCheckAppUpdate" -> PrefManager.autoCheckAppUpdate = value
                "autoCheckRuleUpdate" -> PrefManager.autoCheckRuleUpdate = value
            }
        }
    }

    /**
     * 设置更新渠道偏好显示
     */
    private fun setupAppChannelPreference() {
        findPreference<rikka.preference.SimpleMenuPreference>("appChannel")?.let { preference ->
            // 获取当前值对应的显示文本
            val currentValue = PrefManager.appChannel
            val entryValues = preference.entryValues
            val entries = preference.entries

            // 找到当前值对应的显示文本
            val index = entryValues?.indexOf(currentValue) ?: -1
            if (index >= 0 && index < (entries?.size ?: 0)) {
                preference.summary = entries[index]
            } else {
                preference.summary = currentValue // 如果找不到对应文本，直接显示值
            }

            // 设置值变化监听器，更新显示
            preference.setOnPreferenceChangeListener { _, newValue ->
                val newIndex = entryValues?.indexOf(newValue) ?: -1
                if (newIndex >= 0 && newIndex < (entries?.size ?: 0)) {
                    preference.summary = entries[newIndex]
                }
                true
            }
        }
    }

    /**
     * 显示清除数据库确认对话框
     */
    private fun showClearDatabaseDialog() {
        BaseSheetDialog.create<BottomSheetDialogBuilder>(requireContext())
            .setTitleInt(R.string.setting_clear_database)
            .setMessage(R.string.clear_db_msg)
            .setPositiveButton(R.string.btn_confirm) { _, _ ->
                performClearDatabase()
            }
            .setNegativeButton(R.string.btn_cancel) { _, _ -> }
            .show()
    }

    /**
     * 执行清除数据库操作
     */
    private fun performClearDatabase() {
        val loading = LoadingUtils(requireContext())
        launch {
            loading.show(R.string.clearing_database)
            DatabaseAPI.clear()
            // 同步清理内置缓存与数据目录（仅删除子项，不删除目录本身）
            withIO {
                val ctx = requireContext()
                ctx.deleteSharedPreferences("settings")
            }
            ToastUtils.info(getString(R.string.clear_database_success))
            loading.close()
            SystemUtils.restart()
        }
    }
}
