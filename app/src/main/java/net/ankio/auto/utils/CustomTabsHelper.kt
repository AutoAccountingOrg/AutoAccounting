package net.ankio.auto.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import net.ankio.auto.App
import net.ankio.auto.R
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.dialog.BottomSheetDialogBuilder
import net.ankio.auto.utils.SystemUtils


object CustomTabsHelper {


    private fun createBuilder(): CustomTabsIntent.Builder {
        val builder: CustomTabsIntent.Builder = CustomTabsIntent.Builder()
        builder.setShowTitle(true)
        return builder
    }

    private fun launchHelp(
        context: Context,
        uri: Uri,
    ): Boolean {
        val builder: CustomTabsIntent.Builder = createBuilder()
        val uriBuilder = uri.buildUpon()
        if (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_YES > 0) {
            uriBuilder.appendQueryParameter("night", "1")
        }
        return launchUrl(context, builder.build(), uriBuilder.build())
    }

    fun launchUrl(
        context: Context,
        uri: Uri,
    ): Boolean {
        return launchUrl(context, createBuilder().build(), uri)
    }

    private fun launchUrl(
        context: Context,
        customTabsIntent: CustomTabsIntent,
        uri: Uri,
    ): Boolean {
        return try {
            customTabsIntent.launchUrl(context, uri)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    fun launchUrlOrCopy(
        context: Context,
        url: String?,
    ) {
        val uri = Uri.parse(url)
        if (!launchHelp(context, uri)) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = uri
            try {
                context.startActivity(intent)
            } catch (tr: Throwable) {
                Logger.e("Failed to open default browser", tr)
                try {
                    SystemUtils.copyToClipboard(url)
                    /*BottomSheetDialogBuilder(context)
                        .setTitleInt(R.string.dialog_cannot_open_browser_title)
                        .setMessage(
                            context.getString(
                                R.string.toast_copied_to_clipboard_with_text,
                                url,
                            ),
                        )
                        .setPositiveButton(R.string.ok, null)
                        .show()*/
                } catch (ignored: Throwable) {
                }
            }
        }
    }


}
