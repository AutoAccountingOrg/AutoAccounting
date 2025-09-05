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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ezbook.server.Server
import org.ezbook.server.constant.LogLevel
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.LogModel

object ServerLog {
    /**
     * 输出日志到Logcat并发送到服务器
     *
     * @param type 日志级别（Log.VERBOSE, Log.DEBUG, Log.INFO, Log.WARN, Log.ERROR）
     * @param tag 日志标签
     * @param message 日志消息
     */
    private suspend fun printLog(type: Int, tag: String, message: String) =
        withContext(Dispatchers.IO) {
            val suffix = "[ 自动记账 ] "
            // Logcat 输出
            when (type) {
                Log.VERBOSE -> Log.v(tag, suffix + message)
                Log.DEBUG -> Log.d(tag, suffix + message)
                Log.INFO -> Log.i(tag, suffix + message)
                Log.WARN -> Log.w(tag, suffix + message)
                Log.ERROR -> Log.e(tag, suffix + message)
                else -> Log.i(tag, suffix + message)
            }

            // 构建调用位置信息
            val header = Throwable().stackTrace.getOrNull(3)?.let {
                "(${it.fileName}:${it.lineNumber})"
            } ?: ""

            // 将日志级别转换为服务器端格式
            val logLevel = when (type) {
                Log.DEBUG -> LogLevel.DEBUG
                Log.INFO -> LogLevel.INFO
                Log.WARN -> LogLevel.WARN
                Log.ERROR -> LogLevel.ERROR
                else -> LogLevel.DEBUG
            }

            Db.get().logDao().insert(LogModel().apply {
                level = logLevel
                app = "AutoServer"
                location = tag
                this.message = header + message
            })

        }

    /**
     * 获取调用者的类名作为日志标签
     *
     * @return 调用者的类名（去掉包名和内部类标识）
     */
    private fun getTag(): String {
        return Throwable().stackTrace.getOrNull(2)
            ?.className
            ?.substringAfterLast('.')
            ?.substringBefore('$')
            ?: ""
    }

    /**
     * 输出DEBUG级别日志
     * 注意：仅在DEBUG模式下输出
     *
     * @param message 日志消息
     */
    fun d(message: String) {
        val tag = getTag()
        Server.withIO {
            if (SettingUtils.debugMode()) printLog(Log.DEBUG, tag, message)
        }
    }

    /**
     * 输出INFO级别日志
     *
     * @param message 日志消息
     */
    fun i(message: String) {
        val tag = getTag()
        Server.withIO {
            printLog(Log.INFO, tag, message)
        }
    }

    /**
     * 输出WARN级别日志
     *
     * @param message 日志消息
     */
    fun w(message: String) {
        val tag = getTag()
        Server.withIO {
            printLog(Log.WARN, tag, message)
        }
    }

    /**
     * 输出ERROR级别日志
     *
     * @param message 日志消息
     * @param throwable 异常对象（可选）
     */
    fun e(message: String, throwable: Throwable? = null) {
        val tag = getTag()
        Server.withIO {
            val builder = StringBuilder().apply {
                append(message)
                throwable?.let { append("\n").append(Log.getStackTraceString(it)) }
            }
            printLog(Log.ERROR, tag, builder.toString())
        }
    }
}