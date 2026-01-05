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

import android.content.res.ColorStateList
import android.text.InputType
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import net.ankio.auto.R
import net.ankio.auto.databinding.ComponentAmountDisplayBinding
import net.ankio.auto.ui.api.BaseComponent
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.dialog.BillEditorDialog
import net.ankio.auto.ui.dialog.EditorDialogBuilder

import net.ankio.auto.ui.utils.ListPopupUtilsGeneric
import net.ankio.auto.utils.BillTool
import net.ankio.auto.utils.PrefManager
import org.ezbook.server.constant.BillType
import org.ezbook.server.db.model.BillInfoModel
import kotlin.math.abs

/**
 * 金额显示组件 - 专用于账单编辑对话框
 *
 * 职责：
 * - 显示交易金额
 * - 提供交易类型选择功能（支出/收入/转账）
 * - 根据交易类型设置相应的颜色和图标
 * - 自动更新账单信息中的类型数据
 *
 * 使用方式：
 * ```kotlin
 * val amountDisplay: AmountDisplayComponent = binding.amountDisplay.bindAs()
 * amountDisplay.setBillInfo(billInfoModel)
 * // 点击时会自动弹出类型选择列表并更新账单信息
 * ```
 */
class AmountDisplayComponent(
    binding: ComponentAmountDisplayBinding
) : BaseComponent<ComponentAmountDisplayBinding>(binding) {

    private var currentBillType: BillType = BillType.Expend


    private lateinit var billInfoModel: BillInfoModel

    /**
     * 设置账单信息
     */
    fun setBillInfo(billInfoModel: BillInfoModel) {
        this.billInfoModel = billInfoModel
        refresh()
        setupClickListeners()
    }

    /**
     * 刷新显示 - 根据当前账单信息更新UI
     */
    fun refresh() {
        if (!::billInfoModel.isInitialized) return

        // 更新金额显示
        setAmount(billInfoModel.money)

        // 更新账单类型
        currentBillType = BillTool.getType(billInfoModel.type)
        setBillType(currentBillType)

        // 更新费用显示
        setFeeDisplay(billInfoModel.fee)
    }

    /**
     * 设置所有点击监听器
     */
    private fun setupClickListeners() {
        if (!::billInfoModel.isInitialized) return

        // 设置金额编辑点击监听器
        setupAmountEditor()

        // 设置类型选择点击监听器
        setupTypeSelector()

        // 设置费用编辑点击监听器  
        setupFeeEditor()
    }

    /**
     * 设置金额编辑器
     */
    private fun setupAmountEditor() {
        binding.amountContainer.setOnClickListener {
            BaseSheetDialog.create<EditorDialogBuilder>(context)
                .setInputType(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
                .setTitleInt(R.string.edit_amount)
                .setMessage(billInfoModel.money.toString())
                .setEditorPositiveButton(R.string.sure_msg) { result ->
                    val newAmount = result.toDoubleOrNull() ?: 0.0
                    if (newAmount != billInfoModel.money) {
                        val oldAmount = billInfoModel.money
                        billInfoModel.money = newAmount
                        refresh()
                    }
                }
                .setNegativeButton(R.string.cancel_msg, null)
                .show()
        }
    }

    /**
     * 设置类型选择器
     */
    private fun setupTypeSelector() {
        // 创建可用的交易类型映射；仅在启用资产管理功能时显示“转账”
        val availableTypes = linkedMapOf(
            context.getString(R.string.float_expend) to BillType.Expend,
            context.getString(R.string.float_income) to BillType.Income
        )
        if (PrefManager.featureAssetManage) {
            availableTypes[context.getString(R.string.float_transfer)] = BillType.Transfer
        }

        // 为type_label设置点击监听器
        binding.typeLabel.setOnClickListener {
            ListPopupUtilsGeneric.create<BillType>(context)
                .setAnchor(binding.typeLabel)
                .setList(availableTypes)
                .setSelectedValue(currentBillType)
                .setOnItemClick { _, _, value ->
                    if (value != currentBillType) {
                        val oldType = billInfoModel.type
                        currentBillType = value
                        billInfoModel.type = value
                        updateTypeDisplay()
                        // 发送类型变化事件
                        launch {
                            BillEditorDialog.notifyRefresh()
                        }
                    }
                }
                .show()
        }


    }

    /**
     * 设置费用编辑器
     */
    private fun setupFeeEditor() {
        binding.feeContainer.setOnClickListener {
            BaseSheetDialog.create<EditorDialogBuilder>(context)
                .setInputType(
                    InputType.TYPE_CLASS_NUMBER or
                            InputType.TYPE_NUMBER_FLAG_DECIMAL or
                            InputType.TYPE_NUMBER_FLAG_SIGNED
                )
                .setTitleInt(R.string.edit_fee)
                .setHint(R.string.hint_fee)
                .setMessage(if (billInfoModel.fee == 0.0) "" else billInfoModel.fee.toString())
                .setEditorPositiveButton(R.string.sure_msg) { result ->
                    val newFee = result.toDoubleOrNull() ?: 0.0
                    if (newFee != billInfoModel.fee) {
                        billInfoModel.fee = newFee
                        setFeeDisplay(newFee)
                    }
                }
                .setNegativeButton(R.string.cancel_msg, null)
                .show()
        }
    }

    /**
     * 设置金额显示
     *
     * @param amount 金额数值
     */
    private fun setAmount(amount: Double) {
        binding.amountContainer.text = amount.toString()
    }

    /**
     * 设置账单类型并更新UI样式
     *
     * @param billType 账单类型
     */
    private fun setBillType(billType: BillType) {
        currentBillType = billType
        updateTypeDisplay()
        updateTypeLabelDisplay()
    }

    /**
     * 设置费用显示（仅支出、转账显示）
     *
     * 规则（自动记账）：
     * - fee < 0 表示【手续费】，展示为手续费 abs(fee)
     * - fee > 0 表示【优惠】，展示为优惠 fee
     * - fee = 0 展示为无优惠
     *
     * 示例（仅说明含义，组件不计算 total）：
     * - 支出优惠: total = money + fee
     * - 还款手续费: total = money - fee
     * - 还款优惠: total = money + fee
     *
     * @param fee 费用数值
     */
    private fun setFeeDisplay(fee: Double) {
        val showForType = currentBillType == BillType.Expend || currentBillType == BillType.Transfer
        val visible = PrefManager.featureFee && showForType
        binding.feeContainer.isVisible = visible
        if (!visible) return

        val text = when {
            fee == 0.0 -> context.getString(R.string.no_discount)
            fee < 0 -> context.getString(R.string.handling_fee, abs(fee))
            else -> context.getString(R.string.discounted, fee)
        }
        binding.feeContainer.text = text
    }


    /**
     * 更新类型标签显示
     */
    private fun updateTypeLabelDisplay() {
        // 当未启用资产管理时，将“转账”按“支出”标签展示
        val displayType =
            if (currentBillType == BillType.Transfer && !PrefManager.featureAssetManage) {
                BillType.Expend
            } else currentBillType

        val typeText = when (displayType) {
            BillType.Expend -> context.getString(R.string.float_expend)
            BillType.Income -> context.getString(R.string.float_income)
            BillType.Transfer -> context.getString(R.string.float_transfer)
            else -> context.getString(R.string.float_expend)
        }
        binding.typeLabel.text = typeText
    }

    /**
     * 根据当前账单类型更新显示样式
     * 包括图标、颜色等视觉元素
     */
    private fun updateTypeDisplay() {
        // 当未启用资产管理时，将“转账”的视觉样式按“支出”处理
        val displayType =
            if (currentBillType == BillType.Transfer && !PrefManager.featureAssetManage) {
                BillType.Expend
            } else currentBillType

        val drawableRes = when (displayType) {
            BillType.Expend -> R.drawable.float_minus
            BillType.Income -> R.drawable.float_add
            BillType.Transfer -> R.drawable.float_round
            else -> R.drawable.float_minus
        }

        val tintRes = BillTool.getColor(displayType)
        val drawable = AppCompatResources.getDrawable(context, drawableRes)
        val tint = ColorStateList.valueOf(ContextCompat.getColor(context, tintRes))
        val color = ContextCompat.getColor(context, tintRes)

        binding.amountContainer.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
        TextViewCompat.setCompoundDrawableTintList(binding.amountContainer, tint)
        binding.amountContainer.setTextColor(color)
    }
}
