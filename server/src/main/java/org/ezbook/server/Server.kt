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
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
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
import org.nanohttpd.protocols.http.IHTTPSession
import org.nanohttpd.protocols.http.NanoHTTPD
import org.nanohttpd.protocols.http.NanoHTTPD.SOCKET_READ_TIMEOUT
import org.nanohttpd.protocols.http.response.Response
import org.nanohttpd.protocols.http.response.Response.newFixedLengthResponse
import org.nanohttpd.protocols.http.response.Status


class Server(context:Context) {

    private val port = 52045
    private val server = ServerHttp(port,context)
    init {
        Db.init(context)
    }
    /**
     * 启动服务
     */
    fun startServer(){
        server.start(SOCKET_READ_TIMEOUT, false)
        println("Server started on port $port")


    }





    fun stopServer(){
        server.stop()
    }


    companion object {

        const val versionCode = 1

        /**
         * 获取请求数据
         */
        fun reqData(session: IHTTPSession): String {
            val contentLength: Int = session.headers["content-length"]?.toInt() ?: 0
            val buffer = ByteArray(contentLength)
            session.inputStream.read(buffer, 0, contentLength)
            // 将字节数组转换为字符串
           return String(buffer)

        }

        /**
         * 返回json
         */
        fun json(code:Int = 200,msg:String = "OK",data:Any? = null,count:Int = 0): Response {
            val jsonObject = JsonObject()
            jsonObject.addProperty("code", code)
            jsonObject.addProperty("msg", msg)
            jsonObject.addProperty("count", count)
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
       suspend fun request(path:String,json:String = ""):String? = withContext(Dispatchers.IO){
           runCatching {
               val uri = "http://localhost:52045/$path"
               // 创建一个OkHttpClient对象
               val client = OkHttpClient()
               // set as json post
               val body: RequestBody = json
                   .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
               // 创建一个Request
               val request = Request.Builder().url(uri).post(body)
                   .addHeader("Content-Type", "application/json").build()
               // 发送请求获取响应
               val response = client.newCall(request).execute()
               // 如果请求成功
               response.body?.string()

           }.onFailure {
                it.printStackTrace()
          }.getOrNull()
        }

        private const val TAG = "auto_server"

        /**
         * 日志
         */
        fun log(msg:String){
            Db.get().logDao().insert(LogModel().apply {
                level = LogLevel.INFO
                app = TAG
                message = msg
            })
            Log.d("Server",msg)
        }

        /**
         * 错误日志
         */
        fun log(e:Throwable){

            Db.get().logDao().insert(LogModel().apply {
                level = LogLevel.ERROR
                app = TAG
                message = e.message?:""
            })

            e.printStackTrace()
        }

        fun runOnMainThread(function: () -> Unit) {
            CoroutineScope(Job()).launch {
                function()
                cancel()
            }
        }
    }
}