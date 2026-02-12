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

package net.ankio.auto.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import net.ankio.auto.storage.Logger

/**
 * OCR 无障碍服务
 *
 * 职责：
 * - 跟踪当前前台应用包名（通过窗口变化事件）
 * - 提供截图能力（Android 11+ takeScreenshot API）
 * - 提供收起通知栏能力（performGlobalAction）
 *
 * 该服务仅在用户选择「无障碍」授权方式时由系统启动，
 * 通过静态 [instance] 引用供 [net.ankio.auto.service.ocr.OcrTools] 访问。
 */
class OcrAccessibilityService : AccessibilityService() {

    companion object {
        /** 当前服务实例，null 表示服务未运行 */
        @Volatile
        var instance: OcrAccessibilityService? = null
            private set

        /** 最近一次检测到的前台应用包名 */
        @Volatile
        var topPackage: String? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Logger.d("OCR无障碍服务已连接")
        // 授权后自动返回应用主界面
        packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }

    /**
     * 处理无障碍事件
     * 仅关注窗口状态变化事件，用于跟踪前台应用
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkg = event.packageName?.toString() ?: return
        // 过滤自身包名、系统UI、无效包名（不含.的包名如 android）
        if (pkg == packageName || pkg.startsWith("com.android.") || !pkg.contains('.')) return

        topPackage = pkg

        Logger.d("topPackage:${topPackage}")
    }

    override fun onInterrupt() {
        Logger.w("OCR无障碍服务被中断")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        topPackage = null
        Logger.d("OCR无障碍服务已销毁")
    }
}
