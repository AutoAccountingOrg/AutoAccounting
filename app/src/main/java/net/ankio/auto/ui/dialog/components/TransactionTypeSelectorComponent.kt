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
import androidx.core.view.isVisible
import net.ankio.auto.R
import net.ankio.auto.databinding.ComponentTransactionTypeSelectorBinding
import net.ankio.auto.ui.api.BaseComponent
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.ui.dialog.BillEditorDialog
import net.ankio.auto.utils.BillTool
import net.ankio.auto.storage.Logger
import org.ezbook.server.constant.BillType
import org.ezbook.server.db.model.BillInfoModel
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * 交易类型选择器组件 - 专用于账单编辑对话框
 *
 * 核心功能：
 * 允许用户在支出/收入主类型的基础上，进一步选择具体的子类型（如报销、借贷等）
 *
 * 设计思路：
 * 1. 【三大主类型】将所有账单类型归类为：支出(Expend)、收入(Income)、转账(Transfer)
 * 2. 【子类型扩展】每个主类型下可以有多种业务子类型，满足不同的记账需求
 * 3. 【配置驱动】通过SubTypeConfig配置表统一管理所有类型关系和显示规则
 * 4. 【功能开关】根据用户启用的功能模块动态显示/隐藏对应的子类型选项
 *
 * 类型层次结构详解：
 * ┌─ 支出(Expend)
 * │  ├─ ExpendReimbursement(支出报销) - 先垫付后报销的支出
 * │  ├─ ExpendLending(借出) - 借钱给他人
 * │  └─ ExpendRepayment(支出还款) - 还钱给他人
 * │
 * ├─ 收入(Income)
 * │  ├─ IncomeReimbursement(收入报销) - 收到的报销款
 * │  ├─ IncomeLending(借入) - 从他人借钱
 * │  ├─ IncomeRepayment(收入还款) - 他人还钱给我
 * │  └─ IncomeRefund(退款) - 收到的退款
 * ├─ 还款（TODO）
 * └─ 转账(Transfer) - 账户间资金转移，无子类型
 *
 * 工作流程：
 * 1. initBillInfo() - 初始化账单信息和生命周期
 * 2. refresh() - 根据账单类型刷新UI显示
 * 3. setupForBillType() - 配置主类型对应的子类型选项
 * 4. setupSubTypesForMainType() - 根据功能开关控制子类型显示
 * 5. 用户选择 → setupChipGroupListener() 处理 → 更新账单类型
 *
 * 使用方式：
 * ```kotlin
 * val transactionTypeSelector: TransactionTypeSelectorComponent = binding.transactionTypeSelector.bindAs()
 * transactionTypeSelector.setBillInfo(billInfoModel)
 * ```
 */
class TransactionTypeSelectorComponent(
    binding: ComponentTransactionTypeSelectorBinding
) : BaseComponent<ComponentTransactionTypeSelectorBinding>(binding) {


    private var mainBillType: BillType = BillType.Expend

    private lateinit var billInfoModel: BillInfoModel



    override fun onComponentCreate() {
        super.onComponentCreate()
        setupChipGroupListener()  // 初始化chip选择监听器
    }

    /**
     * 设置账单信息
     *
     * 这是组件的主要入口方法，完成以下工作：
     * 1. 保存账单信息模型的引用，用于后续的类型判断和更新
     * 2. 刷新UI显示，根据当前账单类型配置界面
     * 3. 设置自动类型选择，建立用户操作与数据更新的连接
     *
     * @param billInfoModel 账单信息模型，包含当前账单的所有数据
     */
    fun setBillInfo(billInfoModel: BillInfoModel) {
        this.billInfoModel = billInfoModel      // 保存账单信息模型
        refresh()                               // 根据当前账单信息刷新UI

    }


    /**
     * 刷新显示 - 根据当前账单信息更新UI
     *
     * 刷新流程说明：
     * 1. 从账单的具体类型（如ExpendReimbursement）提取主类型（如Expend）
     * 2. 根据主类型配置对应的子类型选项显示
     * 3. 根据账单的具体类型设置对应的chip为选中状态
     *
     * 例如：账单类型为ExpendReimbursement时
     * → mainBillType = Expend（主类型）
     * → 显示支出相关的子类型选项
     * → chip_reimbursement被选中
     */
    fun refresh() {
        // 使用BillTool.getType将具体类型转换为主类型
        // 例如：ExpendReimbursement → Expend, IncomeRefund → Income
        val newMainBillType = BillTool.getType(billInfoModel.type)

        mainBillType = newMainBillType

        setupForBillType(mainBillType)

        // 根据具体的账单类型设置对应chip的选中状态
        setSelectedType(billInfoModel.type)
    }



    /**
     * 为指定的主账单类型配置选择器
     * 统一处理所有子类型的显示逻辑，避免重复代码
     *
     * 配置逻辑说明：
     * - 支出类型：显示组件，配置支出相关的子类型（报销、借出、还款）
     * - 收入类型：显示组件，配置收入相关的子类型（报销、借入、收款、退款）
     * - 转账类型：隐藏组件，因为转账不需要子类型区分
     * - 其他类型：隐藏组件，防止未知类型导致的显示异常
     *
     * @param billType 主账单类型（支出/收入/转账）
     */
    private fun setupForBillType(billType: BillType) {
        mainBillType = billType  // 保存当前主类型，供其他方法使用

        when (billType) {
            BillType.Expend -> {
                show()  // 显示选择器组件
                setupSubTypesForMainType(isExpendMode = true)  // 配置支出模式的子类型
            }

            BillType.Income -> {
                show()  // 显示选择器组件
                setupSubTypesForMainType(isExpendMode = false)  // 配置收入模式的子类型
            }

            BillType.Transfer -> hide()  // 转账不需要子类型选择，直接隐藏
            else -> hide()  // 未知类型也隐藏，保证界面稳定
        }
    }

    /**
     * 设置当前选中的子类型 - 7个chip一一对应7个子类型
     * @param billType 要选中的账单类型
     */
    private fun setSelectedType(billType: BillType) {
        val chipId = when (billType) {
            BillType.ExpendReimbursement -> R.id.chip_expend_reimbursement
            BillType.IncomeReimbursement -> R.id.chip_income_reimbursement
            BillType.ExpendLending -> R.id.chip_lend
            BillType.IncomeLending -> R.id.chip_borrow
            BillType.ExpendRepayment -> R.id.chip_expend_repayment
            BillType.IncomeRepayment -> R.id.chip_income_repayment
            BillType.IncomeRefund -> R.id.chip_refund
            else -> -1  // 主类型，不选中任何chip
        }

        if (chipId != -1) {
            binding.chipGroup.check(chipId)
        } else {
            binding.chipGroup.clearCheck()
        }
    }

    /**
     * 隐藏整个选择器
     */
    private fun hide() {
        binding.root.visibility = View.GONE
    }

    /**
     * 显示选择器
     */
    fun show() {
        binding.root.visibility = View.VISIBLE
    }

    /**
     * 配置子类型显示逻辑 - 根据当前账单类型和功能开关决定chip显示
     */
    private fun setupSubTypesForMainType(isExpendMode: Boolean) {
        // 支出报销chip：只在支出模式且报销功能启用时显示
        binding.chipExpendReimbursement.isVisible = isExpendMode && PrefManager.featureReimbursement

        // 收入报销chip：只在收入模式且报销功能启用时显示  
        binding.chipIncomeReimbursement.isVisible =
            !isExpendMode && PrefManager.featureReimbursement

        // 借出chip：只在支出模式且债务功能启用时显示
        binding.chipLend.isVisible =
            isExpendMode && PrefManager.featureAssetManage && PrefManager.featureDebt

        // 借入chip：只在收入模式且债务功能启用时显示
        binding.chipBorrow.isVisible =
            !isExpendMode && PrefManager.featureAssetManage && PrefManager.featureDebt

        // 支出还款chip：只在支出模式且债务功能启用时显示
        binding.chipExpendRepayment.isVisible =
            isExpendMode && PrefManager.featureAssetManage && PrefManager.featureDebt

        // 收入还款chip：只在收入模式且债务功能启用时显示
        binding.chipIncomeRepayment.isVisible =
            !isExpendMode && PrefManager.featureAssetManage && PrefManager.featureDebt

        // 退款chip：只在收入模式显示（退款总是收入）
        binding.chipRefund.isVisible = !isExpendMode
    }

    /**
     * 设置Chip组选择监听器 - 7个chip直接映射7个子类型
     */
    private fun setupChipGroupListener() {
        binding.chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull()

            // 每个chip直接对应一个具体的BillType
            val selectedType = if (checkedId == null) {
                mainBillType  // 未选中任何chip，使用主类型
            } else {
                when (checkedId) {
                    R.id.chip_expend_reimbursement -> BillType.ExpendReimbursement
                    R.id.chip_income_reimbursement -> BillType.IncomeReimbursement
                    R.id.chip_lend -> BillType.ExpendLending
                    R.id.chip_borrow -> BillType.IncomeLending
                    R.id.chip_expend_repayment -> BillType.ExpendRepayment
                    R.id.chip_income_repayment -> BillType.IncomeRepayment
                    R.id.chip_refund -> BillType.IncomeRefund
                    else -> mainBillType
                }
            }

            val oldType = billInfoModel.type
            billInfoModel.type = selectedType

            // 检查是否需要更新主类型和chip显示
            val newMainType = BillTool.getType(selectedType)
            if (newMainType != mainBillType) {
                logger.debug { "子类型选择导致主类型变化: $mainBillType -> $newMainType" }
                mainBillType = newMainType
                setupSubTypesForMainType(mainBillType == BillType.Expend)
            }

            // 通知刷新
            launch {
                BillEditorDialog.notifyRefresh()
                logger.debug { "账单类型已更新: $oldType -> $selectedType" }
            }
        }
    }
}
