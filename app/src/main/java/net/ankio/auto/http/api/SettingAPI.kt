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

package net.ankio.auto.http.api

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.http.LocalNetwork

object SettingAPI {
    /**
     * 获取设置
     */
    suspend fun get(key: String, default: String): String = withContext(Dispatchers.IO) {
        val response = LocalNetwork.post("setting/get?key=$key")

        runCatching {
            val json = Gson().fromJson(response, JsonObject::class.java)
            json.get("data").asString
        }.getOrNull() ?: default
    }

    /**
     * 设置
     */
    suspend fun set(key: String, value: String) = withContext(Dispatchers.IO) {
        LocalNetwork.post("setting/set?key=$key", value)
    }

    suspend fun clearDatabase() = withContext(Dispatchers.IO) {
        LocalNetwork.post("db/clear")
    }

    /**
     * 获取所有设置项列表
     */
    suspend fun list(): List<org.ezbook.server.db.model.SettingModel> =
        withContext(Dispatchers.IO) {
            val response = LocalNetwork.post("setting/list")

            runCatching {
                val json = Gson().fromJson(response, JsonObject::class.java)
                Gson().fromJson(
                    json.getAsJsonArray("data"),
                    Array<org.ezbook.server.db.model.SettingModel>::class.java
                ).toList()
            }.getOrNull() ?: emptyList()
    }
}