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
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.BillInfoModel
import java.util.concurrent.TimeUnit

abstract class BaseAi {
    private val prompt = """
You are an AI assistant that extracts specific bill information from raw data. Your task is to identify and extract ONLY the exact transaction details present in the raw data.

CRITICAL REQUIREMENT:
- The "accountNameFrom" field MUST be extracted from the raw data
- If "accountNameFrom" cannot be identified, return {} to indicate parsing failure
- Do NOT process promotional or advertising content

Instructions:
1. Extract ONLY information that is explicitly present in the raw data
2. Do NOT generate or infer any information that isn't clearly stated
3. Generate a bill info JSON with these exact fields:
   {
       "accountNameFrom": "",  // REQUIRED: Source account - must be extracted
       "accountNameTo": "",    // Destination account for transfers
       "cateName": "",        // Must match one from provided Category JSON
       "currency": "",        // Use CNY if not specified
       "fee": 0,             // Transaction fee if present
       "money": 0.00,        // Transaction amount
       "shopItem": "",       // Specific item purchased
       "shopName": "",       // Merchant name
       "type": "",          // Must be: "Transfer", "Income", or "Expend"
       "time": {time}       // Original timestamp, 13-digit milliseconds
   }

Important Rules:
- If accountNameFrom cannot be extracted, return {} immediately
- Leave other fields empty ("") if the information is not explicitly present
- Do not attempt to guess or infer missing information
- Only use category names that exactly match the provided Category JSON

Output Format:
- Return ONLY the JSON object, no explanations or comments

Output Example:
{
       "accountNameFrom": "支付宝余额",
       "accountNameTo": "",
       "cateName": "购物",
       "currency": "CNY",   
       "fee": 0,
       "money": 10.00,  
       "shopItem": "上好佳薯片",
       "shopName": "钱塘江超市",
       "type": "Expend",
       "time": 1630512000000,
}
"""".trimIndent()

    val input = """
Input:
- Raw Data: 
  ```
  {data}
  ```
- Category JSON:
  ```json
  {category}
  ```
    """.trimIndent()

    /**
     * API Key
     */
    var apiKey = ""

    /**
     * API地址
     */
    abstract var api:String

    /**
     * 所用的模型
     */
    abstract var model:String

    /**
     * AI名称
     */
    abstract var name:String

    /**
     * 申请Key的地址
     */
    abstract var createKeyUri:String

    suspend fun getConversations(data: String): Pair<String,String>  {
        val category = Db.get().categoryDao().all().map {
            Pair(it.name, it.type)
        }
        apiKey = Db.get().settingDao().query("${Setting.API_KEY}_$name")?.value ?: ""

      if (apiKey.isEmpty()) throw RuntimeException("api key is empty")

     return Pair(
         prompt.replace("{aiName}",name).replace("{time}",System.currentTimeMillis().toString()),
         input.replace("{data}", data).replace("{category}", Gson().toJson(category)))
    }

    open suspend fun request(data: String): BillInfoModel?{
        val (system,user) = getConversations(data)
        val url = api
        val client = OkHttpClient.Builder().readTimeout(60, TimeUnit.SECONDS).build()

        val json =
            Gson().toJson(
                mapOf(
                    "model" to model,
                    "messages" to listOf(
                        mapOf(
                            "role" to "system",
                            "content" to system,
                        ),
                        mapOf(
                            "role" to "user",
                            "content" to user,
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