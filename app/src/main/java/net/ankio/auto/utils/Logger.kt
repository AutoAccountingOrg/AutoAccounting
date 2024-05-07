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

/**
 * 日志工具类，包含调用日志的类和行号信息，以及异常的堆栈跟踪。
 */
object Logger {
    private var level = Log.VERBOSE

    private var debug = false

    fun init() {
        debug = AppUtils.getDebug()

        // 如果是调试模式，日志级别就是VERBOSE，否则就是ERROR
        level = SpUtils.getInt("log_level", if (debug) Log.VERBOSE else Log.ERROR)
    }

    fun setLevel(level: Int) {
        SpUtils.putInt("log_level", level)
    }

    private fun createLogMessage(message: String): ArrayList<String> {
        val stackTraceElement = Throwable().stackTrace[3] // 获取调用日志方法的堆栈跟踪元素

        val header =
            StringBuilder().apply {
                append("[ ")
                append(Thread.currentThread().name)
                append(" ] ")
                append("(")
                append(stackTraceElement.fileName)
                append(":")
                append(stackTraceElement.lineNumber)
                append(") ")
            }.toString() // 将StringBuilder转换为String
        val list = ArrayList<String>()
        message.lines().forEach {
            val segmentSize = 3 * 1024
            val length = it.length
            if (length <= segmentSize) { // 长度小于等于限制直接打印
                list.add("$header $it")
            } else {
                var msg = it
                while (msg.length > segmentSize) { // 循环分段打印日志
                    val logContent = msg.substring(0, segmentSize)
                    msg = msg.replace(logContent, "")
                    list.add("$header $logContent")
                }
                list.add("$header $msg")
            }
        }
        return list
    }

    private fun getTag(): String {
        return Throwable().stackTrace[2].className.substringBefore('$').substringAfterLast(".")
    }

    private fun printLog(
        type: Int,
        tag: String,
        message: String,
    ) {
        if (message.contains(AutoAccountingServiceUtils.getUrl("/log"))) {
            return
        }
        if (type < level) return

        fun log(it: String) {
            when (type) {
                Log.VERBOSE -> Log.v(tag, it)
                Log.DEBUG -> Log.d(tag, it)
                Log.INFO -> Log.i(tag, it)
                Log.WARN -> Log.w(tag, it)
                Log.ERROR -> Log.e(tag, it)
            }
        }

        val logMessage = createLogMessage(message)
        logMessage.forEach {
            log(it)
        }
        // 一些发起服务请求的不记录到日志文件里面来
        if (logMessage.any { it.contains("127.0.0.1:52045") })return

        AppUtils.getScope().launch {
            AutoAccountingServiceUtils.log(logMessage.joinToString("\n"), AppUtils.getApplication())
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
