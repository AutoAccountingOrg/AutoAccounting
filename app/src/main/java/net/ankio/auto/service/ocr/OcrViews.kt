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

package net.ankio.auto.service.ocr

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.databinding.OcrStatusViewBinding
import net.ankio.auto.storage.Logger
import net.ankio.auto.utils.toThemeCtx

/**
 * OCR状态提示横幅
 *
 * 整个OCR流程的唯一状态出口，承载所有阶段的状态展示：
 * - 加载中：转圈 + 文字（OCR识别 / AI识别）
 * - 成功：✓ + 金额，3秒后回调
 * - 错误：✗ + 错误信息（白名单/截图/识别失败等），3秒后自动关闭
 */
class OcrViews {

    // 横幅视图与窗口管理器
    private var binding: OcrStatusViewBinding? = null
    private var windowManager: WindowManager? = null

    // 当前窗口布局参数（入场/退场动画共享）
    private var layoutParams: WindowManager.LayoutParams? = null

    // 动画引用
    private var currentAnimator: ValueAnimator? = null

    // 横幅实际高度，入场测量后缓存
    private var viewHeight = 0

    // 是否正在退场（防止 dismiss 重入）
    private var dismissing = false

    // 超时安全网 / 结果倒计时
    private var timeoutJob: Job? = null

    /**
     * 显示加载状态横幅
     * @param context 服务上下文
     * @param statusText 状态文本
     */
    fun show(context: Context, statusText: String) {
        // 若已显示，只更新文本（保持加载态）
        if (binding != null) {
            updateStatus(statusText)
            return
        }
        ensureView(context, statusText)
    }

    /**
     * 更新状态文本（保持当前图标状态不变）
     */
    fun updateStatus(statusText: String) {
        binding?.statusText?.text = statusText
    }

    /**
     * 显示成功结果
     * @param context 服务上下文（横幅未创建时用于初始化）
     * @param text 显示文本（如"¥100.00"）
     * @param onDone 3秒后的回调
     */
    fun showSuccess(context: Context, text: String, onDone: () -> Unit) {
        ensureView(context, text)
        switchToResult(R.drawable.ic_check_circle, R.color.success, text)
        resetTimeout(3_000) {
            dismiss()
            onDone()
        }
    }

    /**
     * 显示错误结果
     * @param context 服务上下文（横幅未创建时用于初始化）
     * @param text 错误提示文本
     */
    fun showError(context: Context, text: String) {
        ensureView(context, text)
        switchToResult(R.drawable.ic_error, R.color.danger, text)
        resetTimeout(3_000) { dismiss() }
    }

    /**
     * 关闭状态横幅（带退场动画）
     */
    fun dismiss() {
        if (dismissing || binding == null) {
            removeViewSafely()
            return
        }
        dismissing = true

        timeoutJob?.cancel()
        timeoutJob = null
        currentAnimator?.cancel()

        val params = layoutParams ?: run { removeViewSafely(); return }
        val startY = params.y

        // 退场动画：向上滑出
        currentAnimator = ValueAnimator.ofInt(startY, -viewHeight).apply {
            duration = 300
            interpolator = AccelerateInterpolator(1.5f)
            addUpdateListener { anim ->
                if (binding == null) return@addUpdateListener
                params.y = anim.animatedValue as Int
                runCatching { windowManager?.updateViewLayout(binding?.root, params) }
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    removeViewSafely()
                }
            })
            start()
        }
    }

    // ======================== 内部实现 ========================

    /**
     * 确保横幅视图已创建。已存在则仅更新文本，不存在则创建并播放入场动画。
     */
    private fun ensureView(context: Context, text: String) {
        if (binding != null) {
            binding?.statusText?.text = text
            return
        }

        dismissing = false
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 0
        }
        layoutParams = params

        val themedCtx = context.toThemeCtx()
        binding = OcrStatusViewBinding.inflate(LayoutInflater.from(themedCtx)).also {
            it.statusText.text = text
            it.progressBar.visibility = View.VISIBLE
            it.resultIcon.visibility = View.GONE
            it.root.visibility = View.INVISIBLE
        }

        windowManager?.addView(binding!!.root, params)

        // 入场动画
        binding!!.root.post {
            viewHeight = binding?.root?.height?.coerceAtLeast(80) ?: 80
            params.y = -viewHeight
            windowManager?.updateViewLayout(binding!!.root, params)

            currentAnimator?.cancel()
            currentAnimator = ValueAnimator.ofInt(-viewHeight, 0).apply {
                duration = 400
                interpolator = DecelerateInterpolator(2f)
                addUpdateListener { anim ->
                    if (binding == null) return@addUpdateListener
                    params.y = anim.animatedValue as Int
                    runCatching { windowManager?.updateViewLayout(binding?.root, params) }
                }
                start()
            }
            binding!!.root.visibility = View.VISIBLE
        }

        // 加载态不设超时——由业务方通过 showSuccess/showError/dismiss 控制生命周期
    }

    /**
     * 切换到结果态：隐藏ProgressBar，显示结果图标
     */
    private fun switchToResult(iconRes: Int, colorRes: Int, text: String) {
        val b = binding ?: return
        b.progressBar.visibility = View.GONE
        b.resultIcon.visibility = View.VISIBLE
        b.resultIcon.setImageResource(iconRes)
        b.resultIcon.setColorFilter(ContextCompat.getColor(b.root.context, colorRes))
        b.statusText.text = text
    }

    /**
     * 重置超时/倒计时任务
     */
    private fun resetTimeout(millis: Long, action: () -> Unit) {
        timeoutJob?.cancel()
        timeoutJob = CoroutineScope(Dispatchers.Main).launch {
            delay(millis)
            action()
        }
    }

    /**
     * 移除视图并释放资源（无动画，兜底方法）
     */
    private fun removeViewSafely() {
        currentAnimator?.cancel()
        currentAnimator = null
        timeoutJob?.cancel()
        timeoutJob = null
        binding?.root?.let { view ->
            try {
                if (view.isAttachedToWindow) {
                    windowManager?.removeViewImmediate(view)
                }
            } catch (e: Exception) {
                Logger.w("移除OCR状态横幅失败: ${e.message}")
            }
        }
        binding = null
        windowManager = null
        layoutParams = null
        dismissing = false
    }
}
