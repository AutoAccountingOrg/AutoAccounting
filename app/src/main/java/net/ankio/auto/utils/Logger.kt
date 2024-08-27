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
import net.ankio.auto.App
import net.ankio.auto.BuildConfig
import org.ezbook.server.constant.LogLevel
import org.ezbook.server.db.model.LogModel

/**
 * 日志工具类，包含调用日志的类和行号信息，以及异常的堆栈跟踪。
 */
object Logger {
    /**
     * 获取调用日志的类和行号信息
     */
    private fun getLogHeader(): Triple<String, String, String> {
        val stackTraceElement = Throwable().stackTrace[3] // 获取调用日志方法的堆栈跟踪元素
        return Triple(
            Thread.currentThread().name,
            stackTraceElement.fileName,
            stackTraceElement.lineNumber.toString(),
        )
    }

    /**
     * 获取调用日志的类名
     */
    private fun getTag(): String {
        return Throwable().stackTrace[2].className.substringBefore('$').substringAfterLast(".")
    }

    /**
     * 打印日志
     *
     * @param type 日志类型
     * @param tag 日志标签
     * @param message 日志内容
     */
    private  fun printLog(
        type: Int,
        tag: String,
        message: String,
    ) {
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


        App.launch {
            LogModel.add(
                when (type) {
                    Log.DEBUG -> LogLevel.DEBUG
                    Log.INFO -> LogLevel.INFO
                    Log.WARN -> LogLevel.WARN
                    Log.ERROR -> LogLevel.ERROR
                    else -> LogLevel.DEBUG
                },
                BuildConfig.APPLICATION_ID,
                "$file($line)",
                message
            )
        }
    }

    /**
     * 调试日志
     */
    fun d(message: String) {
        if (!App.debug)return
        printLog(Log.DEBUG, getTag(), message)
    }

    /**
     * 错误日志
     */
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

    /**
     * 常规日志
     */
    fun i(message: String) {
        printLog(Log.INFO, getTag(), message)
    }

    /**
     * 警告日志
     */
    fun w(message: String) {
        printLog(Log.WARN, getTag(), message)
    }
}
