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

package org.ezbook.server

import android.app.Application
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.android.LogcatAppender
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.*
import org.ezbook.server.db.Db
import org.ezbook.server.server.module
import org.ezbook.server.task.BillProcessor
import org.ezbook.server.tools.LogModelAppender
import org.ezbook.server.tools.SettingUtils
import org.slf4j.LoggerFactory
import io.github.oshai.kotlinlogging.KotlinLogging


class Server(private val context: Application) {
    private val port = 52045


    init {
        Db.init(context)
    }

    private lateinit var server: NettyApplicationEngine


    private fun insertDatabaseAppender(debugging: Boolean) {
        val appenderName = "database"
        val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
        // 检查是否已经存在同名的 appender
        val serverLogger = loggerContext.getLogger("org.ezbook.server")
        val existingAppender = serverLogger.getAppender(appenderName)
        if (existingAppender != null) return

//        serverLogger.isAdditive = false
        serverLogger.addAppender(LogModelAppender {
            Db.get().logDao().insert(it)
        }.apply {
            val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
            context = loggerContext
            name = appenderName
            this.packageName = null
            this.debugging = debugging
            start()
        })

        serverLogger.addAppender(
            LogcatAppender().apply {
                context = loggerContext
                name = appenderName
                tagEncoder = PatternLayoutEncoder().apply {
                    context = loggerContext
                    pattern = "%logger{12}"
                    start()
                }
                encoder = PatternLayoutEncoder().apply {
                    context = loggerContext
                    pattern = if (debugging) "[ 自动记账 ] %file:%line - %msg%n" else "[ 自动记账 ] %msg%n"
                    start()
                }
                start()
            })
    }

    /**
     * 启动服务
     */
    fun startServer() {
        insertDatabaseAppender(runBlocking { SettingUtils.debugMode() })
        server = embeddedServer(Netty, port = port) {
            module(context)
        }
        server.start()
        println("Server started on port $port")
        billProcessor = BillProcessor()
        application = context
    }

    fun restartServer() {
        stopServer()
        startServer()
    }

    fun stopServer() {
        server.stop(0, 0)
        billProcessor.shutdown()
    }


    companion object {

        var versionName = "1.0.0"
        var packageName = "net.ankio.auto"
        var debug = false
        lateinit var billProcessor: BillProcessor
        lateinit var application: Application


        /** 全局主线程协程作用域 */
        private val mainJob = Job()
        private val mainScope = CoroutineScope(Dispatchers.Main + mainJob)

        fun withIO(block: suspend () -> Unit) {
            mainScope.launch(Dispatchers.IO) { block() }
        }
    }
}
