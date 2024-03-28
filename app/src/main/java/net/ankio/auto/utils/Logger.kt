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

/**
 * 日志工具类，包含调用日志的类和行号信息，以及异常的堆栈跟踪。
 */
object Logger {


    private var level = Log.VERBOSE

    private var debug = false

    fun init() {
        debug = AppUtils.getDebug()

        //如果是调试模式，日志级别就是VERBOSE，否则就是ERROR
        level = SpUtils.getInt("log_level", if (debug) Log.VERBOSE else Log.ERROR)

    }

    fun setLevel(level: Int) {
        SpUtils.putInt("log_level", level)
    }

    private fun createLogMessage(message: String): String {
        val stackTraceElement = Throwable().stackTrace[4] // 获取调用日志方法的堆栈跟踪元素

        return StringBuilder().apply {
            append("┌───────────────────────────────────────────────────────────────\n")
            append("│ Thread: ")
            append(Thread.currentThread().name)
            append("\n")
            append("│ ")
            append(stackTraceElement.className.substringAfterLast('.'))
            append(".")
            append(stackTraceElement.methodName)
            append("(")
            append(stackTraceElement.fileName)
            append(":")
            append(stackTraceElement.lineNumber)
            append(") \n")
            append("├───────────────────────────────────────────────────────────────\n")
            message.lines().forEach {
                append("│ $it\n")
            }
            append("└───────────────────────────────────────────────────────────────")
        }.toString() // 将StringBuilder转换为String
    }

    private fun getTag(): String {
        return Throwable().stackTrace[2].className.substringAfterLast('.')
    }

    private fun printLog(type: Int, tag: String, message: String) {
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
        logMessage.lines().forEach {

            var msg = it

            val segmentSize = 3 * 1024
            val length = msg.length
            if (length <= segmentSize) {// 长度小于等于限制直接打印
                log(msg)
            } else {
                while (msg.length > segmentSize) {// 循环分段打印日志
                    val logContent = msg.substring(0, segmentSize);
                    msg = msg.replace(logContent, "");
                    log(logContent);
                }
                log(msg)// 打印剩余日志
            }


        }
        AutoAccountingServiceUtils.log(logMessage, AppUtils.getApplication())
    }

    fun d(message: String) {
        printLog(Log.DEBUG, getTag(), message)
    }

    fun e(message: String, throwable: Throwable? = null) {
        val messageInfo = StringBuilder()
        messageInfo.append(message).append("\n")
        if (throwable != null) {
            messageInfo.append("───────────────────────────────────────────────────────────────\n")
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