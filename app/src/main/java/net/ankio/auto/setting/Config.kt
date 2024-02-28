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

package net.ankio.auto.setting

import android.content.Context
import com.quickersilver.themeengine.ThemeChooserDialogBuilder
import com.quickersilver.themeengine.ThemeEngine
import com.quickersilver.themeengine.ThemeMode
import net.ankio.auto.R
import net.ankio.auto.setting.types.ItemType
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.LanguageUtils

object Config {
    fun app(context: Context): ArrayList<SettingItem> {
        return arrayListOf(
            //隐私
            SettingItem(R.string.setting_privacy),
            SettingItem(
                title = R.string.setting_analysis,
                subTitle = R.string.setting_analysis_desc,
                key = "setting_analysis",
                type = ItemType.SWITCH,
                default = true,
            ),
            //语言
            SettingItem(R.string.setting_lang),
            SettingItem(
                title = R.string.setting_lang,
                selectList = LanguageUtils.getLangList(context),
                onGetKeyValue = {
                    LanguageUtils.getAppLang()
                },
                onSavedValue = { value,activity ->
                    LanguageUtils.setAppLanguage(value as String)
                    activity.recreate()
                },
                type = ItemType.TEXT,
            ),
            SettingItem(
                title = R.string.setting_translation,
                subTitle = R.string.setting_help_translation,
                link = context.getString(R.string.translation_url),
                type = ItemType.TEXT,
            ),
            //皮肤
            SettingItem(R.string.setting_skin),
            SettingItem(
                id = "setting_theme",
                title = R.string.setting_theme,
                type = ItemType.COLOR,
                onGetKeyValue = {
                     ThemeEngine.getInstance(context).staticTheme.primaryColor
                },
                onItemClick = { _, activity ->
                    ThemeChooserDialogBuilder(activity)
                        .setTitle(R.string.choose_theme)
                        .setPositiveButton(activity.getString(R.string.ok)) { _, theme ->
                            ThemeEngine.getInstance(activity).staticTheme = theme
                            activity.recreate()
                        }
                        .setNegativeButton(activity.getString(R.string.close))
                        .setNeutralButton(activity.getString(R.string.default_theme)) { _, _ ->
                            ThemeEngine.getInstance(activity).resetTheme()
                            activity.recreate()
                        }
                        .setIcon(R.drawable.ic_theme)
                        .create()
                        .show()
                }
            ),
            SettingItem(
                title = R.string.setting_dark_theme,
                type = ItemType.TEXT,
                default = ThemeMode.AUTO,
                selectList = hashMapOf(
                    context.getString(R.string.always_off) to ThemeMode.DARK,
                    context.getString(R.string.always_on) to ThemeMode.LIGHT,
                    context.getString(R.string.lang_follow_system) to ThemeMode.AUTO
                ),
                onGetKeyValue = {
                    ThemeEngine.getInstance(context).themeMode
                },
                onSavedValue = { value,activity ->
                    ThemeEngine.getInstance(context).themeMode = value as Int
                    activity.recreate()
                }
            ),
            SettingItem(
                title = R.string.setting_use_dark_theme,
                type = ItemType.SWITCH,
                onGetKeyValue = {
                    ThemeEngine.getInstance(context).isTrueBlack
                },
                onSavedValue = { value,activity ->
                    ThemeEngine.getInstance(context).isTrueBlack = value as Boolean
                    activity.recreate()
                }
            ),
            SettingItem(
                idLink = "setting_theme",
                idLinkBoolean = true,
                title = R.string.setting_use_system_theme,
                type = ItemType.SWITCH,
                onGetKeyValue = {
                    ThemeEngine.getInstance(context).isDynamicTheme
                },
                onSavedValue = { value,activity ->
                    ThemeEngine.getInstance(context).isDynamicTheme = value as Boolean
                    activity.recreate()
                }
            ),
            SettingItem(
                title = R.string.setting_use_round_style,
                key = "setting_use_round_style",
                type = ItemType.SWITCH,
            ),
            //更新
            SettingItem(R.string.setting_update),
            SettingItem(
                title = R.string.app_url,
                key = "app_url",
                type = ItemType.INPUT,
            ),
            SettingItem(
                title = R.string.setting_update_type,
                key = "setting_update_type",
                type = ItemType.TEXT,
                default = 0,
                selectList = hashMapOf(
                    context.getString(R.string.stable_version) to 0,
                    context.getString(R.string.continuous_build_version) to 1
                )
            ),
            SettingItem(
                title = R.string.setting_app,
                key = "setting_app",
                type = ItemType.SWITCH,
            ),
            SettingItem(
                title = R.string.setting_rule,
                key = "setting_rule",
                type = ItemType.SWITCH,
            ),
            //更新
            SettingItem(R.string.setting_others),
            SettingItem(
                title = R.string.setting_debug,
                subTitle = R.string.debug_msg,
                key = "setting_debug",
                type = ItemType.SWITCH,
                onSavedValue = { value,activity ->
                    AppUtils.setDebug(value as Boolean)
                },
                default = false,
            ),
        )
    }
}