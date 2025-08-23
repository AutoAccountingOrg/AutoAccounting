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
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import net.ankio.auto.databinding.ComponentRuleInfoBinding
import net.ankio.auto.utils.PrefManager
import org.ezbook.server.constant.BillType
import org.ezbook.server.constant.DefaultData
import org.ezbook.server.constant.Setting

/**
 * 规则信息组件 - 专用于账单编辑对话框
 *
 * 职责：
 * - 显示账单匹配的规则名称
 * - 显示转账手续费（仅转账且有手续费时）
 * - 根据配置控制显示/隐藏
 *
 * 使用方式：
 * ```kotlin
 * ruleInfo.setRuleName("支付宝收款规则")
 * ruleInfo.setFee(0.01, BillType.Transfer)
 * ```
 */
class RuleInfoComponent @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: ComponentRuleInfoBinding =
        ComponentRuleInfoBinding.inflate(LayoutInflater.from(context), this)

    private lateinit var lifecycleOwner: androidx.lifecycle.LifecycleOwner
    private lateinit var billInfoModel: org.ezbook.server.db.model.BillInfoModel

    init {
        orientation = VERTICAL
    }

    /**
     * 统一初始化方法 - 参考BookHeaderComponent.initBillInfo
     */
    fun initBillInfo(
        billInfoModel: org.ezbook.server.db.model.BillInfoModel,
        lifecycleOwner: androidx.lifecycle.LifecycleOwner
    ) {
        this.lifecycleOwner = lifecycleOwner
        this.billInfoModel = billInfoModel
        refresh()
    }

    /**
     * 刷新显示 - 根据当前账单信息更新UI
     */
    fun refresh() {
        if (!::billInfoModel.isInitialized) return

        val billType = net.ankio.auto.utils.BillTool.getType(billInfoModel.type)
        setInfo(billInfoModel.ruleName, billInfoModel.fee, billType)
    }

    /**
     * 设置规则名称
     * 根据配置决定是否显示规则名称
     *
     * @param ruleName 规则名称
     */
    fun setRuleName(ruleName: String) {
        if (PrefManager.showRuleName) {
            binding.ruleName.visibility = View.VISIBLE
            binding.ruleName.setText(ruleName)
        } else {
            binding.ruleName.visibility = View.GONE
        }
    }

    /**
     * 设置手续费显示
     * 只有转账且开启手续费功能且有手续费时才显示
     *
     * @param fee 手续费金额
     * @param billType 账单类型
     */
    fun setFee(fee: Double, billType: BillType) {
        val shouldShowFee = PrefManager.featureFee &&
                billType == BillType.Transfer &&
                fee > 0.0

        if (shouldShowFee) {
            binding.fee.visibility = View.VISIBLE
            binding.fee.setText(fee.toString())
        } else {
            binding.fee.visibility = View.GONE
        }
    }

    /**
     * 同时设置规则名称和手续费
     *
     * @param ruleName 规则名称
     * @param fee 手续费金额
     * @param billType 账单类型
     */
    fun setInfo(ruleName: String, fee: Double, billType: BillType) {
        setRuleName(ruleName)
        setFee(fee, billType)
    }
}
