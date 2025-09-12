/*
 * Copyright (C) 2025 ankio(ankio@ankio.net)
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

package org.ezbook.server.tools

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.UnsynchronizedAppenderBase
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import org.ezbook.server.constant.LogLevel
import org.ezbook.server.db.model.LogModel

class LogModelAppender(
    val callback: suspend CoroutineScope.(model: LogModel) -> Unit
) : UnsynchronizedAppenderBase<ILoggingEvent>() {
    private var scope: CoroutineScope? = null

    var debugging = false

    var packageName: String? = null

    private var actor: SendChannel<LogModel>? = null

    @OptIn(DelicateCoroutinesApi::class)
    override fun append(event: ILoggingEvent) {
        if (scope?.isActive != true || actor?.isClosedForSend ?: true) return

        val logLevel = runCatching {
            LogLevel.valueOf(event.level.levelStr)
        }.getOrDefault(LogLevel.ERROR)

        val location = when {
            !packageName.isNullOrBlank() && debugging -> {
                event.callerData.firstOrNull()?.run { "${event.loggerName}(${fileName}:${lineNumber})" }
            }

            !packageName.isNullOrEmpty() -> event.loggerName

            else -> event.callerData.firstOrNull()?.run { "${fileName}:${lineNumber}" }
        } ?: event.threadName

        val app = packageName ?: event.loggerName

        val model = LogModel().apply {
            level = logLevel
            this.app = app
            this.location = location
            this.message = event.formattedMessage
            time = System.currentTimeMillis()
        }

        scope?.launch {
            actor?.send(model)
        }
    }

    @OptIn(ObsoleteCoroutinesApi::class)
    override fun start() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        actor = scope?.actor(Dispatchers.IO, capacity = 100) {
            for (model in channel) {
                try {
                    callback(model)
                } catch (e: Exception) {
                    // 处理异常，例如记录日志或其他操作
                }
            }
        }
        super.start()
    }

    override fun stop() {
        scope?.cancel()
        scope = null
        super.stop()
    }
}