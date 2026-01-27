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
import org.ezbook.server.log.ServerLog
import org.ezbook.server.tools.runCatchingExceptCancel

/**
 * OpenAI风格API的基础实现类
 * 提供了通用的OpenAI接口实现，继承此类的提供商只需要实现必要的属性即可
 */
abstract class BaseOpenAIProvider : BaseAIProvider() {


    /**
     * 获取可用的模型列表
     */
    override suspend fun getAvailableModels(): List<String> {
        // 日志：开始获取可用模型列表
        ServerLog.d("AI Provider: 获取可用模型列表")
        val request = Request.Builder()
            .url("${getApiUri()}/v1/models")
            .addHeader("Authorization", "Bearer ${getApiKey()}")
            .addUserAgent()
            .build()


        return runCatchingExceptCancel {
            client.newCall(request).execute().use { response ->


                val body = response.body?.string() ?: throw RuntimeException("Empty response body")
                val jsonObject = JsonParser.parseString(body).asJsonObject
                val models = mutableListOf<String>()

                jsonObject.getAsJsonArray("data")?.forEach { model ->
                    models.add(model.asJsonObject.get("id").asString)
                }

                // 日志：获取模型成功，记录数量
                ServerLog.d("AI Provider: 模型获取成功，count=${models.size}")
                return models
            }
        }.getOrElse {
            ServerLog.e("AI Provider: 获取模型失败：${it.message}", it)
            emptyList()
        }
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
    ): Result<String> {
        // 日志：请求发起（不打印内容与密钥）
        ServerLog.d("AI Provider: 发起请求，model=${getModel()}, stream=${onChunk != null}")
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
            .addUserAgent()
            .post(gson.toJson(requestBody).toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        return runCatchingExceptCancel {
            if (onChunk != null) {
                // 流式处理
                ServerLog.d("AI Provider: 开始流式响应处理")
                handleStreamResponse(request, onChunk)
                "" // 流式模式以空串占位
            } else {
                // 普通处理
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val body =
                            response.body?.string()?.removeThink() ?: error("Empty response body")
                        // 日志：非流式失败，打印响应片段
                        ServerLog.e(
                            "AI Provider: 响应失败 code=${response.code}，body=${
                                body.take(
                                    3000
                                )
                            }"
                        )
                        val jsonObject = JsonParser.parseString(body).asJsonObject
                        val message = jsonObject.get("error").asJsonObject.get("message").asString
                        error(message)
                    }

                    val body =
                        response.body?.string()?.removeThink() ?: error("Empty response body")
                    // 日志：非流式成功，打印响应片段以便排查
                    ServerLog.d("AI Provider: 响应成功(非流)，片段=${body.take(3000)}")
                    val jsonObject = JsonParser.parseString(body).asJsonObject

                    jsonObject
                        .getAsJsonArray("choices")
                        ?.get(0)
                        ?.asJsonObject
                        ?.getAsJsonObject("message")
                        ?.get("content")
                        ?.asString
                        ?: error("Empty choices content")
                }
            }
        }.onFailure {
            ServerLog.e("AI Provider(Result): 请求失败：${it.message}", it)
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
                    ServerLog.e("AI Provider: Stream 请求失败: ${response.code}")
                    return@withContext
                }

                val source = response.body?.source()
                if (source == null) {
                    ServerLog.e("AI Provider: Stream 响应体为空")
                    return@withContext
                }

                runCatchingExceptCancel {
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break

                    // 处理SSE格式的数据
                    if (line.startsWith("data: ")) {
                        val data = line.substring(6) // 移除"data: "前缀

                        if (data == "[DONE]") {
                            break
                        }

                        runCatchingExceptCancel {
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
                        }.onFailure {
                            ServerLog.e("AI Provider: Stream 解析数据块失败: ${it.message}")
                        }

                    }
                }
                }.onFailure {
                    ServerLog.e("AI Provider: ${it.message}", it)
                }
            }
        }
    }


} 