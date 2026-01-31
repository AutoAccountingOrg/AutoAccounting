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

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentAnalysisDetailBinding
import net.ankio.auto.http.api.AnalysisTaskAPI
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseFragment
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
class AnalysisDetailFragment : BaseFragment<FragmentAnalysisDetailBinding>() {

    private var taskId: Long = -1
    private var taskModel: AnalysisTaskModel? = null

    companion object {
        private const val ARG_TASK_ID = "task_id"
    }

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

        // 获取任务ID
        taskId = arguments?.getLong(ARG_TASK_ID, -1) ?: -1
        if (taskId == -1L) {
            findNavController().popBackStack()
            return
        }

        setupUI()
        loadTaskDetail()
    }

    override fun onDestroyView() {
        // 在销毁视图前，尽最大可能阻断 WebView 的后续回调
        try {
            binding.webView.apply {
                stopLoading()
            }
        } catch (_: Throwable) {
        }
        super.onDestroyView()
    }

    /**
     * 设置UI组件
     */
    private fun setupUI() {
        // 设置标题
        binding.topAppBar.setTitle(R.string.analysis_detail_title)

        // 设置WebView
        setupWebView()

        // 设置点击事件
        binding.btnShare.setOnClickListener { shareAsImage() }

        // 设置返回按钮
        binding.topAppBar.setNavigationOnClickListener {
            @Suppress("DEPRECATION")
            requireActivity().onBackPressed()
        }
    }

    /**
     * 设置WebView
     */
    private fun setupWebView() {
        binding.webView.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                setSupportZoom(false)
            }

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // 页面加载完成后显示分享按钮
                    if (!uiReady()) return
                    binding.btnShare.visibility = View.VISIBLE
                    binding.webView.visibility = View.VISIBLE
                }
            }
        }
    }

    /**
     * 加载任务详情
     */
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
                    loadHtmlTemplate(task.resultHtml!!)
                } else {
                    showError(getString(R.string.analysis_result_not_found))
                }

            } catch (e: Exception) {
                loading.close()
                Logger.e("加载分析详情失败", e)
                showError(getString(R.string.analysis_load_error, e.message))
            }
        }
    }

    /**
     * 加载 HTML 模板并注入数据
     */
    private fun loadHtmlTemplate(data: String) {
        if (!uiReady()) return

        // 判断数据类型：JSON 或 HTML
        val isJson = data.trimStart().startsWith("{")

        if (isJson) {
            // 新数据：JSON 格式，使用模板
            loadJsonData(data)
        } else {
            // 老数据：HTML 格式，直接显示
            loadLegacyHtml(data)
        }
    }

    /**
     * 加载新格式数据（JSON）
     */
    private fun loadJsonData(jsonData: String) {
        // 加载 ai.html 模板
        binding.webView.loadUrl("file:///android_asset/summary/ai.html")

        // 等待页面加载完成后注入数据
        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (!uiReady()) return

                // 准备完整数据（包含 logo 和时间）
                val logoBase64 = getAppLogoBase64()
                val appName = getString(R.string.app_name)
                val currentTime =
                    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

                // 构建完整 JSON（合并后端数据、固定标签、logo 和时间）
                val finalJson = buildFinalJson(jsonData, logoBase64, appName, currentTime)

                // 调用 setJson 注入数据
                binding.webView.evaluateJavascript(
                    "setJson($finalJson);",
                    null
                )

                binding.btnShare.visibility = View.VISIBLE
                binding.webView.visibility = View.VISIBLE
                binding.statusPage.showContent()
            }
        }
    }

    /**
     * 加载老格式数据（HTML）
     */
    private fun loadLegacyHtml(htmlContent: String) {
        val appName = getString(R.string.app_name)
        val logoBase64 = getAppLogoBase64()
        val wrappedHtml = wrapLegacyHtml(htmlContent, logoBase64, appName)

        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (!uiReady()) return
                binding.btnShare.visibility = View.VISIBLE
                binding.webView.visibility = View.VISIBLE
                binding.statusPage.showContent()
            }
        }

        binding.webView.loadDataWithBaseURL(null, wrappedHtml, "text/html", "UTF-8", null)
    }

    /**
     * 包装老格式 HTML（添加头部和底部）
     */
    private fun wrapLegacyHtml(content: String, logoBase64: String, appName: String): String {
        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <meta name="color-scheme" content="light dark">
            <style>
                :root {
                    --text-primary: #1f2937;
                    --text-secondary: #6b7280;
                }
                @media (prefers-color-scheme: dark) {
                    :root {
                        --text-primary: #e5e7eb;
                        --text-secondary: #9ca3af;
                    }
                }
                body { padding: 1.5rem; }
                .header {
                    display: flex;
                    align-items: center;
                    gap: 12px;
                    padding: 1.5rem;
                }
                .logo img {
                    width: 28px;
                    height: 28px;
                    border-radius: 6px;
                }
                .logo .emoji {
                    font-size: 20px;
                    line-height: 1;
                }
                .period-title {
                    font-size: 18px;
                    font-weight: 600;
                    color: var(--text-primary);
                    margin: 0;
                }
                .footer {
                    text-align: center;
                    padding: 1.5rem;
                    color: var(--text-secondary);
                    font-size: 14px;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <div class="logo">${if (logoBase64.isNotEmpty()) "<img src=\"$logoBase64\" alt=\"Logo\">" else "<span class=\"emoji\">💰</span>"}</div>
                    <p class="period-title">$appName • 财务分析</p>
                </div>
                <div class="content">
                    $content
                </div>
                <div class="footer">
                    由 $appName 生成 • $currentTime
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    /**
     * 构建最终 JSON（合并所有数据）
     */
    private fun buildFinalJson(
        backendAndAiJson: String,
        logoBase64: String,
        appName: String,
        currentTime: String
    ): String {
        return try {
            // 解析后端+AI的数据
            val data = org.json.JSONObject(backendAndAiJson)

            // 添加固定标签
            data.put("reportTitle", "财务全景透视报告")
            data.put("healthScoreLabel", "AI 财务健康分")
            data.put("incomeLabel", "总收入 (含工资/理财)")
            data.put("expenseLabel", "本月总支出")
            data.put("outlierLabel", "独秀指数 (Outlier)")
            data.put("consumeTitle", "🧩 消费结构分析")
            data.put("radarTitle", "🚀 结构画像雷达")
            data.put("riskTitle", "<span>⚠️</span> 异常风险")
            data.put("behaviorTitle", "🔍 消费画像与行为规律")
            data.put("conclusionTitle", "<span>⚖️</span> 综合结论与健康等级")
            data.put("actionTitle", "<span>✅</span> 行动清单")
            data.put("executionTitle", "✅ 执行优先级")
            data.put("recordQualityTitle", "🧭 记录质量提升")

            // 添加 logo 和时间
            data.put("logoBase64", logoBase64)
            data.put("pageHeaderTitle", "$appName • 财务分析")
            data.put("pageFooter", "由 $appName 生成 • $currentTime")
            data.toString()
        } catch (e: Exception) {
            Logger.e("解析 JSON 失败，数据格式可能有误", e)
            "{}" // 返回空对象
        }
    }

    /**
     * 分享为图片
     */
    private fun shareAsImage() {
        val loading = LoadingUtils(requireActivity())

        launch {
            loading.show(getString(R.string.analysis_generating_image))

            try {
                val imageFile = createImageFile()
                val success = captureWebViewToFile(binding.webView, imageFile)

                if (success) {
                    shareImageFile(imageFile)
                } else {
                    ToastUtils.error(getString(R.string.analysis_image_error, "保存失败"))
                }

            } catch (e: Exception) {
                Logger.e("生成分享图片失败", e)
                ToastUtils.error(getString(R.string.analysis_image_error, e.message))
            } finally {
                loading.close()
            }
        }
    }

    /**
     * 截取WebView全量内容并保存为PNG文件
     */
    private suspend fun captureWebViewToFile(webView: WebView, outFile: File): Boolean {
        return try {
            val bitmap = withContext(Dispatchers.Main) {
                val display = resources.displayMetrics
                @Suppress("DEPRECATION") val scale = webView.scale
                val contentHeightPx = (webView.contentHeight * scale).toInt()

                val width = when {
                    webView.width > 0 -> webView.width
                    webView.measuredWidth > 0 -> webView.measuredWidth
                    else -> display.widthPixels
                }
                val height = when {
                    contentHeightPx > 0 -> contentHeightPx
                    webView.height > 0 -> webView.height
                    webView.measuredHeight > 0 -> webView.measuredHeight
                    else -> display.heightPixels
                }

                val wSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
                val hSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
                webView.measure(wSpec, hSpec)
                webView.layout(0, 0, width, height)

                val bmp = createBitmap(width, height)
                val canvas = Canvas(bmp)
                canvas.drawColor(Color.WHITE)
                webView.draw(canvas)
                bmp
            }

            val saved = withContext(Dispatchers.IO) {
                try {
                    outFile.outputStream().use { os ->
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, os)
                    }
                    true
                } catch (e: Exception) {
                    Logger.e("保存截屏文件失败", e)
                    false
                } finally {
                    try {
                        if (!bitmap.isRecycled) bitmap.recycle()
                    } catch (_: Throwable) {
                    }
                }
            }
            saved
        } catch (e: Exception) {
            Logger.e("截取WebView异常", e)
            false
        }
    }

    /**
     * 创建图片文件
     */
    private suspend fun createImageFile(): File = withContext(Dispatchers.IO) {
        val aiCacheDir = File(requireContext().cacheDir, "ai")
        if (!aiCacheDir.exists()) {
            aiCacheDir.mkdirs()
        } else {
            aiCacheDir.delete()
            aiCacheDir.mkdirs()
        }
        val fileName = "analysis_${taskId}_${System.currentTimeMillis()}.png"
        File(aiCacheDir, fileName)
    }

    /**
     * 分享图片文件
     */
    private fun shareImageFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                file
            )

            val shareText = taskModel?.let { "我的${it.title}财务分析报告" } ?: "财务分析报告"

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooserIntent =
                Intent.createChooser(intent, getString(R.string.analysis_share_title))

            // 为Intent Chooser中的所有可能的接收应用授予URI权限
            val packageManager = requireContext().packageManager
            val resInfoList = packageManager.queryIntentActivities(intent, 0)

            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                requireContext().grantUriPermission(
                    packageName,
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            startActivity(chooserIntent)

        } catch (e: Exception) {
            Logger.e("分享图片失败", e)
            ToastUtils.error(getString(R.string.analysis_share_failed))
        }
    }

    /**
     * 获取应用logo的base64编码
     */
    private fun getAppLogoBase64(): String {
        return try {
            val drawable = requireContext().getDrawable(R.mipmap.ic_launcher)
            if (drawable != null) {
                val bitmap = if (drawable is android.graphics.drawable.BitmapDrawable) {
                    drawable.bitmap
                } else {
                    val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 48
                    val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 48
                    val bitmap = createBitmap(width, height)
                    val canvas = Canvas(bitmap)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                    bitmap
                }

                val outputStream = ByteArrayOutputStream()
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, outputStream)
                val byteArray = outputStream.toByteArray()
                "data:image/png;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
            } else {
                ""
            }
        } catch (e: Exception) {
            Logger.e("获取应用logo失败", e)
            ""
        }
    }

    /**
     * 显示错误信息
     */
    private fun showError(message: String) {
        if (!uiReady()) return
        binding.statusPage.showError()
    }
} 