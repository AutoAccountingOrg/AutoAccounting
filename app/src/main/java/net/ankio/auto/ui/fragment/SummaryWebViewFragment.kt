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

package net.ankio.auto.ui.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.MenuItem
import androidx.navigation.fragment.findNavController
import android.content.res.Configuration
import androidx.core.graphics.toColor
import androidx.core.graphics.toColorLong
import com.google.gson.Gson
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentSummaryWebviewBinding
import net.ankio.auto.http.api.BillAPI
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.dialog.DateTimePickerDialog
import net.ankio.auto.ui.theme.DynamicColors
import net.ankio.auto.ui.theme.toARGBHex
import net.ankio.auto.ui.theme.toHex
import net.ankio.auto.ui.utils.resToColor
import net.ankio.auto.utils.CoroutineUtils.withIO
import net.ankio.auto.utils.CoroutineUtils.withMain
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.utils.ThemeUtils
import net.ankio.auto.utils.PeriodSelector
import java.util.Calendar

/**
 * 消费分析报告页面 - WebView版本
 * 使用summary.html展示详细的消费分析数据
 */
class SummaryWebViewFragment : BaseFragment<FragmentSummaryWebviewBinding>() {

    // 当前筛选时间范围（毫秒）
    private var startTime: Long = 0L
    private var endTime: Long = 0L

    private val gson = Gson()

    // WebView是否已加载完成
    private var isWebViewReady = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initDefaultPeriod()
        setupUI()
        setupWebView()

        // 初始化副标题
        binding.topAppBar.subtitle = getPeriodName(startTime, endTime)
    }

    /**
     * 初始化UI
     */
    private fun setupUI() {
        // 返回按钮
        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        // 菜单点击事件
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            onMenuItemClick(menuItem)
        }

        // 下拉刷新
        binding.swipeRefreshLayout.setOnRefreshListener {
            if (isWebViewReady) {
                loadSummary()
            } else {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    /**
     * 菜单点击事件处理
     */
    private fun onMenuItemClick(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_filter_period -> {
                PeriodSelector.show(
                    requireContext(),
                    binding.topAppBar.findViewById(R.id.action_filter_period),
                    startTime,
                    endTime
                ) { start, end, label ->
                    setTimeRange(start to end, label)
                }
                true
            }

            else -> false
        }
    }

    /**
     * 配置WebView
     */
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webView.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true

                // 支持缩放
                setSupportZoom(true)
                builtInZoomControls = false
                displayZoomControls = false

                // 自适应屏幕
                useWideViewPort = true
                loadWithOverviewMode = true
            }

            // 如果debug模式开着，开启webview调试
            if (PrefManager.debugMode) {
                WebView.setWebContentsDebuggingEnabled(true)
            }

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // 页面加载完成后才标记为就绪，并加载真实数据
                    if (!isWebViewReady) {
                        isWebViewReady = true
                        // 注入摘要页所需的 Material 3 颜色变量
                        injectSummaryColors()
                        loadSummary()
                    }
                }
            }

            // 加载本地HTML文件
            loadUrl("file:///android_asset/summary/summary.html")
        }
    }

    /**
     * 注入摘要页使用的 Material 3 颜色变量
     */
    private fun injectSummaryColors() {
        // 判断当前是否为暗色模式
        val isDarkMode = ThemeUtils.isDark

        // 使用 Material 3 的基础色系作为其他变量来源
        val js = """
            (function() {
                window.__isDarkMode = $isDarkMode;
                const root = document.documentElement;
                root.style.setProperty('--color-bg', '${DynamicColors.SurfaceContainerLowest.toHex()}');
                root.style.setProperty('--color-card', '${DynamicColors.Surface.toHex()}');
                root.style.setProperty('--color-card-hover', '${DynamicColors.SurfaceContainerLow.toHex()}');
                root.style.setProperty('--color-text', '${DynamicColors.OnSurface.toHex()}');
                root.style.setProperty('--color-text-light', '${DynamicColors.OnSurfaceVariant.toHex()}');
                
                root.style.setProperty('--color-text-muted', '${DynamicColors.Outline.toHex()}');
                root.style.setProperty('--color-primary', '${DynamicColors.Primary.toHex()}');
                root.style.setProperty('--color-primary-light', '${DynamicColors.PrimaryContainer.toHex()}');
                root.style.setProperty('--color-secondary', '${DynamicColors.Secondary.toHex()}');
                root.style.setProperty('--color-tertiary', '${DynamicColors.Tertiary.toHex()}');
                root.style.setProperty('--color-on-primary', '${DynamicColors.OnPrimary.toHex()}');
                root.style.setProperty('--color-on-secondary', '${DynamicColors.OnSecondary.toHex()}');
                root.style.setProperty('--color-warning', '${DynamicColors.Tertiary.toHex()}');
                root.style.setProperty('--color-warning-bg', '${DynamicColors.TertiaryContainer.toHex()}');
                root.style.setProperty('--color-border', '${DynamicColors.Outline.toHex()}');
                root.style.setProperty('--color-surface-variant', '${DynamicColors.SurfaceVariant.toHex()}');
                root.style.setProperty('--color-outline', '${DynamicColors.Outline.toHex()}');
                root.style.setProperty('--color-gradient-start', '${DynamicColors.PrimaryContainer.toHex()}');
                root.style.setProperty('--color-gradient-end', '${DynamicColors.Primary.toHex()}');
                root.style.setProperty('--color-danger', '${R.color.danger.resToColor().toHex()}');
                root.style.setProperty('--color-danger-bg', '${
            R.color.danger_bg.resToColor().toARGBHex()
        }');
             
                
            })();
        """.trimIndent()


        // 注入颜色变量到 WebView
        binding.webView.evaluateJavascript(js, null)
        Logger.d("[WebView] Summary 颜色已注入")
    }

    /**
     * 使用"本月"作为默认筛选周期
     */
    private fun initDefaultPeriod() {
        val periodData =
            PeriodSelector.calculatePeriodData(requireContext(), PeriodSelector.Period.THIS_MONTH)
        startTime = periodData.startTime
        endTime = periodData.endTime
    }

    /**
     * 加载消费分析数据
     */
    private fun loadSummary() {
        launch {
            try {
                val periodName = getPeriodName(startTime, endTime)

                // 调用服务端API获取数据
                val summaryData = withIO {
                    BillAPI.summary(startTime, endTime, periodName)
                }

                if (summaryData == null) {
                    Logger.e("获取消费分析数据失败")
                    withMain {
                        binding.swipeRefreshLayout.isRefreshing = false
                    }
                    return@launch
                }

                // 直接注入Map数据到WebView
                withMain {
                    injectDataToWebView(summaryData)
                    binding.swipeRefreshLayout.isRefreshing = false
                }
            } catch (e: Exception) {
                Logger.e("加载消费分析数据失败", e)
                withMain {
                    binding.swipeRefreshLayout.isRefreshing = false
                }
            }
        }
    }

    /**
     * 获取周期名称
     */
    private fun getPeriodName(start: Long, end: Long): String {
        val startCal = Calendar.getInstance().apply { timeInMillis = start }
        val endCal = Calendar.getInstance().apply { timeInMillis = end }

        val year = startCal.get(Calendar.YEAR)
        val month = startCal.get(Calendar.MONTH) + 1

        // 判断是否为完整月份
        val isFullMonth = startCal.get(Calendar.DAY_OF_MONTH) == 1 &&
                endCal.get(Calendar.DAY_OF_MONTH) == endCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        return if (isFullMonth) {
            "${year}年${month}月"
        } else {
            PeriodSelector.formatRangeLabel(start, end)
        }
    }

    /**
     * 将数据注入到WebView
     */
    @SuppressLint("SetJavaScriptEnabled")
    private fun injectDataToWebView(summaryData: Map<String, Any?>) {
        // 将Map序列化为JSON对象字符串，直接传给setJson
        val jsonData = gson.toJson(summaryData)
        val javascript = "window.setJson($jsonData)"

        binding.webView.evaluateJavascript(javascript) { result ->
            Logger.i("数据注入完成: $result")
        }
    }

    /**
     * 设置时间范围并刷新数据
     */
    private fun setTimeRange(range: Pair<Long, Long>, label: String? = null) {
        startTime = range.first
        endTime = range.second

        // 更新副标题显示当前周期
        binding.topAppBar.subtitle = label ?: getPeriodName(startTime, endTime)

        // 只有WebView准备好后才能加载数据
        if (isWebViewReady) {
            loadSummary()
        }
    }

    override fun onDestroyView() {
        // 在 super.onDestroyView() 之前清理 WebView，此时 binding 仍然有效
        binding.webView.apply {
            loadDataWithBaseURL(null, "", "text/html", "utf-8", null)
            clearHistory()
            removeAllViews()
            destroy()
        }
        super.onDestroyView()
    }
}
