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
// removed unused: lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
// removed unused: Dispatchers, App, R
import kotlinx.coroutines.launch
import net.ankio.auto.constant.FloatEvent
import net.ankio.auto.R
import net.ankio.auto.databinding.FloatTipBinding
import net.ankio.auto.http.api.BillAPI
import net.ankio.auto.service.OverlayService
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.dialog.BillEditorDialog
// removed unused: AssetsUtils, ToastUtils
import net.ankio.auto.utils.BillTool
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.utils.toThemeCtx
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.model.BillInfoModel
import java.util.Locale
import kotlinx.coroutines.channels.Channel
// removed unused: receiveAsFlow, BuildConfig, AppAdapterManager
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.dialog.BottomSheetDialogBuilder

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

    /** 主题化的上下文，用于UI创建 */
    private val themedContext: Context = service.service().toThemeCtx()

    /** 系统窗口管理器，用于添加和移除浮动窗口 */
    private val windowManager: WindowManager =
        service.service().getSystemService(Context.WINDOW_SERVICE) as WindowManager

    /** 账单通道，用于异步接收和处理账单（无限容量） */
    private val billChannel: Channel<BillInfoModel> = Channel(Channel.UNLIMITED)

    /** 当前正在处理的账单，null表示没有账单在处理 */
    private var currentBill: BillInfoModel? = null

    /** 当前显示的账单编辑对话框，确保同时只有一个对话框 */
    private var currentDialog: BillEditorDialog? = null

    /** 浮动提示视图控制器 */
    private val floatingTip: FloatingTip by lazy { FloatingTip(themedContext, windowManager) }

    init {
        Logger.i("账单窗口管理器已初始化")
        processNextBill()
    }

    protected fun launch(block: suspend CoroutineScope.() -> Unit) {
        service.launch {
            block()
        }
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

        // 关闭浮动窗口
        floatingTip.destroy()

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
        if (billChannel.isClosedForSend) {
            Logger.w("通道已关闭，丢弃账单: ${bill.id}")
            return
        }
        val result = billChannel.trySend(bill)
        if (result.isFailure) {
            Logger.e("发送失败: closed=${billChannel.isClosedForSend} cause=${result.exceptionOrNull()}")
        }
    }

    /**
     * 更新当前编辑对话框中的账单信息
     *
     * @param parentBill 父账单信息，用于重复账单的情况
     */
    fun updateCurrentBill(parentBill: BillInfoModel) {
        Logger.d("使用父账单更新：$parentBill")
        currentBill = parentBill
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


        launch {
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
        if (PrefManager.floatTimeoutOff == 0) {
            Logger.d("超时时间为0，直接显示编辑器")
            handleBillAction(Setting.FLOAT_TIMEOUT_ACTION, bill)
        } else {
            Logger.d("显示浮动提示窗口")
            showFloatingTip(bill)
        }
    }

    // ============ 浮动窗口UI管理 ============

    // 浮动视图由 FloatingTip 负责，无需在此管理视图与移除

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
        floatingTip.show(bill) { event ->
            when (event) {
                is FloatingTip.Event.Click -> handleBillAction(Setting.FLOAT_CLICK, bill)
                is FloatingTip.Event.LongClick -> handleBillAction(Setting.FLOAT_LONG_CLICK, bill)
                is FloatingTip.Event.Timeout -> handleBillAction(Setting.FLOAT_TIMEOUT_ACTION, bill)
            }
        }
    }

    /**
     * 处理账单操作
     *
     * @param configKey 配置键
     * @param bill 账单信息
     */
    private fun handleBillAction(configKey: String, bill: BillInfoModel) {

        // 获取用户配置的操作
        val actionStr = when (configKey) {
            Setting.FLOAT_TIMEOUT_ACTION -> PrefManager.floatTimeoutAction
            Setting.FLOAT_CLICK -> PrefManager.floatClick
            Setting.FLOAT_LONG_CLICK -> PrefManager.floatLongClick
            else -> PrefManager.floatClick
        }

        val action = runCatching { FloatEvent.valueOf(actionStr) }.getOrElse {
            Logger.w("事件类型错误: $configKey => $actionStr")
            FloatEvent.POP_EDIT_WINDOW
        }

        when (action) {
            FloatEvent.AUTO_ACCOUNT -> {
                Logger.d("用户操作: 自动记账")
                BillTool.saveBill(bill)
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
                Logger.d("编辑对话框已确认，处理下一个账单")
                processNextBill()
            }?.show(false)
    }

    /**
     * 删除账单
     *
     * @param bill 要删除的账单
     */
    private fun deleteBill(bill: BillInfoModel) {
        // 二次确认：防止误删
        BaseSheetDialog.create<BottomSheetDialogBuilder>(service.service())
            .setTitleInt(R.string.delete_title)
            .setMessage(R.string.delete_bill_message)
            .setPositiveButton(R.string.sure_msg) { _, _ ->
                Logger.d("确认删除账单: ${bill.id}")
                launch {
                    BillAPI.remove(bill.id)
                }
                processNextBill()

            }
            .setNegativeButton(R.string.cancel_msg) { _, _ ->
                Logger.d("取消删除账单: ${bill.id}")
                // 返回编辑对话框，避免流程卡住
                processNextBill()
            }
            .show(false)
    }


}