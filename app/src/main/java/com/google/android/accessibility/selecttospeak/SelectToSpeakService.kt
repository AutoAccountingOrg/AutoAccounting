/*
 * Copyright (C) 2026 ankio(ankio@ankio.net)
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

package com.google.android.accessibility.selecttospeak

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.google.android.accessibility.selecttospeak.SelectToSpeakService.Companion.instance
import net.ankio.auto.service.AnalysisUtils
import net.ankio.auto.service.CoreService
import net.ankio.auto.service.ocr.PageSignatureManager
import net.ankio.auto.storage.Logger
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.utils.Debounce
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
class SelectToSpeakService : AccessibilityService() {

    companion object {
        /** 当前服务实例，null 表示服务未运行 */
        @Volatile
        var instance: SelectToSpeakService? = null
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

        var structFp: String? = null

    }

    /** 内容变化防抖：最后一次变化后 500ms 执行，保证一定会执行 */
    private val contentChangeDebounce = Debounce<Unit>(delayMs = 500) {
        processContentChange()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        // 设置 FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY，使 WebView 子节点可被遍历（findAccessibilityNodeInfosByText 对 WebView 无效，需先找 WebView 再遍历其子节点）
        @Suppress("DEPRECATION")
        serviceInfo?.let { info ->
            info.flags =
                info.flags or AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY
            serviceInfo = info
        }
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
            }

            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                if (!PrefManager.ocrAccessibilityAutoTrigger) return
                contentChangeDebounce.run()
            }

            else -> {

            }
        }
    }

    /** 防抖后的实际处理：收集结构指纹进行匹配，内容指纹去重 */
    private fun processContentChange() {
        val pkg = topPackage ?: return
        val activity = topActivity ?: ""
        // 收集视图结构指纹用于精确匹配页面
        val structFp = collectStructureFingerprint()
        if (!PageSignatureManager.matches(pkg, activity, structFp)) {
            Logger.d("structFp: pkg=$pkg, activity=$activity, structFp=$structFp")
            return
        }

        // 收集文本内容指纹用于去重（同一页面内容没变就不重复触发）
        val rawText = collectPageText(maxDepth = 50)
        if (rawText.isBlank()) return
        val contentFp = generateFingerprint(rawText)
        if (contentFp == lastContentFingerprint) return
        lastContentFingerprint = contentFp

        if (AnalysisUtils.inWhitelist(contentFp)) {
            Logger.d("inWhitelist: contentFp=$contentFp")
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
     * 收集页面视图结构指纹：取前 2 层类名骨架。
     * 不含文本，不受数据变化影响；同一页面布局始终产生近似结果。
     */
    fun collectStructureFingerprint(): String {
        val root = rootInActiveWindow ?: return ""
        val skeleton = buildStructureSkeleton(root, 0, 5)
        root.recycle()
        return skeleton
    }


    /**
     * 打印Tree的层次结构
     *
     * @param root view
     * @param deep 层级  用于缩进的
     */
    private fun getViewTreeStr(root: AccessibilityNodeInfo?, deep: Int): String {
        val builder = StringBuilder()
        if (root != null) {
            builder.append(getSpace(deep)).append("<").append(root.className)
            builder.append(">\n")
            val childCount = root.childCount
            for (i in 0..<childCount) {
                builder.append(getViewTreeStr(root.getChild(i), deep + 1))
            }
            builder.append(getSpace(deep)).append("</").append(root.className).append(">\n")
        }
        return builder.toString()
    }

    private fun getSpace(deep: Int): String {
        val s = StringBuilder()
        for (i in 0..<deep) {
            s.append("\t")
        }
        return s.toString()
    }

    /** 递归构建类名骨架，每层最多取前 10 个子节点 */
    private fun buildStructureSkeleton(
        node: AccessibilityNodeInfo,
        depth: Int,
        maxDepth: Int
    ): String {
        if (depth > maxDepth) return ""
        val cls = node.className?.toString()?.substringAfterLast('.') ?: "?"
        val childCount = minOf(node.childCount, 10)
        val children = (0 until childCount).mapNotNull { i ->
            node.getChild(i)?.let { child ->
                buildStructureSkeleton(child, depth + 1, maxDepth).also { child.recycle() }
            }
        }.filter { it.isNotEmpty() }
        return if (children.isEmpty()) cls else "$cls[${children.joinToString(",")}]"
    }

    /**
     * 从当前窗口无障碍树收集文本（最多 [maxDepth] 层）
     * 用于生成内容指纹，供 [PageSignatureManager.matches] 模糊匹配
     *
     * 策略：
     * 1. 常规遍历收集原生控件文本
     * 2. 专项处理 WebView：findAccessibilityNodeInfosByText 对 WebView 无效，需先递归找出 WebView 节点再遍历其子控件
     * 3. WebView 子控件无 ID，只能获取 contentDescription 等有限信息；加载完成后可获取包括屏幕外在内的全部内容
     */
    private fun collectPageText(maxDepth: Int): String {
        val root = rootInActiveWindow ?: return ""
        return collectTextFromNodeWithWebView(root, 0, maxDepth).also { root.recycle() }
    }

    /**
     * 遍历收集文本，遇到 WebView 时专项遍历其子控件（需 FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY）
     */
    private fun collectTextFromNodeWithWebView(
        node: AccessibilityNodeInfo,
        depth: Int,
        maxDepth: Int
    ): String {
        if (depth > maxDepth) return ""
        val texts = mutableListOf<String>()
        val childCount = node.childCount
        if (childCount > 0) {
            for (i in 0 until childCount) {
                node.getChild(i)?.let { child ->
                    val grandchildCount = child.childCount
                    if (grandchildCount > 0) {
                        texts.add(collectTextFromNodeWithWebView(child, depth + 1, maxDepth))
                    } else {
                        child.text?.toString()?.takeIf { it.isNotBlank() }?.let { texts.add(it) }
                        child.contentDescription?.toString()?.takeIf { it.isNotBlank() }
                            ?.let { texts.add(it) }
                    }
                    child.recycle()
                }
            }
        } else {
            node.text?.toString()?.takeIf { it.isNotBlank() }?.let { texts.add(it) }
            node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { texts.add(it) }
        }
        return texts.filter { it.isNotBlank() }.joinToString(" ")
    }

    /**
     * 递归遍历：只收集叶子节点的 text/contentDescription。
     * - 有子节点：仅递归，不收集当前节点（避免容器重复）
     * - 无子节点：收集 text 和 contentDescription（含根节点为叶子的情况）
     */
    private fun collectTextFromNode(
        node: AccessibilityNodeInfo,
        depth: Int,
        maxDepth: Int
    ): String {
        if (depth > maxDepth) return ""
        val texts = mutableListOf<String>()
        val childCount = node.childCount
        if (childCount > 0) {
            for (i in 0 until childCount) {
                node.getChild(i)?.let { child ->
                    val grandchildCount = child.childCount
                    if (grandchildCount > 0) {
                        texts.add(collectTextFromNode(child, depth + 1, maxDepth))
                    } else {
                        child.text?.toString()?.takeIf { it.isNotBlank() }?.let { texts.add(it) }
                        child.contentDescription?.toString()?.takeIf { it.isNotBlank() }
                            ?.let { texts.add(it) }
                    }
                    child.recycle()
                }
            }
        } else {
            node.text?.toString()?.takeIf { it.isNotBlank() }?.let { texts.add(it) }
            node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { texts.add(it) }
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
    }
}
