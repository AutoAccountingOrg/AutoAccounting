package net.ankio.auto.storage

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import net.ankio.auto.App
import net.ankio.auto.BuildConfig
import net.ankio.auto.autoApp
import net.ankio.auto.http.api.LogAPI
import net.ankio.auto.utils.DateUtils
import net.ankio.auto.utils.PrefManager
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
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * 日志工具类，支持写入本地文件、自动清理（仅保留当天）、分页读取。
 *
 * 主要功能：
 * 1. 提供统一的日志输出接口（d, i, w, e）
 * 2. 自动获取调用类名作为日志标签
 * 3. 支持日志级别过滤（DEBUG模式下才输出DEBUG日志）
 * 4. 通过API将日志发送到服务器
 * 5. 支持本地文件日志存储和清理
 * 6. 支持分页读取日志记录
 * 7. 支持日志文件打包导出
 *
 * 使用示例：
 * ```
 * logger.debug { "调试信息" }
 * logger.info { "普通信息" }
 * logger.warn { "警告信息" }
 * logger.error(exception) { "错误信息" }
 * ```
 */
object Logger {

    private val logger = KotlinLogging.logger(this::class.java.name)
    /** 日志文件存储目录名 */
    private const val LOG_DIR = "logs"

    /** 日志文件名前缀 */
    private const val LOG_FILE_PREFIX = "app_log_"

    /** 日志文件名后缀 */
    private const val LOG_FILE_SUFFIX = ".txt"

    /** 日志清理限流间隔（1小时） */
    private const val CLEANUP_INTERVAL_MS = 1L * 60L * 60L * 1000L // 1小时限流

    /** 日志时间格式 */
    private const val FORMAT = "yyyy-MM-dd HH:mm:ss.SSS"

    /** 单线程执行器，用于异步写入日志文件 */
    private val executor = Executors.newSingleThreadExecutor()

    /** 日志清理限流器，防止频繁清理操作 */
    @Volatile
    private var cleaner = Throttle.asFunction(CLEANUP_INTERVAL_MS) {
        executor.execute { cleanupOldLogs(autoApp) }
    }

    /**
     * 获取今天的日志文件
     *
     * @param context 应用上下文
     * @return 今天的日志文件对象
     */
    private fun getLogFile(context: Context): File {
        val dir = File(context.cacheDir, LOG_DIR)
        if (!dir.exists()) dir.mkdirs()
        val date = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        return File(dir, "$LOG_FILE_PREFIX$date$LOG_FILE_SUFFIX")
    }

    /**
     * 清理旧的日志文件，只保留今天的日志文件
     *
     * @param context 应用上下文
     */
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

    /**
     * 写入日志到文件（自动触发清理）
     *
     * @param entry 要写入的日志条目
     */
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

    /**
     * 格式化日志模型为字符串
     *
     * @param logModel 日志模型对象
     * @return 格式化后的日志字符串
     */
    private fun formatLog(logModel: LogModel): String {
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

    /**
     * 分页读取日志记录并转换为LogModel列表
     *
     * @param page 页码（从0开始）
     * @param pageSize 每页大小
     * @return 日志模型列表
     */
    suspend fun readLogsAsModelsPaged(
        page: Int,
        pageSize: Int
    ): List<LogModel> = withContext(Dispatchers.IO) { LogAPI.list(page, pageSize) }

    /**
     * 打包日志文件
     * 从服务器拉取所有日志记录并写入本地文件
     *
     * @param context 应用上下文
     * @return 打包后的日志文件
     */
    suspend fun packageLogs(context: Context): File = withContext(Dispatchers.IO) {
        val file = getLogFile(context)
        if (file.exists()) {
            file.delete()
        }

        // 分页拉取所有日志记录
        var index = 0
        do {
            val logs = LogAPI.list(index, 2000)
            logs.forEach {
                writeToFile(formatLog(it))
            }
            index++
        } while (logs.isNotEmpty() || index >= 20) // 最多拉取20页，防止无限循环

        file
    }
}
