/*
 * Copyright (C) 2025 ankio(ankio@ankio.net)
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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.http.LocalNetwork
import net.ankio.auto.storage.Logger
import org.ezbook.server.tools.runCatchingExceptCancel
import org.ezbook.server.db.model.AssetsMapModel

/**
 * 资产映射相关API接口，封装了对后端资产映射的增删改查操作。
 */
object AssetsMapAPI {
    /**
     * 分页获取资产映射列表。
     * @param page 页码，从1开始
     * @param pageSize 每页数量
     * @param searchKeyword 搜索关键词，可为空
     * @return 资产映射模型列表
     */
    suspend fun list(page: Int, pageSize: Int, searchKeyword: String = ""): List<AssetsMapModel> =
        withContext(Dispatchers.IO) {
            val searchParam =
                if (searchKeyword.isNotEmpty()) "&search=${Uri.encode(searchKeyword)}" else ""

            return@withContext runCatchingExceptCancel {
                val resp =
                    LocalNetwork.get<List<AssetsMapModel>>("assets/map/list?page=$page&limit=$pageSize$searchParam")
                        .getOrThrow()
                resp.data ?: emptyList()
            }.getOrElse {
                Logger.e("list error: ${it.message}", it)
                emptyList()
            }
        }

    /**
     * 获取未映射的资产列表。
     * @return 未映射的资产模型列表
     */
    suspend fun empty(): List<AssetsMapModel> = withContext(Dispatchers.IO) {

        return@withContext runCatchingExceptCancel {
            val resp = LocalNetwork.get<List<AssetsMapModel>>("assets/map/empty").getOrThrow()
            resp.data ?: emptyList()
        }.getOrElse {
            Logger.e("empty error: ${it.message}", it)
            emptyList()
        }
    }

    /**
     * 新增或更新资产映射。
     * @param model 资产映射模型
     * @return 映射ID（失败返回 0）
     */
    suspend fun put(model: AssetsMapModel): Long = withContext(Dispatchers.IO) {

        return@withContext runCatchingExceptCancel {
            val resp =
                LocalNetwork.post<Long>("assets/map/put", Gson().toJson(model)).getOrThrow()
            resp.data ?: 0L
        }.getOrElse {
            Logger.e("put error: ${it.message}", it)
            0L
        }
    }

    /**
     * 根据ID删除资产映射。
     * @param id 资产映射ID
     */
    suspend fun remove(id: Long) = withContext(Dispatchers.IO) {

        runCatchingExceptCancel {
            LocalNetwork.post<Long>("assets/map/delete", Gson().toJson(mapOf("id" to id)))
                .getOrThrow()
        }.getOrElse {
            Logger.e("remove error: ${it.message}", it)

        }
    }

    /**
     * 根据账户名称获取资产映射。
     * @param account 账户名称
     * @return 对应的资产映射模型，若不存在则为null
     */
    suspend fun getByName(account: String): AssetsMapModel? = withContext(Dispatchers.IO) {

        return@withContext runCatchingExceptCancel {
            val resp =
                LocalNetwork.get<AssetsMapModel>("assets/map/get?name=${Uri.encode(account)}")
                    .getOrThrow()
            resp.data
        }.getOrElse {
            Logger.e("getByName error: ${it.message}", it)
            null
        }
    }

    /**
     * 重新应用资产映射到历史数据。
     * 此操作会在服务端执行，对所有历史账单重新应用当前的资产映射规则。
     * @return 是否成功（true 表示任务已触发）
     */
    suspend fun reapply(): Boolean = withContext(Dispatchers.IO) {

        return@withContext runCatchingExceptCancel {
            val resp = LocalNetwork.post<Boolean>("assets/map/reapply", "{}").getOrThrow()
            resp.data ?: false
        }.getOrElse {
            Logger.e("reapply error: ${it.message}", it)
            false
        }
    }

}