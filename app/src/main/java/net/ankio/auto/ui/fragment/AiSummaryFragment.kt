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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.ai.SummaryTool
import net.ankio.auto.databinding.FragmentAiSummaryBinding
import net.ankio.auto.storage.CacheManager
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.dialog.PeriodSelectorDialog
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.utils.PrefManager
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * AI账单分析页面
 *
 * 功能概览：
 * 1. 显示AI生成的财务分析报告
 * 2. 支持自定义时间周期（从外部传入）
 * 3. 支持重新生成分析
 * 4. 支持生成分享图片
 */
class AiSummaryFragment : BaseFragment<FragmentAiSummaryBinding>() {

    private lateinit var currentPeriodData: PeriodSelectorDialog.PeriodData

    companion object {
        private const val ARG_PERIOD_DATA = "period_data"
        private val gson = Gson()
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

        // 检查AI月度总结功能是否启用
        if (!PrefManager.aiMonthlySummary) {
            findNavController().popBackStack()
            return
        }

        // 获取传入的周期数据
        val periodDataJson = arguments?.getString(ARG_PERIOD_DATA)
        if (periodDataJson != null) {
            try {
                currentPeriodData =
                    gson.fromJson(periodDataJson, PeriodSelectorDialog.PeriodData::class.java)
            } catch (e: Exception) {
                Logger.e("解析周期数据失败", e)
                findNavController().popBackStack()
            }
        } else {
            findNavController().popBackStack()
        }

        setupUI()
        loadCurrentSummary()
    }

    /**
     * 设置UI组件
     */
    private fun setupUI() {
        // 设置标题
        binding.topAppBar.setTitle(R.string.ai_summary_title)

        // 设置WebView
        setupWebView()

        // 设置点击事件
        binding.btnRegenerate.setOnClickListener { regenerateSummary() }
        binding.btnShare.setOnClickListener { shareAsImage() }

        // 设置返回按钮
        binding.topAppBar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    /**
     * 设置WebView
     */
    private fun setupWebView() {
        binding.webView.apply {
            // 启用整页绘制（仅影响打印/绘图路径），避免只绘制可见区域


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
                    binding.btnShare.visibility = View.VISIBLE

                    binding.webView.visibility = View.VISIBLE
                }
            }
        }
    }


    /**
     * 加载当前的分析
     */
    private fun loadCurrentSummary() {
        loadSummary()
    }

    /**
     * 加载AI分析
     */
    private fun loadSummary(forceRefresh: Boolean = false) {
        val loading = LoadingUtils(requireActivity())

        lifecycleScope.launch {
            loading.show(getString(R.string.ai_summary_generating))

            try {
                // 1) 基于周期参数构建缓存键，避免重复生成
                val cacheKey = buildCacheKey(
                    currentPeriodData.startTime,
                    currentPeriodData.endTime,
                    currentPeriodData.displayName
                )

                Logger.d("缓存键：$cacheKey")

                // 2) 可选：强制刷新时跳过缓存并清理旧缓存；否则尝试从缓存读取
                if (forceRefresh) {
                    withContext(Dispatchers.IO) { CacheManager.remove(cacheKey) }
                } else {
                    val cachedHtml =
                        withContext(Dispatchers.IO) { CacheManager.getString(cacheKey) }
                    if (cachedHtml != null) {
                        loading.close()
                        displayHtml(cachedHtml)
                        return@launch
                    }
                }

                // 3) 缓存未命中，生成摘要并转换为HTML
                val summary = withContext(Dispatchers.IO) {
                    SummaryTool.generateCustomPeriodSummary(
                        currentPeriodData.startTime,
                        currentPeriodData.endTime,
                        currentPeriodData.displayName
                    )
                }

                loading.close()

                if (summary != null) {
                    // 转换为HTML并展示
                    val htmlContent = convertToHtml(summary)
                    displayHtml(htmlContent)
                    // 4) 写入缓存，TTL=1小时
                    withContext(Dispatchers.IO) {
                        try {
                            CacheManager.putString(cacheKey, htmlContent, 60 * 60 * 1000L)
                        } catch (e: Exception) {
                            Logger.e("缓存AI分析HTML失败", e)
                        }
                    }
                } else {
                    showError(getString(R.string.ai_summary_generate_failed))
                }

            } catch (e: Exception) {
                loading.close()
                Logger.e("AI分析生成失败", e)
                showError(getString(R.string.ai_summary_generate_error, e.message))
            }
        }
    }

    /**
     * 构建缓存键：基于起止时间与显示名，确保相同周期生成的内容复用。
     */
    private fun buildCacheKey(start: Long, end: Long, name: String): String {
        // 使用“本地时区日桶”归一化：将时间加上该时刻的时区偏移再按天取整，
        // 避免按UTC除法导致的跨时区/DST（夏令时）边界误差。
        val day = 24 * 60 * 60 * 1000L
        val tz = java.util.TimeZone.getDefault()
        val startDayBucket = (start + tz.getOffset(start)) / day
        val endDayBucket = (end + tz.getOffset(end)) / day
        return "ai:summary:html:${startDayBucket}:${endDayBucket}:${name}"
    }

    /**
     * 直接展示HTML（用于缓存命中或生成后展示）。
     */
    private fun displayHtml(htmlContent: String) {
        binding.webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        binding.layoutActions.visibility = View.VISIBLE
        binding.statusPage.showContent()
    }

    /**
     * 重新生成分析
     */
    private fun regenerateSummary() {
        loadSummary(true)
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
                    ${formatContentAsHtml(content)}
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
     * 格式化内容为HTML
     */
    private fun formatContentAsHtml(content: String): String {
        return content
    }

    /**
     * 分享为图片
     */
    private fun shareAsImage() {


        val loading = LoadingUtils(requireActivity())

        lifecycleScope.launch {
            loading.show(getString(R.string.ai_summary_generating_image))

            try {
                val imageFile = bitmapFile()

                val success = captureWebViewToFile(binding.webView, imageFile)
                if (success) {
                    shareImageFile(imageFile)
                } else {
                    ToastUtils.error(getString(R.string.ai_summary_image_error, "保存失败"))
                }

            } catch (e: Exception) {
                Logger.e("生成分享图片失败", e)
                ToastUtils.error(getString(R.string.ai_summary_image_error, e.message))
            } finally {
                loading.close()
            }
        }
    }


    /**
     * 截取 WebView 全量内容并保存为 PNG 文件。
     * 返回：保存是否成功。
     * 要点：主线程绘制，IO 线程写文件；白底避免透明。
     */
    private suspend fun captureWebViewToFile(
        webView: WebView,
        outFile: File
    ): Boolean {
        return try {
            val bitmap = withContext(Dispatchers.Main) {
                val display = resources.displayMetrics
                val scale = webView.scale
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
            Logger.e("截取 WebView 异常", e)
            false
        }
    }



    /**
     * 保存Bitmap到文件
     */
    private suspend fun bitmapFile(): File = withContext(Dispatchers.IO) {
        // 创建AI缓存目录
        val aiCacheDir = File(requireContext().cacheDir, "ai")
        if (!aiCacheDir.exists()) {
            aiCacheDir.mkdirs()
        } else {
            aiCacheDir.delete()
            aiCacheDir.mkdirs()
        }
        val fileName = "ai_summary_${System.currentTimeMillis()}.png"
        val file = File(aiCacheDir, fileName)

        file
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

            // 生成分享文本
            val shareText = "我的${currentPeriodData!!.displayName}财务分析报告"

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // 为Intent Chooser中的所有可能的接收应用授予URI权限
            val chooserIntent =
                Intent.createChooser(intent, getString(R.string.ai_summary_share_title))

            // 获取所有可以处理该Intent的应用，并为它们授予URI权限
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
            ToastUtils.error(getString(R.string.ai_summary_share_failed))
        }
    }


    /**
     * 显示错误信息
     */
    private fun showError(message: String) {

        binding.layoutActions.visibility = View.GONE
    }


}
