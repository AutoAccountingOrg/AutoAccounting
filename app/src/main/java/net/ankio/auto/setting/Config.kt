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
import android.net.Uri
import androidx.lifecycle.lifecycleScope
import com.hjq.toast.Toaster
import com.quickersilver.themeengine.ThemeChooserDialogBuilder
import com.quickersilver.themeengine.ThemeEngine
import com.quickersilver.themeengine.ThemeMode
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.constant.FloatEvent
import net.ankio.auto.exceptions.PermissionException
import net.ankio.auto.constant.ItemType
import net.ankio.auto.ui.activity.MainActivity
import net.ankio.auto.ui.fragment.home.Setting2Fragment
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.BackupUtils
import net.ankio.auto.utils.LanguageUtils
import net.ankio.auto.utils.Logger
import net.ankio.auto.utils.SpUtils
import net.ankio.auto.utils.UpdateUtils

object Config {
    /**
     * 获取App设置项
     */
    fun app(context: Context,setting2Fragment: Setting2Fragment): ArrayList<SettingItem> {
        return arrayListOf(
            //隐私
            SettingItem(R.string.setting_privacy),
            SettingItem(
                title = R.string.setting_analysis,
                subTitle = R.string.setting_analysis_desc,
                key = "setting_analysis",
                type = ItemType.SWITCH,
                default = true,
                icon = R.drawable.setting2_icon_anonymous
            ),
            //语言
            SettingItem(R.string.setting_lang),
            SettingItem(
                title = R.string.setting_lang,
                selectList = LanguageUtils.getLangList(context),
                onGetKeyValue = {
                    LanguageUtils.getAppLang()
                },
                onSavedValue = { value, activity ->
                    LanguageUtils.setAppLanguage(value as String)
                    activity.recreate()
                },
                type = ItemType.TEXT,
                icon = R.drawable.setting2_icon_language
            ),
            SettingItem(
                title = R.string.setting_translation,
                subTitle = R.string.setting_help_translation,
                link = context.getString(R.string.translation_url),
                type = ItemType.TEXT,
                icon = R.drawable.setting2_icon_translate
            ),
            //皮肤
            SettingItem(R.string.setting_skin),
            SettingItem(
                regex = "setting_use_system=false",
                title = R.string.setting_theme,
                type = ItemType.COLOR,
                icon = R.drawable.setting2_icon_theme,
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
                icon = R.drawable.setting2_icon_dark_theme,
                default = ThemeMode.AUTO,
                selectList = hashMapOf(
                    context.getString(R.string.always_off) to ThemeMode.LIGHT,
                    context.getString(R.string.always_on) to ThemeMode.DARK,
                    context.getString(R.string.lang_follow_system) to ThemeMode.AUTO
                ),
                onGetKeyValue = {
                    ThemeEngine.getInstance(context).themeMode
                },
                onSavedValue = { value, activity ->
                    ThemeEngine.getInstance(context).themeMode = value as Int
                    activity.recreate()
                }
            ),
            SettingItem(
                title = R.string.setting_use_dark_theme,
                type = ItemType.SWITCH,
                icon = R.drawable.setting2_icon_dark_true_theme,
                onGetKeyValue = {
                    ThemeEngine.getInstance(context).isTrueBlack
                },
                onSavedValue = { value, activity ->
                    ThemeEngine.getInstance(context).isTrueBlack = value as Boolean
                    activity.recreate()
                }
            ),
            SettingItem(
                variable = "setting_use_system",
                title = R.string.setting_use_system_theme,
                type = ItemType.SWITCH,
                icon = R.drawable.setting2_icon_system_theme,
                onGetKeyValue = {
                    ThemeEngine.getInstance(context).isDynamicTheme
                },
                onSavedValue = { value, activity ->
                    ThemeEngine.getInstance(context).isDynamicTheme = value as Boolean
                    activity.recreate()
                }
            ),
            SettingItem(
                title = R.string.setting_use_round_style,
                key = "setting_use_round_style",
                icon = R.drawable.setting2_icon_round_theme,
                type = ItemType.SWITCH,
            ),
            //备份
            SettingItem(R.string.setting_backup),

            //备份方式二选一，本地或者Webdav

            SettingItem(
                variable = "setting_use_webdav",
                title = R.string.setting_use_webdav,
                icon = R.drawable.setting2_icon_backup,
                key = "setting_use_webdav",
                default = false,
                type = ItemType.SWITCH,
            ),

            SettingItem(
                regex = "setting_use_webdav=false",
                title = R.string.setting_backup_path,
                icon = R.drawable.setting2_icon_dir,
                type = ItemType.TEXT,
               onGetKeyValue = {
                  val uri =  SpUtils.getString("backup_uri","")
                   if(uri.isNotEmpty()){
                       runCatching {
                           Uri.parse(uri).path
                       }.getOrDefault(context.getString(R.string.setting_backup_path_desc))
                   }else{
                       context.getString(R.string.setting_backup_path_desc)
                   }
               },
                onItemClick = { _, activity ->
                    BackupUtils.requestPermission(activity as MainActivity)
                 //   activity.recreate()
                }
            ),

            SettingItem(
                regex = "setting_use_webdav=false",
                title = R.string.setting_backup_2_local,
            //    subTitle = R.string.setting_backup_2_local_desc,
                type = ItemType.TEXT,
                icon = R.drawable.setting2_icon_to_local,
                onItemClick = { _, activity ->
                    setting2Fragment.lifecycleScope.launch {
                        val loading  = LoadingUtils(activity)
                       runCatching {
                           loading.show(R.string.backup_loading)
                           val backupUtils = BackupUtils(activity)
                           backupUtils.putLocalBackup()
                       }.onSuccess {
                           Toaster.show(R.string.backup_success)
                           loading.close()
                       }.onFailure {
                           //失败请求权限
                           if(it is PermissionException){
                                 BackupUtils.requestPermission(activity as MainActivity)
                           }
                            loading.close()
                           Toaster.show(R.string.backup_error_msg)
                       }
                    }
                }
            ),


            SettingItem(
                regex = "setting_use_webdav=false",
                title = R.string.setting_restore_2_local,
                icon = R.drawable.setting2_icon_from_local,
              //  subTitle = R.string.setting_restore_2_local_desc,
                type = ItemType.TEXT,
                onItemClick = { _, activity ->
                    (activity as MainActivity).restoreLauncher.launch(arrayOf("*/*"))
                }
            ),

            SettingItem(
                regex = "setting_use_webdav=true",
                title = R.string.setting_webdav_host,
                key = "setting_webdav_host",
                default = "https://dav.jianguoyun.com/dav/",
                type = ItemType.INPUT,
            ),
            SettingItem(
                regex = "setting_use_webdav=true",
                title = R.string.setting_webdav_username,
                key = "setting_webdav_username",
                default = "",
                type = ItemType.INPUT,
            ),
            SettingItem(
                regex = "setting_use_webdav=true",
                title = R.string.setting_webdav_password,
                subTitle = R.string.setting_webdav_password_desc,
                key = "setting_webdav_password",
                default = "",
                type = ItemType.INPUT,
            ),

            SettingItem(
                regex = "setting_use_webdav=true",
                title = R.string.setting_backup_2_webdav,
                icon = R.drawable.setting2_icon_webdav_upload,
           //     subTitle = R.string.setting_backup_2_webdav_desc,
                type = ItemType.TEXT,
                onItemClick = { _, activity ->
                    setting2Fragment.lifecycleScope.launch {
                        runCatching {
                            val backupUtils = BackupUtils(activity)
                            backupUtils.putWebdavBackup(activity as MainActivity)
                        }.onSuccess {

                        }.onFailure {
                            Logger.e("备份到webdav失败",it)
                        }
                    }

                }
            ),

            SettingItem(
                regex = "setting_use_webdav=true",
                title = R.string.setting_restore_2_webdav,
                icon = R.drawable.setting2_icon_webdav_download,
           //     subTitle = R.string.setting_backup_2_webdav_desc,
                type = ItemType.TEXT,
                onItemClick = { _, activity ->
                    setting2Fragment.lifecycleScope.launch {
                        runCatching {
                            val backupUtils = BackupUtils(activity)
                            backupUtils.getWebdavBackup(activity as MainActivity)
                        }.onSuccess {

                        }.onFailure {

                            Logger.e("获取webdav备份失败",it)
                        }
                    }
                }
            ),

            //更新
            SettingItem(R.string.setting_update),
            SettingItem(
                title = R.string.app_url,
                type = ItemType.INPUT,
                onGetKeyValue = {
                    UpdateUtils.getUrl()
                },
                onSavedValue = { value, activity ->
                    UpdateUtils.setUrl(value as String)
                },
            ),
            SettingItem(
                title = R.string.setting_update_type,
                key = "setting_update_type",
                icon = R.drawable.setting2_icon_update,
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
                icon = R.drawable.setting2_icon_rule,
                type = ItemType.SWITCH,
            ),
            SettingItem(
                title = R.string.setting_rule,
                key = "setting_rule",
                icon = R.drawable.setting2_icon_category,
                type = ItemType.SWITCH,
            ),
            //其他
            SettingItem(R.string.setting_others),
            SettingItem(
                title = R.string.setting_debug,
                subTitle = R.string.debug_msg,
                key = "setting_debug",
                icon = R.drawable.setting2_icon_debug,
                type = ItemType.SWITCH,
                onSavedValue = { value, _ ->
                    AppUtils.setDebug(value as Boolean)
                },
                default = false,
            ),
        )
    }
    fun setting(context: Context):ArrayList<SettingItem>{
        return arrayListOf(
            //账单
            SettingItem(R.string.setting_bill),
            //备注
            SettingItem(
                title = R.string.setting_bill_remark,
                key = "setting_bill_remark",
                subTitle = R.string.setting_bill_remark_desc,
                type = ItemType.INPUT,
                default = "【商户名称】 - 【商品名称】",
                icon = R.drawable.setting_icon_remark,
                onSavedValue = { value, activity ->
                    AppUtils.getService().set("setting_bill_remark", value as String)
                },
            ),
            //去重
            SettingItem(
                title = R.string.setting_bill_repeat,
                subTitle = R.string.setting_bill_repeat_desc,
                key = "setting_bill_repeat",
                type = ItemType.SWITCH,
                default = true,
                icon = R.drawable.setting_icon_repeat,
                onSavedValue = { value, activity ->
                    AppUtils.getService().set("setting_bill_repeat", value.toString())
                },
            ),
            //悬浮窗
            SettingItem(R.string.setting_float),
            SettingItem(
                title = R.string.setting_float_time,
                key = "setting_float_time",
                subTitle = R.string.setting_float_time_desc,
                type = ItemType.INPUT,
                default = 10
            ),

            SettingItem(
                title = R.string.setting_float_on_badge_click,
                key = "setting_float_on_badge_click",
                type = ItemType.TEXT,
                default = FloatEvent.POP_EDIT_WINDOW.ordinal,
                icon = R.drawable.setting_icon_click,
                selectList = hashMapOf(
                    context.getString(R.string.pop_edit_window) to FloatEvent.POP_EDIT_WINDOW.ordinal,
                    context.getString(R.string.auto_account) to FloatEvent.AUTO_ACCOUNT.ordinal,
                    context.getString(R.string.no_account) to FloatEvent.NO_ACCOUNT.ordinal,
                ),
            ),

            SettingItem(
                title = R.string.setting_float_on_badge_long_click,
                key = "setting_float_on_badge_long_click",
                type = ItemType.TEXT,
                default = FloatEvent.NO_ACCOUNT.ordinal,
                icon = R.drawable.setting_icon_long_click,
                selectList = hashMapOf(
                    context.getString(R.string.pop_edit_window) to FloatEvent.POP_EDIT_WINDOW.ordinal,
                    context.getString(R.string.auto_account) to FloatEvent.AUTO_ACCOUNT.ordinal,
                    context.getString(R.string.no_account) to FloatEvent.NO_ACCOUNT.ordinal,
                ),
            ),

            SettingItem(
                title = R.string.setting_float_on_badge_timeout,
                key = "setting_float_on_badge_timeout",
                type = ItemType.TEXT,
                default = FloatEvent.POP_EDIT_WINDOW.ordinal,
                icon = R.drawable.setting_icon_timeout,
                selectList = hashMapOf(
                    context.getString(R.string.pop_edit_window) to FloatEvent.POP_EDIT_WINDOW.ordinal,
                    context.getString(R.string.auto_account) to FloatEvent.AUTO_ACCOUNT.ordinal,
                    context.getString(R.string.no_account) to FloatEvent.NO_ACCOUNT.ordinal,
                ),
            ),
            //分类
            SettingItem(R.string.setting_category),
            SettingItem(
                title = R.string.setting_auto_create_category,
                key = "setting_auto_create_category",
                subTitle = R.string.setting_auto_create_category_desc,
                type = ItemType.SWITCH,
                default = false,
                icon = R.drawable.setting_icon_auto

            ),
            SettingItem(
                title = R.string.setting_category_show_parent,
                key = "setting_category_show_parent",
                subTitle = R.string.setting_category_show_parent_desc,
                type = ItemType.SWITCH,
                default = false,
                icon = R.drawable.setting_icon_parent,
                onSavedValue = { value, activity ->
                    AppUtils.getService().set("setting_category_show_parent", value.toString())
                },

            ),

            SettingItem(R.string.setting_color),
            SettingItem(
                title = R.string.setting_pay_color,
                key = "setting_pay_color_red",
                type = ItemType.TEXT,
                default = 0,
                icon = R.drawable.setting_icon_color,
                selectList = hashMapOf(
                    context.getString(R.string.setting_pay_color_red) to 0,
                    context.getString(R.string.setting_pay_color_green) to 1,
                ),
            ),

            SettingItem(R.string.setting_book),
            SettingItem(
                title = R.string.setting_book_success,
                key = "setting_book_success",
                // subTitle = R.string.setting_category_show_parent_desc,
                type = ItemType.SWITCH,
                default = true,
                icon = R.drawable.setting_icon_success,

                ),
        )
    }
}