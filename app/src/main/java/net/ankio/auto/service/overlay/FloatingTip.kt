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
import java.util.Locale
import net.ankio.auto.databinding.FloatTipBinding
import net.ankio.auto.databinding.FloatTipLeftBinding
import net.ankio.auto.storage.Logger
import net.ankio.auto.utils.BillTool
import net.ankio.auto.utils.PrefManager
import org.ezbook.server.db.model.BillInfoModel

/**
 * 浮动提示视图控制器
 *
 * 职责：
 * - 负责浮窗的构建、展示、交互与销毁
 * - 对外暴露事件回调：点击、长按、超时
 *
 * 设计：
 * - 保持最少状态：仅持有视图绑定、窗口参数与计时器
 * - 不关心业务决策，由上层通过回调处理
 */
class FloatingTip(
    private val context: Context,
    private val windowManager: WindowManager
) {
    /** 浮窗视图绑定（右侧），按需延迟创建 */
    private val rightBinding: FloatTipBinding by lazy {
        FloatTipBinding.inflate(LayoutInflater.from(context))
    }

    /** 浮窗视图绑定（左侧），按需延迟创建（独立布局） */
    private val leftBinding: FloatTipLeftBinding by lazy {
        FloatTipLeftBinding.inflate(LayoutInflater.from(context))
    }

    /** 简单适配器：统一访问左右绑定的公共视图元素 */
    private data class BindingAdapter(
        val root: View,
        val logo: View,
        val moneyView: android.widget.TextView,
        val timeView: android.widget.TextView,
    )

    /**
     * 获取当前应使用的绑定适配器
     */
    private fun currentBinding(): BindingAdapter {
        return if (PrefManager.floatGravityRight) {
            BindingAdapter(
                root = rightBinding.root,
                logo = rightBinding.logo,
                moneyView = rightBinding.money,
                timeView = rightBinding.time,
            )
        } else {
            BindingAdapter(
                root = leftBinding.root,
                logo = leftBinding.logo,
                moneyView = leftBinding.money,
                timeView = leftBinding.time,
            )
        }
    }

    /** 计时器：控制超时自动触发 */
    private var countDownTimer: CountDownTimer? = null

    /**
     * 浮窗事件类型
     */
    sealed interface Event {
        data object Click : Event
        data object LongClick : Event
        data object Timeout : Event
    }

    /**
     * 展示浮窗
     *
     * @param bill 账单信息
     * @param onEvent 事件回调（点击、长按、超时）
     */
    fun show(
        bill: BillInfoModel,
        onEvent: (Event) -> Unit
    ) {
        Logger.d("为账单显示浮动提示: ${bill.id}")

        // 使用当前侧的绑定
        val b = currentBinding()

        // 1) 填充展示数据
        b.moneyView.text = String.format(Locale.getDefault(), "%.2f", bill.money)
        val colorRes = BillTool.getColor(bill.type)
        val color = ContextCompat.getColor(context, colorRes)
        b.moneyView.setTextColor(color)
        b.timeView.text = String.format("%ss", PrefManager.floatTimeoutOff)
        b.root.visibility = View.INVISIBLE

        // 2) 准备计时器
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(PrefManager.floatTimeoutOff * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt()
                b.timeView.text = String.format("%ss", secondsLeft)
            }

            override fun onFinish() {
                Logger.d("倒计时结束，显示编辑器")
                dismiss()
                onEvent(Event.Timeout)
            }
        }

        // 3) 交互事件
        b.root.setOnClickListener {
            Logger.d("提示被点击")
            countDownTimer?.cancel()
            dismiss()
            onEvent(Event.Click)
        }
        b.root.setOnLongClickListener {
            Logger.d("提示被长按")
            countDownTimer?.cancel()
            dismiss()
            onEvent(Event.LongClick)
            true
        }

        // 4) 添加到窗口并显示
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            x = 0
            y = -120
            gravity = if (PrefManager.floatGravityRight) Gravity.CENTER or Gravity.END
            else Gravity.CENTER or Gravity.START
        }

        try {
            windowManager.addView(b.root, params)
            b.root.post {
                // 基于内容计算尺寸
                val width = b.logo.width + b.moneyView.width + b.timeView.width + 150
                val height = b.logo.height + 60
                params.width = width
                params.height = height
                windowManager.updateViewLayout(b.root, params)

                b.root.visibility = View.VISIBLE
                countDownTimer?.start()
                Logger.d("提示窗口显示成功")
            }
        } catch (e: Exception) {
            Logger.e("添加提示窗口失败", e)
            // 失败时视为超时路径：直接回调上层使用其既有策略
            onEvent(Event.Timeout)
        }
    }

    /**
     * 关闭浮窗
     */
    fun dismiss() {
        // 分别尝试移除左右两套视图
        runCatching {
            if (rightBinding.root.isAttachedToWindow) {
                windowManager.removeViewImmediate(rightBinding.root)
                Logger.d("提示窗口移除成功")
            }
        }.onFailure { Logger.w("移除提示窗口失败: ${it.message}") }

        runCatching {
            if (leftBinding.root.isAttachedToWindow) {
                windowManager.removeViewImmediate(leftBinding.root)
                Logger.d("提示窗口移除成功")
            }
        }.onFailure { Logger.w("移除提示窗口失败: ${it.message}") }
    }

    /**
     * 销毁资源：取消计时器并移除视图
     */
    fun destroy() {
        countDownTimer?.cancel()
        countDownTimer = null
        dismiss()
    }
}


