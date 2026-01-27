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

package org.ezbook.server.ai.providers

import com.google.gson.JsonParser
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.ezbook.server.log.ServerLog
import org.ezbook.server.tools.runCatchingExceptCancel

/**
 * 智谱清言（BigModel）API 提供商实现
 *
 * 说明：
 * - BigModel 原生接口为 v4 路径（/paas/v4），并非 OpenAI 的 /v1。
 * - 请求/响应基本兼容 OpenAI Chat Completions；流式返回采用 SSE（data: {json}）。
 * - 这里直接基于 BaseAIProvider 实现，避免与 BaseOpenAIProvider 的 /v1 路径耦合。
 */
class BigModelProvider : BaseAIProvider() {
    override val name: String = "bm"
    override val createKeyUri: String = "https://bigmodel.cn/usercenter/proj-mgmt/apikeys"

    /**
     * 获取可用模型列表
     * 说明：BigModel 提供以下常用可选模型，返回静态列表，避免额外接口依赖。
     */
    override suspend fun getAvailableModels(): List<String> = listOf(
        "glm-4.5",
        "glm-4.5-air",
        "glm-4.5-x",
        "glm-4.5-airx",
        "glm-4.5-flash",
        "glm-4-plus",
        "glm-4-air-250414",
        "glm-4-airx",
        "glm-4-flashx",
        "glm-4-flashx-250414",
        "glm-z1-air",
        "glm-z1-airx",
        "glm-z1-flash",
        "glm-z1-flashx"
    )

    override val apiUri: String
        get() = "https://open.bigmodel.cn/api"
    override var model: String = "glm-4.5"

    /**
     * 发送聊天请求（支持流式SSE）
     *
     * 请求体：
     * {
     *   "model": "glm-4.5",
     *   "messages": [{"role":"system","content":...},{"role":"user","content":...}],
     *   "temperature": 0.7,
     *   "stream": true|false
     * }
     */
    override suspend fun request(
        system: String,
        user: String,
        onChunk: ((String) -> Unit)?
    ): Result<String> {
        // 日志：避免过度输出，仅记录关键信息
        ServerLog.d("BigModel: 发起请求，model=${getModel()}, stream=${onChunk != null}")

        // 1) 组装 messages
        val messages = mutableListOf<Map<String, String>>()
        if (system.isNotEmpty()) {
            messages.add(mapOf("role" to "system", "content" to system))
        }
        messages.add(mapOf("role" to "user", "content" to user))

        // 2) 组装请求体
        val bodyMap = mutableMapOf(
            "model" to getModel(),
            "messages" to messages,
            "temperature" to 0.7
        )
        if (onChunk != null) bodyMap["stream"] = true

        val url = "${getApiUri()}/paas/v4/chat/completions"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${getApiKey()}")
            .addHeader("Content-Type", "application/json")
            .addUserAgent()
            .apply { if (onChunk != null) addHeader("Accept", "text/event-stream") }
            .post(gson.toJson(bodyMap).toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        return runCatchingExceptCancel {
            if (onChunk != null) {
                // 流式处理：逐行读取 data: 事件
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val err = response.body?.string()?.removeThink()
                        ServerLog.e(
                            "BigModel: 流式请求失败 code=${response.code}, body=${
                                err?.take(
                                    300
                                )
                            }"
                        )
                        error(parseErrorMessage(err))
                    }

                    val source = response.body?.source() ?: return@use
                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        if (!line.startsWith("data: ")) continue
                        val data = line.substring(6).trim()
                        if (data == "[DONE]") break
                        runCatchingExceptCancel {
                            val obj = JsonParser.parseString(data).asJsonObject
                            val choices = obj.getAsJsonArray("choices")
                            if (choices != null && choices.size() > 0) {
                                val delta = choices[0].asJsonObject.getAsJsonObject("delta")
                                val content = delta?.get("content")?.asString
                                if (!content.isNullOrEmpty()) onChunk(content)
                            }
                        }
                    }
                }
                "" // 流式模式返回空串占位
            } else {
                // 非流式：一次性返回
                client.newCall(request).execute().use { response ->
                    val resp =
                        response.body?.string()?.removeThink() ?: error("Empty response body")
                    if (!response.isSuccessful) {
                        ServerLog.e("BigModel: 请求失败 code=${response.code}, body=${resp.take(300)}")
                        error(parseErrorMessage(resp))
                    }

                    val json = JsonParser.parseString(resp).asJsonObject
                    json
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
            ServerLog.e("BigModel(Result): 请求失败：${it.message}", it)
        }
    }

    /**
     * 简单解析错误消息，兼容 OpenAI 风格错误结构：{"error":{"message":...}}
     */
    private suspend fun parseErrorMessage(body: String?): String {
        if (body.isNullOrBlank()) return "Request failed"
        return runCatchingExceptCancel {
            val obj = JsonParser.parseString(body).asJsonObject
            obj.getAsJsonObject("error")?.get("message")?.asString ?: body
        }.getOrElse { body }
    }
}