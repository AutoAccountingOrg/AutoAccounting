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

package net.ankio.auto.utils

import android.content.ComponentName
import android.content.Context.MODE_PRIVATE
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatDelegate
import net.ankio.auto.BuildConfig
import net.ankio.auto.autoApp
import net.ankio.auto.constant.WorkMode
import androidx.core.content.edit
import net.ankio.auto.http.api.BookNameAPI

object PrefManager {



    private const val PREF_DARK_THEME = "dark_theme"
    private const val PREF_BLACK_DARK_THEME = "black_dark_theme"
    private const val PREF_FOLLOW_SYSTEM_ACCENT = "follow_system_accent"
    private const val PREF_THEME_COLOR = "theme_color"

    private val pref = autoApp.getSharedPreferences("settings", MODE_PRIVATE)

    fun init() {
        pref.registerOnSharedPreferenceChangeListener { sharedPreferences, key ->
            val value = sharedPreferences.all[key]
            // TODO 同步到服务端
        }
    }

    var darkTheme: Int
        get() = pref.getInt(PREF_DARK_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        set(value) = pref.edit { putInt(PREF_DARK_THEME, value) }

    var blackDarkTheme: Boolean
        get() = pref.getBoolean(PREF_BLACK_DARK_THEME, false)
        set(value) = pref.edit { putBoolean(PREF_BLACK_DARK_THEME, value) }


    var followSystemAccent: Boolean
        get() = pref.getBoolean(PREF_FOLLOW_SYSTEM_ACCENT, true)
        set(value) = pref.edit { putBoolean(PREF_FOLLOW_SYSTEM_ACCENT, value) }

    var themeColor: String
        get() = pref.getString(PREF_THEME_COLOR, "MATERIAL_DEFAULT")!!
        set(value) = pref.edit { putString(PREF_THEME_COLOR, value) }

    var hideIcon: Boolean
        get() = pref.getBoolean("hideIcon", false)
        set(value) {
            pref.edit { putBoolean("hideIcon", value) }
            val component = ComponentName(autoApp, "com.close.hook.ads.MainActivityLauncher")
            val status =
                if (value) PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                else PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            autoApp.packageManager.setComponentEnabledSetting(
                component,
                status,
                PackageManager.DONT_KILL_APP
            )
        }


    var language: String
        get() = pref.getString("language", "SYSTEM")!!
        set(value) = pref.edit { putString("language", value) }


    var workMode: WorkMode
        get() = WorkMode.valueOf(pref.getString("workMode", WorkMode.Xposed.name)!!)
        set(value) = pref.edit { putString("workMode", value.name) }

    // 记账软件
    var bookApp: String
        get() = pref.getString("bookApp", BuildConfig.APPLICATION_ID)!!
        set(value) = pref.edit { putString("bookApp", value) }

    var introIndex: Int
        get() = pref.getInt("introIndex", 0)
        set(value) = pref.edit { putInt("introIndex", value) }


    var featureAssetManage: Boolean
        get() = pref.getBoolean("featureAssetManage", false)
        set(value) = pref.edit { putBoolean("featureAssetManage", value) }


    var featureMultiBook: Boolean
        get() = pref.getBoolean("featureMultiBook", false)
        set(value) = pref.edit { putBoolean("featureMultiBook", value) }

    var featureReimbursement: Boolean
        get() = pref.getBoolean("featureReimbursement", false)
        set(value) = pref.edit { putBoolean("featureReimbursement", value) }

    var featureLeading: Boolean
        get() = pref.getBoolean("featureLeading", false)
        set(value) = pref.edit { putBoolean("featureLeading", value) }

    var featureFee: Boolean
        get() = pref.getBoolean("featureFee", false)
        set(value) = pref.edit { putBoolean("featureFee", value) }

    var featureMultiCurrency: Boolean
        get() = pref.getBoolean("featureMultiCurrency", false)
        set(value) = pref.edit { putBoolean("featureMultiCurrency", value) }

    var featureTag: Boolean
        get() = pref.getBoolean("featureTag", false)
        set(value) = pref.edit { putBoolean("featureTag", value) }


    var aiFeatureOCR: Boolean
        get() = pref.getBoolean("aiFeatureOCR", false)
        set(value) = pref.edit { putBoolean("aiFeatureOCR", value) }


    var aiFeatureCategory: Boolean
        get() = pref.getBoolean("aiFeatureCategory", false)
        set(value) = pref.edit { putBoolean("aiFeatureCategory", value) }

    var aiFeatureAutoDetection: Boolean
        get() = pref.getBoolean("aiFeatureAutoDetection", false)
        set(value) = pref.edit { putBoolean("aiFeatureAutoDetection", value) }


    var defaultBook: String
        get() = pref.getString("defaultBook", BookNameAPI.DEFAULT_BOOK)!!
        set(value) = pref.edit { putString("defaultBook", value) }

    var ruleVersion: String
        get() = pref.getString("ruleVersion", "none")!!
        set(value) = pref.edit { putString("ruleVersion", value) }

    var ruleUpdate: String
        get() = pref.getString("ruleUpdate", "none")!!
        set(value) = pref.edit { putString("ruleUpdate", value) }

    var uiRoundStyle: Boolean
        get() = pref.getBoolean("uiRoundStyle", true)
        set(value) = pref.edit { putBoolean("uiRoundStyle", value) }


    var localID: String
        get() = pref.getString("localID", "")!!
        set(value) = pref.edit { putString("localID", value) }

    var token: String
        get() = pref.getString("token", "")!!
        set(value) = pref.edit { putString("token", value) }
}