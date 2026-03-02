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
import android.view.accessibility.AccessibilityNodeInfo
import net.ankio.auto.service.OcrAccessibilityService.Companion.instance
import net.ankio.auto.service.ocr.PageSignatureManager
import net.ankio.auto.storage.Logger
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.utils.Throttle
import org.ezbook.server.intent.IntentType

/**
 * OCR 无障碍服务
 *
 * 职责：
 * - 跟踪当前前台应用包名及 Activity（通过窗口变化事件）
 * - 页面切换时自动触发 OCR：当切换到已记住页面或白名单应用时
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

        /** 最近一次检测到的 Activity 类名（TYPE_WINDOW_STATE_CHANGED 时 event.className） */
        @Volatile
        var topActivity: String? = null
            private set

        var lastContentFingerprint: String? = null


        /** 同一页面防抖：关闭账单弹窗后焦点回到原页面会再次触发事件，需较长间隔避免重复记账 */
        private const val SAME_PAGE_DEBOUNCE_MS = 5_000L
    }

    /** 上次自动触发的页面 key 及时间，用于防抖 */
    private var lastAutoTriggerKey = ""
    private var lastAutoTriggerTime = 0L

    /** 内容变化节流：500ms 内只处理一次 */
    private val contentChangeThrottle = Throttle<Unit>(intervalMs = 500) {
        processContentChange()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        // 授权后自动返回应用主界面
        packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName || pkg.startsWith("com.android.") || !pkg.contains('.')) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                topPackage = pkg
                topActivity = event.className?.toString()?.takeIf { it.isNotBlank() }
                Logger.d("Window changed: pkg=$topPackage, activity=$topActivity")
            }

            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                if (!PrefManager.ocrAccessibilityAutoTrigger) return
                contentChangeThrottle.run()
            }

            else -> {

            }
        }
    }

    /** 节流后的实际处理：收集指纹，签名不变则忽略 */
    private fun processContentChange() {
        Logger.d("OCR auto trigger: pkg=$topPackage activity=$topActivity")
        val pkg = topPackage ?: return
        Logger.d(
            "OCR auto trigger: pkg=$pkg activity=$topActivity, match:" + PageSignatureManager.matches(
                pkg,
                topActivity ?: ""
            )
        )

        if (!PageSignatureManager.matches(pkg, topActivity ?: "")) return

        val rawText = collectPageText(maxDepth = 50)
        if (rawText.isBlank()) return
        val contentFingerprint = generateFingerprint(rawText)
        if (contentFingerprint == lastContentFingerprint) return
        lastContentFingerprint = contentFingerprint

        Logger.d("Page content:pkg=$topPackage activity=$topActivity, fp=${contentFingerprint}")

        if (AnalysisUtils.inWhitelist(contentFingerprint)) {
            val intent = Intent(this, CoreService::class.java).apply {
                putExtra("intentType", IntentType.OCR.name)
                putExtra("manual", true)
            }
            startService(intent)
        }

    }

    /**
     * 生成内容指纹：移除可变信息后归一化，相同布局得相同指纹
     * - 千分位逗号移除（1,234.56 → 1234.56）
     * - 数字替换为 #
     */
    fun generateFingerprint(ocrText: String): String {
        if (ocrText.isBlank()) return ""
        return ocrText
            .replace(Regex("(?<=\\d),(?=\\d)"), "")
            .replace(Regex("\\d+(\\.\\d+)?"), "#")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(300)
    }

    /**
     * 从当前窗口无障碍树收集文本（最多 [maxDepth] 层）
     * 用于生成内容指纹，供 [PageSignatureManager.matches] 模糊匹配
     */
    private fun collectPageText(maxDepth: Int): String {
        val root = rootInActiveWindow ?: return ""
        return collectTextFromNode(root, 0, maxDepth).also { root.recycle() }
    }

    /** 递归收集节点及其子节点的 text/contentDescription，深度不超过 maxDepth */
    private fun collectTextFromNode(
        node: AccessibilityNodeInfo,
        depth: Int,
        maxDepth: Int
    ): String {
        if (depth > maxDepth) return ""
        val texts = mutableListOf<String>()
        node.text?.toString()?.takeIf { it.isNotBlank() }?.let { texts.add(it) }
        node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { texts.add(it) }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                texts.add(collectTextFromNode(child, depth + 1, maxDepth))
                child.recycle()
            }
        }
        return texts.filter { it.isNotBlank() }.joinToString(" ")
    }

    override fun onInterrupt() {
        Logger.w("OCR无障碍服务被中断")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        topPackage = null
        topActivity = null
        Logger.d("OCR无障碍服务已销毁")
    }
}
