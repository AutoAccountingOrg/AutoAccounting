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

import com.google.gson.Gson
import com.google.gson.JsonObject
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.SOCKET_READ_TIMEOUT
import fi.iki.elonen.NanoHTTPD.newFixedLengthResponse
import org.ezbook.server.routes.AppDataRoute
import org.ezbook.server.server.ServerHttp


class Server {

    /**
     * 启动服务
     */
    fun startServer(){

        val server = ServerHttp()
        server.start(SOCKET_READ_TIMEOUT, false);
        println("Server started on port 52045");
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
    }
}