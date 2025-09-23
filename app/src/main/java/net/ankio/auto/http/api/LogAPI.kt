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
import org.ezbook.server.constant.LogLevel
import org.ezbook.server.db.model.LogModel
import org.ezbook.server.tools.runCatchingExceptCancel

object LogAPI {
    /**
     * 添加日志
     */
    suspend fun add(level: LogLevel, app: String, location: String, message: String) = withContext(Dispatchers.IO) {
        val log = LogModel(level = level, app = app, location = location, message = message)
        add(logModel = log)
    }

    suspend fun add(logModel: LogModel) = withContext(Dispatchers.IO) {
        runCatchingExceptCancel {
            LocalNetwork.post<String>("log/add", Gson().toJson(logModel)).getOrThrow()
        }.getOrElse {

        }
    }

    /**
     * 获取日志列表（支持筛选）
     * @param page 页码
     * @param limit 每页数量
     * @param appFilter 应用筛选，为空表示所有应用
     * @param levelFilter 日志级别筛选列表，为空表示所有级别
     * @return 日志列表
     */
    suspend fun list(
        page: Int = 1,
        limit: Int = 10,
        appFilter: String = "",
        levelFilter: List<String> = emptyList()
    ): List<LogModel> = withContext(Dispatchers.IO) {

        return@withContext runCatchingExceptCancel {
            val url = buildString {
                append("log/list?page=$page&limit=$limit")
                if (appFilter.isNotEmpty()) append("&app=${Uri.encode(appFilter)}")
                if (levelFilter.isNotEmpty()) append("&levels=${levelFilter.joinToString(",")}")
            }
            val resp = LocalNetwork.get<List<LogModel>>(url).getOrThrow()
            resp.data ?: emptyList()
        }.getOrElse {
            Logger.e("list error: ${it.message}", it)
            emptyList()
        }
    }

    /**
     * 获取所有应用列表（用于筛选器）
     * @return 应用包名列表
     */
    suspend fun getApps(): List<String> = withContext(Dispatchers.IO) {
        return@withContext runCatchingExceptCancel {
            val resp = LocalNetwork.get<List<String>>("log/apps").getOrThrow()
            resp.data ?: emptyList()
        }.getOrElse {
            Logger.e("getApps error: ${it.message}", it)
            emptyList()
        }
    }

    /**
     * 清空日志
     */
    suspend fun clear() = withContext(Dispatchers.IO) {

        runCatchingExceptCancel {
            LocalNetwork.post<String>("log/clear").getOrThrow()
        }.getOrElse {
            Logger.e("clear error: ${it.message}", it)

        }
    }
}