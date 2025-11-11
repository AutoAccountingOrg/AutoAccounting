/*
 * Copyright (C) 2025 ankio
 * Licensed under the Apache License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-3.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.ankio.auto.service.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import net.ankio.auto.R
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.utils.toThemeCtx

/**
 * RepeatToast：重复账单提示用的轻量级悬浮 Toast。
 *
 * 目标：
 * - 外观尽量贴近 `ToastUtils`，但以系统悬浮窗方式显示。
 * - 仅负责展示最多 5 秒，不做复杂交互或状态管理。
 * - 提供右侧一个可点击的动作入口（例如“不要去重”）。
 *
 * 行为：
 * - show() 后在主线程添加悬浮窗；
 * - 若 5 秒内无点击，则自动移除；
 * - 点击动作后立即移除并回调上层；
 * - 多次 show 之间互不影响，每个实例只负责自己的一次展示。
 */
class RepeatToast(
    private val context: Context
) {
    /** 悬浮视图 */
    private var rootView: View? = null

    /** 窗口管理器 */
    private var windowManager: WindowManager? = null

    /** 主线程 Handler，用于超时与安全移除 */
    private val mainHandler = Handler(Looper.getMainLooper())

    /** 是否已显示（防重复添加） */
    private var isShown: Boolean = false

    /** 自动消失的超时任务 */
    private val autoDismissRunnable = Runnable { dismissInternal() }

    /**
     * 显示悬浮 Toast。
     *
     * @param message 提示文案（如“检测到重复账单，已自动合并。”）
     * @param action 文案（如“不要去重”），传空字符串将隐藏动作区
     * @param onAction 点击动作时回调
     */
    fun show(message: String, action: String, onAction: () -> Unit) {
        if (isShown) return
        isShown = true

        // WindowManager 使用应用上下文，避免持有短生命周期引用
        val appCtx = context.applicationContext
        val wm = appCtx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm

        // 视图使用主题化的 Context 进行膨胀，保证主题属性可用
        val themedCtx = context.toThemeCtx()
        val view = LayoutInflater.from(themedCtx).inflate(R.layout.repeat_toast, null)
        rootView = view

        val msgView = view.findViewById<TextView>(R.id.message)
        val actionView = view.findViewById<TextView>(R.id.action)

        msgView.text = message
        if (action.isNotEmpty()) {
            actionView.visibility = View.VISIBLE
            actionView.text = action
            actionView.setOnClickListener {
                Logger.d("RepeatToast action clicked")
                runCatching { onAction.invoke() }
                dismissInternal()
            }
        } else {
            actionView.visibility = View.GONE
        }

        // 布局参数：贴近 Toast 行为，底部居中，略上移
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = ToastUtils.position() or Gravity.CENTER_HORIZONTAL
            y = 200
            // 不设置动画，保持简单稳定
        }

        // 在主线程添加视图
        mainHandler.post {
            try {
                wm.addView(view, params)
                // 5 秒后自动移除
                mainHandler.postDelayed(autoDismissRunnable, 5_000)
            } catch (e: Exception) {
                Logger.e("RepeatToast addView failed", e)
                dismissInternal()
            }
        }
    }

    /**
     * 强制销毁：同步移除视图并清理资源。
     */
    fun destroy() {
        mainHandler.removeCallbacks(autoDismissRunnable)
        dismissInternal()
    }

    /**
     * 内部移除视图，幂等。
     */
    private fun dismissInternal() {
        mainHandler.removeCallbacks(autoDismissRunnable)
        val view = rootView
        val wm = windowManager
        rootView = null
        windowManager = null
        isShown = false

        if (view != null && wm != null) {
            runCatching {
                if (view.isAttachedToWindow) {
                    wm.removeViewImmediate(view)
                } else {
                    wm.removeView(view)
                }
            }.onFailure { Logger.w("RepeatToast removeView failed: ${it.message}") }
        }
    }
} 