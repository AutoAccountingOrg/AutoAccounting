/*
 * Copyright (C) 2024 ankio(ankio@ankio.net)
 * Licensed under the Apache License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package net.ankio.auto.http.api

import android.net.Uri
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.http.LocalNetwork
import org.ezbook.server.db.model.TagModel

/**
 * 标签相关API接口，封装了对后端标签的增删改查操作。
 */
object TagAPI {
    /**
     * 分页获取标签列表。
     * @param page 页码，从1开始
     * @param pageSize 每页数量
     * @param searchKeyword 搜索关键词，可为空
     * @return 标签模型列表
     */
    suspend fun list(page: Int, pageSize: Int, searchKeyword: String = ""): List<TagModel> =
        withContext(Dispatchers.IO) {
            val searchParam =
                if (searchKeyword.isNotEmpty()) "&search=${Uri.encode(searchKeyword)}" else ""
            val response =
                LocalNetwork.get("tag/list?page=$page&limit=$pageSize$searchParam")

            runCatching {
                val json = Gson().fromJson(response, JsonObject::class.java)
                Gson().fromJson(
                    json.getAsJsonArray("data"),
                    Array<TagModel>::class.java
                ).toList()
            }.getOrNull() ?: emptyList()
        }

    /**
     * 获取所有标签列表（不分页）。
     * @return 所有标签模型列表
     */
    suspend fun all(): List<TagModel> = withContext(Dispatchers.IO) {
        val response = LocalNetwork.get("tag/list?limit=0")
        runCatching {
            val json = Gson().fromJson(response, JsonObject::class.java)
            Gson().fromJson(
                json.getAsJsonArray("data"),
                Array<TagModel>::class.java
            ).toList()
        }.getOrNull() ?: emptyList()
    }

    /**
     * 新增或更新标签。
     * @param model 标签模型
     * @return 后端返回的操作结果
     */
    suspend fun put(model: TagModel): JsonObject = withContext(Dispatchers.IO) {
        val response = LocalNetwork.post("tag/put", Gson().toJson(model))

        runCatching {
            Gson().fromJson(response, JsonObject::class.java)
        }.getOrNull() ?: JsonObject()
    }

    /**
     * 根据ID删除标签。
     * @param id 标签ID
     */
    suspend fun remove(id: Long) = withContext(Dispatchers.IO) {
        LocalNetwork.post("tag/delete", Gson().toJson(mapOf("id" to id)))
    }

    /**
     * 根据ID获取标签。
     * @param id 标签ID
     * @return 对应的标签模型，若不存在则为null
     */
    suspend fun getById(id: Long): TagModel? = withContext(Dispatchers.IO) {
        val response = LocalNetwork.get("tag/get?id=$id")

        runCatching {
            val json = Gson().fromJson(response, JsonObject::class.java)
            Gson().fromJson(
                json.getAsJsonObject("data"),
                TagModel::class.java
            )
        }.getOrNull()
    }

    /**
     * 获取标签总数。
     * @return 标签总数
     */
    suspend fun count(): Int = withContext(Dispatchers.IO) {
        val response = LocalNetwork.get("tag/count")

        runCatching {
            val json = Gson().fromJson(response, JsonObject::class.java)
            json.getAsJsonPrimitive("data").asInt
        }.getOrNull() ?: 0
    }

    /**
     * 检查标签名称是否可用。
     * @param name 标签名称
     * @param excludeId 排除的标签ID（用于编辑时排除自己）
     * @return 是否可用
     */
    suspend fun checkNameAvailable(name: String, excludeId: Long = 0): Boolean =
        withContext(Dispatchers.IO) {
            val response = LocalNetwork.get("tag/check?name=${Uri.encode(name)}&id=$excludeId")

            runCatching {
                val json = Gson().fromJson(response, JsonObject::class.java)
                json.getAsJsonPrimitive("data").asBoolean
            }.getOrNull() ?: false
        }
}
