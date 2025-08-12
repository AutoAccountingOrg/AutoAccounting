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
import org.ezbook.server.db.model.AppDataModel

/**
 * 应用数据API接口
 * 提供应用数据的增删改查操作，包括数据列表查询、清空、添加、删除等功能
 * 所有操作都在IO线程中执行，确保不阻塞主线程
 */
object AppDataAPI {
    /**
     * 根据条件查询应用数据列表
     * 支持分页查询和多种筛选条件
     *
     * @param app 应用包名或应用标识，用于筛选特定应用的数据
     * @param type 数据类型，用于筛选特定类型的数据
     * @param match 是否匹配模式，true表示精确匹配，false表示模糊匹配
     * @param page 页码，从1开始计数
     * @param limit 每页数量，限制返回的数据条数
     * @param search 搜索关键词，用于在数据中进行文本搜索
     * @return 返回符合条件的AppDataModel列表，如果查询失败则返回空列表
     */
    suspend fun list(
        app: String,
        type: String,
        match: Boolean?,
        page: Int,
        limit: Int,
        search: String
    ): List<AppDataModel> = withContext(Dispatchers.IO) {
        // 构建查询URL，对搜索关键词进行URL编码以防止特殊字符问题
        val matchParam = match?.toString()?.let { "&match=$it" } ?: ""
        val response = LocalNetwork.get(
            "data/list?page=$page&limit=$limit&app=$app&type=$type$matchParam&search=${
                Uri.encode(
                    search
                )
            }"
        )

        // 安全地解析JSON响应，如果解析失败则返回空列表
        runCatching {
            val json = Gson().fromJson(response, JsonObject::class.java)
            Gson().fromJson(
                json.getAsJsonArray("data"),
                Array<AppDataModel>::class.java
            ).toList()
        }.getOrNull() ?: emptyList()
    }

    /**
     * 清空所有应用数据
     * 此操作会删除数据库中的所有应用数据记录，请谨慎使用
     *
     * @return 返回服务器响应结果
     */
    suspend fun clear() = withContext(Dispatchers.IO) {
        LocalNetwork.post("data/clear")
    }

    /**
     * 添加或更新应用数据
     * 如果数据已存在则更新，不存在则新增
     *
     * @param data 要添加或更新的应用数据模型
     * @return 返回服务器响应结果
     */
    suspend fun put(data: AppDataModel) = withContext(Dispatchers.IO) {
        // 将数据模型转换为JSON格式发送到服务器
        LocalNetwork.post("data/put", Gson().toJson(data))
    }

    /**
     * 根据ID删除指定的应用数据
     *
     * @param id 要删除的数据记录的唯一标识ID
     * @return 返回服务器响应结果
     */
    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        LocalNetwork.post("data/delete", Gson().toJson(mapOf("id" to id)))
    }

    /**
     * 获取所有应用的统计信息
     * 返回系统中所有应用的相关数据统计
     *
     * @return 返回包含应用统计信息的JsonObject，如果获取失败则返回空的JsonObject
     */
    suspend fun apps(): JsonObject = withContext(Dispatchers.IO) {
        val response = LocalNetwork.get("data/apps")

        // 安全地解析JSON响应，提取data字段中的应用统计信息
        runCatching {
            val json = Gson().fromJson(response, JsonObject::class.java)
            json.getAsJsonObject("data")
        }.getOrNull() ?: JsonObject()
    }
}