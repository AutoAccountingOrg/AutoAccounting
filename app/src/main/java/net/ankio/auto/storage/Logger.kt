package net.ankio.auto.storage

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.App
import net.ankio.auto.BuildConfig
import net.ankio.auto.autoApp
import net.ankio.auto.http.api.LogAPI
import net.ankio.auto.utils.DateUtils
import net.ankio.auto.utils.Throttle
import org.ezbook.server.constant.LogLevel
import org.ezbook.server.db.model.LogModel
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * 日志工具类，支持写入本地文件、自动清理（仅保留当天）、分页读取。
 */
object Logger {
    private const val LOG_DIR = "logs"
    private const val LOG_FILE_PREFIX = "app_log_"
    private const val LOG_FILE_SUFFIX = ".txt"
    private const val CLEANUP_INTERVAL_MS = 1L * 60L * 60L * 1000L // 1小时限流
    private const val FORMAT = "yyyy-MM-dd HH:mm:ss.SSS"

    private val executor = Executors.newSingleThreadExecutor()

    @Volatile
    private var cleaner = Throttle.asFunction(CLEANUP_INTERVAL_MS) {
        executor.execute { cleanupOldLogs(autoApp) }
    }

    /** 获取今天的日志文件 */
    private fun getLogFile(context: Context): File {
        val dir = File(context.cacheDir, LOG_DIR)
        if (!dir.exists()) dir.mkdirs()
        val date = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        return File(dir, "$LOG_FILE_PREFIX$date$LOG_FILE_SUFFIX")
    }

    /** 只保留今天的日志文件，其他全部删除 */
    private fun cleanupOldLogs(context: Context) {
        val dir = File(context.cacheDir, LOG_DIR)
        if (!dir.exists()) return
        val todayFileName = getLogFile(context).name
        dir.listFiles()?.forEach { file ->
            if (file.name != todayFileName) {
                file.delete()
            }
        }
    }


    /** 写入日志到文件（自动触发清理） */
    private fun writeToFile(entry: String) {
        val context = autoApp
        executor.execute {
            try {
                cleaner()
                val file = getLogFile(context)
                BufferedWriter(FileWriter(file, true)).use { writer ->
                    writer.append(entry).append('\n')
                }
            } catch (e: Exception) {
                Log.e("Logger", "Write log file error", e)
            }
        }
    }

    /** 格式化日志 */
    fun formatLog(logModel: LogModel): String {
        val timestamp = DateUtils.stampToDate(logModel.time, FORMAT)
        val level = when (logModel.level) {
            LogLevel.DEBUG -> "D"
            LogLevel.INFO -> "I"
            LogLevel.WARN -> "W"
            LogLevel.ERROR -> "E"
            else -> "D"
        }
        return "[$timestamp] $level/${logModel.app}/${logModel.location}: ${logModel.message}"
    }

    /** 输出到Logcat并写入文件 */
    private fun printLog(type: Int, tag: String, message: String) {
        // Logcat 输出
        when (type) {
            Log.VERBOSE -> Log.v(tag, message)
            Log.DEBUG -> Log.d(tag, message)
            Log.INFO -> Log.i(tag, message)
            Log.WARN -> Log.w(tag, message)
            Log.ERROR -> Log.e(tag, message)
        }
        // 构建 entry
        val header = Throwable().stackTrace.getOrNull(3)?.let {
            "(${it.fileName}:${it.lineNumber})"
        } ?: ""


        // 不再写入日志，使用API发送
        val logLevel = when (type) {
            Log.DEBUG -> LogLevel.DEBUG
            Log.INFO -> LogLevel.INFO
            Log.WARN -> LogLevel.WARN
            Log.ERROR -> LogLevel.ERROR
            else -> LogLevel.DEBUG
        }


        App.launch {
            LogAPI.add(logLevel, BuildConfig.APPLICATION_ID, tag + header, message)
        }
    }

    private fun getTag(): String {
        return Throwable().stackTrace.getOrNull(2)
            ?.className
            ?.substringAfterLast('.')
            ?.substringBefore('$')
            ?: ""
    }

    fun d(message: String) {
        if (!BuildConfig.DEBUG) return; printLog(Log.DEBUG, getTag(), message)
    }

    fun i(message: String) {
        printLog(Log.INFO, getTag(), message)
    }

    fun w(message: String) {
        printLog(Log.WARN, getTag(), message)
    }

    fun e(message: String, throwable: Throwable? = null) {
        val builder = StringBuilder().apply {
            append(message)
            throwable?.let { append("\n").append(Log.getStackTraceString(it)) }
        }
        printLog(Log.ERROR, getTag(), builder.toString())
    }

    /**
     * 分页读取今天的日志并转换为 LogModel 列表
     */
    suspend fun readLogsAsModelsPaged(
        page: Int,
        pageSize: Int
    ): List<LogModel> = withContext(Dispatchers.IO) { LogAPI.list(page, pageSize) }

    suspend fun packageLogs(context: Context): File = withContext(Dispatchers.IO) {
        val file = getLogFile(context)
        if (file.exists()) {
            file.delete()
        }
        //拉取所有日志
        var index = 0
        do {
            val logs = LogAPI.list(index, 2000)
            logs.forEach {
                writeToFile(formatLog(it))
            }
            index++
        } while (logs.isNotEmpty() || index >= 20)

        file
    }

}
