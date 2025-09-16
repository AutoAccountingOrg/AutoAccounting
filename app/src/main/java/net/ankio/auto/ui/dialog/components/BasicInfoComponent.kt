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

package net.ankio.auto.ui.dialog.components

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.launch
import net.ankio.auto.databinding.ComponentBasicInfoBinding
import net.ankio.auto.ui.api.BaseComponent
import net.ankio.auto.http.api.BookNameAPI
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.ui.dialog.CategorySelectorDialog
import net.ankio.auto.ui.dialog.DateTimePickerDialog
import net.ankio.auto.ui.utils.ListPopupUtilsGeneric
import net.ankio.auto.ui.adapter.CurrencyDropdownAdapter
import android.widget.ListPopupWindow
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.utils.load
import net.ankio.auto.ui.utils.setCategoryIcon
import net.ankio.auto.ui.utils.setAssetIcon
import net.ankio.auto.utils.BillTool
import net.ankio.auto.utils.DateUtils
import net.ankio.auto.utils.SystemUtils.findLifecycleOwner
import org.ezbook.server.constant.BillType
import org.ezbook.server.constant.Currency
import org.ezbook.server.constant.DefaultData
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.model.BillInfoModel
import org.ezbook.server.db.model.BookNameModel
import org.ezbook.server.db.model.CategoryModel

/**
 * 基础信息组件 - 参考BookHeaderComponent的独立设计模式
 *
 * 职责：
 * - 显示分类、货币、时间、备注信息
 * - 处理分类点击事件，自动弹出分类选择对话框
 * - 处理时间点击事件，自动弹出时间选择对话框
 * - 处理货币点击事件，自动弹出货币选择列表
 * - 自动更新账单信息中的相关数据
 * - 实时同步备注文本到账单模型（使用TextWatcher）
 *
 * 使用方式：
 * ```kotlin
 * val basicInfo: BasicInfoComponent = binding.basicInfo.bindAs()
 * basicInfo.setBillInfo(billInfoModel)
 * // 点击时会自动弹出相应的选择对话框并更新账单信息
 * // 备注文本会实时同步到 billInfoModel，无需手动调用 updateBillInfoFromUI()
 * ```
 */
class BasicInfoComponent(
    binding: ComponentBasicInfoBinding
) : BaseComponent<ComponentBasicInfoBinding>(binding) {

    private lateinit var billInfoModel: BillInfoModel

    override fun onComponentCreate() {
        super.onComponentCreate()
        setupClickListeners()
        setupRemarkWatcher()
    }

    /**
     * 设置账单信息
     */
    fun setBillInfo(billInfoModel: BillInfoModel) {
        this.billInfoModel = billInfoModel
        refresh()
    }

    /**
     * 刷新显示 - 根据当前账单信息更新UI
     */
    fun refresh() {
        val billType = BillTool.getType(billInfoModel.type)

        // 更新分类显示
        updateCategoryDisplay(billType)

        // 更新货币显示
        updateCurrencyDisplay()

        // 更新时间显示
        binding.time.setText(DateUtils.stampToDate(billInfoModel.time))

        // 更新备注
        binding.remark.setText(billInfoModel.remark)

        // 根据账单类型控制分类可见性
        binding.category.visibility = if (billType == BillType.Transfer) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    /**
     * 更新分类显示
     */
    private fun updateCategoryDisplay(billType: BillType) {
        binding.category.setText(billInfoModel.cateName)
        launch {
            val book = BookNameAPI.getBook(billInfoModel.bookName)
            binding.category.imageView().setCategoryIcon(
                billInfoModel.cateName,
                book.remoteId,
                billType.name
            )
        }
    }

    /**
     * 更新货币显示
     */
    private fun updateCurrencyDisplay() {
        if (!PrefManager.featureMultiCurrency) {
            binding.moneyType.visibility = View.GONE
            return
        }

        val currency =
            runCatching { Currency.valueOf(billInfoModel.currency) }.getOrDefault(Currency.CNY)
        binding.moneyType.setText(currency.name(context))
        binding.moneyType.imageView().load(currency.iconUrl())
        // 确保货币图标不被染色，保持原始颜色
        binding.moneyType.setTint(false)
        binding.moneyType.visibility = View.VISIBLE
    }


    /**
     * 设置备注监听器 - 实时同步备注到 billInfoModel
     */
    private fun setupRemarkWatcher() {
        binding.remark.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (::billInfoModel.isInitialized) {
                    billInfoModel.remark = s?.toString() ?: ""
                }
            }
        })
    }

    /**
     * 设置点击事件监听器
     */
    private fun setupClickListeners() {
        binding.category.setOnClickListener {
            launch {
                val book = BookNameAPI.getBook(billInfoModel.bookName)
                showCategorySelector(book)
            }

        }

        binding.time.setOnClickListener {
            showTimeSelector()
        }

        binding.moneyType.setOnClickListener {
            showCurrencySelector()
        }
    }

    /**
     * 显示分类选择对话框
     */
    private fun showCategorySelector(book: BookNameModel) {
        if (!::billInfoModel.isInitialized) {
            return
        }

        // 使用BaseSheetDialog工厂方法创建对话框
        val dialog = BaseSheetDialog.create<CategorySelectorDialog>(context)

        val billType = BillTool.getType(billInfoModel.type)



        dialog.setBook(book.remoteId)
            .setType(billType)
            .setCallback { parent, child ->
                // 更新分类名称
                billInfoModel.cateName = BillTool.getCateName(parent?.name ?: "", child?.name)
                // 刷新显示
                refresh()
            }
            .show()
    }

    /**
     * 显示时间选择对话框
     */
    private fun showTimeSelector() {
        if (!::billInfoModel.isInitialized) {
            return
        }

        // 使用BaseSheetDialog工厂方法创建对话框
        val dialog = BaseSheetDialog.create<DateTimePickerDialog>(context)

        dialog.setDateTimeFromMillis(billInfoModel.time)
            .setOnDateTimeSelected { year, month, day, hour, minute ->
                // 更新时间戳
                billInfoModel.time = DateUtils.getTime(year, month, day, hour, minute, 0)
                // 刷新显示
                refresh()
            }
            .show()
    }

    /**
     * 显示货币选择列表 - 使用带图标的CurrencyDropdownAdapter
     */
    private fun showCurrencySelector() {
        if (!::billInfoModel.isInitialized) {
            return
        }

        // 获取当前选中的货币
        val currentCurrency = runCatching {
            Currency.valueOf(billInfoModel.currency)
        }.getOrDefault(Currency.CNY)

        // 创建货币列表
        val currencyList = Currency.entries.toList()

        // 创建带图标的货币适配器
        val adapter = CurrencyDropdownAdapter(context, currencyList)

        // 创建ListPopupWindow显示选择列表
        val popupWindow = ListPopupWindow(context).apply {
            setAdapter(adapter)
            anchorView = binding.moneyType
            width = ListPopupWindow.WRAP_CONTENT
            height = ListPopupWindow.WRAP_CONTENT
            isModal = true

            setOnItemClickListener { _, _, position, _ ->
                val selectedCurrency = currencyList[position]
                // 更新货币类型
                billInfoModel.currency = selectedCurrency.name
                // 刷新显示
                refresh()
                dismiss()
            }
        }

        // 显示弹窗并选中当前项
        popupWindow.show()
        val currentIndex = currencyList.indexOf(currentCurrency)
        if (currentIndex >= 0) {
            popupWindow.listView?.setSelection(currentIndex)
        }
    }
}
