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

package org.ezbook.server.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ezbook.server.Server

@Entity
class SettingModel {
    @PrimaryKey(autoGenerate = true)
    var id = 0
    var key = ""
    var value = ""

    companion object {
        /**
         * 获取设置
         */
        suspend fun get(key: String, default: String): String = withContext(Dispatchers.IO) {
            val response = Server.request("setting/get?key=$key")

            runCatching {
                val json = Gson().fromJson(response, JsonObject::class.java)
                json.get("data").asString
            }.getOrNull() ?: default
        }

        /**
         * 设置
         */
        suspend fun set(key: String, value: String) = withContext(Dispatchers.IO) {
            Server.request("setting/set?key=$key", value)
        }

       suspend fun clearDatabase() = withContext(Dispatchers.IO) {
            Server.request("db/clear")
        }
    }
}