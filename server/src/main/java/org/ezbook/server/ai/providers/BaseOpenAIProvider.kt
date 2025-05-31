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

import android.util.Log
import com.google.gson.JsonParser
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

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
            .url("$apiUri/models")
            .addHeader("Authorization", "Bearer ${getApiKey()}")
            .build()


        return runCatching {
            client.newCall(request).execute().use { response ->
                //  if (!response.isSuccessful) throw RuntimeException("Failed to get models: ${response.code}")

                val body = response.body.string() ?: throw RuntimeException("Empty response body")
                val jsonObject = JsonParser.parseString(body).asJsonObject
                val models = mutableListOf<String>()

                jsonObject.getAsJsonArray("data")?.forEach { model ->
                    models.add(model.asJsonObject.get("id").asString)
                }

                return models
            }
        }.onFailure {
            Log.e("Request", "${it.message}", it)
        }.getOrElse { emptyList() }
    }

    /**
     * 发送请求到AI并获取响应
     * @param system 系统角色提示词
     * @param user 用户输入
     * @return AI响应内容
     */
    override suspend fun request(system: String, user: String): String? {
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

        val requestBody = mapOf(
            "model" to getModel(),
            "messages" to messages,
            "temperature" to 0.7
        )

        val request = Request.Builder()
            .url("$apiUri/chat/completions")
            .addHeader("Authorization", "Bearer ${getApiKey()}")
            .addHeader("Content-Type", "application/json")
            .post(gson.toJson(requestBody).toRequestBody("application/json".toMediaTypeOrNull()))
            .build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null

                val body = response.body.string().removeThink()
                val jsonObject = JsonParser.parseString(body).asJsonObject

                return jsonObject
                    .getAsJsonArray("choices")
                    ?.get(0)
                    ?.asJsonObject
                    ?.getAsJsonObject("message")
                    ?.get("content")
                    ?.asString
            }
        }.onFailure {
            Log.e("Request", "${it.message}", it)
        }.getOrElse { null }
    }

} 