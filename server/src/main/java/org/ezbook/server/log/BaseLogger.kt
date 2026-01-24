/*
 * Copyright (C) 2026 ankio(ankio@ankio.net)
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

package org.ezbook.server.log

import android.util.Log
import kotlinx.coroutines.runBlocking
import org.ezbook.server.constant.LogLevel
import org.ezbook.server.db.model.LogModel

abstract class BaseLogger : ILogger {
    private val defaultClassName: String = this.javaClass.name

    private val callerData: StackTraceElement?
        get() = Throwable().stackTrace.firstOrNull { element ->
            // 过滤掉 BaseLogger 类及其子类的方法调用
            val className = element.className
            className != BaseLogger::class.java.name &&
                    className != ILogger.defaultImplName &&
                    !ILogger::class.java.isAssignableFrom(Class.forName(className))
        }


    var extendLoggerPrinter: ((String) -> Unit)? = null

    override fun d(msg: String, tr: Throwable?) =
        if (isDebugMode()) log(LogLevel.DEBUG, msg, tr) else Unit

    override fun i(msg: String, tr: Throwable?) = log(LogLevel.INFO, msg, tr)

    override fun w(msg: String, tr: Throwable?) = log(LogLevel.WARN, msg, tr)

    override fun e(msg: String, tr: Throwable?) = log(LogLevel.ERROR, msg, tr)

    fun log(priority: LogLevel, msg: String, tr: Throwable? = null) {
        var className = defaultClassName
        var file = ""
        var line = -1

        if (priority == LogLevel.ERROR || priority == LogLevel.FATAL || isDebugMode()) {
            val caller = callerData
            className = caller?.className?.substringAfterLast('.')?.substringBefore('$') ?: "ServerLog"
            file = caller?.fileName ?: ""
            line = caller?.lineNumber ?: -1
        }

        logcatFormater(priority, file, line, msg, tr)?.let {
            // 如果设置了扩展打印器（如 XposedBridge.log），则只使用扩展打印器
            // 避免重复输出到系统日志
            if (extendLoggerPrinter != null) {
                extendLoggerPrinter?.invoke(it)
            } else {
                Log.println(priority.toAndroidLevel(), className, it)
            }
        }


        logModelFormater(priority, className, file, line, msg, tr)?.let {
            onLogModel(it)
        }
    }

    protected open fun logcatFormater(
        priority: LogLevel, file: String, line: Int, msg: String, tr: Throwable? = null
    ): String? = null

    protected open fun logModelFormater(
        priority: LogLevel, className: String, file: String, line: Int, msg: String, tr: Throwable? = null
    ): LogModel? = null

    /**
     * 如果重写 logModelFormater，则必须实现此方法。
     * 用于处理格式化后的 LogModel。
     */
    protected open fun onLogModel(model: LogModel) {
        throw NotImplementedError()
    }
}