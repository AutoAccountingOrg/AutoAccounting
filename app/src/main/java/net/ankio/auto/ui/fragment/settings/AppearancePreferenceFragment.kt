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

import android.os.Bundle
import android.text.TextUtils
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceDataStore
import net.ankio.auto.R
import net.ankio.auto.autoApp
import net.ankio.auto.ui.api.BasePreferenceFragment
import net.ankio.auto.util.LangList
import net.ankio.auto.utils.PrefManager
import rikka.core.util.ResourceUtils
import rikka.html.text.HtmlCompat
import rikka.material.app.LocaleDelegate
import rikka.material.preference.MaterialSwitchPreference
import rikka.preference.SimpleMenuPreference
import java.util.Locale

/**
 * 外观设置页面 - Linus式极简设计
 *
 * 设计原则：
 * 1. 单一职责 - 只负责外观和语言设置
 * 2. 统一架构 - 继承BasePreferenceFragment保持一致性
 * 3. 简洁实现 - 消除冗余的布局操作
 * 4. 向后兼容 - 保持所有原有功能不变
 *
 * 功能说明：
 * - 语言设置（跟随系统或手动选择）
 * - 主题颜色设置（系统主题色或自定义）
 * - 深色模式设置（跟随系统、开启、关闭）
 * - 纯黑深色主题开关
 */
class AppearancePreferenceFragment : BasePreferenceFragment() {

    override fun getTitleRes(): Int = R.string.setting_title_appearance

    override fun getPreferencesRes(): Int = R.xml.settings_theme

    override fun createDataStore(): PreferenceDataStore = AppearancePreferenceDataStore()

    /**
     * 设置自定义偏好行为（重写BasePreferenceFragment方法）
     */
    override fun setupPreferences() {
        super.setupPreferences()
        setupLanguagePreference()
        setupThemePreferences()
    }

    /**
     * 外观设置专用的数据存储类
     */
    inner class AppearancePreferenceDataStore : PreferenceDataStore() {
        override fun getString(key: String?, defValue: String?): String {
            return when (key) {
                "darkTheme" -> PrefManager.darkTheme.toString()
                "themeColor" -> PrefManager.themeColor
                "language" -> PrefManager.language
                else -> throw IllegalArgumentException("Invalid key: $key")
            }
        }

        override fun putString(key: String?, value: String?) {
            when (key) {
                "darkTheme" -> PrefManager.darkTheme = value!!.toInt()
                "themeColor" -> PrefManager.themeColor = value!!
                "language" -> PrefManager.language = value!!
                else -> throw IllegalArgumentException("Invalid key: $key")
            }
        }

        override fun getBoolean(key: String?, defValue: Boolean): Boolean {
            return when (key) {
                "blackDarkTheme" -> PrefManager.blackDarkTheme
                "followSystemAccent" -> PrefManager.followSystemAccent
                else -> throw IllegalArgumentException("Invalid key: $key")
            }
        }

        override fun putBoolean(key: String?, value: Boolean) {
            when (key) {
                "blackDarkTheme" -> PrefManager.blackDarkTheme = value
                "followSystemAccent" -> PrefManager.followSystemAccent = value
                else -> throw IllegalArgumentException("Invalid key: $key")
            }
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
                    if (lang == "SYSTEM") add(getString(rikka.core.R.string.follow_system))
                    else {
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
                if (!TextUtils.isEmpty(locale.script)) locale.getDisplayScript(userLocale) else locale.getDisplayName(
                    userLocale
                )
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
            if (ResourceUtils.isNightMode(requireContext().resources.configuration))
                activity?.recreate()
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


}