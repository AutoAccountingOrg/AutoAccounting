/*
 * Copyright (C) 2025 ankio(ankio@ankio.net)
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

package net.ankio.auto.service.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.CountDownTimer
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.ankio.auto.App
import net.ankio.auto.R
import net.ankio.auto.constant.FloatEvent
import net.ankio.auto.databinding.FloatTipBinding
import net.ankio.auto.http.api.BillAPI
import net.ankio.auto.service.OverlayService
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.dialog.BillEditorDialog
import net.ankio.auto.ui.utils.AssetsUtils
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.utils.BillTool
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.utils.toThemeCtx
import org.ezbook.server.constant.BillState
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.model.BillInfoModel
import java.util.Locale
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import net.ankio.auto.BuildConfig
import net.ankio.auto.adapter.AppAdapterManager
import net.ankio.auto.ui.api.BaseSheetDialog

/**
 * 账单浮动窗口管理器
 *
 * 职责：
 * 1. 管理账单队列的处理流程
 * 2. 控制浮动提示窗口的显示和交互
 * 3. 协调账单编辑对话框的生命周期
 * 4. 处理用户对账单的各种操作（确认、编辑、删除）
 *
 * 设计原则：
 * - 使用简单的队列替代复杂的Channel，避免过度设计
 * - 明确分离UI逻辑和业务逻辑
 * - 确保窗口生命周期的正确管理，防止内存泄漏
 *
 * @param service 提供生命周期和上下文的覆盖服务
 */
class BillWindowManager(
    private val service: OverlayService
) {
    // ============ 核心状态管理 ============

    /** 浮动窗口超时时间（秒） */
    private val timeoutSeconds: Int = PrefManager.floatTimeoutOff

    /** 主题化的上下文，用于UI创建 */
    private val themedContext: Context = service.service().toThemeCtx()

    /** 系统窗口管理器，用于添加和移除浮动窗口 */
    private val windowManager: WindowManager =
        service.service().getSystemService(Context.WINDOW_SERVICE) as WindowManager

    /** 账单通道，用于异步接收和处理账单 */
    private val billChannel: Channel<BillInfoModel> = Channel(Channel.BUFFERED)

    /** 当前正在处理的账单，null表示没有账单在处理 */
    private var currentBill: BillInfoModel? = null

    /** 当前显示的账单编辑对话框，确保同时只有一个对话框 */
    private var currentDialog: BillEditorDialog? = null

    /** 当前的倒计时器，用于浮动窗口超时控制 */
    private var countDownTimer: CountDownTimer? = null

    init {
        Logger.i("账单窗口管理器已初始化，超时时间: ${timeoutSeconds}秒")
        processNextBill()
    }


    // ============ 公共接口方法 ============

    /**
     * 销毁窗口管理器，清理所有资源
     *
     * 确保：
     * 1. 清理浮动窗口
     * 2. 关闭编辑对话框
     * 3. 停止倒计时器
     * 4. 清空队列
     */
    fun destroy() {
        Logger.d("正在销毁账单窗口管理器...")

        // 停止倒计时器
        countDownTimer?.cancel()
        countDownTimer = null

        removeTipWindow()

        // 关闭编辑对话框
        currentDialog?.dismiss()
        currentDialog = null

        // 关闭通道并清理状态
        billChannel.close()
        currentBill = null

        Logger.d("账单窗口管理器已销毁")
    }

    /**
     * 添加账单到处理通道
     *
     * @param bill 要处理的账单信息
     */
    fun addBill(bill: BillInfoModel) {
        Logger.d("添加账单到处理通道: ${bill.id}")
        service.service().lifecycleScope.launch {
            billChannel.send(bill)
        }
    }

    /**
     * 更新当前编辑对话框中的账单信息
     *
     * @param parentBill 父账单信息，用于重复账单的情况
     */
    fun updateCurrentBill(parentBill: BillInfoModel) {
        currentDialog?.setBillInfo(parentBill)
    }

    // ============ 核心处理逻辑 ============

    /**
     * 处理下一个账单
     *
     * 启动协程等待通道中的下一个账单，收到后进行处理
     * 这种设计简洁优雅：每处理完一个账单就等待下一个，无需复杂的循环
     */
    @OptIn(DelicateCoroutinesApi::class)
    private fun processNextBill() {
        currentBill = null
        currentDialog = null
        // 只有在tipBinding已经初始化的情况下才尝试移除窗口


        service.service().lifecycleScope.launch {
            try {
                Logger.d("等待接收下一个账单...")
                // 阻塞等待下一个账单
                val bill = billChannel.receive()
                Logger.d("成功接收到账单: ${bill.id}")
                processBill(bill)
            } catch (e: Exception) {
                Logger.e("从通道接收账单时出错", e)
                // 如果通道未关闭，继续等待下一个账单
                if (!billChannel.isClosedForReceive) {
                    processNextBill()
                }
            }
        }
    }

    /**
     * 处理单个账单
     *
     * 根据配置决定显示方式：
     * 1. 超时时间为0 -> 直接编辑
     * 2. 关闭自动提示 -> 直接编辑
     * 3. 自动账单 -> 直接编辑
     * 4. 其他情况 -> 显示浮动提示
     *
     * @param bill 要处理的账单
     */
    private fun processBill(bill: BillInfoModel) {
        currentBill = bill
        Logger.i("正在处理账单: ${bill.id}, 金额: ${bill.money}")


        // 决定处理方式
        when {
            // 情况1：超时时间为0，直接进入编辑模式
            timeoutSeconds == 0 -> {
                Logger.d("超时时间为0，直接显示编辑器")
                handleBillAction(Setting.FLOAT_TIMEOUT_ACTION, bill)
            }

            // 情况2：关闭了自动账单提示，直接进入编辑模式
            !PrefManager.showAutoBillTip -> {
                Logger.d("自动账单提示已禁用，直接显示编辑器")
                handleBillAction(Setting.FLOAT_TIMEOUT_ACTION, bill)
            }

            // 情况3：自动账单，直接进入编辑模式
            bill.auto -> {
                Logger.d("自动账单，直接显示编辑器")
                handleBillAction(Setting.FLOAT_TIMEOUT_ACTION, bill)
            }

            // 情况4：显示浮动提示窗口
            else -> {
                Logger.d("显示浮动提示窗口")
                showFloatingTip(bill)
            }
        }
    }

    // ============ 浮动窗口UI管理 ============

    /**
     * 浮动提示窗口的视图绑定
     * 延迟初始化，只在需要时创建，提高性能
     */
    private val tipBinding: FloatTipBinding by lazy {
        FloatTipBinding.inflate(LayoutInflater.from(themedContext))
    }

    /**
     * 安全地移除浮动提示窗口
     *
     * 检查窗口是否已附加，如果是则安全移除
     * 使用removeViewImmediate确保立即移除，避免动画延迟
     */
    private fun removeTipWindow() {
        if (tipBinding.root.isAttachedToWindow) {
            try {
                windowManager.removeViewImmediate(tipBinding.root)
                Logger.d("提示窗口移除成功")
            } catch (e: Exception) {
                Logger.w("移除提示窗口失败: ${e.message}")
            }
        }
    }

    /**
     * 显示浮动提示窗口
     *
     * 功能：
     * 1. 显示账单金额和类型颜色
     * 2. 显示倒计时
     * 3. 处理用户交互（点击、长按）
     * 4. 超时后自动进入编辑模式
     *
     * @param bill 要显示的账单信息
     */
    private fun showFloatingTip(bill: BillInfoModel) {
        Logger.d("为账单显示浮动提示: ${bill.id}")

        // 1. 设置账单信息显示
        setupBillDisplay(bill)

        // 2. 创建并启动倒计时器
        startCountdownTimer(bill)

        // 3. 设置用户交互
        setupUserInteractions(bill)

        // 4. 添加到窗口管理器
        addTipToWindow()
    }

    /**
     * 设置账单信息的显示内容
     */
    private fun setupBillDisplay(bill: BillInfoModel) {
        // 设置金额显示
        tipBinding.money.text = String.format(Locale.getDefault(), "%.2f", bill.money)

        // 设置金额颜色（根据账单类型）
        val colorRes = BillTool.getColor(bill.type)
        val color = ContextCompat.getColor(themedContext, colorRes)
        tipBinding.money.setTextColor(color)

        // 初始化倒计时显示
        tipBinding.time.text = String.format("%ss", timeoutSeconds)

        // 初始状态为不可见，等待布局完成后显示
        tipBinding.root.visibility = View.INVISIBLE
    }

    /**
     * 启动倒计时器
     */
    private fun startCountdownTimer(bill: BillInfoModel) {
        // 取消之前的计时器
        countDownTimer?.cancel()

        countDownTimer = object : CountDownTimer(timeoutSeconds * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt()
                tipBinding.time.text = String.format("%ss", secondsLeft)
            }

            override fun onFinish() {
                Logger.d("倒计时结束，显示编辑器")
                removeTipWindow()
                handleBillAction(Setting.FLOAT_TIMEOUT_ACTION, bill)
            }
        }
    }

    /**
     * 设置用户交互事件
     */
    private fun setupUserInteractions(bill: BillInfoModel) {
        // 点击事件：进入编辑模式
        tipBinding.root.setOnClickListener {
            Logger.d("提示被点击")
            countDownTimer?.cancel()
            removeTipWindow()
            handleBillAction(Setting.FLOAT_CLICK, bill)
        }

        // 长按事件：根据配置处理
        tipBinding.root.setOnLongClickListener {
            Logger.d("提示被长按")
            countDownTimer?.cancel()
            removeTipWindow()
            handleBillAction(Setting.FLOAT_LONG_CLICK, bill)
            true
        }
    }

    /**
     * 将提示窗口添加到窗口管理器
     */
    private fun addTipToWindow() {
        val params = createWindowLayoutParams()

        try {
            windowManager.addView(tipBinding.root, params)

            // 等待布局完成后调整窗口大小并显示
            tipBinding.root.post {
                adjustWindowSize(params)
                tipBinding.root.visibility = View.VISIBLE
                countDownTimer?.start()
                Logger.d("提示窗口显示成功")
            }
        } catch (e: Exception) {
            Logger.e("添加提示窗口失败", e)
            // 失败时直接进入编辑模式
            currentBill?.let { handleBillAction(Setting.FLOAT_TIMEOUT_ACTION, it) }
        }
    }

    /**
     * 创建窗口布局参数
     */
    private fun createWindowLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            x = 0 // 水平居中
            y = -120 // 垂直居中偏上
            gravity = Gravity.CENTER or Gravity.END
        }
    }

    /**
     * 根据内容调整窗口大小
     */
    private fun adjustWindowSize(params: WindowManager.LayoutParams) {
        // 计算实际需要的宽度（logo + 金额 + 时间 + 间距）
        val calculatedWidth = tipBinding.logo.width + tipBinding.money.width +
                tipBinding.time.width + 150 // 150为间距
        val calculatedHeight = tipBinding.logo.height + 60 // 60为上下边距

        // 更新布局参数
        params.width = calculatedWidth
        params.height = calculatedHeight

        // 应用新的布局参数
        windowManager.updateViewLayout(tipBinding.root, params)
    }


    // ============ 业务逻辑处理 ============

    /**
     * 保存账单信息到数据库
     *
     * 功能：
     * 1. 更新账单状态为已编辑
     * 2. 异步保存到数据库
     * 3. 同步账单数据
     * 4. 显示成功提示（如果启用）
     *
     * @param bill 要保存的账单信息
     */
    private fun saveBill(bill: BillInfoModel) {
        Logger.d("保存账单: ${bill.id}")

        // 更新状态
        bill.state = BillState.Edited

        // 异步保存
        service.service().lifecycleScope.launch {
            BillAPI.put(bill)
            AppAdapterManager.adapter().syncBill(bill)
            Logger.d("账单保存成功: ${bill.id}")
        }

        // 显示成功提示
        if (PrefManager.showSuccessPopup && AppAdapterManager.adapter().pkg == BuildConfig.APPLICATION_ID) {
            val message = service.service().getString(
                R.string.auto_success,
                bill.money.toString()
            )
            ToastUtils.info(message)
        }
    }


    /**
     * 处理账单操作
     *
     * @param configKey 配置键
     * @param bill 账单信息
     */
    private fun handleBillAction(configKey: String, bill: BillInfoModel) {
        // 自动账单直接保存
        if (bill.auto) {
            Logger.d("自动账单，直接保存")
            saveBill(bill)
            processNextBill()
            return
        }

        // 获取用户配置的操作
        val action = getUserAction(configKey)

        when (action) {
            FloatEvent.AUTO_ACCOUNT -> {
                Logger.d("用户操作: 自动记账")
                saveBill(bill)
                processNextBill()
            }

            FloatEvent.POP_EDIT_WINDOW -> {
                Logger.d("用户操作: 显示编辑窗口")
                showEditDialog(bill)
            }

            FloatEvent.NO_ACCOUNT -> {
                Logger.d("用户操作: 不记账")
                deleteBill(bill)
            }
        }
    }

    /**
     * 根据配置键获取用户设置的操作
     */
    private fun getUserAction(configKey: String): FloatEvent {
        val configValue = when (configKey) {
            Setting.FLOAT_TIMEOUT_ACTION -> PrefManager.floatTimeoutAction
            Setting.FLOAT_CLICK -> PrefManager.floatClick
            Setting.FLOAT_LONG_CLICK -> PrefManager.floatLongClick
            else -> PrefManager.floatClick
        }

        val actionOrdinal = configValue.toIntOrNull() ?: FloatEvent.POP_EDIT_WINDOW.ordinal
        return FloatEvent.entries.toTypedArray()
            .getOrElse(actionOrdinal) { FloatEvent.POP_EDIT_WINDOW }
    }

    /**
     * 显示编辑对话框
     */
    private fun showEditDialog(bill: BillInfoModel) {
        // 确保只有一个对话框
        currentDialog?.dismiss()

        currentDialog = BaseSheetDialog.create<BillEditorDialog>(service.service())
        currentDialog?.setBillInfo(bill)
            ?.setOnCancel {
                Logger.d("编辑对话框已取消，删除账单")
                deleteBill(bill)
            }
            ?.setOnConfirm { billInfo ->
                saveBill(billInfo)
                Logger.d("编辑对话框已确认，处理下一个账单")
                processNextBill()
            }?.show()
    }

    /**
     * 删除账单
     *
     * @param bill 要删除的账单
     */
    private fun deleteBill(bill: BillInfoModel) {
        Logger.d("删除账单: ${bill.id}")

        service.service().lifecycleScope.launch {
            BillAPI.remove(bill.id)
        }

        processNextBill()
    }


}