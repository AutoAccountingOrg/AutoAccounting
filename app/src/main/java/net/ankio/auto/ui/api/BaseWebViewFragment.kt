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

package net.ankio.auto.ui.api

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.viewbinding.ViewBinding
import net.ankio.auto.R
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.theme.DynamicColors
import net.ankio.auto.ui.theme.toARGBHex
import net.ankio.auto.ui.theme.toHex
import net.ankio.auto.ui.utils.resToColor
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.utils.ThemeUtils

/**
 * 通用的 WebView Fragment 基类
 * 封装了 WebView 的初始化、Material 3 颜色注入、生命周期管理等
 * 不强制要求统一布局，子类需提供 WebView 实例
 */
abstract class BaseWebViewFragment<VB : ViewBinding> : BaseFragment<VB>() {

    protected var isWebViewReady = false
        private set

    /**
     * 子类必须提供布局中的 WebView 实例
     */
    protected abstract fun getWebView(): WebView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        getWebView().apply {
            // 初始隐藏，防止夜间模式白屏闪烁
            visibility = View.INVISIBLE

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                setSupportZoom(true)
                builtInZoomControls = false
                displayZoomControls = false
                useWideViewPort = true
                loadWithOverviewMode = true
            }

            if (PrefManager.debugMode) {
                WebView.setWebContentsDebuggingEnabled(true)
            }

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (!isWebViewReady) {
                        isWebViewReady = true
                        injectMaterialColors()
                        onWebViewReady()
                    }
                }
            }

            loadInitialUrl()?.let { loadUrl(it) }
        }
    }

    /**
     * 获取初始加载的 URL
     */
    protected abstract fun loadInitialUrl(): String?

    /**
     * WebView 加载完成并注入颜色后的回调
     */
    protected open fun onWebViewReady() {}

    /**
     * 注入 Material 3 颜色变量到 WebView
     */
    private fun injectMaterialColors() {
        val isDarkMode = ThemeUtils.isDark
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
                root.style.setProperty('--color-success', '${
            R.color.success.resToColor().toHex()
        }');
                root.style.setProperty('--color-success-bg', '${
            R.color.success_bg.resToColor().toARGBHex()
        }');
            })();
        """.trimIndent()

        getWebView().evaluateJavascript(js) {
            // 颜色注入并渲染后显示 WebView
            getWebView().visibility = View.VISIBLE
        }
        Logger.d("[BaseWebView] Material 3 颜色已注入")
    }

    override fun onDestroyView() {
        try {
            getWebView().apply {
                stopLoading()
                loadDataWithBaseURL(null, "", "text/html", "utf-8", null)
                clearHistory()
                removeAllViews()
                destroy()
            }
        } catch (e: Exception) {
            Logger.e("WebView 销毁异常", e)
        }
        super.onDestroyView()
    }
}
