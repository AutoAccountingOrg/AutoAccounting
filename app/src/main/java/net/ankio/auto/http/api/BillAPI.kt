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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.http.LocalNetwork
import net.ankio.auto.storage.Logger
import org.ezbook.server.tools.runCatchingExceptCancel
import org.ezbook.server.db.model.BillInfoModel
import org.ezbook.server.constant.BillState
import org.ezbook.server.models.SummaryDto
import org.ezbook.server.models.TrendDto
import org.ezbook.server.models.CategoryItemDto
import org.ezbook.server.models.StatsResponse

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

        runCatchingExceptCancel {
            LocalNetwork.post<String>("bill/put", Gson().toJson(billInfoModel)).getOrThrow()
        }.getOrElse {
            Logger.e("put error: ${it.message}", it)

        }
    }

    /**
     * 删除指定ID的账单
     * @param id 账单ID
     * @return 服务器响应结果
     */
    suspend fun remove(id: Long) = withContext(Dispatchers.IO) {

        runCatchingExceptCancel {
            LocalNetwork.post<String>("bill/remove", Gson().toJson(mapOf("id" to id))).getOrThrow()
        }.getOrElse {
            Logger.e("remove error: ${it.message}", it)

        }
    }

    /**
     * 获取指定ID的账单详情
     * @param id 账单ID
     * @return 账单信息模型，如果获取失败则返回null
     */
    suspend fun get(id: Long): BillInfoModel? = withContext(Dispatchers.IO) {

        return@withContext runCatchingExceptCancel {
            val resp = LocalNetwork.get<BillInfoModel>("bill/get?id=$id").getOrThrow()
            resp.data
        }.getOrElse {
            Logger.e("get error: ${it.message}", it)
            null
        }
    }

    /**
     * 获取账单列表
     * @param page 页码
     * @param pageSize 每页数量
     * @param type 账单类型列表
     * @return 账单信息模型列表
     */
    /**
     * 获取账单列表（按月份必填）。
     * [year] 与 [month] 必须提供，否则服务器会返回 400。
     */
    suspend fun list(
        page: Int,
        pageSize: Int,
        type: MutableList<String>,
        year: Int,
        month: Int
    ): List<BillInfoModel> =
        withContext(Dispatchers.IO) {
            val typeName = listOf(
                BillState.Edited.name,
                BillState.Synced.name,
                BillState.Wait2Edit.name
            ).joinToString()
            val syncType = if (type.isNotEmpty()) type.joinToString() else typeName

            return@withContext runCatchingExceptCancel {
                val base = StringBuilder("bill/list?page=").append(page)
                    .append("&limit=").append(pageSize)
                    .append("&type=").append(syncType)
                    .append("&year=").append(year).append("&month=").append(month)
                val resp = LocalNetwork.get<List<BillInfoModel>>(base.toString()).getOrThrow()
                resp.data ?: emptyList()
            }.getOrElse {
                Logger.e("list error: ${it.message}", it)
                emptyList()
            }
        }

    /**
     * 获取需要同步的账单列表
     * @return 需要同步的账单信息模型列表
     */
    suspend fun sync(): List<BillInfoModel> = withContext(Dispatchers.IO) {

        return@withContext runCatchingExceptCancel {
            val resp = LocalNetwork.get<List<BillInfoModel>>("bill/sync/list").getOrThrow()
            resp.data ?: emptyList()
        }.getOrElse {
            Logger.e("sync error: ${it.message}", it)
            emptyList()
        }
    }

    /**
     * 更新账单同步状态
     * @param id 账单ID
     * @param sync 是否同步
     * @return 服务器响应结果
     */
    suspend fun status(id: Long, sync: Boolean) = withContext(Dispatchers.IO) {

        runCatchingExceptCancel {
            LocalNetwork.post<String>(
                "bill/status",
                Gson().toJson(mapOf("id" to id, "sync" to sync))
            ).getOrThrow()
        }.getOrElse {
            Logger.e("status error: ${it.message}", it)

        }
    }

    /**
     * 获取指定分组下的所有账单
     * @param id 分组ID
     * @return 该分组下的账单信息模型列表
     */
    suspend fun getBillByGroup(id: Long): List<BillInfoModel> = withContext(Dispatchers.IO) {

        return@withContext runCatchingExceptCancel {
            val resp = LocalNetwork.get<List<BillInfoModel>>("bill/group?id=$id").getOrThrow()
            resp.data ?: emptyList()
        }.getOrElse {
            Logger.e("getBillByGroup error: ${it.message}", it)
            emptyList()
        }
    }

    /**
     * 清空所有账单数据
     * @return 服务器响应结果
     */
    suspend fun clear() = withContext(Dispatchers.IO) {

        runCatchingExceptCancel {
            LocalNetwork.post<String>("bill/clear").getOrThrow()
        }.getOrElse {
            Logger.e("clear error: ${it.message}", it)

        }
    }

    /**
     * 获取需要编辑的账单列表
     * @return 需要编辑的账单信息模型列表
     */
    suspend fun edit(): List<BillInfoModel> = withContext(Dispatchers.IO) {

        return@withContext runCatchingExceptCancel {
            val resp = LocalNetwork.get<List<BillInfoModel>>("bill/edit").getOrThrow()
            resp.data ?: emptyList()
        }.getOrElse {
            Logger.e("edit error: ${it.message}", it)
            emptyList()
        }
    }

    /**
     * 解除账单去重
     * @param id 账单ID
     * @return 服务器响应结果
     */
    suspend fun unGroup(id: Long) = withContext(Dispatchers.IO) {

        runCatchingExceptCancel {
            LocalNetwork.post<String>("bill/unGroup", Gson().toJson(mapOf("id" to id))).getOrThrow()
        }.getOrElse {
            Logger.e("unGroup error: ${it.message}", it)

        }
    }

    /**
     * 获取指定年月的收支统计
     * @param year 年份
     * @param month 月份（1-12）
     * @return 包含收入和支出总额的Map，如果请求失败则返回null
     */
    suspend fun getMonthlyStats(year: Int, month: Int): Map<String, Double>? =
        withContext(Dispatchers.IO) {

            return@withContext runCatchingExceptCancel {
                val resp = LocalNetwork.post<Map<String, Double>>(
                    "/bill/monthly/stats?year=$year&month=$month",
                    "{}"
                ).getOrThrow()
                resp.data
            }.getOrElse {
                Logger.e("getMonthlyStats error: ${it.message}", it)
                null
            }
        }

    /**
     * 获取指定时间范围的统计数据（服务端计算）。
     */
    suspend fun stats(startTime: Long, endTime: Long): StatsResponse? =
        withContext(Dispatchers.IO) {

            return@withContext runCatchingExceptCancel {
                val url = "bill/stats?start=$startTime&end=$endTime"
                val resp = LocalNetwork.get<StatsResponse>(url).getOrThrow()
                resp.data
            }.getOrElse {
                Logger.e("stats error: ${it.message}", it)
                null
            }
        }
}