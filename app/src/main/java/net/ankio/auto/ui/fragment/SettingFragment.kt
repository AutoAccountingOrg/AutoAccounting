/*
 * Copyright (C) 2023 ankio(ankio@ankio.net)
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
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.ankio.auto.R
import net.ankio.auto.constant.FloatEvent
import net.ankio.auto.constant.ItemType
import net.ankio.auto.databinding.FragmentSettingBinding
import net.ankio.auto.setting.SettingItem
import net.ankio.auto.setting.SettingUtils
import net.ankio.auto.storage.ConfigUtils
import net.ankio.auto.ui.api.BaseActivity
import net.ankio.auto.ui.api.BaseFragment
import org.ezbook.server.constant.AIModel
import org.ezbook.server.constant.Setting
import org.ezbook.server.constant.SyncType

class SettingFragment : BaseFragment() {
    private lateinit var binding: FragmentSettingBinding

    private lateinit var settingRenderUtils: SettingUtils

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentSettingBinding.inflate(layoutInflater)
        val settingItems = setting(requireContext())
        settingRenderUtils =
            SettingUtils(
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

    private fun setting(context: Context): ArrayList<SettingItem> {
        return arrayListOf(
            // 账单
            SettingItem(R.string.setting_bill),
            // 备注
            SettingItem(
                title = R.string.setting_bill_remark,
                key = Setting.NOTE_FORMAT,
                icon = R.drawable.setting_icon_remark,
                subTitle = R.string.setting_bill_remark_desc,
                type = ItemType.INPUT,
                default = "【商户名称】 - 【商品名称】",
                
            ),
            // 去重
            SettingItem(
                title = R.string.setting_bill_repeat,
                key = Setting.AUTO_GROUP,
                icon = R.drawable.setting_icon_repeat,
                subTitle = R.string.setting_bill_repeat_desc,
                type = ItemType.SWITCH,
                default = true,
                
            ),
            // 规则显示
            SettingItem(
                title = R.string.setting_bill_show_rule,
                key = Setting.SHOW_RULE_NAME,
                icon = R.drawable.setting2_icon_rule,
                type = ItemType.SWITCH,
                default = true,
                
            ),
            SettingItem(
                title = R.string.setting_bill_sync_type,
                key = Setting.SYNC_TYPE,
                icon = R.drawable.ic_sync,
                type = ItemType.TEXT,
                selectList =
                hashMapOf(
                    context.getString(R.string.when_open_app) to SyncType.WhenOpenApp.name,
                    context.getString(R.string.bills_limit5) to SyncType.BillsLimit5.name,
                    context.getString(R.string.bills_limit10) to SyncType.BillsLimit10.name,
                ),
                default = SyncType.WhenOpenApp.name,
                
            ),
            // AI
            SettingItem(R.string.setting_ai),
            SettingItem(
                title = R.string.setting_bill_ai,
                key = Setting.USE_AI,
                icon = R.drawable.ic_ai,
                subTitle = R.string.setting_bill_ai_desc,
                type = ItemType.SWITCH,
                default = false,
                
            ),
            SettingItem(
                regex = "${Setting.USE_AI}=true",
                title = R.string.setting_bill_ai_type,
                key = Setting.AI_MODEL,
                type = ItemType.TEXT,
                default = AIModel.Gemini.name,
                icon = R.drawable.ic_support,
                selectList =
                hashMapOf(
                    context.getString(R.string.gemini) to AIModel.Gemini.name,
                    context.getString(R.string.qwen) to AIModel.QWen.name,
                    context.getString(R.string.deepseek) to AIModel.DeepSeek.name,
                    context.getString(R.string.chatgpt) to AIModel.ChatGPT.name,
                    context.getString(R.string.oneapi) to AIModel.OneAPI.name,
                    context.getString(R.string.spark) to AIModel.Spark.name,
                ),
            ),
            SettingItem(
                title = R.string.setting_bill_ai_key,
                regex = "${Setting.USE_AI}=true,${Setting.AI_MODEL}_${AIModel.Gemini.name}=true",
                key = "${Setting.API_KEY}_${AIModel.Gemini.name}",
                type = ItemType.INPUT_PASSWORD,
                default = "",
            ),
            SettingItem(
                title = R.string.setting_bill_ai_key,
                regex = "${Setting.USE_AI}=true,${Setting.AI_MODEL}_${AIModel.QWen.name}=true",
                key = "${Setting.API_KEY}_${AIModel.QWen.name}",
                type = ItemType.INPUT_PASSWORD,
                default = "",
            ),

            SettingItem(
                title = R.string.setting_bill_ai_key,
                regex = "${Setting.USE_AI}=true,${Setting.AI_MODEL}_${AIModel.DeepSeek.name}=true",
                key = "${Setting.API_KEY}_${AIModel.DeepSeek.name}",
                type = ItemType.INPUT_PASSWORD,
                default = "",
            ),
            SettingItem(
                title = R.string.setting_bill_ai_key,
                regex = "${Setting.USE_AI}=true,${Setting.AI_MODEL}_${AIModel.ChatGPT.name}=true",
                key = "${Setting.API_KEY}_${AIModel.ChatGPT.name}",
                type = ItemType.INPUT_PASSWORD,
                default = "",
            ),
            SettingItem(
                title = R.string.setting_bill_ai_key,
                regex = "${Setting.USE_AI}=true,${Setting.AI_MODEL}_${AIModel.Spark.name}=true",
                key = "${Setting.API_KEY}_${AIModel.Spark.name}",
                type = ItemType.INPUT_PASSWORD,
                default = "",
            ),
            SettingItem(
                title = R.string.setting_bill_ai_key,
                regex = "${Setting.USE_AI}=true,${Setting.AI_MODEL}_${AIModel.OneAPI.name}=true",
                key = "${Setting.API_KEY}_${AIModel.OneAPI.name}",
                type = ItemType.INPUT_PASSWORD,
                default = "",
            ),
            SettingItem(
                title = R.string.setting_bill_one_api_uri,
                regex = "${Setting.USE_AI}=true,${Setting.AI_MODEL}_${AIModel.OneAPI.name}=true",
                key = Setting.AI_ONE_API_URI,
                type = ItemType.INPUT,
                default = "",
            ),
            SettingItem(
                title = R.string.setting_bill_one_api_model,
                regex = "${Setting.USE_AI}=true,${Setting.AI_MODEL}_${AIModel.OneAPI.name}=true",
                key = Setting.AI_ONE_API_MODEL,
                type = ItemType.INPUT,
                default = "",
            ),
            // 悬浮窗
            SettingItem(R.string.setting_float),
            SettingItem(
                title = R.string.setting_float_time,
                key = Setting.FLOAT_TIMEOUT_OFF,
                subTitle = R.string.setting_float_time_desc,
                type = ItemType.INPUT,
                default = 10,
                
            ),
            SettingItem(
                title = R.string.setting_float_on_badge_click,
                key = Setting.FLOAT_CLICK,
                icon = R.drawable.setting_icon_click,
                type = ItemType.TEXT,
                selectList =
                hashMapOf(
                    context.getString(R.string.pop_edit_window) to FloatEvent.POP_EDIT_WINDOW.ordinal,
                    context.getString(R.string.auto_account) to FloatEvent.AUTO_ACCOUNT.ordinal,
                    context.getString(R.string.no_account) to FloatEvent.NO_ACCOUNT.ordinal,
                ),
                default = FloatEvent.POP_EDIT_WINDOW.ordinal,
                
            ),
            SettingItem(
                title = R.string.setting_float_on_badge_long_click,
                key = Setting.FLOAT_LONG_CLICK,
                icon = R.drawable.setting_icon_long_click,
                type = ItemType.TEXT,
                selectList =
                hashMapOf(
                    context.getString(R.string.pop_edit_window) to FloatEvent.POP_EDIT_WINDOW.ordinal,
                    context.getString(R.string.auto_account) to FloatEvent.AUTO_ACCOUNT.ordinal,
                    context.getString(R.string.no_account) to FloatEvent.NO_ACCOUNT.ordinal,
                ),
                default = FloatEvent.NO_ACCOUNT.ordinal,
                
            ),
            SettingItem(
                title = R.string.setting_float_on_badge_timeout,
                key = Setting.FLOAT_TIMEOUT_ACTION,
                icon = R.drawable.setting_icon_timeout,
                type = ItemType.TEXT,
                selectList =
                hashMapOf(
                    context.getString(R.string.pop_edit_window) to FloatEvent.POP_EDIT_WINDOW.ordinal,
                    context.getString(R.string.auto_account) to FloatEvent.AUTO_ACCOUNT.ordinal,
                    context.getString(R.string.no_account) to FloatEvent.NO_ACCOUNT.ordinal,
                ),
                default = FloatEvent.POP_EDIT_WINDOW.ordinal,
                
            ),
            // 分类
            SettingItem(R.string.setting_category),
            SettingItem(
                title = R.string.setting_auto_create_category,
                key = Setting.AUTO_CREATE_CATEGORY,
                icon = R.drawable.setting_icon_auto,
                subTitle = R.string.setting_auto_create_category_desc,
                type = ItemType.SWITCH,
                default = false,
                
            ),
            SettingItem(
                title = R.string.setting_category_show_parent,
                key = Setting.CATEGORY_SHOW_PARENT,
                icon = R.drawable.setting_icon_parent,
                subTitle = R.string.setting_category_show_parent_desc,
                type = ItemType.SWITCH,
                default = false,
                
            ),
            SettingItem(
                R.string.setting_asset, regex = "${Setting.SETTING_ASSET_MANAGER}=true"
            ),
            SettingItem(
                title = R.string.setting_auto_asset,
                regex = "${Setting.SETTING_ASSET_MANAGER}=true",
                key = Setting.AUTO_ASSET,
                icon = R.drawable.setting_icon_map,
                subTitle = R.string.setting_auto_asset_desc,
                type = ItemType.SWITCH,
                default = false,
                
            ),
            SettingItem(
                title = R.string.setting_auto_ai_asset,
                regex = "${Setting.SETTING_ASSET_MANAGER}=true",
                key = Setting.AUTO_IDENTIFY_ASSET,
                icon = R.drawable.setting_icon_ai_map,
                subTitle = R.string.setting_auto_ai_asset_desc,
                type = ItemType.SWITCH,
                default = false,
                
            ),
            SettingItem(R.string.setting_color),
            SettingItem(
                title = R.string.setting_pay_color,
                key = Setting.EXPENSE_COLOR_RED,
                icon = R.drawable.setting_icon_color,
                type = ItemType.TEXT,
                selectList =
                hashMapOf(
                    context.getString(R.string.setting_pay_color_red) to 0,
                    context.getString(R.string.setting_pay_color_green) to 1,
                ),
                default = 0,
                
            ),
            SettingItem(R.string.setting_book),
            SettingItem(
                title = R.string.setting_book_success,
                key = Setting.SHOW_SUCCESS_POPUP,
                // subTitle = R.string.setting_category_show_parent_desc,
                icon = R.drawable.setting_icon_success,
                type = ItemType.SWITCH,
                default = true,
                
            ),


            SettingItem(R.string.setting_auto),
            //  regex = "${Setting.USE_WEBDAV}=true",

            SettingItem(
                title = R.string.setting_asset_manager,
                key = Setting.SETTING_ASSET_MANAGER,
                icon = R.drawable.home_app_assets,
                type = ItemType.SWITCH,
                default = true,
                
            ),
            SettingItem(
                title = R.string.setting_currency_manager,
                regex = "${Setting.SETTING_ASSET_MANAGER}=true",
                key = Setting.SETTING_CURRENCY_MANAGER,
                icon = R.drawable.setting2_icon_language,
                type = ItemType.SWITCH,
                default = false,
                
            ),
            SettingItem(
                title = R.string.setting_reimbursement_manager,
                key = Setting.SETTING_REIMBURSEMENT,
                icon = R.drawable.setting_icon_reimbursement,
                type = ItemType.SWITCH,
                default = true,
                
            ),
            SettingItem(
                title = R.string.setting_lending_manager,
                regex = "${Setting.SETTING_ASSET_MANAGER}=true",
                key = Setting.SETTING_DEBT,
                icon = R.drawable.setting_icon_debt,
                type = ItemType.SWITCH,
                default = true,
                
            ),
            SettingItem(
                title = R.string.setting_mutilbooks_manager,
                key = Setting.SETTING_BOOK_MANAGER,
                icon = R.drawable.home_app_book_data,
                type = ItemType.SWITCH,
                default = true,
                
            ),
            SettingItem(
                title = R.string.setting_fee_manager,
                key = Setting.SETTING_FEE,
                icon = R.drawable.setting_icon_fee,
                type = ItemType.SWITCH,
                default = true,
                
            ),
          /*TODO  SettingItem(
                variable = Setting.SETTING_TAG,
                title = R.string.setting_tag_manager,
                key = Setting.SETTING_TAG,
                type = ItemType.SWITCH,
                default = true,
                icon = R.drawable.setting_icon_tag,
            ),*/
        )
    }
}
