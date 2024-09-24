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
import net.ankio.auto.ui.api.BaseActivity
import net.ankio.auto.ui.api.BaseFragment
import org.ezbook.server.constant.Setting
import java.lang.ref.WeakReference

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
                subTitle = R.string.setting_bill_remark_desc,
                type = ItemType.INPUT,
                default = "【商户名称】 - 【商品名称】",
                icon = R.drawable.setting_icon_remark,
            ),
            // 去重
            SettingItem(
                title = R.string.setting_bill_repeat,
                subTitle = R.string.setting_bill_repeat_desc,
                key = Setting.AUTO_GROUP,
                type = ItemType.SWITCH,
                default = true,
                icon = R.drawable.setting_icon_repeat,
            ),
            // 规则显示
            SettingItem(
                title = R.string.setting_bill_show_rule,
                key = Setting.SHOW_RULE_NAME,
                type = ItemType.SWITCH,
                default = true,
                icon = R.drawable.setting2_icon_rule,
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
                type = ItemType.TEXT,
                default = FloatEvent.POP_EDIT_WINDOW.ordinal,
                icon = R.drawable.setting_icon_click,
                selectList =
                hashMapOf(
                    context.getString(R.string.pop_edit_window) to FloatEvent.POP_EDIT_WINDOW.ordinal,
                    context.getString(R.string.auto_account) to FloatEvent.AUTO_ACCOUNT.ordinal,
                    context.getString(R.string.no_account) to FloatEvent.NO_ACCOUNT.ordinal,
                ),
            ),
            SettingItem(
                title = R.string.setting_float_on_badge_long_click,
                key = Setting.FLOAT_LONG_CLICK,
                type = ItemType.TEXT,
                default = FloatEvent.NO_ACCOUNT.ordinal,
                icon = R.drawable.setting_icon_long_click,
                selectList =
                hashMapOf(
                    context.getString(R.string.pop_edit_window) to FloatEvent.POP_EDIT_WINDOW.ordinal,
                    context.getString(R.string.auto_account) to FloatEvent.AUTO_ACCOUNT.ordinal,
                    context.getString(R.string.no_account) to FloatEvent.NO_ACCOUNT.ordinal,
                ),
            ),
            SettingItem(
                title = R.string.setting_float_on_badge_timeout,
                key = Setting.FLOAT_TIMEOUT_ACTION,
                type = ItemType.TEXT,
                default = FloatEvent.POP_EDIT_WINDOW.ordinal,
                icon = R.drawable.setting_icon_timeout,
                selectList =
                hashMapOf(
                    context.getString(R.string.pop_edit_window) to FloatEvent.POP_EDIT_WINDOW.ordinal,
                    context.getString(R.string.auto_account) to FloatEvent.AUTO_ACCOUNT.ordinal,
                    context.getString(R.string.no_account) to FloatEvent.NO_ACCOUNT.ordinal,
                ),
            ),
            // 分类
            SettingItem(R.string.setting_category),
            SettingItem(
                title = R.string.setting_auto_create_category,
                key = Setting.AUTO_CREATE_CATEGORY,
                subTitle = R.string.setting_auto_create_category_desc,
                type = ItemType.SWITCH,
                default = false,
                icon = R.drawable.setting_icon_auto,
            ),
            SettingItem(
                title = R.string.setting_category_show_parent,
                key = Setting.CATEGORY_SHOW_PARENT,
                subTitle = R.string.setting_category_show_parent_desc,
                type = ItemType.SWITCH,
                default = false,
                icon = R.drawable.setting_icon_parent,
            ),
            SettingItem(
                R.string.setting_asset, regex = "${Setting.SETTING_ASSET_MANAGER}=true"
            ),
            SettingItem(
                title = R.string.setting_auto_asset,
                regex = "${Setting.SETTING_ASSET_MANAGER}=true",
                key = Setting.AUTO_ASSET,
                subTitle = R.string.setting_auto_asset_desc,
                type = ItemType.SWITCH,
                default = false,
                icon = R.drawable.setting_icon_map,
            ),
            SettingItem(
                title = R.string.setting_auto_ai_asset,
                regex = "${Setting.SETTING_ASSET_MANAGER}=true",
                key = Setting.AUTO_IDENTIFY_ASSET,
                subTitle = R.string.setting_auto_ai_asset_desc,
                type = ItemType.SWITCH,
                default = false,
                icon = R.drawable.setting_icon_ai_map,
            ),
            SettingItem(R.string.setting_color),
            SettingItem(
                title = R.string.setting_pay_color,
                key = Setting.EXPENSE_COLOR_RED,
                type = ItemType.TEXT,
                default = 0,
                icon = R.drawable.setting_icon_color,
                selectList =
                hashMapOf(
                    context.getString(R.string.setting_pay_color_red) to 0,
                    context.getString(R.string.setting_pay_color_green) to 1,
                ),
            ),
            SettingItem(R.string.setting_book),
            SettingItem(
                title = R.string.setting_book_success,
                key = Setting.SHOW_SUCCESS_POPUP,
                // subTitle = R.string.setting_category_show_parent_desc,
                type = ItemType.SWITCH,
                default = true,
                icon = R.drawable.setting_icon_success,
            ),


            SettingItem(R.string.setting_auto),
            //  regex = "${Setting.USE_WEBDAV}=true",

            SettingItem(
                variable = Setting.SETTING_ASSET_MANAGER,
                title = R.string.setting_asset_manager,
                key = Setting.SETTING_ASSET_MANAGER,
                type = ItemType.SWITCH,
                default = true,
                icon = R.drawable.home_app_assets,
            ),
            SettingItem(
                regex = "${Setting.SETTING_ASSET_MANAGER}=true",
                variable = Setting.SETTING_CURRENCY_MANAGER,
                title = R.string.setting_currency_manager,
                key = Setting.SETTING_CURRENCY_MANAGER,
                type = ItemType.SWITCH,
                default = false,
                icon = R.drawable.setting2_icon_language,
            ),
            SettingItem(
                variable = Setting.SETTING_REIMBURSEMENT,
                title = R.string.setting_reimbursement_manager,
                key = Setting.SETTING_REIMBURSEMENT,
                type = ItemType.SWITCH,
                default = true,
                icon = R.drawable.setting_icon_reimbursement,
            ),
            SettingItem(
                regex = "${Setting.SETTING_ASSET_MANAGER}=true",
                variable = Setting.SETTING_DEBT,
                title = R.string.setting_lending_manager,
                key = Setting.SETTING_DEBT,
                type = ItemType.SWITCH,
                default = true,
                icon = R.drawable.setting_icon_debt,
            ),
            SettingItem(
                variable = Setting.SETTING_BOOK_MANAGER,
                title = R.string.setting_mutilbooks_manager,
                key = Setting.SETTING_BOOK_MANAGER,
                type = ItemType.SWITCH,
                default = true,
                icon = R.drawable.home_app_book_data,
            ),
            SettingItem(
                variable = Setting.SETTING_FEE,
                title = R.string.setting_fee_manager,
                key = Setting.SETTING_FEE,
                type = ItemType.SWITCH,
                default = true,
                icon = R.drawable.setting_icon_fee,
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
