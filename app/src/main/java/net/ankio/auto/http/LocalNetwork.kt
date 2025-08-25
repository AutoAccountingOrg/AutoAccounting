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

package net.ankio.auto.http

import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.storage.Logger
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.Proxy
import java.util.concurrent.TimeUnit

object LocalNetwork {
    // 1. 复用同一个 OkHttpClient，按需配置超时 & 禁用代理
    private val client by lazy {
        RequestsUtils()
    }


    /**
     * 发起网络请求：
     * - 可通过 method 强制指定；
     * - headers 用于附加额外请求头。
     */
    /**
     * 发起网络请求
     *
     * @param path        请求路径（自动拼接到 http://127.0.0.1:52045/）
     * @param payload     可选请求体，非空则默认使用 POST（也可通过 method 强制覆盖）
     */
    suspend fun post(
        path: String,
        payload: Any? = null,
    ): String = withContext(Dispatchers.IO) {
        val url = "http://127.0.0.1:52045/${path.trimStart('/')}"
        client.addHeader("Authorize", "")
        try {
            when (payload) {
                is JsonObject -> {
                    return@withContext client.json(url, payload).second
                }

                else -> {
                    return@withContext client.jsonStr(url, payload.toString()).second

                }

            }
        } catch (e: Exception) {
            Logger.e("请求失败：$e", e)
            return@withContext ""
        }
    }
    /**
     * 发起网络请求：
     * - 当 json != null 且非空时，默认 POST；
     * - 否则 GET；
     * - 可通过 method 强制指定；
     * - headers 用于附加额外请求头。
     */
    /**
     * 发起网络请求
     *
     * @param path        请求路径（自动拼接到 http://127.0.0.1:52045/）
     */
    suspend fun get(
        path: String,
    ): String = withContext(Dispatchers.IO) {
        val url = "http://127.0.0.1:52045/${path.trimStart('/')}"
        client.addHeader("Authorize", "")
        try {
            return@withContext client.get(url).second
        } catch (e: Exception) {
            Logger.e("请求失败：$e", e)
            return@withContext ""
        }
    }

    /**
     * 通用请求方法 - 根据是否有payload自动选择GET或POST
     *
     * @param path        请求路径（自动拼接到 http://127.0.0.1:52045/）
     * @param payload     可选请求体，为空则使用GET请求，否则使用POST请求
     */
    suspend fun request(
        path: String,
        payload: String? = null,
    ): String = withContext(Dispatchers.IO) {
        if (payload.isNullOrEmpty()) {
            get(path)
        } else {
            post(path, payload)
        }
    }

    /**
     * 发起流式POST请求
     * @param path 请求路径
     * @param payload 请求体
     * @param onEvent 事件回调函数，参数为(event, data)
     */
    suspend fun postStream(
        path: String,
        payload: String,
        onEvent: (String, String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val url = "http://127.0.0.1:52045/${path.trimStart('/')}"

        // 创建专用的OkHttpClient用于流式请求
        val streamClient = OkHttpClient.Builder()
            .readTimeout(5, TimeUnit.MINUTES)
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .proxy(Proxy.NO_PROXY)
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "text/event-stream")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        try {
            streamClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Logger.e("流式请求失败: ${response.code}")
                    onEvent("error", "HTTP ${response.code}")
                    return@withContext
                }


                Logger.d("流式请求成功，开始读取数据流")
                val source = response.body?.source()
                if (source == null) {
                    Logger.e("响应体为空")
                    onEvent("error", "Empty response body")
                    return@withContext
                }

                try {
                    var lineCount = 0
                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        lineCount++

                        if (line.trim().isEmpty()) {
                            continue // 跳过空行
                        }

                        Logger.d("读取第${lineCount}行: $line")

                        // 处理SSE格式的数据
                        when {
                            line.startsWith("data: ") -> {
                                val data = line.substring(6) // 移除"data: "前缀
                                Logger.d("处理数据: ${data.take(100)}...")

                                if (data == "[DONE]") {
                                    Logger.d("收到完成信号")
                                    break
                                } else if (data == "[START]") {
                                    Logger.d("SSE连接已建立")
                                    onEvent("message", data)
                                } else {
                                    onEvent("message", data)
                                }
                            }

                            line.startsWith("event: ") -> {
                                val eventType = line.substring(7) // 移除"event: "前缀
                                Logger.d("事件类型: $eventType")
                            }

                            line.startsWith("id: ") -> {
                                val id = line.substring(4) // 移除"id: "前缀
                                Logger.d("事件ID: $id")
                            }

                            else -> {
                                Logger.d("未知SSE行: $line")
                            }
                        }
                    }
                    Logger.d("流式响应处理完成，共读取${lineCount}行")
                } catch (e: Exception) {
                    Logger.e("读取流式响应失败: ${e.message}", e)
                    onEvent("error", e.message ?: "Unknown error")
                }
            }
        } catch (e: Exception) {
            Logger.e("创建流式请求失败: ${e.message}", e)
            onEvent("error", e.message ?: "Unknown error")
        }
    }
}
