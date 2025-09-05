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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.http.LocalNetwork
import net.ankio.auto.storage.Logger
import org.ezbook.server.tools.runCatchingExceptCancel
import org.ezbook.server.db.model.CategoryMapModel

/**
 * CategoryMapAPI 对象提供了与分类映射相关的网络请求操作
 * 包括获取分类映射列表、添加/更新分类映射以及删除分类映射等功能
 */
object CategoryMapAPI {
    /**
     * 获取分类映射列表
     * @param page 页码，从1开始
     * @param pageSize 每页显示的数量
     * @param search 搜索关键词，默认为空字符串
     * @return 返回分类映射模型列表，如果请求失败则返回空列表
     */
    suspend fun list(page: Int, pageSize: Int, search: String = ""): List<CategoryMapModel> =
        withContext(Dispatchers.IO) {

            return@withContext runCatchingExceptCancel {
                val resp = LocalNetwork.get<List<CategoryMapModel>>(
                    "category/map/list?page=$page&limit=$pageSize&search=${
                        Uri.encode(
                            search
                        )
                    }"
                ).getOrThrow()
                resp.data ?: emptyList()
            }.getOrElse {
                Logger.e("list error: ${it.message}", it)
                emptyList()
            }
        }

    /**
     * 添加或更新分类映射
     * @param model 要添加或更新的分类映射模型
     * @return 返回服务器响应结果
     */
    suspend fun put(model: CategoryMapModel) = withContext(Dispatchers.IO) {

        runCatchingExceptCancel {
            LocalNetwork.post<String>("category/map/put", Gson().toJson(model)).getOrThrow()
        }.getOrElse {
            Logger.e("put error: ${it.message}", it)

        }
    }

    /**
     * 删除指定的分类映射
     * @param id 要删除的分类映射ID
     * @return 返回服务器响应结果
     */
    suspend fun remove(id: Long) = withContext(Dispatchers.IO) {

        runCatchingExceptCancel {
            LocalNetwork.post<String>("category/map/delete", Gson().toJson(mapOf("id" to id)))
                .getOrThrow()
        }.getOrElse {
            Logger.e("remove error: ${it.message}", it)

        }
    }
}