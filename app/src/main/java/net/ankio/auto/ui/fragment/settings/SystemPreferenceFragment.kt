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

import android.text.TextUtils
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.Preference
import androidx.preference.PreferenceDataStore
import net.ankio.auto.R
import net.ankio.auto.autoApp
import net.ankio.auto.http.api.DatabaseAPI
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BasePreferenceFragment
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.dialog.BottomSheetDialogBuilder
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.util.LangList
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.utils.SystemUtils
import rikka.core.util.ResourceUtils
import rikka.html.text.HtmlCompat
import rikka.material.app.LocaleDelegate
import rikka.material.preference.MaterialSwitchPreference
import rikka.preference.SimpleMenuPreference
import java.util.Locale

/**
 * 系统设置页面
 * 包含：外观设置（语言、主题）、更新设置、高级功能
 */
class SystemPreferenceFragment : BasePreferenceFragment() {

    override fun getTitleRes(): Int = R.string.setting_title_system

    override fun getPreferencesRes(): Int = R.xml.settings_system

    override fun createDataStore(): PreferenceDataStore = SystemDataStore()

    /**
     * 设置自定义偏好行为
     */
    override fun setupPreferences() {
        super.setupPreferences()

        // 设置语言偏好
        setupLanguagePreference()

        // 设置主题偏好
        setupThemePreferences()

        // 设置更新渠道偏好
        setupAppChannelPreference()

        // 清除数据库
        findPreference<Preference>("clearDatabase")?.setOnPreferenceClickListener {
            showClearDatabaseDialog()
            true
        }
    }

    /**
     * 设置语言选择偏好
     */
    private fun setupLanguagePreference() {
        findPreference<SimpleMenuPreference>("language")?.let {
            val userLocale = autoApp.getLocale(PrefManager.language)
            val entries = buildList {
                for (lang in LangList.LOCALES) {
                    if (lang == "SYSTEM") {
                        add(getString(rikka.core.R.string.follow_system))
                    } else {
                        val locale = Locale.forLanguageTag(lang)
                        add(
                            HtmlCompat.fromHtml(
                                locale.getDisplayName(locale),
                                HtmlCompat.FROM_HTML_MODE_LEGACY
                            )
                        )
                    }
                }
            }
            it.entries = entries.toTypedArray()
            it.entryValues = LangList.LOCALES
            setLanguageSummary(it, userLocale)

            it.setOnPreferenceChangeListener { _, newValue ->
                val locale = autoApp.getLocale(newValue as String)
                updateLocale(locale)
                true
            }
        }
    }

    /**
     * 设置语言偏好的摘要显示
     */
    private fun setLanguageSummary(preference: SimpleMenuPreference, userLocale: Locale) {
        if (preference.value == "SYSTEM") {
            preference.summary = getString(rikka.core.R.string.follow_system)
        } else {
            val locale = Locale.forLanguageTag(preference.value)
            preference.summary =
                if (!TextUtils.isEmpty(locale.script)) {
                    locale.getDisplayScript(userLocale)
                } else {
                    locale.getDisplayName(userLocale)
                }
        }
    }

    /**
     * 更新应用语言设置
     */
    private fun updateLocale(locale: Locale) {
        val config = resources.configuration
        config.setLocale(locale)
        LocaleDelegate.defaultLocale = locale
        requireContext().createConfigurationContext(config)
        requireActivity().recreate()
    }

    /**
     * 设置主题相关偏好
     */
    private fun setupThemePreferences() {
        // 深色主题模式设置
        findPreference<SimpleMenuPreference>("darkTheme")?.setOnPreferenceChangeListener { _, newValue ->
            val newMode = (newValue as String).toInt()
            if (PrefManager.darkTheme != newMode) {
                AppCompatDelegate.setDefaultNightMode(newMode)
            }
            true
        }

        // 纯黑深色主题设置
        findPreference<MaterialSwitchPreference>("blackDarkTheme")?.setOnPreferenceChangeListener { _, _ ->
            if (ResourceUtils.isNightMode(requireContext().resources.configuration)) {
                activity?.recreate()
            }
            true
        }

        // 跟随系统主题色设置
        findPreference<MaterialSwitchPreference>("followSystemAccent")?.setOnPreferenceChangeListener { _, _ ->
            activity?.recreate()
            true
        }

        // 主题颜色设置
        findPreference<SimpleMenuPreference>("themeColor")?.setOnPreferenceChangeListener { _, _ ->
            activity?.recreate()
            true
        }
    }

    /**
     * 设置更新渠道偏好显示
     */
    private fun setupAppChannelPreference() {
        findPreference<SimpleMenuPreference>("appChannel")?.let { preference ->
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
        launch {
            val loading = LoadingUtils(requireContext())
            loading.show(R.string.clearing)
            try {
                DatabaseAPI.clear()
                ToastUtils.info(R.string.clear_success)
                SystemUtils.restart()
            } catch (e: Exception) {
                Logger.e("清除数据库失败", e)
                ToastUtils.error(getString(R.string.clear_database_failed, e.message))
            } finally {
                loading.close()
            }
        }
    }

    /**
     * 系统设置数据存储类
     */
    class SystemDataStore : PreferenceDataStore() {
        override fun getBoolean(key: String?, defValue: Boolean): Boolean {
            return when (key) {
                // 外观设置
                "followSystemAccent" -> PrefManager.followSystemAccent
                "blackDarkTheme" -> PrefManager.blackDarkTheme
                // 更新
                "autoCheckAppUpdate" -> PrefManager.autoCheckAppUpdate
                "autoCheckRuleUpdate" -> PrefManager.autoCheckRuleUpdate
                // 高级功能
                "debugMode" -> PrefManager.debugMode
                "sendErrorReport" -> PrefManager.sendErrorReport
                else -> defValue
            }
        }

        override fun putBoolean(key: String?, value: Boolean) {
            when (key) {
                // 外观设置
                "followSystemAccent" -> PrefManager.followSystemAccent = value
                "blackDarkTheme" -> PrefManager.blackDarkTheme = value
                // 更新
                "autoCheckAppUpdate" -> PrefManager.autoCheckAppUpdate = value
                "autoCheckRuleUpdate" -> PrefManager.autoCheckRuleUpdate = value
                // 高级功能
                "debugMode" -> PrefManager.debugMode = value
                "sendErrorReport" -> PrefManager.sendErrorReport = value
            }
        }

        override fun getString(key: String?, defValue: String?): String? {
            return when (key) {
                // 外观设置
                "language" -> PrefManager.language
                "themeColor" -> PrefManager.themeColor
                "darkTheme" -> PrefManager.darkTheme.toString()
                // 更新
                "appChannel" -> PrefManager.appChannel
                else -> defValue
            }
        }

        override fun putString(key: String?, value: String?) {
            val safeValue = value ?: ""
            when (key) {
                // 外观设置
                "language" -> PrefManager.language = safeValue
                "themeColor" -> PrefManager.themeColor = safeValue
                "darkTheme" -> PrefManager.darkTheme = safeValue.toIntOrNull() ?: -1
                // 更新
                "appChannel" -> PrefManager.appChannel = safeValue
            }
        }
    }
}

