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
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.ezbook.server.constant.LogLevel
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.LogModel
import org.ezbook.server.server.ServerHttp
import org.ezbook.server.task.BillProcessor
import org.nanohttpd.protocols.http.IHTTPSession
import org.nanohttpd.protocols.http.NanoHTTPD
import org.nanohttpd.protocols.http.NanoHTTPD.SOCKET_READ_TIMEOUT
import org.nanohttpd.protocols.http.response.Response
import org.nanohttpd.protocols.http.response.Response.newFixedLengthResponse
import org.nanohttpd.protocols.http.response.Status
import java.net.ConnectException
import java.util.concurrent.TimeUnit
import java.net.Proxy


class Server(context: Context) {

    private val port = 52045
    private val server = ServerHttp(port, context)

    init {
        Db.init(context)
    }


    /**
     * 启动服务
     */
    fun startServer() {
        server.start(SOCKET_READ_TIMEOUT, false)
        println("Server started on port $port")
        billProcessor = BillProcessor()

    }


    fun stopServer() {
        server.stop()
        billProcessor.shutdown()
    }


    companion object {

        const val versionCode = 1
        lateinit var billProcessor: BillProcessor

        /**
         * 获取请求数据
         */
        fun reqData(session: IHTTPSession): String {
            val contentLength: Int = session.headers["content-length"]?.toInt() ?: 0

            // 限制请求体的最大长度，防止恶意请求
            val maxContentLength = 10 * 1024 * 1024 // 10 MB
            if (contentLength > maxContentLength) {
                throw IllegalArgumentException("Request body is too large")
            }

            val buffer = ByteArray(contentLength)
            var totalBytesRead = 0

            while (totalBytesRead < contentLength) {
                val bytesRead = session.inputStream.read(buffer, totalBytesRead, contentLength - totalBytesRead)
                if (bytesRead == -1) {
                    // 如果流结束但数据不完整，抛出异常
                    throw IllegalArgumentException("Content-Length mismatch: expected $contentLength, but got $totalBytesRead")
                }
                totalBytesRead += bytesRead
            }

            // 再次检查实际读取的字节数是否匹配 Content-Length
            if (totalBytesRead != contentLength) {
                throw IllegalArgumentException("Content-Length mismatch: expected $contentLength, but got $totalBytesRead")
            }

            return String(buffer, 0, totalBytesRead)
        }


        /**
         * 返回json
         */
        fun json(code: Int = 200, msg: String = "OK", data: Any? = null): Response {
            val jsonObject = JsonObject()
            jsonObject.addProperty("code", code)
            jsonObject.addProperty("msg", msg)
            jsonObject.add("data", Gson().toJsonTree(data))
            return newFixedLengthResponse(
                Status.OK,
                NanoHTTPD.MIME_PLAINTEXT,
                jsonObject.toString()
            )
        }


        /**
         * 发送请求
         */
        suspend fun request(path: String, json: String = ""): String? =
            withContext(Dispatchers.IO) {
                runCatching {
                    val uri = "http://127.0.0.1:52045/$path"
                    // 创建一个OkHttpClient对象，禁用代理
                    val client = OkHttpClient.Builder()
                        .readTimeout(60, TimeUnit.SECONDS)
                        .proxy(Proxy.NO_PROXY)
                        .build()
                    // set as json post
                    val body: RequestBody = json
                        .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                    // 创建一个Request
                    val request = Request.Builder().url(uri).post(body)
                        .addHeader("Content-Type", "application/json").build()
                    // 发送请求获取响应
                    val response = client.newCall(request).execute()
                    val bodyString = response.body?.string()
                    // 如果请求成功
                    bodyString

                }.onFailure {
                    if (it !is ConnectException) {
                        it.printStackTrace()
                    }

                }.getOrNull()
            }

        private const val TAG = "auto_server"

        /**
         * 日志
         */
        fun log(msg: String) {
            runCatching {
                Db.get().logDao().insert(LogModel().apply {
                    level = LogLevel.INFO
                    app = TAG
                    message = msg
                })
            }
            Log.i("Server", msg)
        }

        /**
         * 日志
         */
        fun logW(msg: String) {
            runCatching {
                Db.get().logDao().insert(LogModel().apply {
                    level = LogLevel.WARN
                    app = TAG
                    message = msg
                })
            }
            Log.w("Server", msg)
        }

        /**
         * 错误日志
         */
        fun log(e: Throwable) {

           runCatching {
               Db.get().logDao().insert(LogModel().apply {
                   level = LogLevel.ERROR
                   app = TAG
                   message = e.message ?: ""
               })
           }

            Log.e("Server", e.message ?: "",e)
        }

        fun runOnMainThread(function: () -> Unit) {
            val handler = Handler(Looper.getMainLooper())
            handler.post {
                function()
            }
        }

        fun isRunOnMainThread() {
            if (Thread.currentThread().name == "main") {
                throw RuntimeException("不允许在主线程运行")
            }
        }
    }
}
