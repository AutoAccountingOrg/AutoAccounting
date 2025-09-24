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
                    val htmlContent = convertToHtml(task.resultHtml!!)
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
     * 获取应用logo的base64编码
     */
    private fun getAppLogoBase64(): String {
        return try {
            // 获取应用logo drawable
            val drawable = requireContext().getDrawable(R.mipmap.ic_launcher)
            if (drawable != null) {
                // 转换为bitmap
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

                // 转换为base64
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, outputStream)
                val byteArray = outputStream.toByteArray()
                "data:image/png;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
            } else {
                "" // 如果获取失败，返回空字符串
            }
        } catch (e: Exception) {
            Logger.e("获取应用logo失败", e)
            "" // 出错时返回空字符串
        }
    }

    /**
     * 将AI生成的内容转换为HTML
     */
    private fun convertToHtml(content: String): String {
        val appName = getString(R.string.app_name)
        val logoBase64 = getAppLogoBase64()

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
          
            <meta name="color-scheme" content="light dark">
            <style>
              
                :root {
                 
                    --text-primary: #1f2937;          /* 浅色主文本（更稳的深灰） */
                    --text-secondary: #6b7280;        /* 浅色次文本 */
                }
                @media (prefers-color-scheme: dark) {
                    :root {
                      
                        --text-primary: #e5e7eb;      /* 深色主文本（近 Gray-200） */
                        --text-secondary: #9ca3af;    /* 深色次文本（Gray-400） */
                    }
                }
                body{
                    padding:1.5rem
                }


                /* 顶部页眉：左侧小 Logo + 周期标题 */
                .header {
                    display: flex;
                    align-items: center;
                    gap: 12px;
                    padding: 1.5rem;
                }
                .logo {
                    display: inline-flex;
                    align-items: center;
                    justify-content: center;
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
                        padding:1.5rem;
                    color: var(--text-secondary);
                    font-size: 14px;
                }
               
            </style>
        </head>
        <body>
            <div class="container">
                <!-- 顶部页眉：左侧小 Logo，不显示应用标题，仅显示周期标题 -->
                <div class="header">
                    <div class="logo">${if (logoBase64.isNotEmpty()) "<img src=\"$logoBase64\" alt=\"Logo\">" else "<span class=\"emoji\">💰</span>"}</div>
                    <p class="period-title">自动记账 • 财务分析</p>
                </div>
                
                <!-- AI分析内容 -->
                <div class="content">
                    ${content}
                </div>
                
                <!-- 底部信息 -->
                <div class="footer">
                    由 $appName 生成 • ${
            SimpleDateFormat(
                "yyyy-MM-dd HH:mm",
                Locale.getDefault()
            ).format(Date())
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