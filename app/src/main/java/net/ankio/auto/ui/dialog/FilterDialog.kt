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

package net.ankio.auto.ui.dialog

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import net.ankio.auto.R
import net.ankio.auto.constant.ItemType
import net.ankio.auto.databinding.DialogFilterBinding
import net.ankio.auto.setting.SettingItem
import net.ankio.auto.setting.SettingUtils
import net.ankio.auto.ui.activity.BaseActivity

class FilterDialog(
    private val context: BaseActivity,
    private val callback: () -> Unit,
) :
    BaseSheetDialog(context) {
    private lateinit var binding: DialogFilterBinding

    private val setting =
        arrayListOf(
            SettingItem(
                title = R.string.data_type,
                key = "dialog_filter_data_type",
                type = ItemType.TEXT,
                default = 0,
                selectList =
                    hashMapOf(
                        context.getString(R.string.data_type_null) to 0,
                        context.getString(R.string.data_type_app) to 1,
                        context.getString(R.string.data_type_notice) to 2,
                        context.getString(R.string.data_type_sms) to 3,
                        context.getString(R.string.data_type_helper) to 4,
                    ),
            ),
            SettingItem(
                title = R.string.data_match,
                key = "dialog_filter_match",
                type = ItemType.TEXT,
                selectList =
                    hashMapOf(
                        context.getString(R.string.data_type_null) to 0,
                        context.getString(R.string.data_rule_match) to 1,
                        context.getString(R.string.data_rule_not_match) to 2,
                    ),
                default = 0,
            ),
            SettingItem(
                title = R.string.data_upload,
                //  subTitle = R.string.data_type_null,
                key = "dialog_filter_upload",
                type = ItemType.TEXT,
                selectList =
                    hashMapOf(
                        context.getString(R.string.data_type_null) to 0,
                        context.getString(R.string.data_data_upload) to 1,
                        context.getString(R.string.data_data_not_upload) to 2,
                    ),
                default = 0,
            ),
            SettingItem(
                title = R.string.data_content,
                key = "dialog_filter_data",
                type = ItemType.INPUT,
                default = "",
            ),
        )
    private lateinit var settingRenderUtils: SettingUtils

    override fun onCreateView(inflater: LayoutInflater): View {
        binding = DialogFilterBinding.inflate(inflater)
        cardView = binding.cardView
        cardViewInner = binding.cardViewInner
        settingRenderUtils =
            SettingUtils(context, binding.container, layoutInflater, setting)
        settingRenderUtils.init()
        binding.buttonSure.setOnClickListener {
            callback()
            dismiss()
        }

        binding.buttonCancel.setOnClickListener {
            dismiss()
        }

        // 更新UI

        return binding.root
    }

    override fun show(
        float: Boolean,
        cancel: Boolean,
    ) {
        super.show(float, cancel)
        settingRenderUtils.onResume()
    }

    override fun dismiss() {
        super.dismiss()
        settingRenderUtils.onDestroy()
    }
}
