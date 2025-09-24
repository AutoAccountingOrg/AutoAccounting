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
import net.ankio.auto.utils.ThemeUtils
import org.ezbook.server.db.model.AnalysisTaskModel
import java.io.File

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
                    val htmlContent = convertToHtml(task.resultHtml!!, task.title)
                    displayHtml(htmlContent)
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
     * 显示HTML内容
     */
    private fun displayHtml(htmlContent: String) {
        if (!uiReady()) return
        binding.webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        binding.statusPage.showContent()
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
     * 将分析内容转换为HTML格式
     */
    private fun convertToHtml(content: String, title: String): String {
        val isDarkMode = ThemeUtils.isDark
        val colorScheme = if (isDarkMode) "dark" else "light"
        val textPrimary = if (isDarkMode) "#e5e7eb" else "#1f2937"
        val textSecondary = if (isDarkMode) "#9ca3af" else "#6b7280"

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <meta name="color-scheme" content="$colorScheme">
            <style>
                :root {
                    --text-primary: $textPrimary;
                    --text-secondary: $textSecondary;
                }
                body {
                    padding: 1.5rem;
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                    line-height: 1.6;
                    color: var(--text-primary);
                    background-color: ${if (isDarkMode) "#111827" else "#ffffff"};
                }
                .header {
                    display: flex;
                    align-items: center;
                    gap: 12px;
                    padding: 1.5rem;
                    border-bottom: 1px solid var(--text-secondary);
                    margin-bottom: 2rem;
                }
                .logo {
                    font-size: 20px;
                }
                .period-title {
                    font-size: 18px;
                    font-weight: 600;
                    color: var(--text-primary);
                    margin: 0;
                }
                .content {
                    max-width: 800px;
                    margin: 0 auto;
                }
                .footer {
                    text-align: center;
                    padding: 1.5rem;
                    color: var(--text-secondary);
                    font-size: 14px;
                    margin-top: 2rem;
                    border-top: 1px solid var(--text-secondary);
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <div class="logo">💰</div>
                    <p class="period-title">自动记账 • $title</p>
                </div>
                
                <div class="content">
                    $content
                </div>
                
                <div class="footer">
                    由自动记账生成 • ${
            java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm",
                java.util.Locale.getDefault()
            ).format(java.util.Date())
        }
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    /**
     * 显示错误信息
     */
    private fun showError(message: String) {
        if (!uiReady()) return
        binding.statusPage.showError()
    }
} 