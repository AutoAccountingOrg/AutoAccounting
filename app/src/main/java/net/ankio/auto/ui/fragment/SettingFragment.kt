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
import net.ankio.auto.ui.activity.BaseActivity

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
            SettingUtils(requireActivity() as BaseActivity, binding.container, layoutInflater, settingItems)
        settingRenderUtils.init()
        scrollView = binding.scrollView
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
                key = "setting_bill_remark",
                subTitle = R.string.setting_bill_remark_desc,
                type = ItemType.INPUT,
                default = "【商户名称】 - 【商品名称】",
                icon = R.drawable.setting_icon_remark,
            ),
            // 去重
            SettingItem(
                title = R.string.setting_bill_repeat,
                subTitle = R.string.setting_bill_repeat_desc,
                key = "setting_bill_repeat",
                type = ItemType.SWITCH,
                default = true,
                icon = R.drawable.setting_icon_repeat,
            ),
            // 悬浮窗
            SettingItem(R.string.setting_float),
            SettingItem(
                title = R.string.setting_float_time,
                key = "setting_float_time",
                subTitle = R.string.setting_float_time_desc,
                type = ItemType.INPUT,
                default = 10,
            ),
            SettingItem(
                title = R.string.setting_float_on_badge_click,
                key = "setting_float_on_badge_click",
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
                key = "setting_float_on_badge_long_click",
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
                key = "setting_float_on_badge_timeout",
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
                key = "setting_auto_create_category",
                subTitle = R.string.setting_auto_create_category_desc,
                type = ItemType.SWITCH,
                default = false,
                icon = R.drawable.setting_icon_auto,
            ),
            SettingItem(
                title = R.string.setting_category_show_parent,
                key = "setting_category_show_parent",
                subTitle = R.string.setting_category_show_parent_desc,
                type = ItemType.SWITCH,
                default = false,
                icon = R.drawable.setting_icon_parent,
            ),
            SettingItem(R.string.setting_asset),
            SettingItem(
                title = R.string.setting_auto_asset,
                key = "setting_auto_asset",
                subTitle = R.string.setting_auto_asset_desc,
                type = ItemType.SWITCH,
                default = false,
                icon = R.drawable.setting_icon_map,
            ),
            SettingItem(
                title = R.string.setting_auto_ai_asset,
                key = "setting_auto_ai_asset",
                subTitle = R.string.setting_auto_ai_asset_desc,
                type = ItemType.SWITCH,
                default = false,
                icon = R.drawable.setting_icon_ai_map,
            ),
            SettingItem(R.string.setting_color),
            SettingItem(
                title = R.string.setting_pay_color,
                key = "setting_pay_color_red",
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
                key = "setting_book_success",
                // subTitle = R.string.setting_category_show_parent_desc,
                type = ItemType.SWITCH,
                default = true,
                icon = R.drawable.setting_icon_success,
            ),


            SettingItem(R.string.setting_auto),

            // TODO 自动记账配置

        )
    }
}
