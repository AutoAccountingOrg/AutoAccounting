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
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.databinding.ComponentTransactionTypeSelectorBinding
import net.ankio.auto.ui.api.BaseComponent
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.ui.dialog.BillEditorDialog
import net.ankio.auto.utils.BillTool
import net.ankio.auto.storage.Logger
import org.ezbook.server.constant.BillType
import org.ezbook.server.db.model.BillInfoModel

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
 * │
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

    private var onTypeSelectedListener: ((BillType) -> Unit)? = null
    private var mainBillType: BillType = BillType.Expend

    private lateinit var billInfoModel: BillInfoModel

    /**
     * 子类型配置数据类 - 定义每个chip的完整配置信息
     *
     * 这个数据类是整个组件的核心配置结构，定义了每个子类型选项的所有属性：
     *
     * @param chipId 对应的UI chip资源ID，用于界面操作和事件绑定
     * @param expendType 当主类型为"支出"时，该chip对应的具体BillType（null表示不适用）
     * @param incomeType 当主类型为"收入"时，该chip对应的具体BillType（null表示不适用）
     * @param visibilityForExpend 在支出模式下是否显示该chip（基础显示规则）
     * @param visibilityForIncome 在收入模式下是否显示该chip（基础显示规则）
     * @param requiredFeatures 该功能需要启用的功能开关列表（所有开关都必须为true才显示）
     *
     * 例如：报销功能的配置
     * - expendType = ExpendReimbursement（支出报销）
     * - incomeType = IncomeReimbursement（收入报销）
     * - 两种模式下都显示
     * - 需要启用报销功能开关
     */
    private data class SubTypeConfig(
        val chipId: Int,
        val expendType: BillType?,
        val incomeType: BillType?,
        val visibilityForExpend: Boolean = false,
        val visibilityForIncome: Boolean = false,
        val requiredFeatures: List<() -> Boolean> = emptyList()
    )

    /**
     * 统一的子类型配置表 - 所有类型映射的单一数据源
     *
     * 这个配置表是整个组件的"大脑"，集中管理所有子类型的配置信息。
     * 采用Map结构，key为chipId，value为完整的配置信息。
     *
     * 设计优势：
     * 1. 【单一数据源】所有类型映射逻辑都在这里，避免分散在各个方法中
     * 2. 【易于维护】新增子类型只需在此添加一条配置，无需修改其他代码
     * 3. 【配置驱动】通过配置数据驱动UI显示和业务逻辑，代码更清晰
     * 4. 【功能解耦】每个子类型的显示条件和业务逻辑完全独立
     *
     * 配置说明：
     * - chip_reimbursement: 报销功能，支出收入都支持
     * - chip_lend: 借出功能，仅支出模式显示
     * - chip_borrow: 借入功能，仅收入模式显示
     * - chip_repayment: 还款功能，支出收入都支持（文本会动态变化）
     * - chip_refund: 退款功能，仅收入模式显示
     */
    private val subTypeConfigs = mapOf(
        // 报销功能配置 - 支出和收入模式都支持
        R.id.chip_reimbursement to SubTypeConfig(
            chipId = R.id.chip_reimbursement,
            expendType = BillType.ExpendReimbursement,    // 支出模式：支出报销（我先垫付，后续报销）
            incomeType = BillType.IncomeReimbursement,    // 收入模式：收入报销（收到报销款）
            visibilityForExpend = true,                   // 支出模式下显示
            visibilityForIncome = true,                   // 收入模式下显示
            requiredFeatures = listOf { PrefManager.featureReimbursement }  // 需要报销功能开关
        ),
        // 借出功能配置 - 仅支出模式支持（借钱给别人是支出）
        R.id.chip_lend to SubTypeConfig(
            chipId = R.id.chip_lend,
            expendType = BillType.ExpendLending,          // 支出模式：借出（借钱给他人）
            incomeType = null,                            // 收入模式下不显示"借出"（逻辑不符）
            visibilityForExpend = true,                   // 仅在支出模式下显示
            visibilityForIncome = false,
            requiredFeatures = listOf(
                { PrefManager.featureAssetManage },
                { PrefManager.featureDebt })  // 需要资产管理和债务功能
        ),
        // 借入功能配置 - 仅收入模式支持（从别人借钱是收入）
        R.id.chip_borrow to SubTypeConfig(
            chipId = R.id.chip_borrow,
            expendType = null,                            // 支出模式下不显示"借入"（逻辑不符）
            incomeType = BillType.IncomeLending,          // 收入模式：借入（从他人借钱）
            visibilityForExpend = false,
            visibilityForIncome = true,                   // 仅在收入模式下显示
            requiredFeatures = listOf(
                { PrefManager.featureAssetManage },
                { PrefManager.featureDebt })  // 需要资产管理和债务功能
        ),
        // 还款功能配置 - 支出和收入模式都支持，但含义不同
        R.id.chip_repayment to SubTypeConfig(
            chipId = R.id.chip_repayment,
            expendType = BillType.ExpendRepayment,        // 支出模式：支出还款（我还钱给他人）
            incomeType = BillType.IncomeRepayment,        // 收入模式：收入还款（他人还钱给我）
            visibilityForExpend = true,                   // 支出模式下显示
            visibilityForIncome = true,                   // 收入模式下显示
            requiredFeatures = listOf(
                { PrefManager.featureAssetManage },
                { PrefManager.featureDebt })  // 需要资产管理和债务功能
        ),
        // 退款功能配置 - 仅收入模式支持（退款是收到钱）
        R.id.chip_refund to SubTypeConfig(
            chipId = R.id.chip_refund,
            expendType = null,                            // 支出模式下不显示"退款"（逻辑不符）
            incomeType = BillType.IncomeRefund,           // 收入模式：收入退款（收到退款）
            visibilityForExpend = false,
            visibilityForIncome = true,                   // 仅在收入模式下显示
            requiredFeatures = emptyList()                // 退款功能默认启用，无需额外开关
        )
    )

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
        setupAutoTypeSelection()               // 设置自动类型选择逻辑
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
        val needsSubTypeReconfiguration = newMainBillType != mainBillType

        mainBillType = newMainBillType

        // 只有主类型变化时才重新配置子类型显示
        if (needsSubTypeReconfiguration) {
            Logger.d("主类型变化，重新配置子类型显示: $mainBillType")
            setupForBillType(mainBillType)
        }

        // 根据具体的账单类型设置对应chip的选中状态
        setSelectedType(billInfoModel.type)
    }

    /**
     * 仅刷新选中状态 - 精细化刷新方法
     * 当只是子类型在同一主类型内切换时使用，避免重新配置显示
     */
    fun refreshSelection() {
        Logger.d("仅刷新选中状态: ${billInfoModel.type}")
        setSelectedType(billInfoModel.type)
    }

    /**
     * 仅刷新显示状态 - 当功能开关变化时使用
     * 重新计算各个chip的显示/隐藏状态，但不改变选中状态
     */
    fun refreshVisibility() {
        Logger.d("刷新显示状态，当前主类型: $mainBillType")
        val isExpendMode = mainBillType == BillType.Expend
        setupSubTypesForMainType(isExpendMode)
    }

    /**
     * 设置自动类型选择，当用户选择子类型时自动更新账单信息并发送事件
     *
     * 自动化流程：
     * 1. 用户点击某个chip（如"报销"）
     * 2. setupChipGroupListener捕获点击事件
     * 3. 根据当前主类型和选中的chip确定具体的BillType
     * 4. 调用此监听器，更新账单模型的type字段
     * 5. 发送精细化的刷新事件，只刷新受影响的组件
     *
     * 这样设计的好处是实现了组件间的松耦合，用户操作自动同步到数据模型
     */
    private fun setupAutoTypeSelection() {
        // 设置内部监听器，处理用户的类型选择操作
        onTypeSelectedListener = { selectedType ->
            val oldType = billInfoModel.type

            // 直接更新账单模型的类型字段
            billInfoModel.type = selectedType

            // 在协程中发送精细化的刷新事件
            launch {
                // 发送账单类型变化事件，只刷新受影响的组件
                BillEditorDialog.notifyBillTypeChanged(oldType, selectedType)

                Logger.d("账单类型已更新: $oldType -> $selectedType")
            }
        }
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
     * 设置当前选中的子类型
     * 使用配置表进行反向查找，逻辑更清晰
     *
     * 查找逻辑说明：
     * 1. 遍历subTypeConfigs配置表中的所有配置项
     * 2. 检查每个配置的expendType或incomeType是否匹配传入的billType
     * 3. 找到匹配的配置后，获取对应的chipId
     * 4. 设置对应chip为选中状态，如果没找到则清除所有选择
     *
     * 例如：billType = ExpendReimbursement
     * → 找到chip_reimbursement配置项
     * → chipId = R.id.chip_reimbursement
     * → 设置该chip为选中状态
     *
     * @param billType 要选中的账单类型（可以是具体的子类型）
     */
    private fun setSelectedType(billType: BillType) {
        // 在配置表中反向查找：从BillType找到对应的chipId
        val chipId = subTypeConfigs.entries.find { (_, config) ->
            // 检查该配置的支出类型或收入类型是否匹配
            config.expendType == billType || config.incomeType == billType
        }?.key ?: -1  // 如果没找到匹配的配置，返回-1

        if (chipId != -1) {
            // 找到对应的chip，设置为选中状态
            binding.chipGroup.check(chipId)
        } else {
            // 没找到匹配的子类型，清除所有选择（显示主类型状态）
            binding.chipGroup.clearCheck()
        }
    }

    /**
     * 清除所有选择
     */
    fun clearSelection() {
        binding.chipGroup.clearCheck()
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
     * 统一配置子类型显示逻辑
     * 根据主类型和功能开关决定每个chip的显示状态
     *
     * 显示逻辑的两层过滤：
     * 第一层：基础可见性 - 根据主类型（支出/收入）决定哪些chip应该显示
     *   例如："借出"只在支出模式显示，"退款"只在收入模式显示
     *
     * 第二层：功能开关 - 根据用户启用的功能模块进一步过滤
     *   例如：即使在支出模式，如果债务功能未启用，"借出"也不显示
     *
     * 最终显示 = 基础可见性 && 所有必需功能都启用
     *
     * @param isExpendMode true为支出模式，false为收入模式
     */
    private fun setupSubTypesForMainType(isExpendMode: Boolean) {
        // 打印功能开关状态，便于调试
        Logger.d("=== 功能开关状态检查 ===")
        Logger.d("当前模式: ${if (isExpendMode) "支出模式" else "收入模式"}")
        Logger.d("报销功能开关: ${PrefManager.featureReimbursement}")
        Logger.d("资产管理功能开关: ${PrefManager.featureAssetManage}")
        Logger.d("债务功能开关: ${PrefManager.featureDebt}")

        // 特殊处理：还款chip的文本在不同模式下含义不同
        // 支出模式："支出还款"（我还钱给别人）
        // 收入模式："收入还款"（别人还钱给我）
        binding.chipRepayment.text = context.getString(
            if (isExpendMode) R.string.expend_repayment else R.string.income_repayment
        )

        // 遍历所有子类型配置，统一处理显示逻辑
        subTypeConfigs.forEach { (chipId, config) ->
            val chipView = binding.root.findViewById<View>(chipId)

            // 【第一层过滤】根据当前主类型确定基础可见性
            // 例如：支出模式下，只有visibilityForExpend=true的chip才考虑显示
            val baseVisibility = if (isExpendMode) {
                config.visibilityForExpend
            } else {
                config.visibilityForIncome
            }

            // 【第二层过滤】检查功能开关
            // 如果requiredFeatures为空，表示无需额外功能，直接启用
            // 如果有要求的功能，则所有功能都必须启用（all条件）
            val featureEnabled = config.requiredFeatures.isEmpty() ||
                    config.requiredFeatures.all { it() }  // 所有必需功能都返回true


            // 【最终决策】两层过滤都通过才显示
            val finalVisibility = baseVisibility && featureEnabled
            chipView.visibility = if (finalVisibility) {
                View.VISIBLE  // 显示该子类型选项
            } else {
                View.GONE     // 隐藏该子类型选项
            }

            Logger.d("  最终显示状态: ${if (finalVisibility) "显示" else "隐藏"}")
        }

        Logger.d("=== 功能开关状态检查完成 ===")
    }

    /**
     * 设置Chip组选择监听器
     * 使用配置表统一处理类型映射，避免重复的when分支
     *
     * 监听逻辑说明：
     * 1. 监听ChipGroup的选择状态变化事件
     * 2. 获取当前选中的chipId（可能为空，表示取消选择）
     * 3. 根据chipId和当前主类型确定具体的BillType
     * 4. 调用类型选择监听器，触发数据更新和UI刷新
     *
     * 类型映射示例：
     * - 主类型=Expend + 选中chip_reimbursement → ExpendReimbursement
     * - 主类型=Income + 选中chip_reimbursement → IncomeReimbursement
     * - 主类型=Expend + 未选中任何chip → Expend（普通支出）
     *
     * 这种设计避免了硬编码的when分支，所有映射逻辑都来自配置表
     */
    private fun setupChipGroupListener() {
        binding.chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            // 获取当前选中的chipId（ChipGroup支持多选，但我们只取第一个）
            val checkedId = checkedIds.firstOrNull()

            // 根据选择情况确定最终的账单类型
            val selectedType = if (checkedId == null) {
                // 情况1：未选中任何子类型chip
                // 返回主类型，例如：普通支出(Expend)、普通收入(Income)
                mainBillType
            } else {
                // 情况2：选中了某个子类型chip
                // 从配置表中获取该chip的配置信息
                val config = subTypeConfigs[checkedId]

                // 根据当前主类型选择对应的子类型
                when (mainBillType) {
                    BillType.Expend -> config?.expendType ?: mainBillType  // 支出子类型
                    BillType.Income -> config?.incomeType ?: mainBillType  // 收入子类型
                    else -> mainBillType  // 转账等其他类型保持不变
                }
            }

            // 触发类型选择监听器，更新账单数据并刷新相关UI
            onTypeSelectedListener?.invoke(selectedType)
        }
    }
}
