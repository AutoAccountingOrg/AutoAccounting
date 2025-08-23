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

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.databinding.ComponentAmountDisplayBinding
import net.ankio.auto.ui.dialog.BillEditorDialog
import net.ankio.auto.ui.utils.ListPopupUtilsGeneric
import net.ankio.auto.utils.BillTool
import org.ezbook.server.constant.BillType
import org.ezbook.server.db.model.BillInfoModel

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
 * amountDisplay.initBillInfo(billInfoModel, lifecycleOwner)
 * // 点击时会自动弹出类型选择列表并更新账单信息
 * ```
 */
class AmountDisplayComponent @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: ComponentAmountDisplayBinding =
        ComponentAmountDisplayBinding.inflate(LayoutInflater.from(context), this)
    private var onTypeChangeListener: ((BillType) -> Unit)? = null
    private var currentBillType: BillType = BillType.Expend
    private var popupUtils: ListPopupUtilsGeneric<BillType>? = null

    private lateinit var lifecycleOwner: LifecycleOwner
    private lateinit var billInfoModel: BillInfoModel

    init {
        orientation = VERTICAL
    }

    /**
     * 统一初始化方法 - 参考BookHeaderComponent.initBillInfo
     */
    fun initBillInfo(billInfoModel: BillInfoModel, lifecycleOwner: LifecycleOwner) {
        this.lifecycleOwner = lifecycleOwner
        this.billInfoModel = billInfoModel
        refresh()
        setupTypeSelector()
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
    }

    /**
     * 设置类型选择器
     */
    private fun setupTypeSelector() {
        if (!::lifecycleOwner.isInitialized || !::billInfoModel.isInitialized) return

        // 创建可用的交易类型映射
        val availableTypes = hashMapOf(
            context.getString(R.string.float_expend) to BillType.Expend,
            context.getString(R.string.float_income) to BillType.Income,
            context.getString(R.string.float_transfer) to BillType.Transfer
        )

        setupTypeSelector(availableTypes, lifecycleOwner.lifecycle)

        // 设置类型变更监听器，自动更新账单信息并发送事件
        onTypeChangeListener = { newType ->
            billInfoModel.type = newType
            // 发送刷新事件给主监听器
            lifecycleOwner.lifecycleScope.launch {
                BillEditorDialog.notifyRefresh()
            }
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
    }


    /**
     * 设置类型选择器可用选项和生命周期
     *
     * @param availableTypes 可选的交易类型映射（显示名称 -> 类型）
     * @param lifecycle 生命周期，用于弹窗管理
     */
    private fun setupTypeSelector(availableTypes: HashMap<String, BillType>, lifecycle: Lifecycle) {
        popupUtils = ListPopupUtilsGeneric(
            context,
            binding.amountContainer,
            availableTypes,
            currentBillType,
            lifecycle
        ) { _, _, value ->
            // value 已经是 BillType 类型，无需类型转换！
            currentBillType = value
            updateTypeDisplay()
            onTypeChangeListener?.invoke(value)
        }

        binding.amountContainer.setOnClickListener {
            popupUtils?.toggle()
        }
    }

    /**
     * 根据当前账单类型更新显示样式
     * 包括图标、颜色等视觉元素
     */
    private fun updateTypeDisplay() {
        val drawableRes = when (currentBillType) {
            BillType.Expend -> R.drawable.float_minus
            BillType.Income -> R.drawable.float_add
            BillType.Transfer -> R.drawable.float_round
            else -> R.drawable.float_minus
        }

        val tintRes = BillTool.getColor(currentBillType)
        val drawable = AppCompatResources.getDrawable(context, drawableRes)
        val tint = ColorStateList.valueOf(ContextCompat.getColor(context, tintRes))
        val color = ContextCompat.getColor(context, tintRes)

        binding.amountContainer.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
        TextViewCompat.setCompoundDrawableTintList(binding.amountContainer, tint)
        binding.amountContainer.setTextColor(color)
    }
}
