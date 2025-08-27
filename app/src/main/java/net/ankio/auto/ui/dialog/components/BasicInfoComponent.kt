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
import net.ankio.auto.ui.api.BaseSheetDialog
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
 *
 * 使用方式：
 * ```kotlin
 * val basicInfo: BasicInfoComponent = binding.basicInfo.bindAs()
 * basicInfo.setBillInfo(billInfoModel)
 * // 点击时会自动弹出相应的选择对话框并更新账单信息
 * ```
 */
class BasicInfoComponent(
    binding: ComponentBasicInfoBinding
) : BaseComponent<ComponentBasicInfoBinding>(binding) {

    private lateinit var billInfoModel: BillInfoModel

    override fun onComponentCreate() {
        super.onComponentCreate()
        setupClickListeners()
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
        binding.moneyType.imageView().setAssetIcon(currency.iconUrl())
        binding.moneyType.visibility = View.VISIBLE
    }

    /**
     * 更新billInfoModel中的备注信息
     */
    fun updateBillInfoFromUI() {
        billInfoModel.remark = binding.remark.text.toString()
    }


    /**
     * 设置点击事件监听器
     */
    private fun setupClickListeners() {
        binding.category.setOnClickListener {
            showCategorySelector()
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
    private fun showCategorySelector() {
        if (!::billInfoModel.isInitialized) {
            return
        }

        // 使用BaseSheetDialog工厂方法创建对话框
        val dialog = BaseSheetDialog.create<CategorySelectorDialog>(context)

        val billType = BillTool.getType(billInfoModel.type)
        dialog.setBook(billInfoModel.bookName)
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
     * 显示货币选择列表
     */
    private fun showCurrencySelector() {
        if (!::billInfoModel.isInitialized) {
            return
        }

        // 创建货币映射（显示名称 -> 枚举值）
        val currencyMap = hashMapOf<String, Currency>()
        Currency.entries.forEach { currency ->
            currencyMap[currency.name(context)] = currency
        }

        // 获取当前选中的货币
        val currentCurrency = runCatching {
            Currency.valueOf(billInfoModel.currency)
        }.getOrDefault(Currency.CNY)

        // 从BaseComponent获取lifecycle
        val lifecycle = context.findLifecycleOwner().lifecycle

        // 使用 ListPopupUtilsGeneric 显示选择列表
        ListPopupUtilsGeneric.create<Currency>(context)
            .setAnchor(binding.moneyType)
            .setList(currencyMap)
            .setSelectedValue(currentCurrency)
            .setOnItemClick { _, _, selectedCurrency ->
                // 更新货币类型 - 无需类型转换
                billInfoModel.currency = selectedCurrency.name
                // 刷新显示
                refresh()
            }
            .toggle()
    }
}
