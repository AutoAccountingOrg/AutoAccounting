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

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PixelFormat
import android.os.CountDownTimer
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.core.content.ContextCompat
import java.util.Locale
import net.ankio.auto.App
import net.ankio.auto.databinding.FloatTipBinding
import net.ankio.auto.databinding.FloatTipLeftBinding
import net.ankio.auto.databinding.FloatTipTopBinding
import net.ankio.auto.http.api.BookNameAPI
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.components.IconView
import net.ankio.auto.ui.utils.DisplayUtils
import net.ankio.auto.ui.utils.setAssetIconByName
import net.ankio.auto.ui.utils.setCategoryIcon
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

    /** 浮窗视图绑定（顶部），按需延迟创建（通知样式布局） */
    private val topBinding: FloatTipTopBinding by lazy {
        FloatTipTopBinding.inflate(LayoutInflater.from(context))
    }

    /** 简单适配器：统一访问左右绑定的公共视图元素 */
    private data class BindingAdapter(
        val root: View,
        val logo: View,
        val moneyView: android.widget.TextView,
        val timeView: android.widget.TextView,
        val categoryView: android.widget.TextView? = null,  // 分类名称（仅顶部布局）
        val remarkView: android.widget.TextView? = null,    // 备注（仅顶部布局）
        val assetView: IconView? = null,                   // 资产视图（仅顶部布局）
    )

    /**
     * 获取当前应使用的绑定适配器
     * 根据位置配置返回对应的绑定（左侧、右侧或顶部）
     */
    private fun currentBinding(): BindingAdapter {
        return when (PrefManager.floatGravityPosition) {
            "right" -> BindingAdapter(
                root = rightBinding.root,
                logo = rightBinding.logo,
                moneyView = rightBinding.money,
                timeView = rightBinding.time,
            )

            "top" -> BindingAdapter(
                // 顶部位置使用通知样式布局
                root = topBinding.root,
                logo = topBinding.logo,
                moneyView = topBinding.money,
                timeView = topBinding.time,
                categoryView = topBinding.category,
                remarkView = topBinding.remark,
                assetView = topBinding.asset,
            )

            else -> BindingAdapter(
                // 默认左侧
                root = leftBinding.root,
                logo = leftBinding.logo,
                moneyView = leftBinding.money,
                timeView = leftBinding.time,
            )
        }
    }

    /** 计时器：控制超时自动触发 */
    private var countDownTimer: CountDownTimer? = null

    /** 当前动画对象，用于取消 */
    private var currentAnimator: ValueAnimator? = null

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
        val colorRes = BillTool.getColor(bill.type)
        val color = ContextCompat.getColor(context, colorRes)

        // 金额显示：顶部布局显示货币符号，左右布局只显示数字
        val moneyText = if (b.categoryView != null) {
            // 顶部布局：显示货币符号
            String.format(Locale.getDefault(), "¥%.2f", bill.money)
        } else {
            // 左右布局：只显示数字
            String.format(Locale.getDefault(), "%.2f", bill.money)
        }
        b.moneyView.text = moneyText
        b.moneyView.setTextColor(color)

        // 倒计时显示
        b.timeView.text = String.format("%ss", PrefManager.floatTimeoutOff)
        b.root.visibility = View.INVISIBLE

        // 顶部布局特有：分类名称和备注
        b.categoryView?.text = bill.cateName.ifEmpty { "未分类" }
        b.remarkView?.text = bill.remark.ifEmpty { "无备注" }
        b.remarkView?.visibility = if (bill.remark.isNotEmpty()) View.VISIBLE else View.GONE

        // 顶部布局特有：资产信息（如果开启了资产管理且资产名称不为空）
        b.assetView?.let { assetView ->
            if (PrefManager.featureAssetManage && bill.accountNameFrom.isNotEmpty()) {
                assetView.visibility = View.VISIBLE
                assetView.setText(bill.accountNameFrom)
                App.launch {
                    assetView.imageView().setAssetIconByName(bill.accountNameFrom)
                }
            } else {
                assetView.visibility = View.GONE
            }
        }

        // 加载分类图标：从 logo (RoundFrameLayout) 中获取 ImageView 并设置分类图标
        val logoImageView = (b.logo as? ViewGroup)?.getChildAt(0) as? ImageView
        if (logoImageView != null && bill.cateName.isNotEmpty()) {
            App.launch {
                val book = BookNameAPI.getBook(bill.bookName)
                logoImageView.setCategoryIcon(
                    name = bill.cateName,
                    bookId = book.remoteId,
                    type = bill.type.name
                )
            }
        }

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
            // 根据位置配置设置重力
            gravity = when (PrefManager.floatGravityPosition) {
                "right" -> Gravity.CENTER or Gravity.END
                "top" -> Gravity.CENTER_HORIZONTAL or Gravity.TOP
                else -> Gravity.CENTER or Gravity.START  // 默认左侧
            }
            x = 0
            // 顶部位置时，y 设置为较小的正值；左右位置时保持原值
            y = if (PrefManager.floatGravityPosition == "top") 0 else -120
        }

        try {
            windowManager.addView(b.root, params)
            b.root.post {
                // 基于内容计算尺寸
                // 顶部布局使用 wrap_content，其他布局使用固定计算
                if (PrefManager.floatGravityPosition == "top") {
                    // 顶部布局：使用 MATCH_PARENT 宽度，高度自适应
                    // 边距通过布局文件的 padding 设置，不需要在这里设置
                    params.width = WindowManager.LayoutParams.MATCH_PARENT
                    params.height = WindowManager.LayoutParams.WRAP_CONTENT
                } else {
                    // 左右布局：基于内容计算尺寸
                    val width = b.logo.width + b.moneyView.width + b.timeView.width + 150
                    val height = b.logo.height + 60
                    params.width = width
                    params.height = height
                }
                windowManager.updateViewLayout(b.root, params)

                // 设置初始位置（屏幕外）并执行滑动动画
                val position = PrefManager.floatGravityPosition
                val screenWidth = context.resources.displayMetrics.widthPixels

                // 根据位置设置初始值和目标值
                val (startValue, endValue, isXAxis) = when (position) {
                    "top" -> {
                        // 顶部：从上方滑下来（y 从负值到目标值）
                        val viewHeight = b.root.height.coerceAtLeast(100) // 确保有最小高度
                        Triple(-viewHeight.toFloat(), params.y.toFloat(), false)
                    }

                    "right" -> {
                        // 右侧：从右侧滑到左边（x 从正值到目标值）
                        Triple(screenWidth.toFloat(), params.x.toFloat(), true)
                    }

                    else -> {
                        // 左侧：从左侧滑到右边（x 从负值到目标值）
                        val viewWidth = b.root.width.coerceAtLeast(100) // 确保有最小宽度
                        Triple(-viewWidth.toFloat(), params.x.toFloat(), true)
                    }
                }

                // 设置初始位置
                if (isXAxis) {
                    params.x = startValue.toInt()
                } else {
                    params.y = startValue.toInt()
                }
                windowManager.updateViewLayout(b.root, params)

                // 取消之前的动画
                currentAnimator?.cancel()

                // 创建滑动动画
                currentAnimator = ValueAnimator.ofFloat(startValue, endValue).apply {
                    duration = 300 // 动画时长 300ms
                    interpolator = DecelerateInterpolator() // 减速插值器，更自然的滑动效果
                    addUpdateListener { animator ->
                        val animatedValue = animator.animatedValue as Float
                        if (isXAxis) {
                            params.x = animatedValue.toInt()
                        } else {
                            params.y = animatedValue.toInt()
                        }
                        windowManager.updateViewLayout(b.root, params)
                    }
                    start()
                }

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
        // 分别尝试移除左右顶部三套视图
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

        runCatching {
            if (topBinding.root.isAttachedToWindow) {
                windowManager.removeViewImmediate(topBinding.root)
                Logger.d("提示窗口移除成功")
            }
        }.onFailure { Logger.w("移除提示窗口失败: ${it.message}") }
    }

    /**
     * 销毁资源：取消计时器和动画，并移除视图
     */
    fun destroy() {
        countDownTimer?.cancel()
        countDownTimer = null
        currentAnimator?.cancel()
        currentAnimator = null
        dismiss()
    }
}


