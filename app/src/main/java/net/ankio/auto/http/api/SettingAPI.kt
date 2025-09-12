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
import io.github.oshai.kotlinlogging.KotlinLogging

object SettingAPI {

    private val logger = KotlinLogging.logger(this::class.java.name)
    /**
     * 获取设置
     */
    suspend fun get(key: String, default: String): String = withContext(Dispatchers.IO) {

        return@withContext runCatchingExceptCancel {
            val resp = LocalNetwork.post<String>("setting/get?key=$key", "{}").getOrThrow()
            resp.data ?: default
        }.getOrElse {
            logger.error(it) { "get error: ${it.message}" }
            default
        }
    }

    /**
     * 设置
     */
    suspend fun set(key: String, value: String) = withContext(Dispatchers.IO) {

        runCatchingExceptCancel {
            LocalNetwork.post<String>("setting/set?key=$key", value).getOrThrow()
        }.getOrElse {
            logger.error(it) { "set error: ${it.message}" }

        }
    }

    suspend fun clearDatabase() = withContext(Dispatchers.IO) {

        runCatchingExceptCancel {
            LocalNetwork.post<String>("db/clear").getOrThrow()
        }.getOrElse {
            logger.error(it) { "clearDatabase error: ${it.message}" }

        }
    }

    /**
     * 获取所有设置项列表
     */
    suspend fun list(): List<org.ezbook.server.db.model.SettingModel> =
        withContext(Dispatchers.IO) {

            return@withContext runCatchingExceptCancel {
                val resp = LocalNetwork.post<List<org.ezbook.server.db.model.SettingModel>>(
                    "setting/list",
                    "{}"
                ).getOrThrow()
                resp.data ?: emptyList()
            }.getOrElse {
                logger.error(it) { "list error: ${it.message}" }
                emptyList()
            }
        }
}