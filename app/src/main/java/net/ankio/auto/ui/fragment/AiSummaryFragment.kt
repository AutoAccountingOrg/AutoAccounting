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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import net.ankio.auto.R
import com.google.gson.Gson
import net.ankio.auto.ai.SummaryTool
import net.ankio.auto.databinding.FragmentAiSummaryBinding
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.dialog.PeriodSelectorDialog
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.utils.PrefManager
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.graphics.createBitmap
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume

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

    private var currentSummary: String? = null
    private var currentPeriodData: PeriodSelectorDialog.PeriodData? = null

    companion object {
        private const val ARG_PERIOD_DATA = "period_data"
        private val gson = Gson()

        /**
         * 创建带周期数据的Fragment实例
         */
        fun newInstance(periodData: PeriodSelectorDialog.PeriodData?): AiSummaryFragment {
            val fragment = AiSummaryFragment()
            val args = Bundle()
            if (periodData != null) {
                args.putString(ARG_PERIOD_DATA, gson.toJson(periodData))
            }
            fragment.arguments = args
            return fragment
        }
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
        currentPeriodData = if (periodDataJson != null) {
            try {
                gson.fromJson(periodDataJson, PeriodSelectorDialog.PeriodData::class.java)
            } catch (e: Exception) {
                Logger.e("解析周期数据失败", e)
                null
            }
        } else {
            null
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
            try {
                WebView.enableSlowWholeDocumentDraw()
            } catch (_: Throwable) {
                // 低版本或ROM不支持时忽略
            }

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                setSupportZoom(false)
            }

            // 白底，避免透明背景导致保存时看起来“空白”
            setBackgroundColor(Color.WHITE)

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // 页面加载完成后显示分享按钮
                    binding.btnShare.visibility =
                        if (currentSummary != null) View.VISIBLE else View.GONE
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
    private fun loadSummary() {
        val loading = LoadingUtils(requireActivity())

        lifecycleScope.launch {
            loading.show(getString(R.string.ai_summary_generating))

            try {
                val summary = withContext(Dispatchers.IO) {
                    if (currentPeriodData != null) {
                        // 使用自定义周期生成分析
                        SummaryTool.generateCustomPeriodSummary(
                            currentPeriodData!!.startTime,
                            currentPeriodData!!.endTime,
                            currentPeriodData!!.displayName
                        )
                    } else {
                        // 使用当前月度分析作为默认值
                        val calendar = Calendar.getInstance()
                        val currentYear = calendar.get(Calendar.YEAR)
                        val currentMonth = calendar.get(Calendar.MONTH) + 1
                        SummaryTool.generateMonthlySummary(currentYear, currentMonth)
                    }
                }

                loading.close()

                if (summary != null) {
                    displaySummary(summary)
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
     * 重新生成分析
     */
    private fun regenerateSummary() {
        loadSummary()
    }

    /**
     * 显示分析结果
     */
    private fun displaySummary(summary: String) {
        currentSummary = summary

        // 将Markdown转换为HTML并显示
        val htmlContent = convertToHtml(summary)
        binding.webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)

        // 显示操作按钮
        binding.layoutActions.visibility = View.VISIBLE
        binding.statusPage.showContent()
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
        val periodName = currentPeriodData?.displayName ?: "当前月份"
        val logoBase64 = getAppLogoBase64()

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
               
                .header {
                    text-align: center;
                    padding-bottom: 20px;
                    border-bottom: 2px solid #e9ecef;
                    margin-bottom: 24px;
                }
                .logo {
                    font-size: 32px;
                    margin: 0 auto 8px;
                    text-align: center;
                }
                .app-title {
                    font-size: 20px;
                    font-weight: 600;
                    color: #2c3e50;
                    margin: 8px 0 4px;
                }
               
                .footer {
                    text-align: center;
                    padding-top: 20px;
                    border-top: 1px solid #e9ecef;
                    margin-top: 24px;
                    color: #6c757d;
                    font-size: 14px;
                }
               
            </style>
        </head>
        <body>
            <div class="container">
                <!-- 顶部Logo和标题 -->
                <div class="header">
                    <div class="logo">
                        ${if (logoBase64.isNotEmpty()) "<img src=\"$logoBase64\" alt=\"Logo\" style=\"width: 48px; height: 48px; border-radius: 8px;\">" else "💰"}
                    </div>
                    <h1 class="app-title">$appName</h1>
                    <p class="period-title">$periodName 财务分析报告</p>
                </div>
                
                <!-- AI分析内容 -->
                <div class="content">
                    ${formatContentAsHtml(content)}
                </div>
                
                <!-- 底部信息 -->
                <div class="footer">
                    <p>由 $appName 生成 • ${
            SimpleDateFormat(
                "yyyy-MM-dd HH:mm",
                Locale.getDefault()
            ).format(Date())
        }</p>
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
        if (currentSummary == null) {
            ToastUtils.error(getString(R.string.ai_summary_no_content))
            return
        }

        val loading = LoadingUtils(requireActivity())

        lifecycleScope.launch {
            loading.show(getString(R.string.ai_summary_generating_image))

            try {
                val bitmap = withContext(Dispatchers.IO) {
                    captureWebViewAsBitmap()
                }

                if (bitmap != null) {
                    val imageFile = saveBitmapToFile(bitmap)
                    shareImageFile(imageFile)
                } else {
                    ToastUtils.error(getString(R.string.ai_summary_image_failed))
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
     * 捕获WebView为Bitmap - 完整内容截图
     *
     * 简洁方案：WebView自己处理滚动，直接获取完整内容高度
     */
    private suspend fun captureWebViewAsBitmap(): Bitmap? = withContext(Dispatchers.Main) {
        try {
            val webView = binding.webView

            // 等待内容布局完成（contentHeight > 0），避免早期绘制成白屏
            if (webView.contentHeight == 0) {
                suspendCancellableCoroutine { cont ->
                    webView.post { cont.resume(Unit) }
                }
            }

            val originalWidth = webView.width
            val originalHeight = webView.height
            if (originalWidth <= 0 || originalHeight <= 0) return@withContext null

            // contentHeight 是 CSS 像素，需要乘以当前缩放比例得到实际像素
            // contentHeight 为 CSS px，转换到实际像素：使用屏幕 density 作为近似（避免依赖隐藏 API）
            val density = webView.resources.displayMetrics.density
            var targetHeight = (webView.contentHeight * density).toInt()
            if (targetHeight <= 0) targetHeight = originalHeight
            val targetWidth = originalWidth

            // 临时使用软件层绘制，避免某些机型硬件加速下画布为白
            val oldLayerType = webView.layerType
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)

            // 保存当前滚动位置并重置到顶部，避免截到中间位置
            val oldScrollY = webView.scrollY
            webView.scrollTo(0, 0)

            // 重新按完整内容高度进行 measure/layout，确保 draw() 能绘制整页
            webView.measure(
                View.MeasureSpec.makeMeasureSpec(targetWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(targetHeight, View.MeasureSpec.EXACTLY)
            )
            webView.layout(
                webView.left,
                webView.top,
                webView.left + targetWidth,
                webView.top + targetHeight
            )

            // 创建目标位图并绘制（先铺白底）
            val bitmap = createBitmap(targetWidth, targetHeight)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            webView.draw(canvas)

            Logger.d("WebView整页截图尺寸: ${targetWidth}x${targetHeight}, density=${density}")

            // 恢复视图状态，避免影响UI
            webView.measure(
                View.MeasureSpec.makeMeasureSpec(originalWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(originalHeight, View.MeasureSpec.EXACTLY)
            )
            webView.layout(
                webView.left,
                webView.top,
                webView.left + originalWidth,
                webView.top + originalHeight
            )
            webView.scrollTo(0, oldScrollY)
            webView.setLayerType(oldLayerType, null)

            bitmap
        } catch (e: Exception) {
            Logger.e("捕获WebView失败", e)
            null
        }
    }

    /**
     * 保存Bitmap到文件
     */
    private suspend fun saveBitmapToFile(bitmap: Bitmap): File = withContext(Dispatchers.IO) {
        // 创建AI缓存目录
        val aiCacheDir = File(requireContext().cacheDir, "ai")
        if (!aiCacheDir.exists()) {
            aiCacheDir.mkdirs()
        }

        // 生成文件名，使用周期信息或时间戳
        val periodName =
            currentPeriodData?.displayName?.replace("[^a-zA-Z0-9\\u4e00-\\u9fa5]".toRegex(), "_")
                ?: "default"
        val fileName = "ai_summary_${periodName}_${System.currentTimeMillis()}.png"
        val file = File(aiCacheDir, fileName)

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

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
            val shareText = if (currentPeriodData != null) {
                "我的${currentPeriodData!!.displayName}财务分析报告"
            } else {
                val calendar = Calendar.getInstance()
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH) + 1
                getString(R.string.ai_summary_share_text, year, month)
            }

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
