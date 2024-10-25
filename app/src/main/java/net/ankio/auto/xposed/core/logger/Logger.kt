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

import de.robv.android.xposed.XposedBridge
import net.ankio.auto.xposed.core.utils.ThreadUtils
import org.ezbook.server.constant.LogLevel
import org.ezbook.server.db.model.LogModel

/**
 * Xposed日志工具
 *
 */
object Logger {


    /**
     * 是否为调试模式
     */
    var debug = true

    private fun getTag(): String {
        return Throwable().stackTrace[3].className.substringBefore('$').substringAfterLast(".")
    }

    /**
     * 打印日志
     */
    fun log(app: String, msg: String) {
        XposedBridge.log("[ 自动记账 ] ( $app ) $msg")
        val tag = getTag()
        //写入自动记账日志
        ThreadUtils.launch {
            LogModel.add(LogLevel.INFO, app, tag, msg)
        }

    }

    /**
     * 只在调试模式输出日志
     */
    fun logD(app: String, msg: String) {
        if (debug) {
            log(app, msg)
        }
    }

    /**
     * 打印错误日志
     */
    fun logE(app: String, e: Throwable) {
        XposedBridge.log("[ 自动记账 ] ( $app ) ${e.message?:""}")
        XposedBridge.log(e)
        val tag = getTag()
        ThreadUtils.launch {
            val log = StringBuilder()
            log.append(e.message).append("\n")
            e.stackTrace.forEach {
                log.append(it.toString()).append("\n")
            }
            LogModel.add(LogLevel.ERROR, app, tag, log.toString())
        }
    }
}