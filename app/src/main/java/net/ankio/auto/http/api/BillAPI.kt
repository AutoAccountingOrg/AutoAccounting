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
import org.ezbook.server.db.model.BillInfoModel
import org.ezbook.server.constant.BillState

/**
 * 账单API接口对象，提供与账单相关的所有网络请求操作
 * 所有方法都是挂起函数，需要在协程作用域内调用
 */
object BillAPI {
    /**
     * 添加或更新账单信息
     * @param billInfoModel 账单信息模型
     * @return 服务器响应结果
     */
    suspend fun put(billInfoModel: BillInfoModel) = withContext(Dispatchers.IO) {
        LocalNetwork.request("bill/put", Gson().toJson(billInfoModel))
    }

    /**
     * 删除指定ID的账单
     * @param id 账单ID
     * @return 服务器响应结果
     */
    suspend fun remove(id: Long) = withContext(Dispatchers.IO) {
        LocalNetwork.request("bill/remove?id=$id")
    }

    /**
     * 获取指定ID的账单详情
     * @param id 账单ID
     * @return 账单信息模型，如果获取失败则返回null
     */
    suspend fun get(id: Long): BillInfoModel? = withContext(Dispatchers.IO) {
        val response = LocalNetwork.request("bill/get?id=$id")
        runCatching {
            val json = Gson().fromJson(response, JsonObject::class.java)
            Gson().fromJson(
                json.getAsJsonObject("data"),
                BillInfoModel::class.java
            )
        }.getOrNull()
    }

    /**
     * 获取账单列表
     * @param page 页码
     * @param pageSize 每页数量
     * @param type 账单类型列表
     * @return 账单信息模型列表
     */
    suspend fun list(page: Int, pageSize: Int, type: MutableList<String>): List<BillInfoModel> =
        withContext(Dispatchers.IO) {
            val typeName = listOf(
                BillState.Edited.name,
                BillState.Synced.name,
                BillState.Wait2Edit.name
            ).joinToString()
            val syncType = if (type.isNotEmpty()) type.joinToString() else typeName

            val response =
                LocalNetwork.request("bill/list?page=$page&limit=$pageSize&type=${syncType}")

            runCatching {
                val json = Gson().fromJson(response, JsonObject::class.java)
                Gson().fromJson(
                    json.getAsJsonArray("data"),
                    Array<BillInfoModel>::class.java
                ).toList()
            }.getOrNull() ?: emptyList()
        }

    /**
     * 获取需要同步的账单列表
     * @return 需要同步的账单信息模型列表
     */
    suspend fun sync(): List<BillInfoModel> = withContext(Dispatchers.IO) {
        val response = LocalNetwork.request("bill/sync/list")

        runCatching {
            val json = Gson().fromJson(response, JsonObject::class.java)
            Gson().fromJson(
                json.getAsJsonArray("data"),
                Array<BillInfoModel>::class.java
            ).toList()
        }.getOrNull() ?: emptyList()
    }

    /**
     * 更新账单同步状态
     * @param id 账单ID
     * @param sync 是否同步
     * @return 服务器响应结果
     */
    suspend fun status(id: Long, sync: Boolean) = withContext(Dispatchers.IO) {
        LocalNetwork.request("bill/status?id=$id&sync=$sync")
    }

    /**
     * 获取指定分组下的所有账单
     * @param id 分组ID
     * @return 该分组下的账单信息模型列表
     */
    suspend fun getBillByGroup(id: Long): List<BillInfoModel> = withContext(Dispatchers.IO) {
        val response = LocalNetwork.request("bill/group?id=$id")

        runCatching {
            val json = Gson().fromJson(response, JsonObject::class.java)
            Gson().fromJson(
                json.getAsJsonArray("data"),
                Array<BillInfoModel>::class.java
            ).toList()
        }.getOrNull() ?: emptyList()
    }

    /**
     * 清空所有账单数据
     * @return 服务器响应结果
     */
    suspend fun clear() = withContext(Dispatchers.IO) {
        LocalNetwork.request("bill/clear")
    }

    /**
     * 获取需要编辑的账单列表
     * @return 需要编辑的账单信息模型列表
     */
    suspend fun edit(): List<BillInfoModel> = withContext(Dispatchers.IO) {
        runCatching {
            val response = LocalNetwork.request("bill/edit")
            val json = Gson().fromJson(response, JsonObject::class.java)
            Gson().fromJson(
                json.getAsJsonArray("data"),
                Array<BillInfoModel>::class.java
            ).toList()
        }.getOrNull() ?: emptyList()
    }

    /**
     * 解除账单分组
     * @param id 账单ID
     * @return 服务器响应结果
     */
    suspend fun unGroup(id: Long) = withContext(Dispatchers.IO) {
        LocalNetwork.request("bill/unGroup?id=$id")
    }
}