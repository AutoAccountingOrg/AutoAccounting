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
import net.ankio.auto.service.ocr.PageSignatureManager
import net.ankio.auto.storage.Logger
import net.ankio.auto.utils.PrefManager
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
     * 跟踪前台应用及 Activity，并在切换到可识别页面时自动触发 OCR
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkg = event.packageName?.toString() ?: return
        // 过滤自身包名、系统UI、无效包名（不含.的包名如 android）
        if (pkg == packageName || pkg.startsWith("com.android.") || !pkg.contains('.')) return

        topPackage = pkg

        Logger.d("topPackage:$topPackage")

        topActivity = event.className?.toString()?.takeIf { it.isNotBlank() }


        if (!PrefManager.ocrAccessibilityAutoTrigger) return
        val rawText = collectPageText(maxDepth = 3)
        val contentFingerprint = PageSignatureManager.generateFingerprint(rawText)
        lastContentFingerprint = contentFingerprint
        Logger.d("topActivity:$topActivity, fingerprint:${contentFingerprint.take(80)}")
        // 无障碍 + OCR 模式下，页面切换时自动触发
        tryAutoTriggerOcr(pkg, topActivity ?: "", contentFingerprint)
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

    /**
     * 尝试自动触发 OCR
     * 条件：OCR 模式 + 无障碍授权 + 防抖通过 + 页面可识别
     * 防抖：同一页面 60 秒内不重复触发（避免关闭账单弹窗后重复记账）；离开可触发页面时重置 key，以便再次进入时能触发
     */
    private fun tryAutoTriggerOcr(
        pkg: String,
        activityName: String,
        contentFingerprint: String? = null
    ) {
        val shouldTrigger = PageSignatureManager.matches(pkg, activityName, contentFingerprint)

        val key = "$pkg/$activityName"
        if (!shouldTrigger) {
            // 离开可触发页面时重置，下次再进入同一页面才能触发
            lastAutoTriggerKey = key
            return
        }

        val now = System.currentTimeMillis()
        if (key == lastAutoTriggerKey || now - lastAutoTriggerTime < SAME_PAGE_DEBOUNCE_MS) return

        lastAutoTriggerKey = key
        lastAutoTriggerTime = now
        Logger.d("页面切换自动触发 OCR: $key")
        val intent = Intent(this, CoreService::class.java).apply {
            putExtra("intentType", IntentType.OCR.name)
            putExtra("manual", true)
        }
        startService(intent)
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
