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

package net.ankio.auto.ui.fragment

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.hjq.toast.Toaster
import com.quickersilver.themeengine.ThemeChooserDialogBuilder
import com.quickersilver.themeengine.ThemeEngine
import com.quickersilver.themeengine.ThemeMode
import kotlinx.coroutines.launch
import net.ankio.auto.App
import net.ankio.auto.BuildConfig
import net.ankio.auto.R
import net.ankio.auto.constant.ItemType
import net.ankio.auto.databinding.FragmentSystemSettingBinding
import net.ankio.auto.exceptions.PermissionException
import net.ankio.auto.setting.SettingItem
import net.ankio.auto.storage.BackupUtils
import net.ankio.auto.storage.ConfigUtils
import net.ankio.auto.storage.Logger
import net.ankio.auto.storage.SpUtils
import net.ankio.auto.ui.activity.MainActivity
import net.ankio.auto.ui.api.BaseActivity
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.update.UpdateChannel
import net.ankio.auto.update.UpdateType
import net.ankio.auto.utils.LanguageUtils
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.model.SettingModel
import net.ankio.auto.setting.SettingUtils as SettingItemUtils

class SystemSettingFragment : BaseFragment() {
    private lateinit var binding: FragmentSystemSettingBinding

    private lateinit var settingRenderUtils: SettingItemUtils

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentSystemSettingBinding.inflate(layoutInflater)
        val settingItems = app(requireContext(), this)
        settingRenderUtils =
            SettingItemUtils(
                requireActivity() as BaseActivity,
                binding.container,
                layoutInflater,
                settingItems
            )
        settingRenderUtils.init()
        //scrollView = WeakReference(binding.scrollView)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        settingRenderUtils.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        settingRenderUtils.onDestroy()
    }


    /**
     * 获取App设置项
     */
    private fun app(
        context: Context,
        systemSettingFragment: SystemSettingFragment,
    ): ArrayList<SettingItem> {
        return arrayListOf(
            // 隐私
            SettingItem(R.string.setting_privacy),
            SettingItem(
                title = R.string.setting_analysis,
                key = Setting.SEND_ERROR_REPORT,
                icon = R.drawable.setting2_icon_anonymous,
                subTitle = R.string.setting_analysis_desc,
                type = ItemType.SWITCH,
                default = true,
                
            ),
            // 语言
            SettingItem(R.string.setting_lang),
            SettingItem(
                title = R.string.setting_lang,
                icon = R.drawable.setting2_icon_translate,
                type = ItemType.TEXT,
                selectList = LanguageUtils.getLangList(context),
                onGetKeyValue = {
                    LanguageUtils.getAppLang()
                },
                onSavedValue = { value, activity ->
                    LanguageUtils.setAppLanguage(value as String)
                    activity.recreateActivity()
                },
                
            ),
            // 皮肤
            SettingItem(R.string.setting_skin),
            SettingItem(
                title = R.string.setting_theme,
                regex = "${Setting.USE_SYSTEM_SKIN}=false",
                icon = R.drawable.setting2_icon_theme,
                type = ItemType.COLOR,
                onGetKeyValue = {
                    ThemeEngine.getInstance(context).staticTheme.primaryColor
                },
                onItemClick = { _, activity ->
                    ThemeChooserDialogBuilder(activity)
                        .setTitle(R.string.choose_theme)
                        .setPositiveButton(activity.getString(R.string.ok)) { _, theme ->
                            ThemeEngine.getInstance(activity).staticTheme = theme
                            activity.recreateActivity()
                        }
                        .setNegativeButton(activity.getString(R.string.close))
                        .setNeutralButton(activity.getString(R.string.default_theme)) { _, _ ->
                            ThemeEngine.getInstance(activity).resetTheme()
                            activity.recreateActivity()
                        }
                        .setIcon(R.drawable.ic_theme)
                        .create()
                        .show()
                },
                
            ),
            SettingItem(
                title = R.string.setting_dark_theme,
                icon = R.drawable.setting2_icon_dark_theme,
                type = ItemType.TEXT,
                selectList =
                hashMapOf(
                    context.getString(R.string.always_off) to ThemeMode.LIGHT,
                    context.getString(R.string.always_on) to ThemeMode.DARK,
                    context.getString(R.string.lang_follow_system) to ThemeMode.AUTO,
                ),
                default = ThemeMode.AUTO,
                onGetKeyValue = {
                    ThemeEngine.getInstance(context).themeMode
                },
                onSavedValue = { value, activity ->
                    ThemeEngine.getInstance(context).themeMode = value as Int
                    activity.recreateActivity()
                },
                
            ),
            SettingItem(
                title = R.string.setting_use_dark_theme,
                icon = R.drawable.setting2_icon_dark_true_theme,
                type = ItemType.SWITCH,
                onGetKeyValue = {
                    ThemeEngine.getInstance(context).isTrueBlack
                },
                onSavedValue = { value, activity ->
                    ThemeEngine.getInstance(context).isTrueBlack = value as Boolean
                    activity.recreateActivity()
                },
                
            ),
            SettingItem(
                title = R.string.setting_use_system_theme,
                key = Setting.USE_SYSTEM_SKIN,
                icon = R.drawable.setting2_icon_system_theme,
                type = ItemType.SWITCH,
                onGetKeyValue = {
                    ThemeEngine.getInstance(context).isDynamicTheme
                },
                onSavedValue = { value, activity ->
                    ThemeEngine.getInstance(context).isDynamicTheme = value as Boolean
                    activity.recreateActivity()
                },
                
            ),
            SettingItem(
                title = R.string.setting_use_round_style,
                key = Setting.USE_ROUND_STYLE,
                icon = R.drawable.setting2_icon_round_theme,
                type = ItemType.SWITCH,
                
            ),
            // 备份
            SettingItem(R.string.setting_backup),
            // 备份方式二选一，本地或者Webdav
            SettingItem(
                title = R.string.setting_use_webdav,
                key = Setting.USE_WEBDAV,
                icon = R.drawable.setting2_icon_backup,
                type = ItemType.SWITCH,
                default = false,

            ),
            SettingItem(
                title = R.string.setting_auto_backup,
                key = Setting.AUTO_BACKUP,
                icon = R.drawable.icon_auto,
                type = ItemType.SWITCH,
                default = false,
                ),
            SettingItem(
                title = R.string.setting_backup_path,
                regex = "${Setting.USE_WEBDAV}=false",
                icon = R.drawable.setting2_icon_dir,
                type = ItemType.TEXT,
                onGetKeyValue = {
                    val uri = ConfigUtils.getString(Setting.LOCAL_BACKUP_PATH, "")
                    if (uri.isNotEmpty()) {
                        runCatching {
                            Uri.parse(uri).path
                        }.getOrDefault(context.getString(R.string.setting_backup_path_desc))
                    } else {
                        context.getString(R.string.setting_backup_path_desc)
                    }
                },
                onItemClick = { _, activity ->
                    BackupUtils.requestPermission(activity as MainActivity)
                },
                
            ),
            SettingItem(
                title = R.string.setting_backup_2_local,
                regex = "${Setting.USE_WEBDAV}=false",
                //    subTitle = R.string.setting_backup_2_local_desc,
                icon = R.drawable.setting2_icon_to_local,
                type = ItemType.TEXT,
                onItemClick = { _, activity ->
                    systemSettingFragment.lifecycleScope.launch {
                        val loading = LoadingUtils(activity)
                        runCatching {
                            loading.show(R.string.backup_loading)
                            val backupUtils = BackupUtils(activity)
                            backupUtils.putLocalBackup()
                        }.onSuccess {
                            Toaster.show(R.string.backup_success)
                            loading.close()
                        }.onFailure {
                            Logger.e("备份失败", it)
                            // 失败请求权限
                            if (it is PermissionException) {
                                BackupUtils.requestPermission(activity as MainActivity)
                            }
                            loading.close()
                            Toaster.show(R.string.backup_error_msg)
                        }
                    }
                },
                
            ),
            SettingItem(
                title = R.string.setting_restore_2_local,
                regex = "${Setting.USE_WEBDAV}=false",
                icon = R.drawable.setting2_icon_from_local,
                //  subTitle = R.string.setting_restore_2_local_desc,
                type = ItemType.TEXT,
                onItemClick = { _, activity ->
                    (activity as MainActivity).restoreLauncher.launch(arrayOf("*/*"))
                },
                
            ),
            SettingItem(
                title = R.string.setting_webdav_host,
                regex = "${Setting.USE_WEBDAV}=true",
                key = Setting.WEBDAV_HOST,
                type = ItemType.INPUT,
                default = "https://dav.jianguoyun.com/dav/",
                
            ),
            SettingItem(
                title = R.string.setting_webdav_username,
                regex = "${Setting.USE_WEBDAV}=true",
                key = Setting.WEBDAV_USER,
                type = ItemType.INPUT,
                default = "",
                
            ),
            SettingItem(
                title = R.string.setting_webdav_password,
                regex = "${Setting.USE_WEBDAV}=true",
                key = Setting.WEBDAV_PASSWORD,
                subTitle = R.string.setting_webdav_password_desc,
                type = ItemType.INPUT_PASSWORD,
                default = "",
                
            ),
            SettingItem(
                title = R.string.setting_backup_2_webdav,
                regex = "${Setting.USE_WEBDAV}=true",
                icon = R.drawable.setting2_icon_webdav_upload,
                //     subTitle = R.string.setting_backup_2_webdav_desc,
                type = ItemType.TEXT,
                onItemClick = { _, activity ->
                    systemSettingFragment.lifecycleScope.launch {
                        runCatching {
                            val backupUtils = BackupUtils(activity)
                            backupUtils.putWebdavBackup(activity as MainActivity)
                        }.onSuccess {
                        }.onFailure {
                            Logger.e("备份到webdav失败", it)
                        }
                    }
                },
                
            ),
            SettingItem(
                title = R.string.setting_restore_2_webdav,
                regex = "${Setting.USE_WEBDAV}=true",
                icon = R.drawable.setting2_icon_webdav_download,
                //     subTitle = R.string.setting_backup_2_webdav_desc,
                type = ItemType.TEXT,
                onItemClick = { _, activity ->
                    systemSettingFragment.lifecycleScope.launch {
                        runCatching {
                            val backupUtils = BackupUtils(activity)
                            backupUtils.getWebdavBackup(activity as MainActivity)
                        }.onSuccess {
                        }.onFailure {
                            Logger.e("获取webdav备份失败", it)
                        }
                    }
                },
                
            ),
            // 更新
            SettingItem(R.string.setting_update),
            SettingItem(
                title = R.string.setting_update_channel,
                key = Setting.UPDATE_CHANNEL,
                icon = R.drawable.setting2_icon_update_channel,
                type = ItemType.TEXT,
                selectList = hashMapOf(
                    context.getString(R.string.update_channel_github_raw) to UpdateChannel.GithubRaw.name,
                    context.getString(R.string.update_channel_cloud) to UpdateChannel.Cloud.name,
                    context.getString(R.string.update_channel_github_proxy) to UpdateChannel.GithubProxy.name,
                    context.getString(R.string.update_channel_github_proxy2) to UpdateChannel.GithubProxy2.name,
                    context.getString(R.string.update_channel_github_mirror) to UpdateChannel.GithubMirror.name,
                    context.getString(R.string.update_channel_github_d) to UpdateChannel.GithubD.name,
                    context.getString(R.string.update_channel_github_kk) to UpdateChannel.GithubKK.name,
                ),
                default = UpdateChannel.GithubRaw.name,
                
            ),
            SettingItem(
                title = R.string.setting_update_type,
                key = Setting.CHECK_UPDATE_TYPE,
                icon = R.drawable.setting2_icon_update,
                type = ItemType.TEXT,
                selectList =
                hashMapOf(
                    context.getString(R.string.version_stable) to UpdateType.Stable.name,
                    context.getString(R.string.version_beta) to UpdateType.Beta.name,
                    context.getString(R.string.version_canary) to UpdateType.Canary.name,
                ),
                default = UpdateType.switchDefaultUpdate(),
                
            ),
            SettingItem(
                title = R.string.setting_app,
                key = Setting.CHECK_APP_UPDATE,
                icon = R.drawable.setting2_icon_rule,
                type = ItemType.SWITCH,
                default = true,
                
            ),
            SettingItem(
                title = R.string.setting_rule,
                key = Setting.CHECK_RULE_UPDATE,
                icon = R.drawable.setting2_icon_category,
                type = ItemType.SWITCH,
                default = true,
                
            ),
            // 其他
            SettingItem(R.string.setting_others),
            SettingItem(
                title = R.string.setting_debug,
                key = Setting.DEBUG_MODE,
                icon = R.drawable.setting2_icon_debug,
                subTitle = R.string.debug_msg,
                type = ItemType.SWITCH,
                default = BuildConfig.DEBUG,
                onSavedValue = { value, _ ->
                    ConfigUtils.putBoolean(Setting.DEBUG_MODE, value as Boolean)
                    App.launch {
                        SettingModel.set(Setting.DEBUG_MODE, value.toString())
                    }
                    SpUtils.putBoolean(Setting.DEBUG_MODE, value as Boolean)
                },
                
            ),
            SettingItem(
                title = R.string.setting_clear_database,
                key = Setting.DEBUG_MODE,
                icon = R.drawable.icon_delete,
                subTitle = R.string.clear_db_desc,
                type = ItemType.TEXT,
                onItemClick = { value, activity ->
                    ToastUtils.info(R.string.clear_db_msg)
                    App.launch {
                        SettingModel.clearDatabase()
                        ToastUtils.info(R.string.clear_success)
                    }
                }
            ),
        )
    }
}