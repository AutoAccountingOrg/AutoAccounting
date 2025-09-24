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
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.ezbook.server.db.Db
import org.ezbook.server.server.module
import org.ezbook.server.task.BillProcessor
import org.ezbook.server.tools.ServerLog
import org.ezbook.server.tools.SettingUtils


class Server(private val context: Application) {

    private val port = PORT


    init {
        Db.init(context)
    }

    private lateinit var server: NettyApplicationEngine

    /**
     * 启动服务
     */
    fun startServer() {
        runBlocking {
            ServerLog.debugging = SettingUtils.debugMode()
        }
        server = embeddedServer(Netty, port = port) {
            module(context)
        }
        server.start()
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
        /**
         * 固定服务端口，供外部引用，保持单一来源。
         */
        const val PORT: Int = 52045

        var versionName = "1.0.0"
        var packageName = "net.ankio.auto"
        var debug = false
        lateinit var billProcessor: BillProcessor
        lateinit var application: Application

        /**
         * 统一的协程异常处理器：防止单个异常导致整个作用域崩溃
         */
        private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            ServerLog.e("协程执行异常: ${throwable.message}", throwable)
        }

        /** 全局协程作用域 - 使用 SupervisorJob 防止异常传播 */
        private val mainJob = SupervisorJob()
        private val mainScope = CoroutineScope(Dispatchers.Main + mainJob + exceptionHandler)

        fun withIO(block: suspend () -> Unit) {
            mainScope.launch(Dispatchers.IO) { block() }
        }


    }
}
