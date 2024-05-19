/*
 * Copyright (C) 2024 ankio(ankio@ankio.net)
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

package net.ankio.auto.utils

import android.util.Log
import kotlinx.coroutines.launch
import net.ankio.auto.utils.server.model.LogModel

/**
 * 日志工具类，包含调用日志的类和行号信息，以及异常的堆栈跟踪。
 */
object Logger {
    private fun getLogHeader(): Triple<String, String, String> {
        val stackTraceElement = Throwable().stackTrace[3] // 获取调用日志方法的堆栈跟踪元素
        return Triple(
            Thread.currentThread().name,
            stackTraceElement.fileName,
            stackTraceElement.lineNumber.toString(),
        )
    }

    private fun getTag(): String {
        return Throwable().stackTrace[2].className.substringBefore('$').substringAfterLast(".")
    }

    private fun printLog(
        type: Int,
        tag: String,
        message: String,
    ) {
        if (message.contains("log/put") || message.contains("log/get")) {
            return
        }

        fun log(it: String) {
            when (type) {
                Log.VERBOSE -> Log.v(tag, it)
                Log.DEBUG -> Log.d(tag, it)
                Log.INFO -> Log.i(tag, it)
                Log.WARN -> Log.w(tag, it)
                Log.ERROR -> Log.e(tag, it)
            }
        }
        val (thread, file, line) = getLogHeader()

        val header =
            StringBuilder().apply {
                append("[ ")
                append(thread)
                append(" ] ")
                append("(")
                append(file)
                append(":")
                append(line)
                append(") ")
            }.toString()
        val list = ArrayList<String>()
        message.lines().forEach {
            val segmentSize = 3 * 1024
            val length = it.length
            if (length <= segmentSize) { // 长度小于等于限制直接打印
                list.add(it)
            } else {
                var msg = it
                while (msg.length > segmentSize) { // 循环分段打印日志
                    val logContent = msg.substring(0, segmentSize)
                    msg = msg.replace(logContent, "")
                    list.add(logContent)
                }
                list.add(msg)
            }
        }
        list.forEach {
            log(header + it)
        }
        // 一些发起服务请求的不记录到日志文件里面来
        if (list.any { it.contains("127.0.0.1:52045") })return

        AppUtils.getScope().launch {
            LogModel.put(
                LogModel().apply {
                    date = DateUtils.getTime(System.currentTimeMillis())
                    app = AppUtils.getApplication().packageName
                    hook = 0
                    this.thread = thread
                    this.line = "$file:$line"
                    level =
                        when (type) {
                            Log.DEBUG -> LogModel.LOG_LEVEL_DEBUG
                            Log.INFO -> LogModel.LOG_LEVEL_INFO
                            Log.WARN -> LogModel.LOG_LEVEL_WARN
                            Log.ERROR -> LogModel.LOG_LEVEL_ERROR
                            else -> LogModel.LOG_LEVEL_DEBUG
                        }
                    log = message
                },
            )
        }
    }

    fun d(message: String) {
        printLog(Log.DEBUG, getTag(), message)
    }

    fun e(
        message: String,
        throwable: Throwable? = null,
    ) {
        val messageInfo = StringBuilder()
        messageInfo.append(message).append("\n")
        if (throwable != null) {
            messageInfo.append(throwable.javaClass.name).append(": ").append(throwable.message)
                .append("\n")
            // 循环遍历异常堆栈信息，添加到messageInfo
            throwable.stackTrace.forEach {
                messageInfo.append("  ").append("at ").append(it.className).append(".")
                    .append(it.methodName).append("(")
                    .append(it.fileName).append(":")
                    .append(it.lineNumber).append(")\n")
            }
        }
        printLog(Log.ERROR, getTag(), messageInfo.toString())
    }

    fun i(message: String) {
        printLog(Log.INFO, getTag(), message)
    }

    fun w(message: String) {
        printLog(Log.WARN, getTag(), message)
    }
}
