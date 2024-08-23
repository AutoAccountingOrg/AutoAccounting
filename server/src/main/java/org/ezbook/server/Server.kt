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
import com.google.gson.Gson
import com.google.gson.JsonObject
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.SOCKET_READ_TIMEOUT
import fi.iki.elonen.NanoHTTPD.newFixedLengthResponse
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.ezbook.server.db.Db
import org.ezbook.server.server.ServerHttp


class Server(context:Context) {

    private val port = 52045
    private val server = ServerHttp(port)
    init {
        Db.init(context)
    }
    /**
     * 启动服务
     */
    fun startServer(){
        server.start(SOCKET_READ_TIMEOUT, false);
        println("Server started on port 52045");
    }

    fun stopServer(){
        server.stop()
    }


    companion object {
        fun json(code:Int = 200,msg:String = "OK",data:Any? = null,count:Int = 0): NanoHTTPD.Response {
            val jsonObject = JsonObject();
            jsonObject.addProperty("code",code)
            jsonObject.addProperty("msg",msg)
            jsonObject.addProperty("count",count)
            jsonObject.add("data", Gson().toJsonTree(data))
            return newFixedLengthResponse(jsonObject.toString())
        }

       suspend fun request(path:String,json:String = ""):String?{
          return runCatching {
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

           }.getOrNull()
        }
    }
}