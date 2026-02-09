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
import android.view.View
import net.ankio.auto.R
import net.ankio.auto.databinding.AdapterCurrencySelectBinding
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import net.ankio.auto.ui.utils.load
import org.ezbook.server.constant.Currency
import org.ezbook.server.db.model.CurrencyModel

/**
 * 币种选择适配器
 *
 * 数据类型：CurrencyModel（直接持有 code、rate、timestamp）
 * 显示信息：通过 Currency 枚举查表获取图标和本地化名称
 */
class CurrencySelectorAdapter(
    private val context: Context
) : BaseAdapter<AdapterCurrencySelectBinding, CurrencyModel>() {

    companion object {
        /** 币种代码 → Currency 枚举缓存，避免 bind 时 valueOf + try-catch */
        private val currencyMap: Map<String, Currency> by lazy {
            Currency.entries.associateBy { it.name }
        }
    }

    /** 已选中的币种代码集合 */
    private val selectedCodes = mutableSetOf<String>()

    /** 单选回调：直接返回选中的 CurrencyModel */
    private var singleSelectCallback: ((CurrencyModel) -> Unit)? = null

    /** 本位币代码，用于汇率显示 */
    private var baseCurrencyCode: String = ""

    /** 设置选中项，仅精确刷新状态变化的条目 */
    fun setSelectedCodes(codes: Set<String>) {
        val changed = (selectedCodes - codes) + (codes - selectedCodes)
        selectedCodes.clear()
        selectedCodes.addAll(codes)
        // 仅通知选中状态实际变化的条目
        if (changed.isNotEmpty()) {
            getItems().forEachIndexed { index, model ->
                if (model.code in changed) notifyItemChanged(index)
            }
        }
    }

    /** 设置单选回调 —— 点击条目直接返回 CurrencyModel */
    fun setSingleSelectCallback(callback: (CurrencyModel) -> Unit) {
        this.singleSelectCallback = callback
    }

    /** 设置本位币代码（用于汇率展示） */
    fun setBaseCurrency(code: String) {
        this.baseCurrencyCode = code
    }

    /** 获取当前选中的币种代码集合 */
    fun getSelectedCodes(): Set<String> = selectedCodes.toSet()

    override fun onInitViewHolder(holder: BaseViewHolder<AdapterCurrencySelectBinding, CurrencyModel>) {
        holder.binding.root.setOnClickListener {
            val model = holder.item ?: return@setOnClickListener

            if (singleSelectCallback != null) {
                singleSelectCallback?.invoke(model)
            } else {
                // 多选模式：切换勾选状态
                val code = model.code
                val checked = !selectedCodes.contains(code)
                if (checked) selectedCodes.add(code) else selectedCodes.remove(code)
                holder.binding.checkbox.isChecked = checked
            }
        }
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder<AdapterCurrencySelectBinding, CurrencyModel>,
        data: CurrencyModel,
        position: Int
    ) {
        val binding = holder.binding
        val currency = currencyMap[data.code]

        // 图标 + 代码 + 名称
        binding.currencyIcon.load(currency?.iconUrl(), R.drawable.float_money)
        binding.currencyCode.text = data.code
        binding.currencyName.text = currency?.name(context) ?: data.code

        // 单选模式隐藏 checkbox，多选模式显示
        val isSingleSelect = singleSelectCallback != null
        binding.checkbox.visibility = if (isSingleSelect) View.GONE else View.VISIBLE
        if (!isSingleSelect) {
            binding.checkbox.isChecked = data.code in selectedCodes
        }

        // 汇率显示：非本位币且 rate 有效时展示
        val showRate = baseCurrencyCode.isNotEmpty()
                && data.code != baseCurrencyCode
                && data.rate > 0
        if (showRate) {
            binding.rateText.text = "≈ ${formatRate(data.rate)} $baseCurrencyCode"
            binding.rateText.visibility = View.VISIBLE
        } else {
            binding.rateText.visibility = View.GONE
        }
    }

    /** 格式化汇率数值：保留合理精度 */
    private fun formatRate(rate: Double): String = when {
        rate >= 100 -> String.format("%.0f", rate)
        rate >= 1 -> String.format("%.2f", rate)
        else -> String.format("%.4f", rate)
    }

    override fun areItemsSame(oldItem: CurrencyModel, newItem: CurrencyModel): Boolean =
        oldItem.code == newItem.code

    override fun areContentsSame(oldItem: CurrencyModel, newItem: CurrencyModel): Boolean =
        oldItem == newItem
}
