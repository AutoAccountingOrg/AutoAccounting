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

package net.ankio.auto.sdk.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.sdk.exception.AutoAccountingException
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket
import java.net.URL


object RequestUtils {
    suspend fun post(
        url: String,
        query: HashMap<String, String>? = null,
        data: String? = null,
        headers: HashMap<String, String> = HashMap()
    ): String = withContext(Dispatchers.IO) {
        val (host, port) = parseUrl(url)

        var response: String? = null
        var isSuccess = false

        // 创建 Socket 连接
        var socket: Socket? = null
        var writer: BufferedWriter? = null
        var reader: BufferedReader? = null

        try {
            socket = Socket(host, port)
            socket.soTimeout = 60000 // 设置读取超时时间为 10 秒
            writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
            reader = BufferedReader(InputStreamReader(socket.getInputStream()))

            // 构建请求头
            val requestHeaders = StringBuilder()
            headers.forEach { (key, value) ->
                requestHeaders.append("$key: $value\r\n")
            }
            val parsedUrl = URL(url)
            var path = parsedUrl.path

            if (!query.isNullOrEmpty()) {
                path += query.entries.joinToString("&", prefix = "?") { "${it.key}=${it.value}" }
            }

            // 构建请求行和请求体
            val requestBody = data ?: ""
            val requestLine = "POST $path HTTP/1.1\r\n"
            val contentLength = "Content-Length: ${requestBody.toByteArray().size}\r\n"

            // 发送 HTTP 请求
            writer.write(requestLine+"Host: $host\r\n")
            writer.write(requestHeaders.toString())
            writer.write(contentLength)
            writer.write("Connection: close\r\n") // 关闭连接
            writer.write("\r\n")
            writer.write(requestBody)
            writer.flush()

            // 读取响应状态行
            val statusLine = reader.readLine()
            val statusParts = statusLine.split(" ")
            if (statusParts.size >= 3) {
                val statusCode = statusParts[1].toInt()
                isSuccess = statusCode == 200
            }

            if(!isSuccess){
                throw AutoAccountingException("未授权自动记账",AutoAccountingException.CODE_SERVER_AUTHORIZE)
            }



            // 跳过响应头部分
            var line: String?
            do {
                line = reader.readLine()
            } while (!line.isNullOrEmpty())

            // 读取响应体
            response = reader.readText()
            Logger.i("请求成功：\n${path}\n${requestBody}\n${response}")
        } catch (e: Exception) {
            e.printStackTrace()
            Logger.i("请求异常：\n${url}\n${e.message}", e)
            throw AutoAccountingException("请求异常：\n${url}\n${e.message}",AutoAccountingException.CODE_SERVER_ERROR)
        } finally {
            // 关闭资源
            kotlin.runCatching {
                writer?.close()
                reader?.close()
                socket?.close()

            }
        }


        //请求失败有两个可能，一个是401，一个是服务未启动


        response ?: throw AutoAccountingException("请求失败",AutoAccountingException.CODE_SERVER_ERROR)
    }


    private fun parseUrl(url: String): Pair<String, Int> {
        val parsedUrl = URL(url)
        val host = parsedUrl.host
        val port = if (parsedUrl.port != -1) parsedUrl.port else parsedUrl.defaultPort
        return Pair(host, port)
    }
}
