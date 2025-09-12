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
import org.ezbook.server.constant.LogLevel
import org.ezbook.server.db.model.LogModel
import org.ezbook.server.tools.runCatchingExceptCancel
import io.github.oshai.kotlinlogging.KotlinLogging

object LogAPI {

    private val logger = KotlinLogging.logger(this::class.java.name)
    /**
     * 添加日志
     */
    suspend fun add(level: LogLevel, app: String, location: String, message: String) = add(
        LogModel().apply {
            this.level = level
            this.app = app
            this.location = location
            this.message = message
        })

    suspend fun add(log: LogModel) = withContext(Dispatchers.IO) {

        runCatchingExceptCancel {
            LocalNetwork.post<String>("log/add", Gson().toJson(log)).getOrThrow()
        }.getOrElse {

        }
    }

    /**
     * 获取日志列表
     * @param page 页码
     * @param limit 每页数量
     * @return 日志列表
     */
    suspend fun list(page: Int = 1, limit: Int = 10): List<LogModel> = withContext(Dispatchers.IO) {

        return@withContext runCatchingExceptCancel {
            val resp = LocalNetwork.get<List<LogModel>>("log/list?page=$page&limit=$limit").getOrThrow()
            resp.data ?: emptyList()
        }.getOrElse {
            logger.error(it) { "list error: ${it.message}" }
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
            logger.error(it) { "clear error: ${it.message}" }

        }
    }
}