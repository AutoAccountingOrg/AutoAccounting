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

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import net.ankio.auto.databinding.OcrViewBinding
import net.ankio.auto.storage.Logger
import io.github.oshai.kotlinlogging.KotlinLogging

class OcrViews {

    private val logger = KotlinLogging.logger(this::class.java.name)
    // 悬浮窗相关变量
    private var floatView: View? = null
    private var windowManager: WindowManager? = null

    // UI 线程的超时处理器，用于在开启后 30 秒检查并强制关闭悬浮窗
    private val timeoutHandler = Handler(Looper.getMainLooper())
    private var timeoutPosted = false

    /**
     * 显示OCR识别动画界面
     * 创建一个全屏悬浮窗来显示识别动画
     */
    fun startOcrView(context: Context) {
        // 已经显示则不再重复
        if (floatView != null) return

        // 检查悬浮窗权限
        if (!Settings.canDrawOverlays(context)) {
            logger.error { "不支持显示悬浮窗" }
            return
        }

        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // 配置悬浮窗参数
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        layoutParams.gravity = Gravity.CENTER

        // 创建并显示悬浮窗
        floatView = OcrViewBinding.inflate(LayoutInflater.from(context)).root
        windowManager?.addView(floatView, layoutParams)

        // 启动后 30 秒检查：若仍未关闭，则强制结束，防止悬浮窗长时间滞留
        if (!timeoutPosted) {
            timeoutPosted = true
            timeoutHandler.postDelayed({
                timeoutPosted = false
                if (floatView != null) {
                    logger.warn { "OCR悬浮窗超时未关闭，已强制结束" }
                    stopOcrView()
                }
            }, 30_000)
        }
    }

    /**
     * 关闭OCR识别动画界面
     */
    fun stopOcrView() {
        // 关闭前先移除所有超时回调，避免重复触发
        timeoutHandler.removeCallbacksAndMessages(null)
        timeoutPosted = false
        floatView?.let { view ->
            try {
                // 使用 removeViewImmediate 避免异步移除导致的 Surface 残留/队列死亡
                if (view.isAttachedToWindow) {
                    windowManager?.removeViewImmediate(view)
                }
            } catch (e: Exception) {
                logger.warn { "移除OCR悬浮窗失败: ${e.message}" }
            } finally {
                floatView = null
                windowManager = null
            }
        }
    }

}