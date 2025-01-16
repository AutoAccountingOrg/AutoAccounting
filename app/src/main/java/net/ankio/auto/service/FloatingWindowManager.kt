/*
 * Copyright (C) 2024 ankio(ankio@ankio.net)
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

package net.ankio.auto.service

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.CountDownTimer
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.quickersilver.themeengine.ThemeEngine
import kotlinx.coroutines.Dispatchers
import net.ankio.auto.App
import net.ankio.auto.R
import net.ankio.auto.constant.FloatEvent
import net.ankio.auto.databinding.FloatTipBinding
import net.ankio.auto.storage.ConfigUtils
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.activity.ErrorActivity
import net.ankio.auto.ui.dialog.FloatEditorDialog
import net.ankio.auto.ui.utils.AssetsUtils
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.utils.BillTool
import org.ezbook.server.constant.BillState
import org.ezbook.server.constant.BillType
import org.ezbook.server.constant.DefaultData
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.model.BillInfoModel
import org.ezbook.server.tools.FloatingIntent
import java.util.Locale

/**
 * 浮动窗口管理器，负责处理和管理浮动窗口的显示和操作。
 *
 * 该类通过[Context]、[FloatingIntent]和[FloatingQueue]初始化，并提供了一系列方法来处理账单信息、显示提示信息、
 * 以及管理浮动窗口的生命周期。浮动窗口管理器还负责根据主题变化更新界面，并在必要时停止或终止处理流程。
 *
 * @param context 应用程序上下文，用于获取系统服务和资源。
 * @param intent 包含账单信息和操作意图的对象。
 * @param floatingQueue 浮动任务队列，用于管理浮动任务的执行。
 */
class FloatingWindowManager(
    private val context: FloatingWindowService,
    private val intent: FloatingIntent,
    private val floatingQueue: FloatingQueue
) {
    private var timeCount: Int = 0
    private var lastTheme = ThemeEngine.getInstance(App.app).getTheme()
    private var themedContext: Context
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    init {
        timeCount = runCatching {
            ConfigUtils.getString(Setting.FLOAT_TIMEOUT_OFF, DefaultData.FLOAT_TIMEOUT_OFF).toInt()
        }.getOrDefault(10)
        Logger.i("FloatingWindowManager Start，Timeout：$timeCount s")
        val appTheme = ContextThemeWrapper(App.app, R.style.AppTheme)
        themedContext = App.getThemeContext(appTheme)
        lastTheme = ThemeEngine.getInstance(App.app).getTheme()
    }

    /**
     * 初始化应用主题。
     *
     * 该方法首先从 [ThemeEngine] 获取当前主题，并与上一次的主题进行比较。
     * 如果当前主题与上一次的主题不同，则更新 [lastTheme] 并创建一个新的 [ContextThemeWrapper]，
     * 该包装器基于当前主题和应用主题样式 [R.style.AppTheme]。
     * 最后，将新的主题上下文存储在 [themedContext] 中。
     *
     * @see ThemeEngine
     * @see ContextThemeWrapper
     * @see R.style.AppTheme
     */
    private fun initTheme() {
        val theme = ThemeEngine.getInstance(App.app).getTheme()
        if (lastTheme != theme) {
            lastTheme = theme
            val appTheme = ContextThemeWrapper(App.app, R.style.AppTheme)
            themedContext = App.getThemeContext(appTheme)
        }
    }

    /**
     * 终止账单队列。
     *
     * 该方法会关闭浮动队列（floatingQueue）并确保在主线程中执行关闭操作。
     *
     * 注意：此方法仅应在特定情况下调用，以避免意外终止进程。
     */
    private fun killProcess() {
        App.launch(Dispatchers.Main) {
            floatingQueue.shutdown()
        }
    }

    /**
     * 停止当前正在进行的进程（记账进程）
     *
     * 该方法通过在主线程中调用 `floatingQueue.processStop()` 来停止当前的进程。
     * 使用 `App.launch` 确保操作在主线程中执行，以避免线程安全问题。
     */
    private fun stopProcess() {
        App.launch(Dispatchers.Main) {
            floatingQueue.processStop()
        }
    }

    /**
     * 处理账单信息。
     *
     * 该方法首先初始化主题，然后获取传入的账单信息模型对象。
     * 如果传入的账单信息模型对象的 `parent` 属性不为空，则说明是重复账单，此时会发送一个本地广播通知更新账单。
     * 如果传入的账单信息模型对象的 `timeCount` 为 0 或 `intent.showTip` 为 `false`，则调用 `callBillInfoEditor` 方法显示编辑悬浮窗。
     * 否则，调用 `showTip` 方法显示提示信息。
     *
     * 如果处理过程中发生异常，则记录错误日志，并跳转到错误页面。
     */
    fun process() {
        initTheme()
        val billInfoModel = intent.billInfoModel
        val from = intent.from
        Logger.i("Server start => $intent, From = $from")
        Logger.i("BillInfo：$billInfoModel")
        runCatching {
            processBillInfo()
            billInfoModel.state = BillState.Edited
            if (billInfoModel.id > 0) {
                App.launch {
                    BillInfoModel.put(billInfoModel)
                }
            }
        }.onFailure {
            // 提醒用户报告错误
            Logger.e("Failed to record bill", it)
            // 跳转错误页面
            val intent2 = Intent(App.app, ErrorActivity::class.java)
            intent2.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            val sb = StringBuilder()
            sb.append("自动记账未获取到悬浮窗权限，记账失败！\n")
            sb.append("请在设置中手动授予该权限！\n")
            sb.append(it.message).append("\n")
            it.stackTrace.forEach { message ->
                sb.append(message.toString())
                sb.append("\n")
            }
            intent2.putExtra("msg", sb.toString())
            context.startActivity(intent2)
            //停止处理任务
            killProcess()
        }
    }

    /**
     * 延迟初始化的浮动提示绑定对象。
     *
     * 该变量通过[FloatTipBinding.inflate]方法从布局资源中创建，并使用[LayoutInflater]进行实例化。
     * 初始化过程在首次访问时进行，确保只有在需要时才会创建对象，从而提高性能。
     *
     * @see FloatTipBinding
     * @see LayoutInflater
     */
    private val tipBinding: FloatTipBinding by lazy {
        FloatTipBinding.inflate(LayoutInflater.from(themedContext))
    }

    /**
     * 移除提示视图。
     *
     * 该方法首先检查提示视图是否已附加到窗口。如果已附加，则尝试从窗口管理器中移除该视图。
     * 如果在移除过程中发生异常，将记录警告日志。
     *
     * @see android.view.WindowManager.removeView
     */
    fun removeTip() {
        if (tipBinding.root.isAttachedToWindow) {
            runCatching {
                windowManager.removeView(tipBinding.root)
            }.onFailure {
                Logger.w("Failed to remove view ${it.message}")
            }
        }
    }

    /**
     * 显示提示信息。
     *
     * 该方法用于在屏幕上显示一个悬浮的提示信息，包含账单信息和倒计时。
     * 提示信息包括账单金额、类型颜色、倒计时时间，并且可以通过点击或长按来触发不同的操作。
     *
     * @param billInfoModel 包含账单信息的模型对象。
     */
    private fun showTip(billInfoModel: BillInfoModel) {
        tipBinding.root.visibility = View.INVISIBLE
        tipBinding.money.text = String.format(Locale.getDefault(), "%.2f", billInfoModel.money)

        val colorRes = BillTool.getColor(billInfoModel.type)
        val color = ContextCompat.getColor(themedContext, colorRes)
        tipBinding.money.setTextColor(color)
        tipBinding.time.text = String.format("%ss", timeCount.toString())

        val countDownTimer = object : CountDownTimer(timeCount * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                tipBinding.time.text = String.format("%ss", (millisUntilFinished / 1000).toString())
            }

            override fun onFinish() {
                // 取消倒计时
                removeTip()
                callBillInfoEditor(Setting.FLOAT_TIMEOUT_ACTION, billInfoModel)
            }
        }


        tipBinding.root.setOnClickListener {
            countDownTimer.cancel() // 定时器停止
            removeTip()
            callBillInfoEditor(Setting.FLOAT_CLICK, billInfoModel)
        }

        tipBinding.root.setOnLongClickListener {
            countDownTimer.cancel() // 定时器停止
            removeTip()
            // 不记录
            callBillInfoEditor(Setting.FLOAT_LONG_CLICK, billInfoModel)
            true
        }

        // 设置 WindowManager.LayoutParams
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            x = 0 // 居中
            y = -120 // 居中偏上
            gravity = Gravity.CENTER or Gravity.END
        }

        // 将视图添加到 WindowManager
        windowManager.addView(tipBinding.root, params)
        tipBinding.root.post {
            val widthInner =
                tipBinding.logo.width + tipBinding.money.width + tipBinding.time.width + 150 // logo间隔
            // 更新悬浮窗的宽度和高度
            params.width = widthInner // 新宽度，单位：像素
            params.height = tipBinding.logo.height + 60 // 新高度，单位：像素
            // 应用新的布局参数
            windowManager.updateViewLayout(tipBinding.root, params)
            tipBinding.root.visibility = View.VISIBLE
            countDownTimer.start()
        }
    }

    /**
     * 处理账单信息。
     *
     * 该方法根据条件决定是否显示编辑悬浮窗或提示信息。
     *
     * 如果 `timeCount` 为 0 或 `intent.showTip` 为 `false`，则调用 `callBillInfoEditor` 方法显示编辑悬浮窗，并返回。
     * 否则，调用 `showTip` 方法显示提示信息。
     *
     */
    private fun processBillInfo() {

        val billInfoModel = intent.billInfoModel
        if (timeCount == 0 || !intent.showTip || billInfoModel.auto) {
            callBillInfoEditor(Setting.FLOAT_TIMEOUT_ACTION, billInfoModel)
            // 显示编辑悬浮窗
            return
        }
        showTip(billInfoModel)
    }


    /**
     * 记录账单信息并更新状态。
     *
     * 该方法首先将传入的账单信息对象的状态设置为[BillState.Edited]，然后异步地将该账单信息保存到数据库中。
     * 如果配置中启用了显示成功弹窗的选项，方法还会显示一个包含账单金额的成功提示。
     *
     * @param billInfoModel2 需要记录的账单信息对象。
     */
    private fun recordBillInfo(billInfoModel2: BillInfoModel) {
        billInfoModel2.state = BillState.Edited
        App.launch {
            BillInfoModel.put(billInfoModel2)
        }
        if (ConfigUtils.getBoolean(Setting.SHOW_SUCCESS_POPUP, DefaultData.SHOW_SUCCESS_POPUP)) {
            ToastUtils.info(
                context.getString(
                    R.string.auto_success,
                    billInfoModel2.money.toString(),
                ),
            )
        }
    }

    /**
     * 根据配置调用账单信息编辑器。
     *
     * 该方法根据传入的 `key` 和 `billInfoModel`，通过配置工具获取对应的整数值，
     * 并根据该值执行不同的操作：
     * - 如果值为 `FloatEvent.AUTO_ACCOUNT.ordinal`，则调用 `recordBillInfo` 方法进行记账，并停止处理。
     * - 如果值为 `FloatEvent.POP_EDIT_WINDOW.ordinal`，则显示浮动编辑对话框，允许用户编辑账单信息。
     *   编辑完成后，根据用户的选择执行相应的操作。
     * - 如果值为 `FloatEvent.NO_ACCOUNT.ordinal`，则删除账单信息并停止处理。
     *
     * @param key 配置键，用于获取对应的整数值。
     * @param billInfoModel 账单信息模型，包含需要编辑或处理的账单信息。
     */
    private fun callBillInfoEditor(key: String, billInfoModel: BillInfoModel) {
        Logger.d("CallBillInfoEditor: $key, $billInfoModel")
        App.launch(Dispatchers.Main) {
            AssetsUtils.setMapAssets(themedContext, true, billInfoModel) {
                if (billInfoModel.auto) {
                    recordBillInfo(billInfoModel)
                    stopProcess()
                    return@setMapAssets
                }
                when (ConfigUtils.getInt(key, FloatEvent.POP_EDIT_WINDOW.ordinal)) {
                    FloatEvent.AUTO_ACCOUNT.ordinal -> {
                        // 记账
                        recordBillInfo(billInfoModel)
                        stopProcess()
                    }

                    FloatEvent.POP_EDIT_WINDOW.ordinal -> {

                        //对于退款账单额外处理
                        if (billInfoModel.type == BillType.Income && billInfoModel.remark.contains(
                                Regex("退[款货]")
                            )
                        ) {
                            billInfoModel.type = BillType.IncomeRefund
                        }

                        runCatching {
                            FloatEditorDialog(themedContext, billInfoModel, true, onCancelClick = {
                                App.launch {
                                    BillInfoModel.remove(it.id)
                                }
                                stopProcess()
                            }, onConfirmClick = {
                                stopProcess()
                            }, floatingWindowService = context).show(true)
                        }.onFailure {
                            stopProcess()
                            Logger.e("Failed to show editor", it)
                        }

                    }

                    FloatEvent.NO_ACCOUNT.ordinal -> {
                        App.launch {
                            BillInfoModel.remove(billInfoModel.id)
                        }
                        stopProcess()
                    }
                }
            }
        }
    }



}