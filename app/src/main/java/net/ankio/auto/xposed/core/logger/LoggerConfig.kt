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

package net.ankio.auto.xposed.core.logger

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.android.LogcatAppender
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import io.github.oshai.kotlinlogging.KotlinLogging
import net.ankio.auto.BuildConfig
import net.ankio.auto.http.api.LogAPI
import org.ezbook.server.tools.LogModelAppender
import org.slf4j.LoggerFactory

object LoggerConfig {
    fun init(packageName: String, debugging: Boolean) {
        val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
        loggerContext.reset()

        when (packageName) {
            BuildConfig.APPLICATION_ID -> listOfNotNull(
                createLogcatAppender(loggerContext, debugging),
                createNetworkAppender(loggerContext, null, debugging),
            )

            "com.android.phone" -> listOfNotNull(
                createLogcatAppender(loggerContext, debugging, " [ 自动记账 ] ")
            )

            else -> listOfNotNull(
                createXposedAppender(loggerContext, packageName, debugging),
//                createNetworkAppender(loggerContext, packageName, debugging),
//                if (debugging) createLogcatAppender(loggerContext, true, "[ 自动记账 ] ") else null
            )
        }.forEach(loggerContext.getLogger(Logger.ROOT_LOGGER_NAME)::addAppender)
    }

    private fun createLogcatAppender(
        loggerContext: LoggerContext, debugging: Boolean, prefix: String = "", appenderName: String = "logcat"
    ) = LogcatAppender().apply {
        context = loggerContext
        name = appenderName
        tagEncoder = PatternLayoutEncoder().apply {
            context = loggerContext
            pattern = "%logger{12}"
            start()
        }
        encoder = PatternLayoutEncoder().apply {
            context = loggerContext
            pattern = prefix + if (debugging) "%file:%line - %msg%n" else "%msg%n"
            start()
        }
        start()
    }

    private fun createXposedAppender(
        loggerContext: LoggerContext, packageName: String, debugging: Boolean, appenderName: String = "xposed"
    ) = XposedBridgeAppender().apply {
        context = loggerContext
        name = appenderName
        encoder = PatternLayout().apply {
            context = loggerContext
            pattern = if (debugging) {
                "[ 自动记账 ] [$packageName] [%.1level] %logger{12} %file:%line - %msg"
            } else {
                "[ 自动记账 ] [$packageName] [%.1level] %logger{12} - %msg"
            }
            start()
        }
        start()
    }

    private fun createNetworkAppender(
        loggerContext: LoggerContext, packageName: String?, debugging: Boolean, appenderName: String = "network"
    ) = LogModelAppender {
        LogAPI.add(it)
    }.apply {
        context = loggerContext
        name = appenderName
        this.packageName = packageName
        this.debugging = debugging
        start()
    }
}