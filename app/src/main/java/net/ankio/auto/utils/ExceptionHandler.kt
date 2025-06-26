package net.ankio.auto.utils

import android.content.Context
import android.content.Intent
import android.os.Build
import com.tencent.bugly.crashreport.CrashReport
import net.ankio.auto.BuildConfig
import net.ankio.auto.autoApp
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.activity.ErrorActivity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

class ExceptionHandler private constructor(private val context: Context) :
    Thread.UncaughtExceptionHandler {

    private var mDefaultHandler: Thread.UncaughtExceptionHandler? = null

    init {
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    companion object {

        // 初始化，在Application的onCreate里调用即可
        fun init(context: Context) {
            ExceptionHandler(context)
        }


        // 日志拼接，支持递归 cause
        private fun buildExceptionLog(e: Throwable, label: String = "主线程异常"): String {
            val sb = StringBuilder()
            sb.append("【$label】\n")
            sb.append("App版本: ${BuildConfig.VERSION_NAME}\n")
            sb.append("版本号: ${BuildConfig.VERSION_CODE}\n")
            sb.append("设备型号: ${Build.MODEL}\n")
            sb.append("系统版本: ${Build.VERSION.RELEASE}\n")
            sb.append("品牌: ${Build.BRAND}\n")
            sb.append(
                "异常时间: ${
                    SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        Locale.getDefault()
                    ).format(Date())
                }\n\n"
            )
            collectStackTrace(e, sb)
            return sb.toString()
        }

        private fun collectStackTrace(e: Throwable, sb: StringBuilder) {
            sb.append(e.toString()).append("\n")
            e.stackTrace.forEach { sb.append("\tat $it\n") }
            e.cause?.let {
                sb.append("Caused by: ")
                collectStackTrace(it, sb)
            }
        }

        private fun saveLogToLocal(ctx: Context, msg: String, tag: String = "crash") {
            try {
                val logDir = File(ctx.cacheDir, "crash")
                if (!logDir.exists()) logDir.mkdirs()
                val file = File(logDir, "${tag}_${System.currentTimeMillis()}.log")
                FileOutputStream(file).use { it.write(msg.toByteArray()) }
                Logger.d(msg)
            } catch (ex: Exception) {
                // ignore
            }
        }
    }

    override fun uncaughtException(
        t: Thread,
        e: Throwable,
    ) {
        val msg = buildExceptionLog(e, "主线程未捕获异常")
        Logger.e("Handler UncaughtException", e)
        saveLogToLocal(context, msg)
        // Bugly 上报
        CrashReport.postCatchedException(e)

        // 跳转错误界面
        try {
            val intent = Intent(context, ErrorActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra("msg", msg)
            intent.putExtra("isErrorActivity", true)
            context.startActivity(intent)
        } catch (ex: Exception) {
            Logger.e("Failed to start ErrorActivity", ex)
        }

        exitProcess(0)
    }
}


