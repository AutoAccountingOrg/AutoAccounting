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
 *  limitations under the License.
 */

package net.ankio.auto.http.api

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.http.LocalNetwork
import net.ankio.auto.storage.Logger
import org.ezbook.server.db.model.AnalysisTaskModel
import org.ezbook.server.tools.runCatchingExceptCancel

/**
 * AI分析任务API客户端
 */
object AnalysisTaskAPI {

    /**
     * 获取所有分析任务
     */
    suspend fun getAllTasks(): List<AnalysisTaskModel> = withContext(Dispatchers.IO) {
        return@withContext runCatchingExceptCancel {
            val resp =
                LocalNetwork.post<List<AnalysisTaskModel>>("/analysis/all", "{}").getOrThrow()
            resp.data ?: emptyList()
        }.getOrElse {
            Logger.e("getAllTasks error: ${it.message}", it)
            emptyList()
        }
    }

    /**
     * 创建新的分析任务
     */
    suspend fun createTask(title: String, startTime: Long, endTime: Long): Long? =
        withContext(Dispatchers.IO) {
            return@withContext runCatchingExceptCancel {
                val requestData = mapOf(
                    "title" to title,
                    "startTime" to startTime,
                    "endTime" to endTime
                )
                val resp = LocalNetwork.post<Long>("/analysis/create", Gson().toJson(requestData))
                    .getOrThrow()
                resp.data
            }.getOrElse {
                Logger.e("createTask error: ${it.message}", it)
                null
            }
        }

    /**
     * 获取任务详情
     */
    suspend fun getTaskById(id: Long): AnalysisTaskModel? = withContext(Dispatchers.IO) {
        return@withContext runCatchingExceptCancel {
            val requestData = mapOf("id" to id)
            val resp =
                LocalNetwork.post<AnalysisTaskModel>("/analysis/detail", Gson().toJson(requestData))
                    .getOrThrow()
            resp.data
        }.getOrElse {
            Logger.e("getTaskById error: ${it.message}", it)
            null
        }
    }

    /**
     * 删除任务
     */
    suspend fun deleteTask(id: Long): Boolean = withContext(Dispatchers.IO) {
        return@withContext runCatchingExceptCancel {
            val requestData = mapOf("id" to id)
            val resp = LocalNetwork.post<String>("/analysis/delete", Gson().toJson(requestData))
                .getOrThrow()
            resp.code == 200
        }.getOrElse {
            Logger.e("deleteTask error: ${it.message}", it)
            false
        }
    }

    /**
     * 清空所有任务
     */
    suspend fun clearAllTasks(): Boolean = withContext(Dispatchers.IO) {
        return@withContext runCatchingExceptCancel {
            val resp = LocalNetwork.post<String>("/analysis/clear", "{}").getOrThrow()
            resp.code == 200
        }.getOrElse {
            Logger.e("clearAllTasks error: ${it.message}", it)
            false
        }
    }
}

 