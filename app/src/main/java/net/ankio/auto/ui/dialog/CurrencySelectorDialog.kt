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
import kotlinx.coroutines.withTimeoutOrNull
import net.ankio.auto.databinding.DialogCurrencySelectBinding
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.adapter.CurrencySelectorAdapter
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.components.WrapContentLinearLayoutManager
import org.ezbook.server.constant.Currency
import org.ezbook.server.db.model.CurrencyModel
import org.ezbook.server.tools.CurrencyService

/**
 * 币种选择对话框
 *
 * 输入：String（币种代码）—— 调用方传什么就收什么
 * 内部：CurrencyModel —— 加载后全程 CurrencyModel
 * 输出：List<CurrencyModel> —— 回调返回完整模型
 *
 * 两种选择模式：
 * - 单选（账单编辑）：点击即返回，List 长度为 1
 * - 多选（设置页勾选常用币种）：确认后返回选中列表
 */
class CurrencySelectorDialog internal constructor(
    context: android.content.Context
) : BaseSheetDialog<DialogCurrencySelectBinding>(context) {

    companion object {
        /** 币种代码 → Currency 枚举缓存 */
        private val currencyMap: Map<String, Currency> by lazy {
            Currency.entries.associateBy { it.name }
        }
    }

    private lateinit var adapter: CurrencySelectorAdapter

    /** 统一回调：返回 List<CurrencyModel> */
    private var callback: ((List<CurrencyModel>) -> Unit)? = null

    /** 配置项：初始选中的币种代码 */
    private var selectedCodes: Set<String> = emptySet()

    /** 配置项：单选模式 */
    private var singleSelectMode = false

    /** 配置项：过滤范围（为空时展示全量） */
    private var filterCodes: Set<String>? = null

    /** 配置项：本位币代码 */
    private var baseCurrencyCode: String = ""

    /** 加载后的数据源：全程 CurrencyModel */
    private var sourceModels: List<CurrencyModel> = emptyList()

    // ---- 链式配置（输入全部为 String） ----

    fun setSelectedCodes(codes: Set<String>) = apply { this.selectedCodes = codes }
    fun setSingleSelectMode(enabled: Boolean) = apply { this.singleSelectMode = enabled }
    fun setFilterCodes(codes: Set<String>?) = apply { this.filterCodes = codes }
    fun setBaseCurrency(code: String) = apply { this.baseCurrencyCode = code }
    fun setCallback(callback: (List<CurrencyModel>) -> Unit) = apply { this.callback = callback }

    // ---- 生命周期 ----

    override fun onViewCreated(view: View?) {
        super.onViewCreated(view)

        Logger.d("CurrencySelector opened | mode=${if (singleSelectMode) "single" else "multi"} | base=$baseCurrencyCode | filter=${filterCodes?.size ?: "all"} | selected=$selectedCodes")

        adapter = CurrencySelectorAdapter(context).apply {
            setBaseCurrency(baseCurrencyCode)
        }

        val recyclerView = binding.statusPage.contentView ?: return
        recyclerView.layoutManager = WrapContentLinearLayoutManager(context)
        recyclerView.adapter = adapter

        binding.confirmButton.visibility = if (singleSelectMode) View.GONE else View.VISIBLE

        // 先显示 loading，数据在 show() 之后加载
        binding.statusPage.showLoading()

        // 搜索
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!binding.statusPage.isContent()) return
                filterByQuery(s?.toString().orEmpty())
            }
        })

        // 多选确认
        binding.confirmButton.setOnClickListener {
            val codes = adapter.getSelectedCodes()
            val result = sourceModels.filter { it.code in codes }
            Logger.d("CurrencySelector multi-select confirmed | count=${result.size} | codes=$codes")
            callback?.invoke(result)
            dismiss()
        }
    }

    override fun show(cancel: Boolean) {
        super.show(cancel)
        // post 到下一帧：确保 decorView 已 attach，uiReady() 完全可靠
        binding.root.post { loadModels() }
    }

    // ---- 数据加载 ----

    /** 从 CurrencyService 加载，最长等待 30 秒，超时显示错误 */
    private fun loadModels() {
        launch {
            Logger.d("CurrencySelector loading rates | base=$baseCurrencyCode")
            val allModels = withTimeoutOrNull(30_000L) {
                CurrencyService.getModels(baseCurrencyCode)
            }

            if (!uiReady()) {
                Logger.d("CurrencySelector load finished but UI destroyed, skip")
                return@launch
            }

            // 超时或加载失败
            if (allModels.isNullOrEmpty()) {
                Logger.e("CurrencySelector load failed: ${if (allModels == null) "timeout(30s)" else "empty response"}")
                binding.statusPage.showError()
                return@launch
            }

            Logger.d("CurrencySelector load success | total=${allModels.size}")

            // 按过滤范围取子集；API 未返回的补默认模型
            sourceModels = if (filterCodes.isNullOrEmpty()) {
                allModels.values.toList()
            } else {
                filterCodes!!.map { code -> allModels[code] ?: CurrencyModel(code = code) }
            }

            Logger.d("CurrencySelector data ready | display=${sourceModels.size} | selected=${selectedCodes.size}")

            // 加载完成后再设置选中状态
            adapter.setSelectedCodes(selectedCodes)

            // 单选回调
            if (singleSelectMode) {
                adapter.setSingleSelectCallback { model ->
                    Logger.d("CurrencySelector single-select: ${model.code} | rate=${model.rate}")
                    callback?.invoke(listOf(model))
                    dismiss()
                }
            }

            adapter.replaceItems(sortedBySelection(sourceModels))
            binding.statusPage.showContent()
        }
    }

    // ---- 搜索过滤 ----

    private fun filterByQuery(query: String) {
        val filtered = if (query.isBlank()) {
            sourceModels
        } else {
            val lowerQuery = query.lowercase()
            sourceModels.filter { model ->
                model.code.lowercase().contains(lowerQuery) ||
                        (currencyMap[model.code]?.name(context)?.lowercase()
                            ?.contains(lowerQuery) == true)
            }
        }
        adapter.replaceItems(sortedBySelection(filtered))
        if (filtered.isEmpty()) binding.statusPage.showEmpty() else binding.statusPage.showContent()
    }

    // ---- 辅助 ----

    private fun sortedBySelection(list: List<CurrencyModel>): List<CurrencyModel> {
        val selected = adapter.getSelectedCodes()
        return list.sortedByDescending { it.code in selected }
    }
}
