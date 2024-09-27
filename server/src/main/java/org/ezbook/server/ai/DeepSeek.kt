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

package org.ezbook.server.ai

import com.google.gson.Gson
import com.google.gson.JsonParser
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.ezbook.server.Server
import org.ezbook.server.constant.AIModel
import org.ezbook.server.db.model.BillInfoModel
import java.util.concurrent.TimeUnit

class DeepSeek:BaseAi() {
    override fun createApiKeyUri(): String {
        return "https://platform.deepseek.com/api_keys"
    }

    override var aiName: String
        get() = AIModel.DeepSeek.name
        set(value) {}
    override fun request(data: String): BillInfoModel? {
        val (system,user) = getConversations(data)
        //curl https://api.deepseek.com/chat/completions \
        //  -H "Content-Type: application/json" \
        //  -H "Authorization: Bearer <DeepSeek API Key>" \
        //  -d '{
        //        "model": "deepseek-chat",
        //        "messages": [
        //          {"role": "system", "content": "You are a helpful assistant."},
        //          {"role": "user", "content": "Hello!"}
        //        ],
        //        "stream": false
        //      }'
        val url =
            "https://api.deepseek.com/chat/completions"

        val client = OkHttpClient.Builder().readTimeout(60, TimeUnit.SECONDS).build()

        val json =
            Gson().toJson(
                mapOf(
                    "model" to "deepseek-chat",
                    "messages" to listOf(
                        mapOf(
                            "role" to "system",
                            "content" to "$system",
                        ),
                        mapOf(
                            "role" to "user",
                            "content" to "$user",
                        ),
                    ),
                    "stream" to false
                )
            )

        Server.log("Request Data: $json")


        val request = Request.Builder()
            .url(url)
            .header("Authorization","Bearer $apiKey")
            .post(json.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()?:""
        Server.log("Request Body: $responseBody")
        if (!response.isSuccessful) {
            Server.log(Throwable("Unexpected response code: ${response.code}"))
            response.close()
        } else {
            return runCatching {
                /**
                 * {
                 *     "id": "cb34e519-bfb0-4705-8470-a05ee3321dc0",
                 *     "object": "chat.completion",
                 *     "created": 1727459706,
                 *     "model": "deepseek-chat",
                 *     "choices": [
                 *         {
                 *             "index": 0,
                 *             "message": {
                 *                 "role": "assistant",
                 *                 "content": "Hello! How can I assist you today?"
                 *             },
                 *             "logprobs": null,
                 *             "finish_reason": "stop"
                 *         }
                 *     ],
                 *     "usage": {
                 *         "prompt_tokens": 11,
                 *         "completion_tokens": 9,
                 *         "total_tokens": 20,
                 *         "prompt_cache_hit_tokens": 0,
                 *         "prompt_cache_miss_tokens": 11
                 *     },
                 *     "system_fingerprint": "fp_1c141eb703"
                 * }
                 */

                val jsonObject = JsonParser.parseString(responseBody).asJsonObject
                val choices = jsonObject.getAsJsonArray("choices")

                val firstChoice = choices[0].asJsonObject
                val reason = firstChoice.get("finish_reason").asString
                Server.logW("AI Finish Reason: $reason")
                val message = firstChoice.getAsJsonObject("message")
                val content = message.get("content").asString.replace("```json","").replace("```","").trim()
                Gson().fromJson(content, BillInfoModel::class.java)
            }.onFailure {
                Server.log(it)
            }.getOrNull()
        }
        return null
    }
}