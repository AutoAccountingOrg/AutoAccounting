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
        LocalNetwork.post("bill/put", Gson().toJson(billInfoModel))
    }

    /**
     * 删除指定ID的账单
     * @param id 账单ID
     * @return 服务器响应结果
     */
    suspend fun remove(id: Long) = withContext(Dispatchers.IO) {
        LocalNetwork.post("bill/remove", Gson().toJson(mapOf("id" to id)))
    }

    /**
     * 获取指定ID的账单详情
     * @param id 账单ID
     * @return 账单信息模型，如果获取失败则返回null
     */
    suspend fun get(id: Long): BillInfoModel? = withContext(Dispatchers.IO) {
        val response = LocalNetwork.get("bill/get?id=$id")
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
                LocalNetwork.get("bill/list?page=$page&limit=$pageSize&type=${syncType}")

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
        val response = LocalNetwork.get("bill/sync/list")

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
        LocalNetwork.post("bill/status", Gson().toJson(mapOf("id" to id, "sync" to sync)))
    }

    /**
     * 获取指定分组下的所有账单
     * @param id 分组ID
     * @return 该分组下的账单信息模型列表
     */
    suspend fun getBillByGroup(id: Long): List<BillInfoModel> = withContext(Dispatchers.IO) {
        val response = LocalNetwork.get("bill/group?id=$id")

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
        LocalNetwork.post("bill/clear")
    }

    /**
     * 获取需要编辑的账单列表
     * @return 需要编辑的账单信息模型列表
     */
    suspend fun edit(): List<BillInfoModel> = withContext(Dispatchers.IO) {
        runCatching {
            val response = LocalNetwork.get("bill/edit")
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
        LocalNetwork.post("bill/unGroup", Gson().toJson(mapOf("id" to id)))
    }

    /**
     * 获取指定年月的收支统计
     * @param year 年份
     * @param month 月份（1-12）
     * @return 包含收入和支出总额的Map，如果请求失败则返回null
     */
    suspend fun getMonthlyStats(year: Int, month: Int): Map<String, Double>? =
        withContext(Dispatchers.IO) {
            val response = LocalNetwork.post("/bill/monthly/stats?year=$year&month=$month", "{}")
            runCatching {
                val json = Gson().fromJson(response, JsonObject::class.java)
                val data = json.getAsJsonObject("data")
                mapOf(
                    "income" to data.get("income").asDouble,
                    "expense" to data.get("expense").asDouble
                )
            }.getOrNull()
        }


    /**
     * 获取账单摘要字符串（服务端生成）
     * @param startTime 开始时间戳（毫秒）
     * @param endTime 结束时间戳（毫秒）
     * @param periodName 周期名称
     * @return 格式化的摘要字符串，如果获取失败则返回null
     */
    suspend fun getBillSummary(startTime: Long, endTime: Long, periodName: String): String? =
        withContext(Dispatchers.IO) {
            val response =
                LocalNetwork.get("bill/summary?start=$startTime&end=$endTime&period=$periodName")
            runCatching {
                val json = Gson().fromJson(response, JsonObject::class.java)
                json.get("data").asString
            }.getOrNull()
        }
}