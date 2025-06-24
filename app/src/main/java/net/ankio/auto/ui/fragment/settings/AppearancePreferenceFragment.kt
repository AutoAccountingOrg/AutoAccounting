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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceDataStore
import androidx.preference.PreferenceFragmentCompat
import net.ankio.auto.autoApp
import net.ankio.auto.util.LangList
import net.ankio.auto.utils.PrefManager
import rikka.core.util.ResourceUtils
import rikka.html.text.HtmlCompat
import rikka.material.app.LocaleDelegate
import rikka.material.preference.MaterialSwitchPreference
import rikka.preference.SimpleMenuPreference
import java.util.Locale
import net.ankio.auto.R

class AppearancePreferenceFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = SettingsPreferenceDataStore()
        setPreferencesFromResource(R.xml.settings_theme, rootKey)

        setupLanguagePreference()
        setupThemePreferences()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    class SettingsPreferenceDataStore : PreferenceDataStore() {
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

    private fun updateLocale(locale: Locale) {
        val config = resources.configuration
        config.setLocale(locale)
        LocaleDelegate.defaultLocale = locale
        requireContext().createConfigurationContext(config)
        requireActivity().recreate()
    }

    private fun setupThemePreferences() {
        findPreference<SimpleMenuPreference>("darkTheme")?.setOnPreferenceChangeListener { _, newValue ->
            val newMode = (newValue as String).toInt()
            if (PrefManager.darkTheme != newMode) {
                AppCompatDelegate.setDefaultNightMode(newMode)
            }
            true
        }

        findPreference<MaterialSwitchPreference>("blackDarkTheme")?.setOnPreferenceChangeListener { _, _ ->
            if (ResourceUtils.isNightMode(requireContext().resources.configuration))
                activity?.recreate()
            true
        }

        findPreference<MaterialSwitchPreference>("followSystemAccent")?.setOnPreferenceChangeListener { _, _ ->
            activity?.recreate()
            true
        }

        findPreference<SimpleMenuPreference>("themeColor")?.setOnPreferenceChangeListener { _, _ ->
            activity?.recreate()
            true
        }
    }


}