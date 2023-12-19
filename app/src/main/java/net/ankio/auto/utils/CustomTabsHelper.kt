package net.ankio.auto.utils


import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.ankio.auto.R
import rikka.html.text.HtmlCompat


/**
 * Created by fytho on 2017/12/15.
 */
object CustomTabsHelper {
    private var sOnCreateIntentBuilderListener: OnCreateIntentBuilderListener? = null
    fun setOnCreateIntentBuilderListener(onCreateIntentBuilderListener: OnCreateIntentBuilderListener?) {
        sOnCreateIntentBuilderListener = onCreateIntentBuilderListener
    }

    private fun createBuilder(): CustomTabsIntent.Builder {
        val builder: CustomTabsIntent.Builder = CustomTabsIntent.Builder()
        builder.setShowTitle(true)
        return builder
    }

    private fun launchHelp(context: Context, uri: Uri): Boolean {
        val builder: CustomTabsIntent.Builder = createBuilder()
        if (sOnCreateIntentBuilderListener != null) {
            sOnCreateIntentBuilderListener!!.onCreateHelpIntentBuilder(context, builder)
        }
        val uriBuilder = uri.buildUpon()
        if (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_YES > 0) {
            uriBuilder.appendQueryParameter("night", "1")
        }
        return launchUrl(context, builder.build(), uriBuilder.build())
    }

    fun launchUrl(context: Context, uri: Uri): Boolean {
        return launchUrl(context, createBuilder().build(), uri)
    }

    private fun launchUrl(context: Context, customTabsIntent: CustomTabsIntent, uri: Uri): Boolean {
        return try {
            customTabsIntent.launchUrl(context, uri)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    fun launchUrlOrCopy(context: Context, url: String?) {
        val uri = Uri.parse(url)
        if (!launchHelp(context, uri)) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = uri
            try {
                context.startActivity(intent)
            } catch (tr: Throwable) {
                try {
                    ClipboardUtils.put(context, url)
                    MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.dialog_cannot_open_browser_title)
                        .setMessage(
                            HtmlCompat.fromHtml(
                                context.getString(
                                    R.string.toast_copied_to_clipboard_with_text,
                                    url
                                )
                            )
                        )
                        .setPositiveButton(R.string.ok, null)
                        .show()
                } catch (ignored: Throwable) {
                }
            }
        }
    }

    interface OnCreateIntentBuilderListener {
        fun onCreateHelpIntentBuilder(context: Context?, builder: CustomTabsIntent.Builder?)
    }
}