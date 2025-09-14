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
import android.widget.ArrayAdapter
import net.ankio.auto.R
import net.ankio.auto.databinding.ComponentPaymentInfoBinding
import net.ankio.auto.ui.api.BaseComponent
import net.ankio.auto.ui.components.IconView
import net.ankio.auto.ui.utils.setAssetIconByName
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.dialog.AssetsSelectorDialog
import net.ankio.auto.ui.dialog.BillSelectorDialog
import net.ankio.auto.utils.BillTool
import net.ankio.auto.http.api.AssetsAPI
import net.ankio.auto.storage.Logger
import net.ankio.auto.utils.PrefManager
import org.ezbook.server.constant.AssetsType
import org.ezbook.server.constant.BillAction
import org.ezbook.server.constant.BillType
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.model.BillInfoModel
import androidx.core.widget.doAfterTextChanged

/**
 * 支付信息组件 - 参考BookHeaderComponent的独立设计模式
 *
 * 职责：
 * - 显示账户信息和关系
 * - 处理账户点击事件，自动弹出资产选择对话框
 * - 处理账单选择事件，自动弹出账单选择对话框
 * - 根据账单类型配置UI显示模式
 * - 自动更新账单信息中的账户数据
 *
 * 使用方式：
 * ```kotlin
 * val paymentInfo: PaymentInfoComponent = binding.paymentInfo.bindAs()
 * paymentInfo.setBillInfo(billInfoModel)
 * // 点击时会自动弹出相应的选择对话框并更新账单信息
 * ```
 */
class PaymentInfoComponent(
    binding: ComponentPaymentInfoBinding
) : BaseComponent<ComponentPaymentInfoBinding>(binding) {

    private lateinit var billInfoModel: BillInfoModel

    override fun onComponentCreate() {
        super.onComponentCreate()
        setupClickListeners()
        setupDebtDropdowns()
        setupTextSync()
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
        Logger.d("刷新：$billInfoModel")
        configureUIForBillType()
        updateAccountDisplay()
        updateChooseBillDisplay()
        updateDebtDropdownOptions()
    }

    /**
     * 根据账单类型配置UI显示模式
     *
     * 业务逻辑：
     * - 普通收支：只涉及一个账户，显示单账户模式
     * - 转账：涉及两个账户间的资金流动，显示转账模式
     * - 借出/还款：从自己账户到他人，显示账户→人员模式
     * - 借入/收款：从他人到自己账户，显示人员→账户模式
     * - 退款/报销：需要关联原始账单，额外显示选择账单按钮
     */
    private fun configureUIForBillType() {
        if (!PrefManager.featureAssetManage) binding.root.visibility = View.GONE
        // 重置所有容器状态 - 确保UI状态干净
        binding.singleAccountContainer.visibility = View.GONE
        binding.transferContainer.visibility = View.GONE
        binding.debtAccountToPersonContainer.visibility = View.GONE
        binding.debtPersonToAccountContainer.visibility = View.GONE
        binding.chooseBillButton.visibility = View.GONE
        binding.root.visibility = View.VISIBLE

        when (billInfoModel.type) {
            // 普通收支：支出、收入、支出报销 - 单账户操作
            BillType.Expend, BillType.Income, BillType.ExpendReimbursement -> {
                binding.singleAccountContainer.visibility = View.VISIBLE
            }

            // 转账：资金在两个账户间流动 - 显示从账户到账户
            BillType.Transfer -> {
                binding.transferContainer.visibility = View.VISIBLE
            }

            // 借出/还款：从我的账户给别人 - 显示账户→人员
            BillType.ExpendLending, BillType.ExpendRepayment -> {
                binding.debtAccountToPersonContainer.visibility = View.VISIBLE
            }

            // 借入/收款：别人给我的账户 - 显示人员→账户
            BillType.IncomeLending, BillType.IncomeRepayment -> {
                binding.debtPersonToAccountContainer.visibility = View.VISIBLE
            }

            // 退款/收入报销：需要关联原始账单 - 单账户+选择账单按钮
            BillType.IncomeRefund, BillType.IncomeReimbursement -> {
                binding.singleAccountContainer.visibility = View.VISIBLE
                binding.chooseBillButton.visibility = View.VISIBLE
            }
        }
    }

    /**
     * 更新账户显示 - 从billInfoModel获取数据填充UI
     *
     * 数据流：billInfoModel → UI组件
     * - accountNameFrom：资金来源账户/人员
     * - accountNameTo：资金去向账户/人员
     * - 图标异步加载，避免阻塞UI线程
     */
    private fun updateAccountDisplay() {
        when (billInfoModel.type) {
            // 单账户模式：只使用 accountNameFrom
            BillType.Expend, BillType.Income, BillType.ExpendReimbursement,
            BillType.IncomeRefund, BillType.IncomeReimbursement -> {
                binding.singleAccount.setText(billInfoModel.accountNameFrom)
                launch {
                    binding.singleAccount.imageView()
                        .setAssetIconByName(billInfoModel.accountNameFrom)
                }
            }

            // 转账模式：accountNameFrom → accountNameTo
            BillType.Transfer -> {
                binding.transferFrom.setText(billInfoModel.accountNameFrom)
                binding.transferTo.setText(billInfoModel.accountNameTo)
                launch {
                    binding.transferFrom.imageView()
                        .setAssetIconByName(billInfoModel.accountNameFrom)
                    binding.transferTo.imageView().setAssetIconByName(billInfoModel.accountNameTo)
                }
            }

            // 账户→人员模式：accountNameFrom(账户) → accountNameTo(人员)
            BillType.ExpendLending, BillType.ExpendRepayment -> {
                binding.debtAccount.setText(billInfoModel.accountNameFrom)
                binding.debtPersonInput.setText(billInfoModel.accountNameTo)
                launch {
                    binding.debtAccount.imageView()
                        .setAssetIconByName(billInfoModel.accountNameFrom)
                }
            }

            // 人员→账户模式：accountNameFrom(人员) → accountNameTo(账户)
            BillType.IncomeLending, BillType.IncomeRepayment -> {
                binding.debtPersonInput2.setText(billInfoModel.accountNameFrom)
                binding.debtAccount2.setText(billInfoModel.accountNameTo)
                launch {
                    binding.debtAccount2.imageView().setAssetIconByName(billInfoModel.accountNameTo)
                }
            }
        }
    }

    /**
     * 更新选择账单按钮显示
     */
    private fun updateChooseBillDisplay() {
        val selectedBills = billInfoModel.extendData
            .split(", ")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val text = if (selectedBills.isEmpty()) {
            context.getString(R.string.float_choose_bill)
        } else {
            context.getString(R.string.float_choose_bills, selectedBills.size)
        }
        binding.chooseBillButton.text = text
    }




    /**
     * 设置债务下拉选择器 - 简化版本
     */
    private fun setupDebtDropdowns() {
        // 设置监听器（只设置一次）
        binding.debtPersonInput.setOnItemClickListener { _, _, _, _ ->
            if (::billInfoModel.isInitialized) {
                billInfoModel.accountNameTo = binding.debtPersonInput.text.toString()
            }
        }

        binding.debtPersonInput2.setOnItemClickListener { _, _, _, _ ->
            if (::billInfoModel.isInitialized) {
                billInfoModel.accountNameFrom = binding.debtPersonInput2.text.toString()
            }
        }

        // 初始化下拉选项
        updateDebtDropdownOptions()
    }

    /**
     * 文本变更同步：监听可编辑输入框，双向同步到 billInfoModel
     */
    private fun setupTextSync() {
        // 单账户模式输入框

        // 债务人员输入框（用户可能手动输入而非下拉点击）
        binding.debtPersonInput.doAfterTextChanged { text ->
            if (!::billInfoModel.isInitialized) return@doAfterTextChanged
            billInfoModel.accountNameTo = text?.toString() ?: ""
        }
        binding.debtPersonInput2.doAfterTextChanged { text ->
            if (!::billInfoModel.isInitialized) return@doAfterTextChanged
            billInfoModel.accountNameFrom = text?.toString() ?: ""
        }

    }

    /**
     * 更新债务下拉选项 - 根据账单类型设置正确的选项
     */
    private fun updateDebtDropdownOptions() {
        if (!::billInfoModel.isInitialized) return
        
        launch {
            val allAssets = AssetsAPI.list()
            val borrowers = allAssets.filter { it.type == AssetsType.BORROWER }.map { it.name }
            val creditors = allAssets.filter { it.type == AssetsType.CREDITOR }.map { it.name }

            // 直接根据账单类型设置对应的下拉选项和提示文本
            when (billInfoModel.type) {
                BillType.ExpendLending -> {
                    // 借出：选择欠款人
                    binding.debtPersonInput.setAdapter(
                        ArrayAdapter(
                            context,
                            android.R.layout.simple_dropdown_item_1line,
                            borrowers
                        )
                    )
                    binding.debtPersonInputLayout.hint =
                        context.getString(R.string.float_expend_debt)
                }

                BillType.ExpendRepayment -> {
                    // 还款：选择债主
                    binding.debtPersonInput.setAdapter(
                        ArrayAdapter(
                            context,
                            android.R.layout.simple_dropdown_item_1line,
                            creditors
                        )
                    )
                    binding.debtPersonInputLayout.hint =
                        context.getString(R.string.float_income_debt)
                }

                BillType.IncomeLending -> {
                    // 借入：选择债主
                    binding.debtPersonInput2.setAdapter(
                        ArrayAdapter(
                            context,
                            android.R.layout.simple_dropdown_item_1line,
                            creditors
                        )
                    )
                    binding.debtPersonInputLayout2.hint =
                        context.getString(R.string.float_income_debt)
                }

                BillType.IncomeRepayment -> {
                    // 收款：选择欠款人
                    binding.debtPersonInput2.setAdapter(
                        ArrayAdapter(
                            context,
                            android.R.layout.simple_dropdown_item_1line,
                            borrowers
                        )
                    )
                    binding.debtPersonInputLayout2.hint =
                        context.getString(R.string.float_expend_debt)
                }

                else -> {

                }
            }
        }
    }

    /**
     * 设置点击事件监听器
     */
    private fun setupClickListeners() {
        // 单账户点击
        binding.singleAccount.setOnClickListener {
            showAssetSelector(true)
        }

        // 转账账户点击
        binding.transferFrom.setOnClickListener {
            showAssetSelector(true)
        }
        binding.transferTo.setOnClickListener {
            showAssetSelector(false)
        }

        // 债务账户点击
        binding.debtAccount.setOnClickListener {
            showAssetSelector(true)
        }
        binding.debtAccount2.setOnClickListener {
            showAssetSelector(false)
        }

        // 选择账单按钮点击
        binding.chooseBillButton.setOnClickListener {
            showBillSelector()
        }
    }

    /**
     * 显示资产选择对话框
     * @param isFirstAccount 是否为第一个账户
     */
    private fun showAssetSelector(isFirstAccount: Boolean) {
        if (!::billInfoModel.isInitialized) {
            return
        }

        // 使用BaseSheetDialog工厂方法创建对话框
        val dialog = BaseSheetDialog.create<AssetsSelectorDialog>(context)

        // 根据账单类型设置资产过滤
        val filter = when (billInfoModel.type) {
            BillType.ExpendReimbursement,
            BillType.IncomeRefund, BillType.IncomeReimbursement ->
                listOf(AssetsType.CREDIT, AssetsType.NORMAL)  // 限制为信用卡和普通账户

            else -> emptyList()  // 其他类型不限制资产类型
        }

        dialog.setFilter(filter)
            .setCallback { selectedAsset ->
                // 更新账户名称
                updateAccountName(isFirstAccount, selectedAsset.name)
                // 刷新显示
                refresh()
            }
            .show()
    }

    /**
     * 显示账单选择对话框
     */
    private fun showBillSelector() {
        if (!::billInfoModel.isInitialized) {
            return
        }

        // 使用BaseSheetDialog工厂方法创建对话框
        val dialog = BaseSheetDialog.create<BillSelectorDialog>(context)

        // 解析当前选中的账单
        val selectedBills = billInfoModel.extendData
            .split(", ")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toMutableList()

        // 根据账单类型确定关联账单的类型
        // 业务规则：收入报销需要关联支出报销账单，其他退款关联普通账单

        val type = when (billInfoModel.type) {
            BillType.IncomeReimbursement -> BillAction.SYNC_REIMBURSE_BILL  // 报销账单
            BillType.IncomeRefund -> BillAction.SYNC_RECENT_EXPENSE_BILL
            else -> BillAction.SYNC_RECENT_EXPENSE_BILL
        }



        dialog.setSelectedBills(selectedBills)
            .setBillType(type)
            .setCallback {
                // 更新选中的账单
                billInfoModel.extendData = selectedBills.joinToString(", ")
                // 刷新显示
                refresh()
            }
            .show()
    }

    /**
     * 更新账户名称 - 根据账单类型和位置确定更新哪个字段
     *
     * 复杂逻辑说明：
     * - 单账户类型：只能更新 accountNameFrom，忽略 isFirstAccount
     * - 转账类型：根据 isFirstAccount 决定更新来源还是目标账户
     * - 债务类型：只能更新账户部分，人员部分通过输入框直接修改
     *
     * @param isFirstAccount 是否为第一个账户（在转账和债务场景中有意义）
     * @param accountName 新的账户名称
     */
    private fun updateAccountName(isFirstAccount: Boolean, accountName: String) {

        when (billInfoModel.type) {
            // 单账户类型：只更新来源账户，isFirstAccount 参数无意义
            BillType.Expend, BillType.Income, BillType.ExpendReimbursement, BillType.IncomeRefund, BillType.IncomeReimbursement -> {
                billInfoModel.accountNameFrom = accountName
            }

            // 转账类型：根据位置更新对应账户
            BillType.Transfer -> {
                if (isFirstAccount) {
                    billInfoModel.accountNameFrom = accountName  // 转出账户
                } else {
                    billInfoModel.accountNameTo = accountName    // 转入账户
                }
            }

            // 借出/还款：只能更新来源账户（我的账户），目标是人员不能通过资产选择器更新
            BillType.ExpendLending, BillType.ExpendRepayment -> {
                if (isFirstAccount) {
                    billInfoModel.accountNameFrom = accountName
                }
                // isFirstAccount=false 时不更新，因为目标是人员而非账户
            }

            // 借入/收款：只能更新目标账户（我的账户），来源是人员不能通过资产选择器更新
            BillType.IncomeLending, BillType.IncomeRepayment -> {
                if (!isFirstAccount) {
                    billInfoModel.accountNameTo = accountName
                }
                // isFirstAccount=true 时不更新，因为来源是人员而非账户
            }
        }
    }
}

