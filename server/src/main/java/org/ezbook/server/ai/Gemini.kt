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

class Gemini : BaseAi() {
    override var aiName = AIModel.Gemini.name
    override  fun createApiKeyUri(): String {
        return "https://aistudio.google.com/app/apikey"
    }


    override  fun request(data: String): BillInfoModel? {
        val (system,user) = getConversations(data)
        val url =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent?key=$apiKey"

        val client = OkHttpClient.Builder().readTimeout(60, TimeUnit.SECONDS).build()

        val json =
            Gson().toJson(
                mapOf(
                    "contents" to listOf(
                        mapOf(
                            "parts" to
                                    listOf(
                                        mapOf(
                                            "text" to "$user \n\n $system",
                                        )
                                    )
                        ),
                    )
                )
            )

        Server.log("Request Data: $json")


        val request = Request.Builder()
            .url(url)
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
                val jsonObject = JsonParser.parseString(responseBody).asJsonObject
                val candidates = jsonObject.getAsJsonArray("candidates")

                val firstCandidate = candidates[0].asJsonObject
               val reason = firstCandidate.get("finishReason").asString
               Server.logW("AI Finish Reason: $reason")
                val content = firstCandidate.getAsJsonObject("content")
                val parts = content.getAsJsonArray("parts")
                val text = parts[0].asJsonObject.get("text").asString.replace("```json","").replace("```","").trim()
               Gson().fromJson(text, BillInfoModel::class.java)
            }.onFailure {
                Server.log(it)
            }.getOrNull()
        }
        return null
    }

}