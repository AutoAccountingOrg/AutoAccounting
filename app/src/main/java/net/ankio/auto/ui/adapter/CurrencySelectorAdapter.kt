/*
 * Copyright (C) 2025 ankio(ankio@ankio.net)
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

package net.ankio.auto.ui.adapter

import android.content.Context
import net.ankio.auto.R
import net.ankio.auto.databinding.AdapterCurrencySelectBinding
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import net.ankio.auto.ui.utils.load
import org.ezbook.server.constant.Currency

/**
 * 币种选择适配器
 *
 * 职责：
 * - 展示币种列表（代码 + 名称 + 复选框）
 * - 管理多选/单选状态
 * - 支持外部读取选中结果
 */
class CurrencySelectorAdapter(
    private val context: Context
) : BaseAdapter<AdapterCurrencySelectBinding, Currency>() {

    /** 已选中的币种代码集合 */
    private val selectedCodes = mutableSetOf<String>()

    /** 单选回调：非空时为单选模式，点击即回调 */
    private var singleSelectCallback: ((String) -> Unit)? = null

    /**
     * 设置初始选中项
     * @param codes 已选中的币种代码集合
     */
    fun setSelectedCodes(codes: Set<String>) {
        selectedCodes.clear()
        selectedCodes.addAll(codes)
        notifyDataSetChanged()
    }

    /**
     * 设置单选回调 —— 启用后，点击条目立即回调并不维护勾选状态
     * @param callback 选中币种代码的回调
     */
    fun setSingleSelectCallback(callback: (String) -> Unit) {
        this.singleSelectCallback = callback
    }

    /**
     * 获取当前选中的币种代码集合
     */
    fun getSelectedCodes(): Set<String> = selectedCodes.toSet()

    override fun onInitViewHolder(holder: BaseViewHolder<AdapterCurrencySelectBinding, Currency>) {
        // 整行点击
        holder.binding.root.setOnClickListener {
            val currency = holder.item ?: return@setOnClickListener
            val code = currency.name

            if (singleSelectCallback != null) {
                // 单选模式：直接回调
                singleSelectCallback?.invoke(code)
            } else {
                // 多选模式：切换勾选状态
                if (selectedCodes.contains(code)) {
                    selectedCodes.remove(code)
                    holder.binding.checkbox.isChecked = false
                } else {
                    selectedCodes.add(code)
                    holder.binding.checkbox.isChecked = true
                }
            }
        }
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder<AdapterCurrencySelectBinding, Currency>,
        data: Currency,
        position: Int
    ) {
        val binding = holder.binding
        binding.currencyIcon.load(data.iconUrl(), R.drawable.float_money)
        binding.currencyCode.text = data.name
        binding.currencyName.text = data.name(context)
        binding.checkbox.isChecked = data.name in selectedCodes
    }

    override fun areItemsSame(oldItem: Currency, newItem: Currency): Boolean =
        oldItem.name == newItem.name

    override fun areContentsSame(oldItem: Currency, newItem: Currency): Boolean =
        oldItem == newItem
}
