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
import org.ezbook.server.db.model.BillInfoModel

class QWen : BaseAi(){
    override fun createApiKeyUri(): String {
       return "https://help.aliyun.com/zh/model-studio/developer-reference/get-api-key?spm=a2c4g.11186623.0.0.74b04823nZ57nT"
    }

    override fun request(data: String): BillInfoModel? {
        val (system,user) = getConversations(data)
        //curl -X POST https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions \
        //-H "Authorization: Bearer $DASHSCOPE_API_KEY" \
        //-H "Content-Type: application/json" \
        //-d '{
        //    "model": "qwen-plus",
        //    "messages": [
        //        {
        //            "role": "system",
        //            "content": "You are a helpful assistant."
        //        },
        //        {
        //            "role": "user",
        //            "content": "你是谁？"
        //        }
        //    ]
        //}'
        val url =
            "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"

        val client = OkHttpClient()

        val json =
            Gson().toJson(
                mapOf(
                    "model" to "qwen-turbo",
                    "messages" to listOf(
                        mapOf(
                            "role" to "system",
                            "content" to "$system",
                        ),
                        mapOf(
                            "role" to "user",
                            "content" to "$user",
                        ),
                    )
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
                 *   "choices": [
                 *     {
                 *       "message": {
                 *         "role": "assistant",
                 *         "content": "我是通义千问，由阿里云开发的AI助手。我被设计用来回答各种问题、提供信息和与用户进行对话。有什么我可以帮助你的吗？"
                 *       },
                 *       "finish_reason": "stop",
                 *       "index": 0,
                 *       "logprobs": null
                 *     }
                 *   ],
                 *   "object": "chat.completion",
                 *   "usage": {
                 *     "prompt_tokens": 22,
                 *     "completion_tokens": 36,
                 *     "total_tokens": 58
                 *   },
                 *   "created": 1721044596,
                 *   "system_fingerprint": null,
                 *   "model": "qwen-plus",
                 *   "id": "chatcmpl-94149c5a-137f-9b87-b2c8-61235e85f540"
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