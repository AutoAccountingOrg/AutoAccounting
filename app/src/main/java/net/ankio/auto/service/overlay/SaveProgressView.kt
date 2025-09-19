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
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.ankio.auto.storage.Logger

/**
 * 保存进度悬浮窗 - 1像素透明窗口
 * 用于顺利拉起记账App
 */
class SaveProgressView {
    // 悬浮窗相关变量
    private var floatView: View? = null
    private var windowManager: WindowManager? = null

    // 超时任务引用：防止悬浮窗长时间不关闭
    private var timeoutJob: Job? = null

    /**
     * 显示保存进度悬浮窗
     * 创建一个1像素透明悬浮窗表示正在保存
     */
    fun show(context: Context) {
        // 已经显示则不再重复
        if (floatView != null) return


        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // 配置悬浮窗参数 - 1像素透明窗口
        val layoutParams = WindowManager.LayoutParams(
            1, // 宽度1像素
            1, // 高度1像素
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        layoutParams.gravity = Gravity.TOP or Gravity.START

        // 创建透明View
        floatView = View(context).apply {
            alpha = 0f // 完全透明
        }

        windowManager?.addView(floatView, layoutParams)

        // 启动后 10 秒检查：若仍未关闭，则强制结束
        timeoutJob?.cancel()
        timeoutJob = CoroutineScope(Dispatchers.Main).launch {
            delay(10_000)
            if (floatView != null) {
                Logger.w("保存进度悬浮窗超时未关闭，已强制结束")
                hide()
            }
        }
    }

    /**
     * 关闭保存进度悬浮窗
     */
    fun hide() {
        // 关闭前取消超时任务，避免重复触发
        timeoutJob?.cancel()
        timeoutJob = null

        floatView?.let { view ->
            try {
                windowManager?.removeView(view)
                Logger.d("保存进度悬浮窗已关闭")
            } catch (e: Exception) {
                Logger.e("关闭保存进度悬浮窗失败: ${e.message}")
            }
        }

        floatView = null
        windowManager = null
    }
}
