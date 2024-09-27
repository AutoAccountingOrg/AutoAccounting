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
import org.ezbook.server.Server
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.BillInfoModel
import org.ezbook.server.db.model.RuleModel

abstract class BaseAi {
    open var aiName = ""
    val prompt = """
You are an AI assistant tasked with generating a bill info JSON based on the provided raw data. Your goal is to create a clear, concise, and informative bill info JSON that follows best practices.

Instructions:
1. Analyze the provided raw data.
2. Generate a bill info JSON following this format:
   ```json
   {
       "accountNameFrom": "",
       "accountNameTo": "",
       "app": "",
       "auto": false,
       "bookName": "默认账本",
       "cateName": "",
       "channel": "",
       "currency": "",
       "extendData": "",
       "fee": 0,
       "groupId": -1,
       "id": 0,
       "money": 0.00,
       "remark": "",
       "ruleName": "{aiName} 识别",
       "shopItem": "",
       "shopName": "",
       "state": "",
       "time": {time},
       "type": ""
   }
   ```
3. Explanation of JSON Fields:
   - type: Must be a string; one of `Transfer`, `Income`, or `Expend`.
   - time: Extract from raw data; Must be a 13-digit integer (milliseconds since epoch); Don't change it if extraction fails.
   - state: Always set to `Wait2Edit`.
   - shopName/shopItem: Extract from raw data; set to empty string if extraction fails.
   - remark: Always an empty string.
   - ruleName: Don't change it.
   - money/fee: Double-precision number; set to 0 if extraction fails.
   - currency: Extract from raw data; set to `CNY` if extraction fails.
   - cateName: Choose from Category JSON, distinguishing between income and expenses.
   - accountNameFrom/To: Extract for Transfer type; set to empty string if extraction fails.
   - Fields not to modify: `id`, `groupId`, `extendData`, `channel`, `bookName`, `auto`, `app` , `ruleName`.
4. If you can't analyze anything, export an empty JSON object: `{}`

Output:
- Provide only the bill info JSON, without additional explanation or commentary.

Example:
{
    "accountNameFrom": "",
    "accountNameTo": "",
    "app": "",
    "auto": false,
    "bookName": "默认账本",
    "cateName": "",
    "channel": "",
    "currency": "",
    "extendData": "",
    "fee": 0,
    "groupId": -1,
    "id": 0,
    "money": 0.00,
    "remark": "",
    "ruleName": "",
    "shopItem": "",
    "shopName": "",
    "state": "",
    "time": 0,
    "type": ""
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

    var apiKey = ""


    abstract  fun createApiKeyUri(): String


     fun getConversations(data: String): Pair<String,String>  {
         Server.isRunOnMainThread()
        val category = Db.get().categoryDao().all().map {
            Pair(it.name, it.type)
        }
        apiKey = Db.get().settingDao().query(Setting.API_KEY)?.value ?: ""

      if (apiKey.isEmpty()) throw RuntimeException("api key is empty")

     return Pair(
         prompt.replace("{aiName}",aiName).replace("{time}",System.currentTimeMillis().toString()),
         input.replace("{data}", data).replace("{category}", Gson().toJson(category)))
    }

    abstract  fun request(data: String): BillInfoModel?


}