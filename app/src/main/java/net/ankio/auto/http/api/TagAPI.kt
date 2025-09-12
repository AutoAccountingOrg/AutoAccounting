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
import net.ankio.auto.storage.Logger
import org.ezbook.server.tools.runCatchingExceptCancel
import org.ezbook.server.db.model.TagModel
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * 标签相关API接口，封装了对后端标签的增删改查操作。
 */
object TagAPI {

    private val logger = KotlinLogging.logger(this::class.java.name)
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

            return@withContext runCatchingExceptCancel {
                val resp =
                    LocalNetwork.get<List<TagModel>>("tag/list?page=$page&limit=$pageSize$searchParam")
                        .getOrThrow()
                resp.data ?: emptyList()
            }.getOrElse {
                logger.error(it) { "list error: ${it.message}" }
                emptyList()
            }
        }

    /**
     * 获取所有标签列表（不分页）。
     * @return 所有标签模型列表
     */
    suspend fun all(): List<TagModel> = withContext(Dispatchers.IO) {

        return@withContext runCatchingExceptCancel {
            val resp = LocalNetwork.get<List<TagModel>>("tag/list?limit=0").getOrThrow()
            resp.data ?: emptyList()
        }.getOrElse {
            logger.error(it) { "all error: ${it.message}" }
            emptyList()
        }
    }

    /**
     * 新增或更新标签。
     * @param model 标签模型
     * @return 后端返回的操作结果
     */
    suspend fun put(model: TagModel): JsonObject = withContext(Dispatchers.IO) {

        return@withContext runCatchingExceptCancel {
            val resp = LocalNetwork.post<JsonObject>("tag/put", Gson().toJson(model)).getOrThrow()
            resp.data ?: JsonObject()
        }.getOrElse {
            logger.error(it) { "put error: ${it.message}" }
            JsonObject()
        }
    }

    /**
     * 根据ID删除标签。
     * @param id 标签ID
     */
    suspend fun remove(id: Long) = withContext(Dispatchers.IO) {

        runCatchingExceptCancel {
            LocalNetwork.post<String>("tag/delete", Gson().toJson(mapOf("id" to id))).getOrThrow()
        }.getOrElse {
            logger.error(it) { "remove error: ${it.message}" }
        }
    }

    /**
     * 根据ID获取标签。
     * @param id 标签ID
     * @return 对应的标签模型，若不存在则为null
     */
    suspend fun getById(id: Long): TagModel? = withContext(Dispatchers.IO) {

        return@withContext runCatchingExceptCancel {
            val resp = LocalNetwork.get<TagModel>("tag/get?id=$id").getOrThrow()
            resp.data
        }.getOrElse {
            logger.error(it) { "getById error: ${it.message}" }
            null
        }
    }

    /**
     * 获取标签总数。
     * @return 标签总数
     */
    suspend fun count(): Int = withContext(Dispatchers.IO) {

        return@withContext runCatchingExceptCancel {
            val resp = LocalNetwork.get<Int>("tag/count").getOrThrow()
            resp.data ?: 0
        }.getOrElse {
            logger.error(it) { "count error: ${it.message}" }
            0
        }
    }

    /**
     * 检查标签名称是否可用。
     * @param name 标签名称
     * @param excludeId 排除的标签ID（用于编辑时排除自己）
     * @return 是否可用
     */
    suspend fun checkNameAvailable(name: String, excludeId: Long = 0): Boolean =
        withContext(Dispatchers.IO) {

            return@withContext runCatchingExceptCancel {
                val resp =
                    LocalNetwork.get<Boolean>("tag/check?name=${Uri.encode(name)}&id=$excludeId")
                        .getOrThrow()
                resp.data ?: false
            }.getOrElse {
                logger.error(it) { "checkNameAvailable error: ${it.message}" }
                false
            }
        }

    /**
     * 获取所有已存在的标签分组。
     * @return 已存在分组列表，去重且按预定义顺序排序
     */
    suspend fun getGroups(): List<String> = withContext(Dispatchers.IO) {
        val allTags = all()
        val existingGroups = allTags.map { it.group.ifEmpty { "其他" } }.distinct()

        // 预定义的分组顺序
        val predefinedGroups = listOf("其他")

        // 按预定义顺序排序，未在预定义中的分组排在最后
        val sortedGroups = mutableListOf<String>()
        predefinedGroups.forEach { group ->
            if (existingGroups.contains(group)) {
                sortedGroups.add(group)
            }
        }

        // 添加不在预定义列表中的分组
        existingGroups.forEach { group ->
            if (!predefinedGroups.contains(group)) {
                sortedGroups.add(group)
            }
        }

        sortedGroups
    }

    /**
     * 批量插入标签（重置模式：先清除所有现有标签，再插入新标签）。
     * @param tags 标签列表
     * @return 后端返回的操作结果
     */
    suspend fun batchInsert(tags: List<TagModel>): JsonObject = withContext(Dispatchers.IO) {

        return@withContext runCatchingExceptCancel {
            val resp = LocalNetwork.post<JsonObject>("tag/batch", Gson().toJson(tags)).getOrThrow()
            resp.data ?: JsonObject()
        }.getOrElse {
            logger.error(it) { "batchInsert error: ${it.message}" }
            JsonObject()
        }
    }
}
