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

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.hjq.toast.Toaster
import com.quickersilver.themeengine.ThemeChooserDialogBuilder
import com.quickersilver.themeengine.ThemeEngine
import com.quickersilver.themeengine.ThemeMode
import kotlinx.coroutines.launch
import net.ankio.auto.App
import net.ankio.auto.BuildConfig
import net.ankio.auto.R
import net.ankio.auto.constant.FloatEvent
import net.ankio.auto.databinding.FragmentSettingDetailBinding
import net.ankio.auto.exceptions.PermissionException
import net.ankio.auto.setting.SettingItem
import net.ankio.auto.setting.SettingUtils
import net.ankio.auto.storage.BackupUtils
import net.ankio.auto.storage.ConfigUtils
import net.ankio.auto.storage.Logger
import net.ankio.auto.storage.SpUtils
import net.ankio.auto.ui.activity.MainActivity
import net.ankio.auto.ui.api.BaseActivity
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.dialog.AppsDialog
import net.ankio.auto.ui.utils.AppUtils
import net.ankio.auto.ui.utils.DonateUtils
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.ui.utils.viewBinding
import net.ankio.auto.update.UpdateChannel
import net.ankio.auto.update.UpdateType
import net.ankio.auto.utils.LanguageUtils
import net.ankio.auto.xposed.Apps
import org.ezbook.server.constant.AIModel
import org.ezbook.server.constant.Setting
import org.ezbook.server.constant.SyncType
import org.ezbook.server.db.model.SettingModel

class SettingDetailFragment:BaseFragment() {
    override val binding: FragmentSettingDetailBinding by viewBinding(FragmentSettingDetailBinding::inflate)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 检查Bundle是否为空
        val bundle = arguments
        if(bundle == null){
            findNavController().popBackStack()
            return
        }
        val title = bundle.getString("title")
        val id = bundle.getInt("id")
        binding.topAppBar.setTitle(title)
        //
        switchFragment(id)
    }


    private lateinit var settingUtils: SettingUtils
    override fun onResume() {
        super.onResume()
        if (this::settingUtils.isInitialized) {
            settingUtils.onResume()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this::settingUtils.isInitialized) {
            settingUtils.onDestroy()
        }
    }
    private fun switchFragment(id: Int) {
        val settingItems =  when(id){
            R.id.setting_bill-> renderBillFragment()
            R.id.setting_popup->renderPopupFragment()
            R.id.setting_features-> renderFeaturesFragment()
            R.id.setting_appearance-> renderAppearance()
            R.id.setting_experimental-> renderExperimental()
            R.id.setting_backup-> renderBackup()
            R.id.setting_others-> renderOthers()
            else -> arrayListOf()
        }
        settingUtils =
            SettingUtils(
                requireActivity() as BaseActivity,
                binding.container,
                layoutInflater,
                settingItems
            )
        settingUtils.init()
    }

    private fun renderBillFragment(): ArrayList<SettingItem>{
        return arrayListOf(
// 账单
            SettingItem.Title(R.string.setting_bill_remark),
            // 备注
            SettingItem.Input(
                title = R.string.setting_bill_remark,
                key = Setting.NOTE_FORMAT,
                icon = R.drawable.setting_icon_remark,
                subTitle = R.string.setting_bill_remark_desc,
                default = "【商户名称】【商品名称】",
                ),

            SettingItem.Title(R.string.setting_bill_repeat),
            SettingItem.Switch(
                title = R.string.setting_bill_repeat,
                key = Setting.AUTO_GROUP,
                icon = R.drawable.setting_icon_repeat,
                subTitle = R.string.setting_bill_repeat_desc,
                default = true,
                ),
            SettingItem.Title(R.string.setting_bill_category),
            SettingItem.Switch(
                title = R.string.setting_category_show_parent,
                key = Setting.CATEGORY_SHOW_PARENT,
                icon = R.drawable.setting_icon_parent,
                subTitle = R.string.setting_category_show_parent_desc,
                default = false,

                ),
            SettingItem.Title(R.string.setting_color),
            SettingItem.Select(
                title = R.string.setting_pay_color,
                key = Setting.EXPENSE_COLOR_RED,
                icon = R.drawable.setting_icon_color,
                selectList =
                hashMapOf(
                    requireContext().getString(R.string.setting_pay_color_red) to 0,
                    requireContext().getString(R.string.setting_pay_color_green) to 1,
                ),
                default = 0,

                ),
            SettingItem.Title(R.string.setting_bill_tip),
            SettingItem.Switch(
                title = R.string.setting_bill_show_rule,
                key = Setting.SHOW_RULE_NAME,
                icon = R.drawable.setting2_icon_rule,
                default = true,
            ),
            SettingItem.Switch(
                title = R.string.setting_bill_auto_record,
                key = Setting.SHOW_AUTO_BILL_TIP,
                icon = R.drawable.ic_tip,
                default = false,
            ),
            SettingItem.Switch(
                title = R.string.setting_book_success,
                key = Setting.SHOW_SUCCESS_POPUP,
                // subTitle = R.string.setting_category_show_parent_desc,
                icon = R.drawable.setting_icon_success,
                default = true,
                ),

            SettingItem.Title(R.string.setting_bill_sync),

            SettingItem.Select(
                title = R.string.setting_bill_sync_type,
                key = Setting.SYNC_TYPE,
                icon = R.drawable.ic_sync,
                selectList =
                hashMapOf(
                    requireContext().getString(R.string.when_open_app) to SyncType.WhenOpenApp.name,
                    requireContext().getString(R.string.bills_limit5) to SyncType.BillsLimit5.name,
                    requireContext().getString(R.string.bills_limit10) to SyncType.BillsLimit10.name,
                ),
                default = SyncType.WhenOpenApp.name,

                ),

            SettingItem.Card(R.string.setting_bill_sync_tip),
        )
    }

    private fun renderPopupFragment(): ArrayList<SettingItem>{
        return arrayListOf(
            SettingItem.Title(R.string.setting_float_time),
            SettingItem.Input(
                title = R.string.setting_float_time,
                key = Setting.FLOAT_TIMEOUT_OFF,
                subTitle = R.string.setting_float_time_desc,
                default = 10,
                ),
            SettingItem.Title(R.string.setting_popup_event),
            SettingItem.Select(
                title = R.string.setting_float_on_badge_click,
                key = Setting.FLOAT_CLICK,
                icon = R.drawable.setting_icon_click,
                selectList =
                hashMapOf(
                    requireContext().getString(R.string.pop_edit_window) to FloatEvent.POP_EDIT_WINDOW.ordinal,
                    requireContext().getString(R.string.auto_account) to FloatEvent.AUTO_ACCOUNT.ordinal,
                    requireContext().getString(R.string.no_account) to FloatEvent.NO_ACCOUNT.ordinal,
                ),
                default = FloatEvent.POP_EDIT_WINDOW.ordinal,

                ),
            SettingItem.Select(
                title = R.string.setting_float_on_badge_long_click,
                key = Setting.FLOAT_LONG_CLICK,
                icon = R.drawable.setting_icon_long_click,
               
                selectList =
                hashMapOf(
                    requireContext().getString(R.string.pop_edit_window) to FloatEvent.POP_EDIT_WINDOW.ordinal,
                    requireContext().getString(R.string.auto_account) to FloatEvent.AUTO_ACCOUNT.ordinal,
                    requireContext().getString(R.string.no_account) to FloatEvent.NO_ACCOUNT.ordinal,
                ),
                default = FloatEvent.NO_ACCOUNT.ordinal,

                ),
            SettingItem.Select(
                title = R.string.setting_float_on_badge_timeout,
                key = Setting.FLOAT_TIMEOUT_ACTION,
                icon = R.drawable.setting_icon_timeout,
               
                selectList =
                hashMapOf(
                    requireContext().getString(R.string.pop_edit_window) to FloatEvent.POP_EDIT_WINDOW.ordinal,
                    requireContext().getString(R.string.auto_account) to FloatEvent.AUTO_ACCOUNT.ordinal,
                    requireContext().getString(R.string.no_account) to FloatEvent.NO_ACCOUNT.ordinal,
                ),
                default = FloatEvent.POP_EDIT_WINDOW.ordinal,

                ),
            SettingItem.Title(R.string.setting_popup_style),
            SettingItem.Switch(
                title = R.string.setting_use_round_style,
                key = Setting.USE_ROUND_STYLE,
                icon = R.drawable.setting2_icon_round_theme,
                default = false,
            ),
        )
    }

    private fun renderFeaturesFragment(): ArrayList<SettingItem>{
        return arrayListOf(
            SettingItem.Card(R.string.setting_features),
            SettingItem.Switch(
                title = R.string.setting_asset_manager,
                key = Setting.SETTING_ASSET_MANAGER,
                icon = R.drawable.home_app_assets,
                default = true,

                ),
            SettingItem.Switch(
                title = R.string.setting_currency_manager,
                key = Setting.SETTING_CURRENCY_MANAGER,
                icon = R.drawable.setting2_icon_language,
                default = false,

                ),
            SettingItem.Switch(
                title = R.string.setting_reimbursement_manager,
                key = Setting.SETTING_REIMBURSEMENT,
                icon = R.drawable.setting_icon_reimbursement,
                default = true,

                ),
            SettingItem.Switch(
                title = R.string.setting_lending_manager,
                key = Setting.SETTING_DEBT,
                icon = R.drawable.setting_icon_debt,
                default = true,

                ),
            SettingItem.Switch(
                title = R.string.setting_mutilbooks_manager,
                key = Setting.SETTING_BOOK_MANAGER,
                icon = R.drawable.home_app_book_data,
                default = true,

                ),
            SettingItem.Switch(
                title = R.string.setting_fee_manager,
                key = Setting.SETTING_FEE,
                icon = R.drawable.setting_icon_fee,
                default = true,

                ),
        )
    }


    private fun renderAppearance(): ArrayList<SettingItem>{
        return arrayListOf(
            SettingItem.Title(R.string.setting_lang),
            SettingItem.Select(
                title = R.string.setting_lang,
                icon = R.drawable.setting2_icon_translate,
                selectList = LanguageUtils.getLangList(requireContext()),
                onGetKeyValue = {
                    LanguageUtils.getAppLang()
                },
                onSavedValue = { value, activity ->
                    LanguageUtils.setAppLanguage(value as String)
                    activity.recreateActivity()
                },
            ),
            // 皮肤
            SettingItem.Title(R.string.setting_skin),
            SettingItem.Color(
                title = R.string.setting_theme,
                regex = "${Setting.USE_SYSTEM_SKIN}=false",
                icon = R.drawable.setting2_icon_theme,
                onGetKeyValue = {
                    ThemeEngine.getInstance(requireContext()).staticTheme.primaryColor
                },
                onItemClick = { value, activity ->
                    ThemeChooserDialogBuilder(requireContext())
                        .setTitle(R.string.choose_theme)
                        .setPositiveButton(requireContext().getString(R.string.ok)) {_,theme ->
                            ThemeEngine.getInstance(requireContext()).staticTheme = theme
                            activity.recreateActivity()
                        }
                        .setNegativeButton(requireContext().getString(R.string.close))
                        .setNeutralButton(requireContext().getString(R.string.default_theme)) {_,_ ->
                            ThemeEngine.getInstance(requireContext()).resetTheme()
                            activity.recreateActivity()
                        }
                        .setIcon(R.drawable.ic_theme)
                        .create()
                        .show()
                }),
            SettingItem.Select(
                title = R.string.setting_dark_theme,
                icon = R.drawable.setting2_icon_dark_theme,
                selectList =
                hashMapOf(
                    requireContext().getString(R.string.always_off) to ThemeMode.LIGHT,
                    requireContext().getString(R.string.always_on) to ThemeMode.DARK,
                    requireContext().getString(R.string.lang_follow_system) to ThemeMode.AUTO,
                ),
                default = ThemeMode.AUTO,
                onGetKeyValue = {
                    ThemeEngine.getInstance(requireContext()).themeMode
                },
                onSavedValue = { value, activity ->
                    ThemeEngine.getInstance(requireContext()).themeMode = value as Int
                    activity.recreateActivity()
                },

                ),
            SettingItem.Switch(
                title = R.string.setting_use_dark_theme,
                icon = R.drawable.setting2_icon_dark_true_theme,
                onGetKeyValue = {
                    ThemeEngine.getInstance(requireContext()).isTrueBlack
                },
                onSavedValue = { value, activity ->
                    ThemeEngine.getInstance(requireContext()).isTrueBlack = value as Boolean
                    activity.recreateActivity()
                },

                ),
            SettingItem.Switch(
                title = R.string.setting_use_system_theme,
                key = Setting.USE_SYSTEM_SKIN,
                icon = R.drawable.setting2_icon_system_theme,
                onGetKeyValue = {
                    ThemeEngine.getInstance(requireContext()).isDynamicTheme
                },
                onSavedValue = { value, activity ->
                    ThemeEngine.getInstance(requireContext()).isDynamicTheme = value as Boolean
                    activity.recreateActivity()
                },

                ),
        )
    }

    private fun renderExperimental(): ArrayList<SettingItem>{
        return arrayListOf(
            SettingItem.Title(R.string.setting_ai),
            SettingItem.Switch(
                title = R.string.setting_bill_ai,
                key = Setting.USE_AI,
                icon = R.drawable.ic_ai,
                subTitle = R.string.setting_bill_ai_desc,
                default = false,

                ),
            SettingItem.Select(
                regex = "${Setting.USE_AI}=true",
                title = R.string.setting_bill_ai_type,
                key = Setting.AI_MODEL,
                default = AIModel.DeepSeek.name,
                icon = R.drawable.ic_support,
                selectList =
                hashMapOf(
                    requireContext().getString(R.string.gemini) to AIModel.Gemini.name,
                    requireContext().getString(R.string.qwen) to AIModel.QWen.name,
                    requireContext().getString(R.string.deepseek) to AIModel.DeepSeek.name,
                    requireContext().getString(R.string.chatgpt) to AIModel.ChatGPT.name,
                    requireContext().getString(R.string.oneapi) to AIModel.OneAPI.name,
                ),
            ),
            SettingItem.Input(
                title = R.string.setting_bill_ai_key,
                regex = "${Setting.USE_AI}=true,${Setting.AI_MODEL}_${AIModel.Gemini.name}=true",
                key = "${Setting.API_KEY}_${AIModel.Gemini.name}",
                isPassword = true,
                default = "",
            ),
            SettingItem.Input(
                title = R.string.setting_bill_ai_key,
                regex = "${Setting.USE_AI}=true,${Setting.AI_MODEL}_${AIModel.QWen.name}=true",
                key = "${Setting.API_KEY}_${AIModel.QWen.name}",
                isPassword = true,
                default = "",
            ),

            SettingItem.Input(
                title = R.string.setting_bill_ai_key,
                regex = "${Setting.USE_AI}=true,${Setting.AI_MODEL}_${AIModel.DeepSeek.name}=true",
                key = "${Setting.API_KEY}_${AIModel.DeepSeek.name}",
                isPassword = true,
                default = "",
            ),
            SettingItem.Input(
                title = R.string.setting_bill_ai_key,
                regex = "${Setting.USE_AI}=true,${Setting.AI_MODEL}_${AIModel.ChatGPT.name}=true",
                key = "${Setting.API_KEY}_${AIModel.ChatGPT.name}",
                isPassword = true,
                default = "",
            ),
            SettingItem.Input(
                title = R.string.setting_bill_ai_key,
                regex = "${Setting.USE_AI}=true,${Setting.AI_MODEL}_${AIModel.OneAPI.name}=true",
                key = "${Setting.API_KEY}_${AIModel.OneAPI.name}",
                isPassword = true,
                default = "",
            ),
            SettingItem.Input(
                title = R.string.setting_bill_one_api_uri,
                regex = "${Setting.USE_AI}=true,${Setting.AI_MODEL}_${AIModel.OneAPI.name}=true",
                key = Setting.AI_ONE_API_URI,
                subTitle = R.string.setting_bill_one_api_uri_desc,
                default = "",
            ),
            SettingItem.Input(
                title = R.string.setting_bill_one_api_model,
                regex = "${Setting.USE_AI}=true,${Setting.AI_MODEL}_${AIModel.OneAPI.name}=true",
                key = Setting.AI_ONE_API_MODEL,
                default = "",
            ),
            SettingItem.Title(
                R.string.setting_asset,
            ),
            SettingItem.Switch(
                title = R.string.setting_auto_asset,
                key = Setting.AUTO_ASSET,
                icon = R.drawable.setting_icon_map,
                subTitle = R.string.setting_auto_asset_desc,
                default = false,

                ),
            SettingItem.Switch(
                title = R.string.setting_auto_ai_asset,
                key = Setting.AUTO_IDENTIFY_ASSET,
                icon = R.drawable.setting_icon_ai_map,
                subTitle = R.string.setting_auto_ai_asset_desc,
                default = false,

                ),
            SettingItem.Title(
                R.string.setting_category,
            ),
            SettingItem.Switch(
                title = R.string.setting_auto_create_category,
                key = Setting.AUTO_CREATE_CATEGORY,
                icon = R.drawable.setting_icon_auto,
                subTitle = R.string.setting_auto_create_category_desc,
                default = false,

                ),
        )
    }

    private fun renderBackup():ArrayList<SettingItem>{
        return arrayListOf(

            // 备份
            SettingItem.Title(R.string.setting_backup),
            // 备份方式二选一，本地或者Webdav
            SettingItem.Switch(
                title = R.string.setting_use_webdav,
                key = Setting.USE_WEBDAV,
                icon = R.drawable.setting2_icon_backup,
               // type = ItemType.SWITCH,
                default = false,

                ),
            SettingItem.Switch(
                title = R.string.setting_auto_backup,
                key = Setting.AUTO_BACKUP,
                icon = R.drawable.icon_auto,
               // type = ItemType.SWITCH,
                default = false,
            ),
            SettingItem.Text(
                title = R.string.setting_backup_path,
                regex = "${Setting.USE_WEBDAV}=false",
                icon = R.drawable.setting2_icon_dir,
                
                onGetKeyValue = {
                    val uri = ConfigUtils.getString(Setting.LOCAL_BACKUP_PATH, "")
                    if (uri.isNotEmpty()) {
                        runCatching {
                            Uri.parse(uri).path
                        }.getOrDefault(requireContext().getString(R.string.setting_backup_path_desc))
                    } else {
                        requireContext().getString(R.string.setting_backup_path_desc)
                    }
                },
                onItemClick = { activity,binding ->
                    BackupUtils.requestPermission(activity as MainActivity)
                },

                ),
            SettingItem.Text(
                title = R.string.setting_backup_2_local,
                regex = "${Setting.USE_WEBDAV}=false",
                //    subTitle = R.string.setting_backup_2_local_desc,
                icon = R.drawable.setting2_icon_to_local,
                
                onItemClick = { activity,binding ->
                    lifecycleScope.launch {
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
            SettingItem.Text(
                title = R.string.setting_restore_2_local,
                regex = "${Setting.USE_WEBDAV}=false",
                icon = R.drawable.setting2_icon_from_local,
                //  subTitle = R.string.setting_restore_2_local_desc,
                
                onItemClick = { activity,binding ->
                    BackupUtils.requestRestore(activity as MainActivity)
                },

                ),
            SettingItem.Input(
                title = R.string.setting_webdav_host,
                regex = "${Setting.USE_WEBDAV}=true",
                key = Setting.WEBDAV_HOST,
               // type = ItemType.INPUT,
                default = "https://dav.jianguoyun.com/dav/",

                ),
            SettingItem.Input(
                title = R.string.setting_webdav_username,
                regex = "${Setting.USE_WEBDAV}=true",
                key = Setting.WEBDAV_USER,
              //  type = ItemType.INPUT,
                default = "",

                ),
            SettingItem.Input(
                title = R.string.setting_webdav_password,
                regex = "${Setting.USE_WEBDAV}=true",
                key = Setting.WEBDAV_PASSWORD,
                subTitle = R.string.setting_webdav_password_desc,
                isPassword = true,
                default = "",

                ),
            SettingItem.Text(
                title = R.string.setting_backup_2_webdav,
                regex = "${Setting.USE_WEBDAV}=true",
                icon = R.drawable.setting2_icon_webdav_upload,
                //     subTitle = R.string.setting_backup_2_webdav_desc,
               // 
                onItemClick = { activity,binding ->
                    lifecycleScope.launch {
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
            SettingItem.Text(
                title = R.string.setting_restore_2_webdav,
                regex = "${Setting.USE_WEBDAV}=true",
                icon = R.drawable.setting2_icon_webdav_download,
                //     subTitle = R.string.setting_backup_2_webdav_desc,
               // 
                onItemClick = { activity,binding ->
                    lifecycleScope.launch {
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
            SettingItem.Title(R.string.setting_update),
            SettingItem.Select(
                title = R.string.setting_update_channel,
                key = Setting.UPDATE_CHANNEL,
                icon = R.drawable.setting2_icon_update_channel,
                selectList = hashMapOf(
                    requireContext().getString(R.string.update_channel_github_raw) to UpdateChannel.GithubRaw.name,
                    requireContext().getString(R.string.update_channel_cloud) to UpdateChannel.Cloud.name,
                    requireContext().getString(R.string.update_channel_github_proxy) to UpdateChannel.GithubProxy.name,
                    requireContext().getString(R.string.update_channel_github_proxy2) to UpdateChannel.GithubProxy2.name,
                    requireContext().getString(R.string.update_channel_github_mirror) to UpdateChannel.GithubMirror.name,
                    requireContext().getString(R.string.update_channel_github_d) to UpdateChannel.GithubD.name,
                    requireContext().getString(R.string.update_channel_github_kk) to UpdateChannel.GithubKK.name,
                ),
                default = UpdateChannel.GithubRaw.name,

                ),
            SettingItem.Select(
                title = R.string.setting_update_type,
                key = Setting.CHECK_UPDATE_TYPE,
                icon = R.drawable.setting2_icon_update,
                //
                selectList =
                hashMapOf(
                    requireContext().getString(R.string.version_stable) to UpdateType.Stable.name,
                    requireContext().getString(R.string.version_beta) to UpdateType.Beta.name,
                    requireContext().getString(R.string.version_canary) to UpdateType.Canary.name,
                ),
                default = UpdateType.switchDefaultUpdate(),

                ),
            SettingItem.Switch(
                title = R.string.setting_app,
                key = Setting.CHECK_APP_UPDATE,
                icon = R.drawable.setting2_icon_rule,
              //  type = ItemType.SWITCH,
                default = true,

                ),
            SettingItem.Switch(
                title = R.string.setting_rule,
                key = Setting.CHECK_RULE_UPDATE,
                icon = R.drawable.setting2_icon_category,
              //  type = ItemType.SWITCH,
                default = true,

                ),
            // 其他
        );
    }

    private fun renderOthers():ArrayList<SettingItem>{
        return arrayListOf(
            SettingItem.Title(R.string.setting_privacy),
            SettingItem.Switch(
                title = R.string.setting_analysis,
                key = Setting.SEND_ERROR_REPORT,
                icon = R.drawable.setting2_icon_anonymous,
                subTitle = R.string.setting_analysis_desc,
                default = true,

                ),
            SettingItem.Title(R.string.setting_others),
            SettingItem.Switch(
                title = R.string.setting_load_success,
                key = Setting.LOAD_SUCCESS,
                icon = R.drawable.setting_icon_success,
                subTitle = R.string.load_msg,
                default = true,
                onSavedValue = { value, _ ->
                    ConfigUtils.putBoolean(Setting.LOAD_SUCCESS, value)
                    App.launch {
                        SettingModel.set(Setting.LOAD_SUCCESS, value.toString())
                    }
                    SpUtils.putBoolean(Setting.LOAD_SUCCESS, value)
                },

                ),
            SettingItem.Switch(
                title = R.string.setting_debug,
                key = Setting.DEBUG_MODE,
                icon = R.drawable.setting2_icon_debug,
                subTitle = R.string.debug_msg,
                default = BuildConfig.DEBUG,
                onSavedValue = { value, _ ->
                    ConfigUtils.putBoolean(Setting.DEBUG_MODE, value)
                    App.launch {
                        SettingModel.set(Setting.DEBUG_MODE, value.toString())
                    }
                    SpUtils.putBoolean(Setting.DEBUG_MODE, value)
                },

                ),
            SettingItem.Text(
                title = R.string.setting_clear_database,
                icon = R.drawable.icon_delete,
                subTitle = R.string.clear_db_desc,
                onItemClick = { activity,binding ->
                    ToastUtils.info(R.string.clear_db_msg)
                    App.launch {
                        SettingModel.clearDatabase()
                        ToastUtils.info(R.string.clear_success)
                    }
                }
            ),
            SettingItem.Title(R.string.setting_hooks),
            SettingItem.Card(R.string.setting_hooks_msg),
            SettingItem.Text(
                title = R.string.setting_hook_auto,
                drawable = {
                    val pkg = SpUtils.getString(Setting.HOOK_AUTO_SERVER, Apps.getServerRunInApp())
                    val info = AppUtils.get(pkg)
                    info?.icon
                },
                onGetKeyValue = {
                    SpUtils.getString(Setting.HOOK_AUTO_SERVER, Apps.getServerRunInApp())
                },
                onItemClick = { activity,binding ->
                    AppsDialog(requireContext()) {
                        SpUtils.putString(Setting.HOOK_AUTO_SERVER, it.packageName)
                        binding.icon.setImageDrawable(it.icon)
                        binding.subTitle.text = it.packageName
                    }.showInFragment(this,false,true)
                }
            ),
            SettingItem.Text(
                title = R.string.setting_hook_wechat,
                drawable = {
                    val pkg = SpUtils.getString(Setting.HOOK_WECHAT, "mm.tencent.com")
                    val info = AppUtils.get(pkg)
                    info?.icon
                },
                onGetKeyValue = {
                    SpUtils.getString(Setting.HOOK_WECHAT, "mm.tencent.com")
                },
                onItemClick = { activity,binding ->
                    AppsDialog(requireContext()) {
                        SpUtils.putString(Setting.HOOK_WECHAT, it.packageName)
                        binding.icon.setImageDrawable(it.icon)
                        binding.icon.imageTintList = null
                        binding.subTitle.text = it.packageName
                    }.showInFragment(this,false,true)
                }
            ),
            SettingItem.Title(R.string.setting_donate),
            SettingItem.Text(
                title = R.string.donate_alipay,
                drawable = {
                    AppCompatResources.getDrawable(requireContext(),R.drawable.alipay)
                },
                onItemClick = { activity,binding ->
                    DonateUtils.alipay(activity)
                }
            ),
            SettingItem.Text(
                title = R.string.donate_wechat,
                drawable = {
                    AppCompatResources.getDrawable(requireContext(),R.drawable.wechat)
                },
                onItemClick = { activity,binding ->
                  DonateUtils.wechat(activity)
                }
            ),
        )
    }

}