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
import net.ankio.auto.xposed.core.utils.AppRuntime
import net.ankio.auto.xposed.core.utils.ThreadUtils
import org.ezbook.server.constant.LogLevel
import org.ezbook.server.db.model.LogModel
import net.ankio.auto.http.api.LogAPI

/**
 * Xposed日志工具
 *
 */
object Logger {


    private fun getTag(): String {
        return Throwable().stackTrace[3].className.substringBefore('$').substringAfterLast(".")
    }

    private val TAG = "Logger"

    /**
     * 打印日志
     */
    fun log(app: String, msg: String) {
        if (AppRuntime.debug) {
            Log.d(TAG, "[ 自动记账 ] ( $app ) $msg")
        }
        val tag = getTag()
        //写入自动记账日志
        ThreadUtils.launch {
            LogAPI.add(LogLevel.INFO, app, tag, msg)
        }

    }

    /**
     * 只在调试模式输出日志
     */
    fun logD(app: String, msg: String) {
        if (AppRuntime.debug) {
            log(app, msg)
        }
    }

    /**
     * 打印错误日志
     */
    fun logE(app: String, e: Throwable) {
        Log.d(TAG, "[ 自动记账 ] ( $app ) ${e.message ?: ""}")
        Log.e(TAG, e.message ?: "")
        val tag = getTag()
        ThreadUtils.launch {
            val log = StringBuilder()
            log.append(e.message).append("\n")
            e.stackTrace.forEach {
                log.append(it.toString()).append("\n")
            }
            LogAPI.add(LogLevel.ERROR, app, tag, log.toString())
        }
    }
}