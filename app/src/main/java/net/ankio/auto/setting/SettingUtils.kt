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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.viewbinding.ViewBinding
import net.ankio.auto.constant.ItemType
import net.ankio.auto.databinding.SettingItemColorBinding
import net.ankio.auto.databinding.SettingItemInputBinding
import net.ankio.auto.databinding.SettingItemSwitchBinding
import net.ankio.auto.databinding.SettingItemTextBinding
import net.ankio.auto.databinding.SettingItemTitleBinding
import net.ankio.auto.ui.api.BaseActivity
import net.ankio.auto.utils.CustomTabsHelper
import net.ankio.auto.ui.utils.ListPopupUtils
import net.ankio.auto.storage.ConfigUtils

class SettingUtils(
    private val context: BaseActivity,
    private val container: ViewGroup,
    private val inflater: LayoutInflater,
    private val settingItems: ArrayList<SettingItem>,
) {
    private val viewBinding = HashMap<SettingItem, ViewBinding>()
    private val resume = HashMap<SettingItem, () -> Unit>()
    private val destroy = HashMap<SettingItem, () -> Unit>()

    fun init() {
        settingItems.forEach {
            val binding =
                when (it.type) {
                    ItemType.SWITCH -> renderSwitch(it)
                    ItemType.TITLE -> renderTitle(it)
                    ItemType.TEXT -> renderText(it)
                    ItemType.INPUT -> renderInput(it)
                    ItemType.COLOR -> renderColor(it)
                }
            viewBinding[it] = binding
            container.addView(binding.root)
        }
    }

    fun onResume() {
        resume.forEach {
            val resume = it.value
            resume.invoke()
        }
    }

    fun onDestroy() {
        destroy.forEach {
            val destroy = it.value
            destroy.invoke()
        }
        viewBinding.clear()
        resume.clear()
        destroy.clear()
    }

    private fun renderTitle(settingItem: SettingItem): SettingItemTitleBinding {
        val binding = SettingItemTitleBinding.inflate(inflater, container, false)
        binding.title.setText(settingItem.title)
        return binding
    }

    private fun setVisibility(
        variable: String,
        variableBoolean: Boolean,
    ) {
        val trueKey = "$variable=true"
        val falseKey = "$variable=false"
        viewBinding.forEach { (item, binding) ->
            if (item.regex == trueKey) {
                binding.root.visibility = if (variableBoolean) View.VISIBLE else View.GONE
            } else if (item.regex == falseKey) {
                binding.root.visibility = if (variableBoolean) View.GONE else View.VISIBLE
            }
        }
    }

    private fun renderSwitch(settingItem: SettingItem): SettingItemSwitchBinding {
        val binding = SettingItemSwitchBinding.inflate(inflater, container, false)
        binding.title.setText(settingItem.title)
        settingItem.subTitle?.let {
            binding.subTitle.setText(it)
        } ?: run {
            binding.subTitle.visibility = View.GONE
        }

        fun setLinkVisibility(isChecked: Boolean) {
            settingItem.variable?.apply {
                setVisibility(this, isChecked)
            }
        }

        settingItem.icon?.let {
            binding.icon.setImageDrawable(AppCompatResources.getDrawable(context, it))
        } ?: run {
            binding.icon.visibility = View.GONE
        }

        fun onClickSwitch() {
            val isChecked = binding.switchWidget.isChecked
            setLinkVisibility(isChecked)
            settingItem.onItemClick?.invoke(isChecked, context) ?: settingItem.key?.let {
                ConfigUtils.putBoolean(
                    it,
                    isChecked,
                )
            }
            settingItem.onSavedValue?.invoke(isChecked, context)
        }

        binding.root.setOnClickListener {
            binding.switchWidget.isChecked = !binding.switchWidget.isChecked
            onClickSwitch()
        }

        binding.switchWidget.setOnClickListener {
            onClickSwitch()
        }

        resume[settingItem] = {
            binding.switchWidget.isChecked = settingItem.onGetKeyValue?.invoke()?.let {
                it as Boolean
            } ?: settingItem.key?.let {
                getFromSp(it, (settingItem.default ?: false)) as Boolean
            } ?: false
            setLinkVisibility(binding.switchWidget.isChecked)
        }

        return binding
    }

    private fun renderText(settingItem: SettingItem): SettingItemTextBinding {
        val binding = SettingItemTextBinding.inflate(inflater, container, false)
        binding.title.setText(settingItem.title)
        settingItem.subTitle?.let {
            binding.subTitle.setText(it)
        } ?: run {
            binding.subTitle.visibility = View.GONE
        }

        settingItem.icon?.let {
            binding.icon.setImageDrawable(AppCompatResources.getDrawable(context, it))
        } ?: run {
            binding.icon.visibility = View.GONE
        }

        settingItem.link?.apply {
            binding.root.setOnClickListener {
                CustomTabsHelper.launchUrlOrCopy(context, this)
            }
        } ?: run {
            binding.root.setOnClickListener {
                settingItem.onItemClick?.invoke("", context)
            }
        }

        resume[settingItem] = {
            val savedValue =
                settingItem.onGetKeyValue?.invoke() ?: getFromSp(
                    settingItem.key ?: "",
                    settingItem.default ?: "",
                )

            settingItem.selectList?.let {
                fun setValue(savedValue: Any) {
                    for ((key, value) in it) {
                        if (value == savedValue) {
                            binding.subTitle.text = key
                            binding.subTitle.visibility = View.VISIBLE
                            break
                        }
                    }
                }

                setValue(savedValue)

                val listPopupUtils =
                    ListPopupUtils(context, binding.title, it, savedValue) { pos, key, value ->
                        binding.subTitle.text = key

                        settingItem.onItemClick?.invoke(value, context) ?: settingItem.key?.let { item ->
                            ConfigUtils.putString(item, value.toString())
                            saveToSp(item, value)
                        }

                        settingItem.onSavedValue?.invoke(value, context)
                        setValue(value)
                    }

                binding.root.setOnClickListener {
                    listPopupUtils.toggle()
                }
            } ?: run {
                val savedValueString = savedValue.toString()
                if (savedValueString.isNotEmpty()) {
                    binding.subTitle.text = savedValueString
                    binding.subTitle.visibility = View.VISIBLE
                }
            }
        }

        //    setCenterItem(binding.title,binding.subTitle)

        return binding
    }

    private fun renderInput(settingItem: SettingItem): SettingItemInputBinding {
        val binding = SettingItemInputBinding.inflate(inflater, container, false)
        resume[settingItem] = {
            settingItem.onGetKeyValue?.invoke()?.let {
                binding.input.setText(it.toString())
            } ?: run {
                settingItem.key?.apply {
                    binding.input.setText(
                        ConfigUtils.getString(
                            settingItem.key,
                            (settingItem.default ?: "").toString(),
                        ),
                    )
                }
            }
        }
        destroy[settingItem] = {
            val result = binding.input.text.toString()
            settingItem.onItemClick?.invoke(result, context)
                ?: settingItem.key?.let {
                    ConfigUtils.putString(
                        it,
                        result,
                    )
                }
            settingItem.onSavedValue?.invoke(result, context)
        }

        binding.inputLayout.setHint(settingItem.title)
        settingItem.subTitle?.let {
            binding.inputLayout.helperText = context.getString(it)
        }
        return binding
    }

    private fun renderColor(settingItem: SettingItem): SettingItemColorBinding {
        val binding = SettingItemColorBinding.inflate(inflater, container, false)
        binding.title.setText(settingItem.title)
        settingItem.icon?.let {
            binding.icon.setImageDrawable(AppCompatResources.getDrawable(context, it))
        } ?: run {
            binding.icon.visibility = View.GONE
        }
        resume[settingItem] = {
            val color = context.getColor(settingItem.onGetKeyValue?.invoke() as Int)
            binding.colorView.setCardBackgroundColor(color)
        }

        binding.root.setOnClickListener {
            settingItem.onItemClick?.invoke("", context)
        }
        return binding
    }

    private fun getFromSp(
        key: String,
        default: Any,
    ): Any {
        return when (default) {
            is Boolean -> ConfigUtils.getBoolean(key, default)
            is String -> ConfigUtils.getString(key, default)
            is Int -> ConfigUtils.getInt(key, default)
            else -> default
        }
    }

    private fun saveToSp(
        key: String,
        value: Any,
    ) {
        when (value) {
            is Boolean -> ConfigUtils.putBoolean(key, value)
            is String -> ConfigUtils.putString(key, value)
            is Int -> ConfigUtils.putInt(key, value)
        }
    }
}
