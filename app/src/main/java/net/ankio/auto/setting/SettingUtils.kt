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

import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.InputType
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.util.forEach
import androidx.core.widget.addTextChangedListener
import androidx.viewbinding.ViewBinding
import com.google.android.material.textfield.TextInputLayout
import net.ankio.auto.databinding.SettingItemCardBinding
import net.ankio.auto.databinding.SettingItemColorBinding
import net.ankio.auto.databinding.SettingItemInputBinding
import net.ankio.auto.databinding.SettingItemSwitchBinding
import net.ankio.auto.databinding.SettingItemTextBinding
import net.ankio.auto.databinding.SettingItemTitleBinding
import net.ankio.auto.storage.ConfigUtils
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseActivity
import net.ankio.auto.ui.utils.ListPopupUtils
import net.ankio.auto.utils.CustomTabsHelper

/**
 * 设置项工具类，用于管理和渲染设置界面的各个项目
 * @param context 基础Activity上下文
 * @param container 容器视图组
 * @param inflater 布局填充器
 * @param settingItems 设置项列表
 */
class SettingUtils(
    private val context: BaseActivity,
    private val container: ViewGroup,
    private val inflater: LayoutInflater,
    private val settingItems: List<SettingItem>
) {
    private val viewBindings = SparseArray<ViewBinding>()
    private val resumeCallbacks = HashMap<SettingItem, () -> Unit>()
    private val destroyCallbacks = HashMap<SettingItem, () -> Unit>()
    private val visibilityConditions = HashMap<String, Boolean>()


    fun init() {
        settingItems.forEach { item ->
            try {
                val binding = when (item) {
                    is SettingItem.Switch -> renderSwitch(item)
                    is SettingItem.Title -> renderTitle(item)
                    is SettingItem.Text -> renderText(item)
                    is SettingItem.Input -> renderInput(item)
                    is SettingItem.Select -> renderSelect(item)
                    is SettingItem.Color -> renderColor(item)
                    is SettingItem.Card -> renderCard(item)
                }
                viewBindings.put(item.hashCode(), binding)
                container.addView(binding.root)
            } catch (e: Exception) {
                Logger.e("Failed to render setting item: ${item.title}", e)
            }
        }
    }

    private fun renderCard(item: SettingItem): ViewBinding {
        return SettingItemCardBinding.inflate(inflater, container, false).apply {
            title.setText(item.title)
        }
    }

    fun onResume() {
        resumeCallbacks.forEach { it.value.invoke() }
    }

    fun onDestroy() {
        destroyCallbacks.forEach { it.value.invoke() }
        viewBindings.clear()
        resumeCallbacks.clear()
        destroyCallbacks.clear()
        visibilityConditions.clear()
    }
    private fun updateVisibility() {
        viewBindings.forEach { _, binding ->
            val item = settingItems.find { viewBindings[it.hashCode()] == binding }
            item?.let {
                val regex = when (it) {
                    is SettingItem.Switch -> it.regex
                    is SettingItem.Text -> it.regex
                    is SettingItem.Input -> it.regex
                    is SettingItem.Select -> it.regex
                    is SettingItem.Color -> it.regex
                    is SettingItem.Title -> it.regex
                    is SettingItem.Card -> it.regex
                }

                regex?.let { conditions ->
                    val isVisible = conditions.split(",").all { condition ->
                        isConditionMet(condition.trim())
                    }
                    binding.root.visibility = if (isVisible) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun isConditionMet(condition: String): Boolean {
        return try {
            val (key, expectedValue) = condition.split("=")
            val actualValue = visibilityConditions[key] ?: false
            actualValue.toString() == expectedValue
        } catch (e: Exception) {
            Logger.e("Invalid condition format: $condition", e)
            false
        }
    }
    private fun bindCommonViews(binding: ViewBinding, item: SettingItem) {
        when (binding) {
            is SettingItemSwitchBinding -> {
                binding.title.setText(item.title)
                (item as? SettingItem.Switch)?.icon?.let {
                    binding.icon.setImageDrawable(AppCompatResources.getDrawable(context, it))
                } ?: run {
                    binding.icon.visibility = View.GONE
                }
            }
            is SettingItemTextBinding -> {
                binding.title.setText(item.title)
                when (item) {
                    is SettingItem.Text -> {
                        var setImage = false;
                        (item as? SettingItem.Text)?.icon?.let {
                            setImage = true
                            binding.icon.setImageDrawable(AppCompatResources.getDrawable(context, it))
                        }
                        (item as? SettingItem.Text)?.drawable?.let {
                            setImage = true
                            it.invoke()?.let { drawable ->
                                binding.icon.setImageDrawable(drawable)
                                binding.icon.imageTintList = null
                            }
                        }
                        if (!setImage) {
                            binding.icon.visibility = View.GONE
                        }
                    }

                    is SettingItem.Select -> {
                        (item as? SettingItem.Select)?.icon?.let {
                            binding.icon.setImageDrawable(AppCompatResources.getDrawable(context, it))
                        } ?: run {
                            binding.icon.visibility = View.GONE
                        }
                    }

                    else -> {
                        binding.icon.visibility = View.GONE
                    }
                }

            }
            is SettingItemColorBinding -> {
                binding.title.setText(item.title)
                (item as? SettingItem.Color)?.icon?.let {
                    binding.icon.setImageDrawable(AppCompatResources.getDrawable(context, it))
                } ?: run {
                    binding.icon.visibility = View.GONE
                }
            }
        }
    }

    private fun renderSwitch(item: SettingItem.Switch): SettingItemSwitchBinding {
        return SettingItemSwitchBinding.inflate(inflater, container, false).apply {
            bindCommonViews(this, item)
            
            item.subTitle?.let {
                subTitle.setText(it)
            } ?: run {
                subTitle.visibility = View.GONE
            }

            val updateSwitch = { isChecked: Boolean ->
                item.key?.let { ConfigUtils.putBoolean(it, isChecked) }
                item.onItemClick?.invoke(isChecked, context)
                item.onSavedValue?.invoke(isChecked, context)
                item.key?.let { key ->
                    visibilityConditions[key] = isChecked
                    updateVisibility()
                }
            }


            root.setOnClickListener {
                switchWidget.isChecked = !switchWidget.isChecked
               // updateSwitch(switchWidget.isChecked)
            }
            var firstSwitchBinding = true

            switchWidget.setOnCheckedChangeListener { _, isChecked ->
                if (firstSwitchBinding) {
                    firstSwitchBinding = false
                    return@setOnCheckedChangeListener
                }
                updateSwitch(isChecked)
            }

            resumeCallbacks[item] = {
                val savedValue = item.onGetKeyValue?.invoke() 
                    ?: item.key?.let { ConfigUtils.getBoolean(it, item.default) }?: item.default
                
                switchWidget.isChecked = savedValue
                item.key?.let { key ->
                    visibilityConditions[key] = savedValue
                    updateVisibility()
                }
            }
        }
    }

    private fun renderTitle(item: SettingItem.Title): SettingItemTitleBinding {
        return SettingItemTitleBinding.inflate(inflater, container, false).apply {
            title.setText(item.title)
        }
    }

    private fun renderText(item: SettingItem.Text): SettingItemTextBinding {
        return SettingItemTextBinding.inflate(inflater, container, false).apply {

            bindCommonViews(this, item)


            val setData = {
                if ( item.subTitle == null) {
                    if (item.onGetKeyValue != null) {
                        val savedValue = item.onGetKeyValue.invoke()
                        if (!savedValue.isNullOrEmpty()) {
                            subTitle.visibility = View.VISIBLE
                            subTitle.text = savedValue
                        }

                    }
                }else {
                    subTitle.setText(item.subTitle)
                    subTitle.visibility = View.VISIBLE
                }
            }

            resumeCallbacks[item] = {
                setData()
            }



            root.setOnClickListener {
                item.link?.let { link ->
                    CustomTabsHelper.launchUrl(context, Uri.parse(link))
                }
                item.onItemClick?.invoke(context,this)
            }
        }
    }

    private fun renderSelect(item: SettingItem.Select): SettingItemTextBinding {
        return SettingItemTextBinding.inflate(inflater, container, false).apply {
            bindCommonViews(this, item)

            val setData = { value:Any ->
                subTitle.text = item.selectList.entries.find { it.value == value }?.key
                subTitle.visibility = View.VISIBLE
                item.selectList.entries.forEach {
                    visibilityConditions["${item.key}_${it.value}"] = false
                }
                if (item.key != null && value is String) {
                    visibilityConditions["${item.key}_$value"] = true
                    updateVisibility()
                }
            }
            val updateValue = { value: Any ->
                item.onSavedValue?.invoke(value, context)
                if (item.key != null) saveToSp(item.key, value)
                setData(value)
            }

            fun getData() = item.onGetKeyValue?.invoke()
                ?: item.key?.let { getFromSp(it, item.default) }?: item.default

            resumeCallbacks[item] = {
                val data = getData()
                setData(data)
            }

            root.setOnClickListener {
                ListPopupUtils(
                    context,
                    title,
                    item.selectList,
                    getData()
                ) { _, _, value ->
                    updateValue(value)
                }.toggle()
            }
        }
    }

    private fun renderInput(item: SettingItem.Input): SettingItemInputBinding {
        return SettingItemInputBinding.inflate(inflater, container, false).apply {
            inputLayout.setHint(item.title)
            
            if (item.isPassword) {
                inputLayout.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
                input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }

            item.subTitle?.let {
                inputLayout.helperText = context.getString(it)
            }

            resumeCallbacks[item] = {
                val savedValue = item.onGetKeyValue?.invoke()
                    ?: getFromSp(item.key, item.default)
                input.setText(savedValue.toString())
            }

            input.addTextChangedListener(afterTextChanged = { editable ->
                val text = editable.toString()
                saveToSp(item.key, text)
                item.onSavedValue?.invoke(text, context)
            })

            destroyCallbacks[item] = {
                val text = input.text.toString()
                item.onItemClick?.invoke(text, context)
            }
        }
    }

    private fun renderColor(item: SettingItem.Color): SettingItemColorBinding {
        return SettingItemColorBinding.inflate(inflater, container, false).apply {
            bindCommonViews(this, item)

            val updateColor = { color: Int ->
                colorView.setCardBackgroundColor(context.getColor(color))
            }

            resumeCallbacks[item] = {
                val savedColor = item.onGetKeyValue.invoke()
                updateColor(savedColor)
            }

            root.setOnClickListener {
                item.onItemClick?.invoke(item.onGetKeyValue.invoke(), context)
            }
        }
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
