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
 *  limitations under the License.
 */

package net.ankio.auto.ui.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.webkit.JavascriptInterface
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentAnalysisDetailBinding
import net.ankio.auto.http.api.AnalysisTaskAPI
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseWebViewFragment
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.utils.DateUtils
import org.ezbook.server.db.model.AnalysisTaskModel
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * AI分析详情页面
 * 用于查看AI生成的财务分析报告
 */
class AnalysisDetailFragment : BaseWebViewFragment<FragmentAnalysisDetailBinding>() {

    private var taskId: Long = -1
    private var taskModel: AnalysisTaskModel? = null
    private var isPrivacyMode = false

    /** JSON 模式导出时由 JS 回调，需在此关闭 loading */
    private var exportLoading: LoadingUtils? = null

    companion object {
        private const val ARG_TASK_ID = "task_id"
    }

    override fun getWebView(): WebView = binding.webView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        WebView.enableSlowWholeDocumentDraw()
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        taskId = arguments?.getLong(ARG_TASK_ID, -1) ?: -1
        if (taskId == -1L) {
            findNavController().popBackStack()
            return
        }

        setupUI()
        loadTaskDetail()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupUI() {
        // 注册导出桥接：WebView.draw() 无法捕获 ECharts Canvas，需通过 html2canvas + JS 回传
        binding.webView.addJavascriptInterface(ExportBridge(), "AndroidExport")

        binding.topAppBar.apply {
            setTitle(R.string.analysis_detail_title)
            setNavigationOnClickListener { findNavController().popBackStack() }
            inflateMenu(R.menu.analysis_detail_menu)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_privacy_mode -> {
                        togglePrivacyMode(it)
                        true
                    }

                    else -> false
                }
            }
        }
        binding.btnShare.setOnClickListener { shareAsImage() }
    }

    /**
     * JS Bridge：接收 html2canvas 导出的 base64 图片
     * 注意：@JavascriptInterface 方法在 WebView 线程调用，需 post 到主线程执行 UI
     */
    private inner class ExportBridge {
        @JavascriptInterface
        fun onImageCaptured(dataUrl: String) {
            view?.post { handleCapturedImage(dataUrl) }
        }

        @JavascriptInterface
        fun onCaptureFailed(msg: String) {
            view?.post {
                Logger.e("导出失败: $msg")
                ToastUtils.error(getString(R.string.analysis_generating_image) + ": $msg")
                exportLoading?.close()
                exportLoading = null
            }
        }
    }

    private fun handleCapturedImage(dataUrl: String) {
        launch {
            try {
                val base64 = dataUrl.removePrefix("data:image/png;base64,")
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                val file = File(
                    File(requireContext().cacheDir, "ai").apply { if (!exists()) mkdirs() },
                    "analysis_${taskId}_${System.currentTimeMillis()}.png"
                )
                withContext(Dispatchers.IO) {
                    file.writeBytes(bytes)
                }
                shareImageFile(file)
            } catch (e: Exception) {
                Logger.e("分享失败", e)
                ToastUtils.error(getString(R.string.analysis_generating_image) + ": ${e.message}")
            } finally {
                exportLoading?.close()
                exportLoading = null
            }
        }
    }

    private fun togglePrivacyMode(item: android.view.MenuItem) {
        isPrivacyMode = !isPrivacyMode
        item.setIcon(if (isPrivacyMode) R.drawable.ic_visibility_off else R.drawable.ic_visibility)
        binding.webView.evaluateJavascript("togglePrivacyMode($isPrivacyMode)", null)
    }

    override fun loadInitialUrl(): String? = null

    private fun loadTaskDetail() {
        val loading = LoadingUtils(requireActivity())
        launch {
            loading.show(getString(R.string.loading))
            try {
                val task = AnalysisTaskAPI.getTaskById(taskId)
                loading.close()
                if (task != null && !task.resultHtml.isNullOrBlank()) {
                    taskModel = task
                    binding.topAppBar.title = task.title
                    binding.topAppBar.subtitle =
                        DateUtils.formatTimeRange(requireContext(), task.startTime, task.endTime)
                    handleData(task.resultHtml!!)
                } else {
                    ToastUtils.error(getString(R.string.analysis_result_not_found))
                }
            } catch (e: Exception) {
                loading.close()
                Logger.e("加载分析详情失败", e)
            }
        }
    }

    private fun handleData(data: String) {
        val isJson = data.trimStart().startsWith("{")
        if (isJson) {
            binding.webView.loadUrl("file:///android_asset/summary/ai.html")
        } else {
            val wrappedHtml = wrapLegacyHtml(data, getAppLogoBase64(), getString(R.string.app_name))
            binding.webView.loadDataWithBaseURL(null, wrappedHtml, "text/html", "UTF-8", null)
            binding.btnShare.visibility = View.VISIBLE
            binding.webView.visibility = View.VISIBLE
            binding.statusPage.showContent()
        }
    }

    override fun onWebViewReady() {
        val data = taskModel?.resultHtml ?: return
        if (data.trimStart().startsWith("{")) {
            // 检查原始数据是否为空对象
            if (data.trim() == "{}") {
                ToastUtils.error(getString(R.string.analysis_result_empty))
                findNavController().popBackStack()
                return
            }

            val finalJson = buildFinalJson(
                data, getAppLogoBase64(), getString(R.string.app_name),
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            )

            // 检查构建后的JSON是否为空对象（解析失败的情况）
            if (finalJson == "{}") {
                ToastUtils.error(getString(R.string.analysis_result_parse_error))
                findNavController().popBackStack()
                return
            }

            binding.webView.evaluateJavascript("setJson($finalJson);", null)
            binding.btnShare.visibility = View.VISIBLE
            binding.webView.visibility = View.VISIBLE
            binding.statusPage.showContent()
        }
    }

    private fun wrapLegacyHtml(content: String, logoBase64: String, appName: String): String {
        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        return """
        <!DOCTYPE html><html><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0">
        <style>
            :root { --text-primary: #1f2937; --text-secondary: #6b7280; }
            @media (prefers-color-scheme: dark) { :root { --text-primary: #e5e7eb; --text-secondary: #9ca3af; } }
            body { padding: 1.5rem; font-family: sans-serif; color: var(--text-primary); }
            .header { display: flex; align-items: center; gap: 12px; padding-bottom: 1.5rem; }
            .logo img { width: 28px; height: 28px; border-radius: 6px; }
            .period-title { font-size: 18px; font-weight: 600; margin: 0; }
            .footer { text-align: center; padding: 1.5rem; color: var(--text-secondary); font-size: 14px; }
        </style>
        <script>
            function togglePrivacyMode(enabled) {
                function processTextNodes(node) {
                    if (node.nodeType === Node.TEXT_NODE) {
                        if (enabled) {
                            // 支持格式：¥100, ¥ 100, ¥-100, ¥ -100
                            if (/¥\s*-?[\d,.]+/.test(node.nodeValue)) {
                                if (!node._originalText) node._originalText = node.nodeValue;
                                node.nodeValue = node.nodeValue.replace(/¥\s*-?[\d,.]+/g, '¥***');
                            }
                        } else {
                            if (node._originalText) {
                                node.nodeValue = node._originalText;
                                delete node._originalText;
                            }
                        }
                    } else if (node.nodeType === Node.ELEMENT_NODE) {
                        node.childNodes.forEach(processTextNodes);
                    }
                }
                processTextNodes(document.body);
            }
        </script>
        </head><body>
            <div class="header">
                <div class="logo">${if (logoBase64.isNotEmpty()) "<img src=\"$logoBase64\">" else "💰"}</div>
                <p class="period-title">$appName • 财务分析</p>
            </div>
            <div class="content">$content</div>
            <div class="footer">由 $appName 生成 • $currentTime</div>
        </body></html>
        """.trimIndent()
    }

    private fun buildFinalJson(json: String, logo: String, name: String, time: String): String {
        return try {
            val data = org.json.JSONObject(json)
            val labels = mapOf(
                "reportTitle" to "财务全景透视报告",
                "healthScoreLabel" to "AI 财务健康分",
                "incomeLabel" to "总收入 (含工资/理财)",
                "expenseLabel" to "本月总支出",
                "outlierLabel" to "独秀指数 (Outlier)",
                "consumeTitle" to "🧩 消费结构分析",
                "radarTitle" to "🚀 结构画像雷达",
                "riskTitle" to "<span>⚠️</span> 异常风险",
                "behaviorTitle" to "🔍 消费画像与行为规律",
                "conclusionTitle" to "<span>⚖️</span> 综合结论与健康等级",
                "actionTitle" to "<span>✅</span> 行动清单",
                "executionTitle" to "✅ 执行优先级",
                "recordQualityTitle" to "🧭 记录质量提升"
            )
            labels.forEach { (k, v) -> data.put(k, v) }
            data.put("logoBase64", logo)
            data.put("pageHeaderTitle", "$name • 财务分析")
            data.put("pageFooter", "由 $name 生成 • $time")
            data.toString()
        } catch (e: Exception) {
            Logger.e(e)
            "{}"
        }
    }

    private fun shareAsImage() {
        val isJsonMode = taskModel?.resultHtml?.trimStart()?.startsWith("{") == true
        if (isJsonMode) {
            // ai.html 含 ECharts Canvas，WebView.draw() 无法捕获，改用 html2canvas + JS Bridge
            val loading = LoadingUtils(requireActivity())
            exportLoading = loading
            loading.show(getString(R.string.analysis_generating_image))
            binding.webView.evaluateJavascript(
                "if(typeof captureForExport==='function')captureForExport();else if(window.AndroidExport)window.AndroidExport.onCaptureFailed('captureForExport 未定义');",
                null
            )
            return
        }
        // 旧版 HTML 无图表，可直接用 draw 截屏
        val loading = LoadingUtils(requireActivity())
        launch {
            loading.show(getString(R.string.analysis_generating_image))
            try {
                val file = File(
                    File(requireContext().cacheDir, "ai").apply { if (!exists()) mkdirs() },
                    "analysis_${taskId}_${System.currentTimeMillis()}.png"
                )
                val bitmap = withContext(Dispatchers.Main) {
                    val width = binding.webView.width.takeIf { it > 0 }
                        ?: resources.displayMetrics.widthPixels
                    val height = (binding.webView.contentHeight * binding.webView.scale).toInt()
                        .takeIf { it > 0 } ?: binding.webView.height
                    binding.webView.measure(
                        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
                    )
                    binding.webView.layout(0, 0, width, height)
                    val bmp = createBitmap(width, height)
                    val canvas = Canvas(bmp)
                    canvas.drawColor(Color.WHITE)
                    binding.webView.draw(canvas)
                    bmp
                }
                withContext(Dispatchers.IO) {
                    file.outputStream().use {
                        bitmap.compress(
                            android.graphics.Bitmap.CompressFormat.PNG,
                            100,
                            it
                        )
                    }
                }
                shareImageFile(file)
            } catch (e: Exception) {
                Logger.e("分享失败", e)
            } finally {
                loading.close()
            }
        }
    }

    private fun shareImageFile(file: File) {
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "我的财务分析报告")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = android.content.ClipData.newRawUri("", uri)
        }
        val chooser = Intent.createChooser(intent, getString(R.string.analysis_share_title))
        chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(chooser)
    }

    private fun getAppLogoBase64(): String {
        return try {
            val drawable = requireContext().getDrawable(R.mipmap.ic_launcher) ?: return ""
            val bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            val os = ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, os)
            "data:image/png;base64," + Base64.encodeToString(os.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            ""
        }
    }
}
