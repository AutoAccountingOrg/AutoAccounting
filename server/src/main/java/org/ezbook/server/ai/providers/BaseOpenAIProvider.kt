/*
 * Copyright (C) 2024 ankio(ankio@ankio.net)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.ezbook.server.ai.providers


import com.google.gson.JsonParser
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.ezbook.server.Server

/**
 * OpenAI风格API的基础实现类
 * 提供了通用的OpenAI接口实现，继承此类的提供商只需要实现必要的属性即可
 */
abstract class BaseOpenAIProvider : BaseAIProvider() {



    /**
     * 获取可用的模型列表
     */
    override suspend fun getAvailableModels(): List<String> {
        val request = Request.Builder()
            .url("${getApiUri()}/v1/models")
            .addHeader("Authorization", "Bearer ${getApiKey()}")
            .build()


        return runCatching {
            client.newCall(request).execute().use { response ->
                //  if (!response.isSuccessful) throw RuntimeException("Failed to get models: ${response.code}")

                val body = response.body?.string() ?: throw RuntimeException("Empty response body")
                val jsonObject = JsonParser.parseString(body).asJsonObject
                val models = mutableListOf<String>()

                jsonObject.getAsJsonArray("data")?.forEach { model ->
                    models.add(model.asJsonObject.get("id").asString)
                }

                return models
            }
        }.onFailure {
            Server.log(it)
        }.getOrElse { emptyList() }
    }


    /**
     * 发送请求到AI并获取响应（支持流式输出）
     * @param system 系统角色提示词
     * @param user 用户输入
     * @param onChunk 流式输出时的数据块回调函数
     * @return AI响应内容（非流式）或null（流式）
     */
    override suspend fun request(
        system: String,
        user: String,
        onChunk: ((String) -> Unit)?
    ): String? {
        val messages = mutableListOf<Map<String, String>>()

        // 添加系统消息
        if (system.isNotEmpty()) {
            messages.add(
                mapOf(
                    "role" to "system",
                    "content" to system
                )
            )
        }

        // 添加用户消息
        messages.add(
            mapOf(
                "role" to "user",
                "content" to user
            )
        )

        val requestBody = mutableMapOf(
            "model" to getModel(),
            "messages" to messages,
            "temperature" to 0.7
        )

        // 如果启用流式输出，添加stream参数
        if (onChunk != null) {
            requestBody["stream"] = true
        }

        val request = Request.Builder()
            .url("${getApiUri()}/v1/chat/completions")
            .addHeader("Authorization", "Bearer ${getApiKey()}")
            .addHeader("Content-Type", "application/json")
            .apply {
                if (onChunk != null) {
                    addHeader("Accept", "text/event-stream")
                }
            }
            .post(gson.toJson(requestBody).toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        if (onChunk != null) {
            // 流式处理
            handleStreamResponse(request, onChunk)
            return null // 流式模式返回null
        } else {
            // 普通处理
            return client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val body = response.body?.string()?.removeThink() ?: return null
                    Server.log("AI响应失败：${body}")
                    val jsonObject = JsonParser.parseString(body).asJsonObject
                    val message = jsonObject.get("error").asJsonObject.get("message").asString
                    error(message)
                }

                val body = response.body?.string()?.removeThink() ?: return null
                val jsonObject = JsonParser.parseString(body).asJsonObject

                jsonObject
                    .getAsJsonArray("choices")
                    ?.get(0)
                    ?.asJsonObject
                    ?.getAsJsonObject("message")
                    ?.get("content")
                    ?.asString
            }
        }
    }

    /**
     * 处理流式响应
     * @param request HTTP请求
     * @param onChunk 数据块回调函数
     */
    private suspend fun handleStreamResponse(request: Request, onChunk: (String) -> Unit) {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Server.log("StreamRequest 请求失败: ${response.code}")
                    return@withContext
                }

                val source = response.body?.source()
                if (source == null) {
                    Server.log("StreamRequest 响应体为空")
                    return@withContext
                }

                try {
                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: break

                        // 处理SSE格式的数据
                        if (line.startsWith("data: ")) {
                            val data = line.substring(6) // 移除"data: "前缀

                            if (data == "[DONE]") {
                                break
                            }

                            try {
                                val jsonObject = JsonParser.parseString(data).asJsonObject
                                val choices = jsonObject.getAsJsonArray("choices")
                                if (choices != null && choices.size() > 0) {
                                    val choice = choices[0].asJsonObject
                                    val delta = choice.getAsJsonObject("delta")
                                    val content = delta?.get("content")?.asString

                                    if (content != null) {
                                        onChunk(content)
                                    }
                                }
                            } catch (e: Exception) {
                                Server.log("StreamRequest 解析数据块失败: ${e.message}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Server.log(e)
                }
            }
        }
    }


} 