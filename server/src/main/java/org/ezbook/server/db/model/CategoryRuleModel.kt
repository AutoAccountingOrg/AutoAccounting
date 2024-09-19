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
class CategoryRuleModel {
    fun toJson(): String {
        return Gson().toJson(this)
    }

    @PrimaryKey(autoGenerate = true)
    var id = 0L

    var enabled = true // 是否启用该规则
    var sort = 0 // 排序
    var creator = "user" // system为系统创建, user为用户创建

    var js = "" // js代码
    // var text = ""

    var element: String = "" //数据json

    companion object {
        suspend fun list(page: Int = 1, limit: Int = 10): List<CategoryRuleModel> = withContext(
            Dispatchers.IO
        ) {
            val response = Server.request("category/rule/list?page=$page&limit=$limit")

            runCatching {
                val json = Gson().fromJson(response, JsonObject::class.java)
                Gson().fromJson(
                    json.getAsJsonArray("data"),
                    Array<CategoryRuleModel>::class.java
                ).toList()
            }.getOrNull() ?: emptyList()
        }

        suspend fun put(model: CategoryRuleModel): JsonObject = withContext(Dispatchers.IO) {
            val response = Server.request("category/rule/put", Gson().toJson(model))


            runCatching {
                val json = Gson().fromJson(response, JsonObject::class.java)
                Gson().fromJson(
                    json.getAsJsonArray("data"),
                    JsonObject::class.java
                )
            }.getOrNull() ?: JsonObject()
        }

        suspend fun remove(id: Long) = withContext(Dispatchers.IO) {
            Server.request("category/rule/delete?id=$id")
        }


    }
}