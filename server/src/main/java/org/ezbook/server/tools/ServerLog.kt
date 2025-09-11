/*
 * Copyright (C) 2025 ankio(ankio@ankio.net)
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

package org.ezbook.server.tools

import android.util.Log
import org.ezbook.server.Server
import org.ezbook.server.constant.LogLevel
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.LogModel

object ServerLog {
    /**
     * 输出日志到Logcat并发送到服务器
     *
     * @param type 日志级别（Log.VERBOSE, Log.DEBUG, Log.INFO, Log.WARN, Log.ERROR）
     * @param tag 日志标签（调用方类名）
     * @param header 源位置标头，形如 (File.kt:123)
     * @param message 日志消息
     */
    private suspend fun printLog(type: Int, tag: String, header: String, message: String) {
        val suffix = "[ 自动记账 ]$header"
        val priority = when (type) {
            Log.VERBOSE, Log.DEBUG, Log.INFO, Log.WARN, Log.ERROR -> type
            else -> Log.INFO
        }
        Log.println(priority, tag, suffix + message)

        val logLevel = when (type) {
            Log.DEBUG -> LogLevel.DEBUG
            Log.INFO -> LogLevel.INFO
            Log.WARN -> LogLevel.WARN
            Log.ERROR -> LogLevel.ERROR
            else -> LogLevel.DEBUG
        }

        // 组装日志并直接入库（客户端未批量，这里也不批量）
        val model = LogModel().apply {
            level = logLevel
            app = "AutoServer"
            // location 统一为 "类名(File.kt:行号)"
            location = (if (tag.isNotEmpty()) tag else "ServerLog") + header
            this.message = message
            time = System.currentTimeMillis()
        }
        try {
            Db.get().logDao().insert(model)
        } catch (e: Exception) {
            Log.e("ServerLog", "write db failed: ${e.message}")
        }
    }

    /** 单次栈捕获，返回 (tag, header) */
    private fun getCallerInfo(): Pair<String, String> {
        val frames = Throwable().stackTrace
        // 0:getStackTrace,1:<init>,2:getCallerInfo,3:上层(d/i/w/e),4+:业务
        var index = 0
        while (index < frames.size && frames[index].className == ServerLog::class.java.name) {
            index++
        }
        val candidate = frames.getOrNull(index) ?: frames.getOrNull(3)


        val tag = candidate?.className?.substringAfterLast('.')?.substringBefore('$') ?: "ServerLog"
        val header = candidate?.let { "(${it.fileName}:${it.lineNumber})" } ?: ""
        return tag to header
    }

    /**
     * 输出DEBUG级别日志
     * 注意：仅在DEBUG模式下输出
     *
     * @param message 日志消息
     */
    fun d(message: String) {
        val (tag, header) = getCallerInfo()
        Server.withIO { if (SettingUtils.debugMode()) printLog(Log.DEBUG, tag, header, message) }
    }

    /**
     * 输出INFO级别日志
     *
     * @param message 日志消息
     */
    fun i(message: String) {
        val (tag, header) = getCallerInfo()
        Server.withIO { printLog(Log.INFO, tag, header, message) }
    }

    /**
     * 输出WARN级别日志
     *
     * @param message 日志消息
     */
    fun w(message: String) {
        val (tag, header) = getCallerInfo()
        Server.withIO { printLog(Log.WARN, tag, header, message) }
    }

    /**
     * 输出ERROR级别日志
     *
     * @param message 日志消息
     * @param throwable 异常对象（可选）
     */
    fun e(message: String, throwable: Throwable? = null) {
        val (tag, header) = getCallerInfo()
        Server.withIO {
            val builder = StringBuilder().apply {
                append(message)
                throwable?.let { append("\n").append(Log.getStackTraceString(it)) }
            }
            printLog(Log.ERROR, tag, header, builder.toString())
        }
    }
}