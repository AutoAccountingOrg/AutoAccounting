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
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import kotlinx.coroutines.delay
import net.ankio.auto.constant.WorkMode
import net.ankio.auto.service.api.ICoreService
import net.ankio.auto.storage.Logger

/**
 * 保存进度保活悬浮窗
 *
 * 设计目标：
 * - 使用 1 像素透明悬浮窗实现「保活」效果，保证在保存账单等关键流程时系统不打断
 * - 引用计数（reference count）式生命周期：多次 show 叠加，成对 hide 才移除，避免早关
 * - 主线程安全：所有窗口操作均在主线程执行，防止崩溃与状态竞争
 * - 安全超时兜底：长时间未关闭时自动回收，避免意外泄漏
 *
 * 使用方式：
 * - 与旧版保持兼容：`show(context)` 与 `hide()`，无需改动调用方
 * - 可反复调用 `show` 叠加「保活」；每次 `hide` 仅减少一次引用
 */
class SaveProgressView {
    // ============ 内部状态 ============

    /** 透明悬浮视图（1px） */
    private var floatView: View? = null

    /** 系统窗口管理器 */
    private var windowManager: WindowManager? = null

    // ============ 对外接口 ============

    /**
     * 显示保活悬浮窗。
     *
     * 行为：
     * - 第一次调用时创建并添加 1px 全透明悬浮窗
     * - 之后的调用仅增加引用计数，不重复添加视图
     * - 启动安全超时（兜底回收），避免异常情况下泄漏
     */
    fun show(service: ICoreService) {
        // 始终在主线程执行窗口操作
        // 初始化 WindowManager（使用 Application Context，避免持有短生命周期引用）
        if (windowManager == null) {
            windowManager =
                service.context().applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        }

        // 首次展示时创建并添加 1px 透明窗口
        if (floatView == null) {
            val layoutParams = WindowManager.LayoutParams(
                1, // 宽度 1 像素
                1, // 高度 1 像素
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
            }

            floatView = View(service.context()).apply { alpha = 0f }

            runCatching {
                windowManager?.addView(floatView, layoutParams)
                Logger.d("保活悬浮窗已创建并显示")
            }.onFailure {
                Logger.e("添加保活悬浮窗失败: ${it.message}")
                // 创建失败不增加引用计数，直接返回
                floatView = null
                windowManager = null
                return
            }
        }

        if (WorkMode.isXposed()) {
            service.launch {
                delay(10_000)
                Logger.w("保活悬浮窗安全超时到期，强制清理")
                forceCloseInternal()
            }
            }

        }

    /**
     * 强制销毁：清零计数并移除视图，取消超时任务。
     * 一般无需调用，仅在上层需要明确回收时使用。
     */
    fun destroy() {
        forceCloseInternal()
    }


    /**
     * 强制关闭与回收
     */
    private fun forceCloseInternal() {
        floatView?.let { view ->
            runCatching {
                if (view.isAttachedToWindow) {
                    windowManager?.removeViewImmediate(view)
                } else {
                    windowManager?.removeView(view)
                }
                Logger.d("保活悬浮窗已被强制关闭")
            }.onFailure {
                Logger.e("强制关闭保活悬浮窗失败: ${it.message}")
            }
        }
        floatView = null
        windowManager = null
    }
}
