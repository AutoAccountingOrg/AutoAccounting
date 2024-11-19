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
import android.os.Handler
import android.os.Looper
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.withCharset
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ezbook.server.constant.LogLevel
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.LogModel
import org.ezbook.server.server.module
import org.ezbook.server.task.BillProcessor


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
        server.stop(0,0)
        billProcessor.shutdown()
    }


    companion object {

        const val versionCode = 1
        lateinit var billProcessor: BillProcessor

        /**
         * 发送请求
         */
        suspend fun request(path: String, json: String = ""): String? =
            withContext(Dispatchers.IO) {
                val client = HttpClient(CIO)
                runCatching {
                    // 基础 URI
                    val baseUri = "http://127.0.0.1:52045"
                    // 清理路径，避免双斜杠，除了协议部分
                    val cleanedPath = path.replace(Regex("/+"), "/").let {
                        if (it.startsWith("/")) it else "/$it"
                    }
                    val uri = "$baseUri$cleanedPath"
                    // 发送 POST 请求并获取响应
                    client.post<HttpResponse>(uri) {
                        contentType(ContentType.Application.Json.withCharset(Charsets.UTF_8))
                        body = json
                    }.let { response ->
                        // 检查响应状态并读取响应体
                        if (response.status == HttpStatusCode.OK) {
                            response.readText()
                        } else {
                            null // 或者处理错误状态
                        }
                    }
                }.onFailure {
                    it.printStackTrace() // 打印异常信息
                }.getOrNull().also {
                    client.close() // 确保关闭客户端以释放资源
                }
            }

        private const val TAG = "auto_server"

        /**
         * 日志
         */
        suspend  fun log(msg: String)  = withContext(Dispatchers.IO){
            runCatching {
                Db.get().logDao().insert(LogModel().apply {
                    level = LogLevel.INFO
                    app = TAG
                    message = msg
                })
            }
            Log.i("AutoServer", msg)
        }

        /**
         * 日志
         */
        suspend  fun logW(msg: String)  = withContext(Dispatchers.IO){
            runCatching {
                Db.get().logDao().insert(LogModel().apply {
                    level = LogLevel.WARN
                    app = TAG
                    message = msg
                })
            }
            Log.w("AutoServer", msg)
        }

        /**
         * 错误日志
         */
        suspend fun log(e: Throwable)  = withContext(Dispatchers.IO){

           runCatching {
               Db.get().logDao().insert(LogModel().apply {
                   level = LogLevel.ERROR
                   app = TAG
                   message = e.message ?: ""
               })
           }

            Log.e("AutoServer", e.message ?: "",e)
        }


    }
}
