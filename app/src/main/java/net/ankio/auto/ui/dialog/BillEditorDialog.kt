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

package net.ankio.auto.ui.dialog

import android.app.Activity
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.databinding.FloatEditorRefactoredBinding
import net.ankio.auto.exceptions.BillException
import net.ankio.auto.http.api.AssetsAPI
import net.ankio.auto.http.api.BillAPI
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.dialog.components.*
import net.ankio.auto.ui.api.bindAs

import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.utils.BillTool
import org.ezbook.server.constant.BillState
import org.ezbook.server.constant.BillType
import org.ezbook.server.constant.DefaultData
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.model.AssetsModel
import org.ezbook.server.db.model.BillInfoModel
import org.ezbook.server.db.model.BookNameModel
import org.ezbook.server.db.model.CategoryModel
import java.util.Calendar

/**
 * 精细化刷新事件类型
 * 定义不同的刷新场景，避免不必要的全量刷新
 */
sealed class RefreshEvent {
    /**
     * 账单类型变化事件 - 影响类型选择器、金额显示颜色、分类选择等
     * @param oldType 旧的账单类型
     * @param newType 新的账单类型
     */
    data class BillTypeChanged(val oldType: BillType, val newType: BillType) : RefreshEvent()

    /**
     * 金额变化事件 - 影响金额显示组件
     * @param oldAmount 旧金额
     * @param newAmount 新金额
     */
    data class AmountChanged(val oldAmount: Double, val newAmount: Double) : RefreshEvent()

    /**
     * 账户变化事件 - 影响支付信息组件
     * @param accountType 账户变化类型（from账户或to账户）
     */
    data class AccountChanged(val accountType: AccountType) : RefreshEvent() {
        enum class AccountType { FROM, TO, BOTH }
    }

    /**
     * 分类变化事件 - 影响基础信息组件
     */
    object CategoryChanged : RefreshEvent()

    /**
     * 时间变化事件 - 影响基础信息组件
     */
    object TimeChanged : RefreshEvent()

    /**
     * 全量刷新事件 - 刷新所有组件（谨慎使用）
     */
    object FullRefresh : RefreshEvent()

    /**
     * 功能开关变化事件 - 影响类型选择器的显示
     */
    object FeatureToggleChanged : RefreshEvent()
}

/**
 * 账单编辑对话框 - 组件协调器模式
 *
 * 职责：
 * - 协调各个UI组件的初始化和数据流
 * - 处理组件间的通信和状态同步
 * - 管理对话框的生命周期和回调
 *
 * 设计原则：
 * 1. 单一职责：只做组件协调，不处理具体UI逻辑
 * 2. 数据流清晰：统一的初始化和刷新机制
 * 3. 无特殊情况：用配置驱动，而非条件分支
 *
 * 使用方式：
 * ```kotlin
 * BillEditorDialog.create(activity)
 *     .setBillInfo(billInfo)
 *     .setOnConfirm { bill -> processBill(bill) }
 *     .setOnCancel { bill -> handleCancel(bill) }
 *     .show()
 * ```
 */
class BillEditorDialog internal constructor(
    context: android.content.Context
) : BaseSheetDialog<FloatEditorRefactoredBinding>(context) {

    private var billInfoModel: BillInfoModel? = null
    private var onCancelClick: ((billInfoModel: BillInfoModel) -> Unit)? = null
    private var onConfirmClick: ((billInfoModel: BillInfoModel) -> Unit)? = null

    // 核心状态
    private var currentBillType = BillType.Expend

    /**
     * 设置账单信息
     * @param billInfo 账单信息模型
     * @return 当前对话框实例，支持链式调用
     */
    fun setBillInfo(billInfo: BillInfoModel) = apply {
        this.billInfoModel = billInfo
        this.currentBillType = BillTool.getType(billInfo.type)
        initializeAllComponents()
    }


    /**
     * 链式配置方法 - 参考BookHeaderComponent的简洁设计
     */
    fun setOnCancel(callback: (billInfoModel: BillInfoModel) -> Unit) = apply {
        this.onCancelClick = callback
    }

    fun setOnConfirm(callback: (billInfoModel: BillInfoModel) -> Unit) = apply {
        this.onConfirmClick = callback
    }

    override fun onViewCreated(view: View?) {
        super.onViewCreated(view)

        // 基础设置
        val parent = view?.parent as? View ?: return
        parent.setPadding(0, 0, 0, 0)

        initializeAllComponents()
        // 设置事件监听
        setupEventListeners()

        // 设置全局刷新监听
        setupGlobalRefreshListener()
        Logger.d("账单编辑器初始化")
    }

    /**
     * 统一组件初始化 - 采用BaseComponent模式
     */
    private fun initializeAllComponents() {
        val billInfo = billInfoModel ?: return

        // 初始化基础状态
        currentBillType = BillTool.getType(billInfo.type)

        // 使用 bindAs() 创建组件实例并设置账单信息
        val bookHeader: BookHeaderComponent = binding.bookHeader.bindAs()
        bookHeader.setBillInfo(billInfo)

        val amountDisplay: AmountDisplayComponent = binding.amountDisplay.bindAs()
        amountDisplay.setBillInfo(billInfo)

        val ruleInfo: RuleInfoComponent = binding.ruleInfo.bindAs()
        ruleInfo.setBillInfo(billInfo)

        val transactionTypeSelector: TransactionTypeSelectorComponent =
            binding.transactionTypeSelector.bindAs()
        transactionTypeSelector.setBillInfo(billInfo)

        val paymentInfo: PaymentInfoComponent = binding.paymentInfo.bindAs()
        paymentInfo.setBillInfo(billInfo)

        val basicInfo: BasicInfoComponent = binding.basicInfo.bindAs()
        basicInfo.setBillInfo(billInfo)

        val actionButtons: ActionButtonsComponent = binding.actionButtons.bindAs()
        actionButtons.setBillInfo(billInfo)

        // 设置操作按钮事件
        setupActionButtonEvents(actionButtons)
    }

    /**
     * 设置事件监听器 - 组件已自包含选择器逻辑，这里只处理操作按钮
     */
    private fun setupEventListeners() {
        // 事件监听器在 initializeAllComponents 中设置
    }

    /**
     * 设置操作按钮事件
     */
    private fun setupActionButtonEvents(actionButtons: ActionButtonsComponent) {
        val billInfo = billInfoModel ?: return

        actionButtons.setOnCancelClickListener {
            dismiss()
            onCancelClick?.invoke(billInfo)
        }
        actionButtons.setOnConfirmClickListener {
            saveBill()
        }
    }

    /**
     * 设置全局刷新监听器 - 支持精细化刷新
     */
    private fun setupGlobalRefreshListener() {
        launch {
            refreshEvent.collect { event ->
                handleRefreshEvent(event)
            }
        }
    }

    /**
     * 处理精细化刷新事件
     * @param event 刷新事件
     */
    private fun handleRefreshEvent(event: RefreshEvent) {
        Logger.d("收到刷新事件: ${event::class.simpleName}")

        when (event) {
            is RefreshEvent.BillTypeChanged -> {
                // 账单类型变化：影响类型选择器、金额显示、分类等
                Logger.d("账单类型变化: ${event.oldType} -> ${event.newType}")

                val oldMainType = BillTool.getType(event.oldType)
                val newMainType = BillTool.getType(event.newType)

                // 类型变化需要重新初始化组件
                Logger.d("账单类型变化，重新初始化相关组件")
                initializeAllComponents()
            }

            is RefreshEvent.AmountChanged -> {
                // 金额变化：重新初始化金额显示组件
                Logger.d("金额变化: ${event.oldAmount} -> ${event.newAmount}")
                val amountDisplay: AmountDisplayComponent = binding.amountDisplay.bindAs()
                amountDisplay.setBillInfo(billInfoModel!!)
            }

            is RefreshEvent.AccountChanged -> {
                // 账户变化：重新初始化支付信息组件
                Logger.d("账户变化: ${event.accountType}")
                val paymentInfo: PaymentInfoComponent = binding.paymentInfo.bindAs()
                paymentInfo.setBillInfo(billInfoModel!!)
            }

            RefreshEvent.CategoryChanged -> {
                // 分类变化：重新初始化基础信息组件
                Logger.d("分类变化")
                val basicInfo: BasicInfoComponent = binding.basicInfo.bindAs()
                basicInfo.setBillInfo(billInfoModel!!)
            }

            RefreshEvent.TimeChanged -> {
                // 时间变化：重新初始化基础信息组件
                Logger.d("时间变化")
                val basicInfo: BasicInfoComponent = binding.basicInfo.bindAs()
                basicInfo.setBillInfo(billInfoModel!!)
            }

            RefreshEvent.FeatureToggleChanged -> {
                // 功能开关变化：重新初始化类型选择器
                Logger.d("功能开关变化，重新初始化类型选择器")
                val transactionTypeSelector: TransactionTypeSelectorComponent =
                    binding.transactionTypeSelector.bindAs()
                transactionTypeSelector.setBillInfo(billInfoModel!!)
            }

            RefreshEvent.FullRefresh -> {
                // 全量刷新：刷新所有组件
                Logger.d("执行全量刷新")
                refreshAllComponents()
            }
        }
    }


    /**
     * 刷新所有组件 - 需要重新获取组件实例
     */
    private fun refreshAllComponents() {
        // 重新初始化所有组件
        initializeAllComponents()
    }


    /**
     * 保存账单 - 简化的保存逻辑
     */
    private fun saveBill() {
        val billInfo = billInfoModel ?: return

        lifecycleScope.launch {
            try {
                // 收集组件中可能需要手动同步的数据
                val basicInfo: BasicInfoComponent = binding.basicInfo.bindAs()
                basicInfo.updateBillInfoFromUI()

                // 保存账单
                billInfo.state = BillState.Edited
                BillAPI.put(billInfo)

                // 显示成功提示
                if (PrefManager.showSuccessPopup) {
                    ToastUtils.info(ctx.getString(R.string.auto_success, billInfo.money.toString()))
                }

                // 同步账单数据
                BillTool.syncBills()

                // 关闭对话框并回调
                dismiss()
                onConfirmClick?.invoke(billInfo)

            } catch (e: BillException) {
                ToastUtils.error(e.message ?: "未知错误")
                Logger.e("保存账单失败", e)
            } catch (e: Exception) {
                Logger.e("保存账单异常", e)
            }
        }
    }


    /**
     * 软键盘显示时滚动到底部
     */
    override fun onImeVisible() {
        binding.scrollView.post {
            binding.scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    companion object {
        // 精细化刷新事件流
        val refreshEvent = MutableSharedFlow<RefreshEvent>(replay = 1)

        /**
         * 创建账单编辑对话框 - 统一的工厂方法
         *
         * 支持多种上下文类型：
         * - Activity: 普通界面使用
         * - Fragment: Fragment中使用
         * - LifecycleService: 悬浮窗模式使用
         *
         * @param context 上下文，自动推断LifecycleOwner
         * @return 对话框实例，支持链式调用
         */
        fun create(context: android.content.Context): BillEditorDialog {
            return BaseSheetDialog.create<BillEditorDialog>(context)
        }

        /**
         * 通知所有组件刷新 - 保持向后兼容
         */
        fun notifyRefresh() {
            refreshEvent.tryEmit(RefreshEvent.FullRefresh)
        }

        /**
         * 通知特定事件的精细化刷新
         * @param event 刷新事件类型
         */
        fun notifyRefresh(event: RefreshEvent) {
            refreshEvent.tryEmit(event)
        }

        /**
         * 通知账单类型变化
         * @param oldType 旧类型
         * @param newType 新类型
         */
        fun notifyBillTypeChanged(oldType: BillType, newType: BillType) {
            refreshEvent.tryEmit(RefreshEvent.BillTypeChanged(oldType, newType))
        }

        /**
         * 通知金额变化
         * @param oldAmount 旧金额
         * @param newAmount 新金额
         */
        fun notifyAmountChanged(oldAmount: Double, newAmount: Double) {
            refreshEvent.tryEmit(RefreshEvent.AmountChanged(oldAmount, newAmount))
        }
    }
}
