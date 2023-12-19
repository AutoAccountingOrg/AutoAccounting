package net.ankio.auto.utils
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

class ClipboardUtils {
    companion object {
        fun put(context: Context, text: String?) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("text", text)
            clipboard.setPrimaryClip(clip)
        }

        fun get(context: Context): String? {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = clipboard.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                return clipData.getItemAt(0).text.toString()
            }
            return null
        }
    }
}
