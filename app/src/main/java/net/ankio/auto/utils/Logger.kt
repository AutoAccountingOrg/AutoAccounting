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
import net.ankio.auto.BuildConfig

/**
 * 日志工具类，包含调用日志的类和行号信息，以及异常的堆栈跟踪。
 */
object Logger {

    private fun createLogMessage(message: String): String {
        val stackTraceElement = Throwable().stackTrace[2] // 获取调用日志方法的堆栈跟踪元素

        return StringBuilder().apply {
            append("┌───────────────────────────────────────────────────────────────\n")
            append("│ Logger For 自动记账                                            \n")
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


    private fun printLog(type: Int,tag: String,message: String){
        if(!BuildConfig.DEBUG)return
        message.lines().forEach {
            when(type){
                Log.VERBOSE->Log.v(tag, it)
                Log.DEBUG->Log.d(tag, it)
                Log.INFO->Log.i(tag, it)
                Log.WARN->Log.w(tag, it)
                Log.ERROR->Log.e(tag, it)
            }
        }
    }

    fun d(message: String) {
        printLog(Log.DEBUG,getTag(), createLogMessage(message))
    }

    fun e(message: String, throwable: Throwable? = null) {
        val messageInfo = StringBuilder()
        messageInfo.append(message).append("\n")

        if (throwable != null) {
            messageInfo.append("───────────────────────────────────────────────────────────────\n")
            messageInfo.append(throwable.javaClass.name).append(": ").append(throwable.message).append("\n")
            // 循环遍历异常堆栈信息，添加到messageInfo
            throwable.stackTrace.forEach {
                messageInfo.append("  ").append("at ").append(it.className).append(".")
                    .append(it.methodName).append("(")
                    .append(it.fileName).append(":")
                    .append(it.lineNumber).append(")\n")
            }
        }
        printLog(Log.ERROR,getTag(), createLogMessage(messageInfo.toString()))
    }

    fun i(message: String) {
        printLog(Log.INFO,getTag(), createLogMessage(message))
    }

    fun w(message: String) {
        printLog(Log.WARN,getTag(), createLogMessage(message))
    }

}
