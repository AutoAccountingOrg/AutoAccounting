package net.ankio.auto.storage

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.actor
import net.ankio.auto.BuildConfig
import net.ankio.auto.autoApp
import net.ankio.auto.http.api.LogAPI
import net.ankio.auto.utils.DateUtils
import org.ezbook.server.constant.LogLevel
import org.ezbook.server.db.model.LogModel
import org.ezbook.server.tools.BaseLogger
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

/**
 * 日志工具类，支持发送到服务端和按需导出到本地文件。
 *
 * 主要功能：
 * 1. 提供统一的日志输出接口（d, i, w, e）
 * 2. 自动获取调用类名作为日志标签
 * 3. 支持日志级别过滤（DEBUG模式下才输出DEBUG日志）
 * 4. 通过API将日志发送到服务器
 * 5. 支持导出时生成本地日志文件
 * 6. 支持分页读取日志记录
 *
 * 使用示例：
 * ```
 * Logger.d("调试信息")
 * Logger.i("普通信息")
 * Logger.w("警告信息")
 * Logger.e("错误信息", exception)
 * ```
 */
object Logger : BaseLogger() {
    /** 日志文件存储目录名 */
    private const val LOG_DIR = "logs"

    /** 日志时间格式 */
    private const val FORMAT = "yyyy-MM-dd HH:mm:ss.SSS"

    /**
     * 删除日志文件夹并重新创建
     *
     * @param context 应用上下文
     */
    private fun cleanLogDir(context: Context) {
        val dir = File(context.cacheDir, LOG_DIR)
        if (dir.exists()) {
            dir.deleteRecursively()
        }
        dir.mkdirs()
    }

    private val scope = CoroutineScope(SupervisorJob())

    @OptIn(ObsoleteCoroutinesApi::class)
    private val actor = scope.actor<LogModel>(Dispatchers.IO) {
        for (log in channel) {
            try {
                LogAPI.add(log)
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e("Logger", "Send log to server error", e)
                }
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

    override fun logcatFormater(
        priority: LogLevel, file: String, line: Int, msg: String, tr: Throwable?
    ): String {
        var prefix = "[ 自动记账 ]"
        if (line != -1) prefix = "$prefix($file:$line) "

        return prefix + msg
    }

    override fun logModelFormater(
        priority: LogLevel, className: String, file: String, line: Int, msg: String, tr: Throwable?
    ): LogModel = LogModel(
        level = priority,
        app = BuildConfig.APPLICATION_ID,
        // location 统一为 "类名(File.kt:行号)"
        location = className + if (line != -1) "($file:$line)" else "",
        message = "$msg\n${tr?.stackTrace?.joinToString("\n") ?: ""}".trimEnd()
    )

    override fun onLogModel(model: LogModel) {
        scope.launch { actor.send(model) }
    }


    /**
     * 生成系统信息头部
     *
     * @param context 应用上下文
     * @return 系统信息字符串
     */
    private suspend fun generateSystemInfo(context: Context): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val appVersion = "${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})"
        val androidVersion = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        val deviceInfo = "${Build.BRAND} ${Build.MODEL}"

        // 获取规则版本
        val ruleVersion = runCatching {
            net.ankio.auto.utils.PrefManager.ruleVersion.ifEmpty { "未知" }
        }.getOrElse { "获取失败" }

        return buildString {
            appendLine("=".repeat(60))
            appendLine("自动记账日志文件")
            appendLine("=".repeat(60))
            appendLine("导出时间: $timestamp")
            appendLine("应用版本: $appVersion")
            appendLine("规则版本: $ruleVersion")
            appendLine("安卓版本: $androidVersion")
            appendLine("设备信息: $deviceInfo")
            appendLine("=".repeat(60))
            appendLine()
        }
    }

    /**
     * 打包日志文件
     * 删除日志文件夹并重建，然后从服务器拉取所有日志记录并写入本地文件
     *
     * @param context 应用上下文
     * @return 打包后的日志文件
     */
    suspend fun packageLogs(context: Context): File = withContext(Dispatchers.IO) {
        // 清理日志目录并重建
        cleanLogDir(context)
        val dir = File(context.cacheDir, LOG_DIR)
        if (!dir.exists()) dir.mkdirs()
        val date = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
        val file = File(dir, "AutoAccounting_$date.log")

        // 写入系统信息头部
        BufferedWriter(FileWriter(file, false)).use { writer ->
            writer.append(generateSystemInfo(context))
        }

        // 分页拉取所有日志记录并追加到文件
        var index = 0
        do {
            val logs = LogAPI.list(index, 1000)
            if (logs.isNotEmpty()) {
                BufferedWriter(FileWriter(file, true)).use { writer ->
                    logs.forEach { entry ->
                        writer.append(formatLog(entry)).append('\n')
                    }
                }
            }
            index++
        } while (logs.isNotEmpty() && index < 20) // 最多拉取20页，防止无限循环

        file
    }
}
