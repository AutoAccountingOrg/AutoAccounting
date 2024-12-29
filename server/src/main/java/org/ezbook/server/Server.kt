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

import android.content.Context
import android.util.Log
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.ezbook.server.constant.LogLevel
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.LogModel
import org.ezbook.server.server.module
import org.ezbook.server.task.BillProcessor
import java.net.ConnectException
import java.net.Proxy
import java.util.concurrent.TimeUnit


class Server(private val context: Context) {

    private val port = 52045


    init {
        Db.init(context)
    }

    private lateinit var server: NettyApplicationEngine

    /**
     * 启动服务
     */
    fun startServer() {
        server = embeddedServer(Netty, port = port) {
            module(context)
        }
        server.start()
        println("Server started on port $port")
        billProcessor = BillProcessor()
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
        var packageName = "net.ankio.auto.xposed"
        lateinit var billProcessor: BillProcessor

        /**
         * 发送请求
         */
        suspend fun request(path: String, json: String = ""): String? =
            withContext(Dispatchers.IO) {
                runCatching {
                    val uri = "http://127.0.0.1:52045/$path"
                    val client = OkHttpClient.Builder()
                        .readTimeout(60, TimeUnit.SECONDS)
                        .proxy(Proxy.NO_PROXY)
                        .build()

                    val body: RequestBody =
                        json.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

                    val request = Request.Builder()
                        .url(uri)
                        .post(body)
                        .addHeader("Content-Type", "application/json")
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw Exception("Unexpected code $response")
                        response.body?.string()
                    }
                }.onFailure {
                    if (it !is ConnectException) {
                        it.printStackTrace()
                    }
                    log("请求失败: ${it.message}")
                }.getOrNull()
            }

        private const val TAG = "AutoServer"

        /**
         * 日志
         */
        suspend fun log(msg: String) = withContext(Dispatchers.IO) {
            runCatching {
                Db.get().logDao().insert(LogModel().apply {
                    level = LogLevel.INFO
                    app = TAG
                    message = msg
                })
            }
            Log.i(TAG, msg)
        }

        /**
         * 日志
         */
        suspend fun logW(msg: String) = withContext(Dispatchers.IO) {
            runCatching {
                Db.get().logDao().insert(LogModel().apply {
                    level = LogLevel.WARN
                    app = TAG
                    message = msg
                })
            }
            Log.w(TAG, msg)
        }

        /**
         * 错误日志
         */
        suspend fun log(e: Throwable) = withContext(Dispatchers.IO) {

            runCatching {
                Db.get().logDao().insert(LogModel().apply {
                    level = LogLevel.ERROR
                    app = TAG
                    message = e.message ?: ""
                })
            }
            Log.e(TAG, e.message ?: "", e)
        }


    }
}
