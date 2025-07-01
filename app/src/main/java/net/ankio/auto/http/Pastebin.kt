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

package net.ankio.auto.http

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.storage.Logger
import net.ankio.auto.utils.DateUtils

/**
 * 上传数据到Pastebin,并设置2个月有效期
 */
object Pastebin {
    suspend fun add(data: String): Pair<String, String> = withContext(Dispatchers.IO) {
        val request = RequestsUtils()
        request.addHeader("Accept","application/json")
       val (code,body) = request.form(
           "https://bin.ankio.net/", hashMapOf(
               "data" to data,
               "ttl" to (60 * 24 * 60).toString()
            )
        )

        val json = JsonParser.parseString(body).asJsonObject
        val url = json.get("data").asString
        val timeout = DateUtils.stampToDate(DateUtils.twoMonthsLater())
        Pair(url,timeout)
    }
}