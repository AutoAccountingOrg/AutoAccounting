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
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.autoApp
import net.ankio.auto.databinding.ComponentPaymentInfoBinding
import net.ankio.auto.http.api.AssetsAPI
import net.ankio.auto.ui.components.IconView
import net.ankio.auto.ui.utils.setAssetIcon
import net.ankio.auto.ui.utils.setAssetIconByName
import net.ankio.auto.ui.dialog.AssetsSelectorDialog
import net.ankio.auto.ui.dialog.BillSelectorDialog
import net.ankio.auto.utils.BillTool
import org.ezbook.server.constant.AssetsType
import org.ezbook.server.constant.BillType
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.model.BillInfoModel

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
 * paymentInfo.initBillInfo(billInfoModel, lifecycleOwner)
 * // 点击时会自动弹出相应的选择对话框并更新账单信息
 * ```
 */
class PaymentInfoComponent @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: ComponentPaymentInfoBinding =
        ComponentPaymentInfoBinding.inflate(LayoutInflater.from(context), this)

    private lateinit var lifecycleOwner: LifecycleOwner
    private lateinit var billInfoModel: BillInfoModel

    init {
        orientation = VERTICAL
        setupClickListeners()
    }

    /**
     * 统一初始化方法 - 参考BookHeaderComponent.initBillInfo
     */
    fun initBillInfo(billInfoModel: BillInfoModel, lifecycleOwner: LifecycleOwner) {
        this.lifecycleOwner = lifecycleOwner
        this.billInfoModel = billInfoModel
        refresh()
    }

    /**
     * 刷新显示 - 根据当前账单信息更新UI
     */
    fun refresh() {
        val billType = BillTool.getType(billInfoModel.type)
        configureUIForBillType(billType)
        updateAccountDisplay()
        updateChooseBillDisplay()
    }

    /**
     * 根据账单类型配置UI显示模式
     */
    private fun configureUIForBillType(billType: BillType) {
        hideAllContainers()
        when (billType) {
            BillType.Expend, BillType.Income, BillType.ExpendReimbursement -> {
                showSingleAccountMode()
            }

            BillType.Transfer -> {
                showTransferMode()
            }

            BillType.ExpendLending, BillType.ExpendRepayment -> {
                showDebtMode(firstIsAccount = true, secondIsInput = true)
            }

            BillType.IncomeLending, BillType.IncomeRepayment -> {
                showDebtMode(firstIsAccount = false, secondIsInput = false)
            }

            BillType.IncomeRefund, BillType.IncomeReimbursement -> {
                showSingleAccountMode()
                showChooseBillMode()
            }

            else -> {
                visibility = View.GONE
            }
        }
    }

    /**
     * 更新账户显示 - 从billInfoModel获取数据
     */
    private fun updateAccountDisplay() {
        val billType = BillTool.getType(billInfoModel.type)
        when (billType) {
            BillType.Expend, BillType.Income, BillType.ExpendReimbursement,
            BillType.IncomeRefund, BillType.IncomeReimbursement -> {
                setAssetItem(billInfoModel.accountNameFrom, binding.singleAccount)
            }

            BillType.Transfer -> {
                setAssetItem(billInfoModel.accountNameFrom, binding.transferFrom)
                setAssetItem(billInfoModel.accountNameTo, binding.transferTo)
            }

            BillType.ExpendLending, BillType.ExpendRepayment -> {
                setAssetItem(billInfoModel.accountNameFrom, binding.debtFirstAccount)
                binding.debtSecondInput.setText(billInfoModel.accountNameTo)
            }

            BillType.IncomeLending, BillType.IncomeRepayment -> {
                binding.debtFirstInput.setText(billInfoModel.accountNameFrom)
                setAssetItem(billInfoModel.accountNameTo, binding.debtSecondAccount)
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
     * 隐藏所有容器 - 重置状态
     */
    private fun hideAllContainers() {
        binding.singleAccountContainer.visibility = View.GONE
        binding.transferContainer.visibility = View.GONE
        binding.debtContainer.visibility = View.GONE
        binding.chooseBillButton.visibility = View.GONE
        visibility = View.VISIBLE
    }

    /**
     * 显示单账户模式
     */
    private fun showSingleAccountMode() {
        binding.singleAccountContainer.visibility = View.VISIBLE
    }

    /**
     * 显示转账模式
     */
    private fun showTransferMode() {
        binding.transferContainer.visibility = View.VISIBLE
    }

    /**
     * 显示债务模式 - 简化版本，移除内部选择器逻辑
     */
    private fun showDebtMode(firstIsAccount: Boolean, secondIsInput: Boolean) {
        binding.debtContainer.visibility = View.VISIBLE

        // 配置第一个位置
        if (firstIsAccount) {
            binding.debtFirstAccount.visibility = View.VISIBLE
            binding.debtFirstInputLayout.visibility = View.GONE
        } else {
            binding.debtFirstAccount.visibility = View.GONE
            binding.debtFirstInputLayout.visibility = View.VISIBLE
            binding.debtFirstInputLayout.setHint(R.string.float_income_debt)
        }

        // 配置第二个位置
        if (secondIsInput) {
            binding.debtSecondAccount.visibility = View.GONE
            binding.debtSecondInputLayout.visibility = View.VISIBLE
            binding.debtSecondInputLayout.setHint(R.string.float_expend_debt)
        } else {
            binding.debtSecondAccount.visibility = View.VISIBLE
            binding.debtSecondInputLayout.visibility = View.GONE
        }
    }

    /**
     * 显示选择账单模式
     */
    private fun showChooseBillMode() {
        binding.chooseBillButton.visibility = View.VISIBLE
    }


    /**
     * 设置资产项显示
     */
    private fun setAssetItem(name: String, view: IconView) {
        view.setText(name)
        lifecycleOwner.lifecycleScope.launch {
            view.imageView().setAssetIconByName(name)
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
        binding.debtFirstAccount.setOnClickListener {
            showAssetSelector(true)
        }
        binding.debtSecondAccount.setOnClickListener {
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
        if (!::lifecycleOwner.isInitialized || !::billInfoModel.isInitialized) {
            return
        }

        // 根据 lifecycleOwner 的类型创建对应的对话框
        val dialog = when (lifecycleOwner) {
            is android.app.Activity -> AssetsSelectorDialog.create(lifecycleOwner as android.app.Activity)
            is androidx.fragment.app.Fragment -> AssetsSelectorDialog.create(lifecycleOwner as androidx.fragment.app.Fragment)
            is androidx.lifecycle.LifecycleService -> AssetsSelectorDialog.create(lifecycleOwner as androidx.lifecycle.LifecycleService)
            else -> {
                // 如果无法确定类型，尝试从 context 获取 Activity
                val activity = context as? android.app.Activity
                if (activity != null) {
                    AssetsSelectorDialog.create(activity)
                } else {
                    return // 无法创建对话框
                }
            }
        }

        // 根据账单类型设置资产过滤
        val billType = BillTool.getType(billInfoModel.type)
        val filter = when (billType) {
            BillType.ExpendReimbursement, BillType.IncomeRefund, BillType.IncomeReimbursement ->
                listOf(AssetsType.CREDIT, AssetsType.NORMAL)

            else -> emptyList()
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
        if (!::lifecycleOwner.isInitialized || !::billInfoModel.isInitialized) {
            return
        }

        // 根据 lifecycleOwner 的类型创建对应的对话框
        val dialog = when (lifecycleOwner) {
            is android.app.Activity -> BillSelectorDialog.create(lifecycleOwner as android.app.Activity)
            is androidx.fragment.app.Fragment -> BillSelectorDialog.create(lifecycleOwner as androidx.fragment.app.Fragment)
            is androidx.lifecycle.LifecycleService -> BillSelectorDialog.create(lifecycleOwner as androidx.lifecycle.LifecycleService)
            else -> {
                // 如果无法确定类型，尝试从 context 获取 Activity
                val activity = context as? android.app.Activity
                if (activity != null) {
                    BillSelectorDialog.create(activity)
                } else {
                    return // 无法创建对话框
                }
            }
        }

        // 解析当前选中的账单
        val selectedBills = billInfoModel.extendData
            .split(", ")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toMutableList()

        // 根据账单类型确定账单类型
        val billType = BillTool.getType(billInfoModel.type)
        val type = when (billType) {
            BillType.IncomeReimbursement -> Setting.HASH_BAOXIAO_BILL
            else -> Setting.HASH_BILL
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
     * 更新账户名称
     * @param isFirstAccount 是否为第一个账户
     * @param accountName 新的账户名称
     */
    private fun updateAccountName(isFirstAccount: Boolean, accountName: String) {
        val billType = BillTool.getType(billInfoModel.type)
        when (billType) {
            BillType.Expend, BillType.Income, BillType.ExpendReimbursement, BillType.IncomeRefund, BillType.IncomeReimbursement -> {
                billInfoModel.accountNameFrom = accountName
            }

            BillType.Transfer -> {
                if (isFirstAccount) {
                    billInfoModel.accountNameFrom = accountName
                } else {
                    billInfoModel.accountNameTo = accountName
                }
            }

            BillType.ExpendLending, BillType.ExpendRepayment -> {
                if (isFirstAccount) {
                    billInfoModel.accountNameFrom = accountName
                }
            }

            BillType.IncomeLending, BillType.IncomeRepayment -> {
                if (!isFirstAccount) {
                    billInfoModel.accountNameTo = accountName
                }
            }
        }
    }
}

