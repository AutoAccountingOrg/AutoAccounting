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

package net.ankio.auto.xposed.core.logger

import android.util.Log
import net.ankio.auto.http.api.LogAPI
import net.ankio.auto.xposed.core.utils.AppRuntime
import net.ankio.auto.xposed.core.utils.CoroutineUtils
import org.ezbook.server.constant.LogLevel

/**
 * Xposed日志工具
 *
 */
object Logger {

    /**
     * 一次性获取调用方 TAG 与 header，例如 (File.kt:123)
     * 避免重复创建 Throwable 与二次解析栈
     */
    private fun getCallerInfo(): Pair<String, String> {
        val frames = Throwable().stackTrace
        var index = 0
        while (index < frames.size && frames[index].className == Logger::class.java.name) {
            index++
        }
        val f = frames.getOrNull(index) ?: frames.getOrNull(3)

        val tag = f?.className
            ?.substringAfterLast('.')
            ?.substringBefore('$')
            ?: "Logger"
        val header = f?.let { "(${it.fileName}:${it.lineNumber})" } ?: ""
        return tag to header
    }

    private val TAG = "Logger"

    /**
     * 是否存在 Xposed 框架（通过反射检测），避免在非 Xposed 进程中触发类加载错误。
     */
    private val xposedBridgeClass by lazy {
        runCatching { Class.forName("de.robv.android.xposed.XposedBridge") }.getOrNull()
    }

    /** 反射调用 XposedBridge.log(String) */
    private fun xposedLogString(message: String) {
        val clazz = xposedBridgeClass ?: return
        runCatching { clazz.getMethod("log", String::class.java).invoke(null, message) }
    }

    /** 反射调用 XposedBridge.log(Throwable) */
    private fun xposedLogThrowable(e: Throwable) {
        val clazz = xposedBridgeClass ?: return
        runCatching { clazz.getMethod("log", Throwable::class.java).invoke(null, e) }
    }

    /**
     * 统一输出到 Xposed（若存在）与 Android Log。
     * 仅在调试模式下输出到本地，以保持与既有行为一致。
     */
    private fun callXposedOrLogger(
        app: String,
        msg: String,
        priority: Int,
        tag: String,
        header: String
    ) {
        if (!AppRuntime.debug) return
        val formatted = "[ 自动记账 ][ $app ]$header $msg"
        xposedLogString(formatted)
        Log.println(priority, tag, formatted)
    }
    /**
     * 打印日志
     */
    fun log(app: String, msg: String) {

        val (tag, header) = getCallerInfo()
        callXposedOrLogger(app, msg, Log.DEBUG, tag, header)
        //写入自动记账日志
        CoroutineUtils.withIO {
            LogAPI.add(LogLevel.INFO, app, tag + header, msg)
        }

    }

    /**
     * 只在调试模式输出日志
     */
    fun logD(app: String, msg: String) {
        if (AppRuntime.debug) log(app, msg)
    }

    /**
     * 打印错误日志
     */
    fun logE(app: String, e: Throwable) {
        val msg = e.message ?: ""
        val (tag, header) = getCallerInfo()
        xposedLogThrowable(e)
        val formatted = "[ 自动记账 ][ $app ]$header $msg"
        Log.e(tag, formatted)
        CoroutineUtils.withIO {
            val log = StringBuilder()
            log.append(e.message).append("\n")
            e.stackTrace.forEach {
                log.append(it.toString()).append("\n")
            }
            LogAPI.add(LogLevel.ERROR, app, tag + header, log.toString())
        }
    }
}