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
You are an AI assistant tasked with generating a bill info JSON based on the provided raw data. Your goal is to create a clear, concise, and informative bill info JSON that follows best practices.

Instructions:
1. Analyze the provided raw data.
2. Generate a bill info JSON following this format:
   ```json
   {
       "accountNameFrom": "",
       "accountNameTo": "",
       "cateName": "",
       "currency": "",   
       "fee": 0,
       "money": 0.00,  
       "shopItem": "",
       "shopName": "",
       "type": "",
       "time": {time},
   }
   ```
3. Explanation of JSON Fields:
   - type: Must be a string; one of `Transfer`, `Income`, or `Expend`.
   - time: Extract from raw data; Must be a 13-digit integer (milliseconds since epoch); Don't change it if extraction fails.
   - shopName/shopItem: Extract from raw data; set to empty string if extraction fails.
   - remark: Always an empty string.
   - money/fee: Double-precision number; set to 0 if extraction fails.
   - currency: Extract from raw data; set to `CNY` if extraction fails.
   - cateName: Choose from Category JSON, distinguishing between income and expenses.
   - accountNameFrom/To: Extract for Transfer type; set to empty string if extraction fails.
4. If you can't analyze anything, export an empty JSON object: `{}`

Output:
- Provide only the bill info JSON, without additional explanation or commentary.

Example:
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
    """.trimIndent()

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

    fun getConversations(data: String): Pair<String,String>  {
         Server.isRunOnMainThread()
        val category = Db.get().categoryDao().all().map {
            Pair(it.name, it.type)
        }
        apiKey = Db.get().settingDao().query("${Setting.API_KEY}_$name")?.value ?: ""

      if (apiKey.isEmpty()) throw RuntimeException("api key is empty")

     return Pair(
         prompt.replace("{aiName}",name).replace("{time}",System.currentTimeMillis().toString()),
         input.replace("{data}", data).replace("{category}", Gson().toJson(category)))
    }

    open fun request(data: String): BillInfoModel?{
        val (system,user) = getConversations(data)
        var url = api
        if(!url.contains("v1/chat/completions")){
            url = "$api/v1/chat/completions"
        }
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