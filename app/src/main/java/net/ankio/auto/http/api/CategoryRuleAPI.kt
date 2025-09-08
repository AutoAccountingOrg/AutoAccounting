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
import net.ankio.auto.storage.Logger
import org.ezbook.server.tools.runCatchingExceptCancel
import org.ezbook.server.db.model.CategoryRuleModel

/**
 * 分类规则API客户端
 * 该对象提供了与本地网络API的分类规则端点进行交互的方法
 */
object CategoryRuleAPI {
    /**
     * 获取分类规则列表，支持分页
     *
     * @param page 页码（默认：1）
     * @param limit 每页最大条目数（默认：10）
     * @return 返回[CategoryRuleModel]对象列表
     */
    suspend fun list(page: Int = 1, limit: Int = 10): List<CategoryRuleModel> =
        withContext(Dispatchers.IO) {

            return@withContext runCatchingExceptCancel {
                val resp = LocalNetwork.post<List<CategoryRuleModel>>(
                    "category/rule/list?page=$page&limit=$limit",
                    "{}"
                ).getOrThrow()
                resp.data ?: emptyList()
            }.getOrElse {
                Logger.e("list error: ${it.message}", it)
                emptyList()
            }
    }

    /**
     * 创建或更新分类规则
     *
     * @param model 要创建或更新的[CategoryRuleModel]对象
     * @return 返回规则ID
     */
    suspend fun put(model: CategoryRuleModel): Long = withContext(Dispatchers.IO) {

        return@withContext runCatchingExceptCancel {
            val resp = LocalNetwork.post<Long>("category/rule/put", Gson().toJson(model))
                .getOrThrow()
            resp.data ?: 0L
        }.getOrElse {
            Logger.e("put error: ${it.message}", it)
            0L
        }
    }

    /**
     * 根据ID删除分类规则
     *
     * @param id 要删除的分类规则ID
     */
    suspend fun remove(id: Long) = withContext(Dispatchers.IO) {

        runCatchingExceptCancel {
            LocalNetwork.post<String>("category/rule/delete", Gson().toJson(mapOf("id" to id)))
                .getOrThrow()
        }.getOrElse {
            Logger.e("remove error: ${it.message}", it)
        }
    }
}