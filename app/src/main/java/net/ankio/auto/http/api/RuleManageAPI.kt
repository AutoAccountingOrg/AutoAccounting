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

import android.net.Uri
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.http.LocalNetwork
import org.ezbook.server.db.model.RuleModel

object RuleManageAPI {
    /**
     * 根据条件查询
     * @param app 应用
     * @param type 类型
     * @param page 页码
     * @param limit 每页数量
     * @return 规则列表
     */
    suspend fun list(
        app: String,
        type: String,
        creator: String,
        page: Int,
        limit: Int,
        search: String = ""
    ): List<RuleModel> = withContext(Dispatchers.IO) {
        val response = LocalNetwork.get(
            "rule/list?page=$page&limit=$limit&app=$app&creator=${creator}&type=$type&search=${
                Uri.encode(search)
            }"
        )

        runCatching {
            val json = Gson().fromJson(response, JsonObject::class.java)
            Gson().fromJson(json.getAsJsonArray("data"), Array<RuleModel>::class.java).toList()
        }.getOrNull() ?: emptyList()
    }

    /**
     * 获取所有系统规则
     */
    suspend fun system(name: String): RuleModel? = withContext(Dispatchers.IO) {
        val response = LocalNetwork.get("rule/system?name=${Uri.encode(name)}")

        runCatching {
            val json = Gson().fromJson(response, JsonObject::class.java)
            Gson().fromJson(json.getAsJsonObject("data"), RuleModel::class.java)
        }.getOrNull()
    }

    suspend fun deleteSystemRule() = withContext(Dispatchers.IO) {
        LocalNetwork.post("rule/deleteSystemRule")
    }

    suspend fun put(rule: RuleModel) = withContext(Dispatchers.IO) {
        LocalNetwork.post("rule/put", Gson().toJson(rule))
    }

    /**
     * 添加规则
     */
    suspend fun add(rule: RuleModel): Int = withContext(Dispatchers.IO) {
        val response = LocalNetwork.post("rule/add", Gson().toJson(rule))
        runCatching {
            val json = Gson().fromJson(response, JsonObject::class.java)
            json.get("data").asInt
        }.getOrDefault(0)
    }

    /**
     * 更新规则
     */
    suspend fun update(rule: RuleModel) = withContext(Dispatchers.IO) {
        LocalNetwork.post("rule/update", Gson().toJson(rule))
    }

    /**
     * 删除规则
     */
    suspend fun delete(id: Int) = withContext(Dispatchers.IO) {
        LocalNetwork.post("rule/delete", Gson().toJson(mapOf("id" to id)))
    }

    /**
     * 获取app列表
     */
    suspend fun apps(): JsonObject = withContext(Dispatchers.IO) {
        val response = LocalNetwork.get("rule/apps")

        runCatching {
            val json = Gson().fromJson(response, JsonObject::class.java)
            json.getAsJsonObject("data")
        }.getOrNull() ?: JsonObject()
    }
}