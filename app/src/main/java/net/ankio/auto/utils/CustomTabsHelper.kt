package net.ankio.auto.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import net.ankio.auto.storage.Logger
import androidx.core.net.toUri
import net.ankio.auto.autoApp

/**
 * CustomTabs 浏览器启动工具
 * 重新设计：明确上下文要求，统一错误处理
 */
object CustomTabsHelper {

    /**
     * 启动 CustomTabs 浏览器打开链接
     * @param context 必须是Activity上下文或包含FLAG_ACTIVITY_NEW_TASK的上下文
     * @param uri 要打开的链接
     * @return 成功返回 true，失败返回 false
     */
    fun launchUrl(context: Context, uri: Uri): Boolean {
        return runCatching {
            val builder = CustomTabsIntent.Builder().setShowTitle(true)

            // 统一处理夜间模式
            val finalUri = if (ThemeUtils.isDark) {
                uri.buildUpon().appendQueryParameter("night", "1").build()
            } else {
                uri
            }

            builder.build().launchUrl(context, finalUri)
            true
        }.getOrElse { e ->
            Logger.e("CustomTabs启动失败: ${e.message}", e)
            false
        }
    }

    /**
     * 兼容旧API - 使用Application上下文但添加必要标志
     * @param uri 要打开的链接
     * @return 成功返回 true，失败返回 false
     */
    fun launchUrl(uri: Uri): Boolean {
        return launchUrlWithFallback(autoApp, uri)
    }

    /**
     * 带回退机制的URL启动 - 这是正确的实现方式
     * @param context 上下文（任何类型都可以）
     * @param uri 要打开的链接
     * @return 成功返回 true，失败返回 false
     */
    private fun launchUrlWithFallback(context: Context, uri: Uri): Boolean {
        // 统一处理夜间模式
        val finalUri = if (ThemeUtils.isDark) {
            uri.buildUpon().appendQueryParameter("night", "1").build()
        } else {
            uri
        }

        // 1. 尝试普通Intent（带NEW_TASK标志）
        return runCatching {
            val intent = Intent(Intent.ACTION_VIEW, finalUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.getOrElse { e ->
            Logger.e("浏览器启动失败: ${e.message}", e)
            false
        }
    }

    /**
     * 尝试打开链接，失败则复制到剪贴板
     * 统一的错误处理和回退机制
     */
    fun launchUrlOrCopy(url: String?) {
        if (url.isNullOrBlank()) return

        val uri = url.toUri()

        // 1. 尝试启动浏览器（已包含回退逻辑）
        if (launchUrlWithFallback(autoApp, uri)) return

        // 2. 最后复制到剪贴板
        runCatching {
            SystemUtils.copyToClipboard(url)
        }.onFailure { e ->
            Logger.e("复制到剪贴板失败: ${e.message}", e)
        }
    }
}
