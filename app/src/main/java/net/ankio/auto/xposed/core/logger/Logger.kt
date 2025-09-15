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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.launch
import net.ankio.auto.BuildConfig
import net.ankio.auto.http.api.LogAPI
import net.ankio.auto.xposed.core.utils.AppRuntime
import net.ankio.auto.xposed.core.utils.CoroutineUtils
import org.ezbook.server.constant.LogLevel
import org.ezbook.server.db.model.LogModel
import org.ezbook.server.tools.BaseLogger

/**
 * Xposed日志工具
 *
 */
object Logger : BaseLogger() {
    var app = "unknown"

    private val scope = CoroutineScope(SupervisorJob())

    @OptIn(ObsoleteCoroutinesApi::class)
    private val actor = scope.actor(Dispatchers.IO) {
        for (log in channel) {
            try {
                LogAPI.add(log)
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e("Logger", "Send log to server error", e)
                }
            }
        }
    }

    override fun xposedBridgeFormater(
        priority: LogLevel, className: String, file: String, line: Int, msg: String, tr: Throwable?
    ): String {
        var prefix = "[ 自动记账 ][ $app ]"
        if (line != -1) prefix = "$prefix $className($file:$line) "

        return "$prefix $msg\n${tr?.stackTrace?.joinToString("\n")}".trimEnd()
    }

    override fun logcatFormater(priority: LogLevel, file: String, line: Int, msg: String, tr: Throwable?): String? {
        if (xposedBridgeLogMethod != null) return null

        var prefix = "[ 自动记账 ][ $app ]"
        if (line != -1) prefix = "$prefix($file:$line) "
        return "$prefix $msg\n${tr?.stackTrace?.joinToString("\n")}".trimEnd()
    }

    override fun logModelFormater(
        priority: LogLevel, className: String, file: String, line: Int, msg: String, tr: Throwable?
    ): LogModel = LogModel(
        level = priority,
        app = this@Logger.app,
        // location 统一为 "类名(File.kt:行号)"
        location = className + if (line != -1) "($file:$line)" else "",
        message = "$msg\n${tr?.stackTrace?.joinToString("\n") ?: ""}".trimEnd()
    )

    override fun onLogModel(model: LogModel) {
        scope.launch { actor.send(model) }
    }
}