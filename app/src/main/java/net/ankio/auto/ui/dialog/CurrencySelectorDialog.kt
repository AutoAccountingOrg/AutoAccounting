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

package net.ankio.auto.ui.dialog

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import net.ankio.auto.databinding.DialogCurrencySelectBinding
import net.ankio.auto.ui.adapter.CurrencySelectorAdapter
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.components.WrapContentLinearLayoutManager
import org.ezbook.server.constant.Currency

/**
 * 币种选择对话框
 *
 * 支持两种使用模式：
 * 1. 多选模式（默认）：勾选常用币种列表，适用于设置页面
 * 2. 单选模式：选择一个币种后立即返回，适用于账单编辑
 *
 * 使用方式：
 * ```kotlin
 * // 多选模式（设置页面勾选常用币种）
 * BaseSheetDialog.create<CurrencySelectorDialog>(context)
 *     .setSelectedCodes(existingCodes)
 *     .setCallback { selectedCodes -> ... }
 *     .show()
 *
 * // 单选模式（账单编辑选择币种）
 * BaseSheetDialog.create<CurrencySelectorDialog>(context)
 *     .setSingleSelectMode(true)
 *     .setFilterCodes(favoriteCodes)
 *     .setSelectedCodes(setOf(currentCode))
 *     .setCallback { selectedCodes -> ... }
 *     .show()
 * ```
 */
class CurrencySelectorDialog internal constructor(
    context: android.content.Context
) : BaseSheetDialog<DialogCurrencySelectBinding>(context) {

    /** 适配器 */
    private lateinit var adapter: CurrencySelectorAdapter

    /** 选择完成回调 */
    private var callback: ((Set<String>) -> Unit)? = null

    /** 初始选中的币种代码 */
    private var initialCodes: Set<String> = emptySet()

    /** 单选模式：点击即选中并关闭 */
    private var singleSelectMode = false

    /** 过滤码集合：为空时展示全量，否则仅展示此集合内的币种 */
    private var filterCodes: Set<String>? = null

    /** 当前使用的币种源列表（过滤后） */
    private val sourceCurrencies: List<Currency>
        get() {
            val filter = filterCodes
            return if (filter.isNullOrEmpty()) {
                Currency.entries.toList()
            } else {
                Currency.selectedEntries(filter)
            }
        }

    /**
     * 设置初始选中的币种代码
     * @param codes 已选中的币种代码集合
     */
    fun setSelectedCodes(codes: Set<String>) = apply {
        this.initialCodes = codes
        if (::adapter.isInitialized) {
            adapter.setSelectedCodes(codes)
        }
    }

    /**
     * 设置单选模式
     * @param enabled true 时点击即选中并关闭弹窗
     */
    fun setSingleSelectMode(enabled: Boolean) = apply {
        this.singleSelectMode = enabled
    }

    /**
     * 设置过滤范围 —— 仅展示指定币种
     * @param codes 允许展示的币种代码集合，null 或空时展示全量
     */
    fun setFilterCodes(codes: Set<String>?) = apply {
        this.filterCodes = codes
    }

    /**
     * 设置选择完成回调
     * @param callback 返回选中的币种代码集合
     */
    fun setCallback(callback: (Set<String>) -> Unit) = apply {
        this.callback = callback
    }

    override fun onViewCreated(view: View?) {
        super.onViewCreated(view)

        // 初始化适配器
        adapter = CurrencySelectorAdapter(context).apply {
            setSelectedCodes(initialCodes)
            if (singleSelectMode) {
                // 单选模式：点击后立即回调并关闭
                setSingleSelectCallback { code ->
                    callback?.invoke(setOf(code))
                    dismiss()
                }
            }
        }

        // 设置 RecyclerView
        val recyclerView = binding.statusPage.contentView ?: return
        recyclerView.layoutManager = WrapContentLinearLayoutManager(context)
        recyclerView.adapter = adapter

        // 填充数据（选中项置顶）
        adapter.replaceItems(sortedBySelection(sourceCurrencies))
        binding.statusPage.showContent()

        // 单选模式下隐藏确认按钮
        binding.confirmButton.visibility = if (singleSelectMode) View.GONE else View.VISIBLE

        // 搜索过滤
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterCurrencies(s?.toString().orEmpty())
            }
        })

        // 确认按钮（多选模式）
        binding.confirmButton.setOnClickListener {
            callback?.invoke(adapter.getSelectedCodes())
            dismiss()
        }
    }

    /**
     * 根据关键词过滤币种列表
     * 匹配币种代码或本地化名称（不区分大小写）
     */
    private fun filterCurrencies(query: String) {
        val source = sourceCurrencies
        val filtered = if (query.isBlank()) {
            source
        } else {
            val lowerQuery = query.lowercase()
            source.filter {
                it.name.lowercase().contains(lowerQuery) ||
                        it.name(context).lowercase().contains(lowerQuery)
            }
        }
        adapter.replaceItems(sortedBySelection(filtered))

        if (filtered.isEmpty()) {
            binding.statusPage.showEmpty()
        } else {
            binding.statusPage.showContent()
        }
    }

    /**
     * 排序：已选中的币种置顶，其余保持原有顺序
     */
    private fun sortedBySelection(list: List<Currency>): List<Currency> {
        val selected = adapter.getSelectedCodes()
        return list.sortedByDescending { it.name in selected }
    }
}
