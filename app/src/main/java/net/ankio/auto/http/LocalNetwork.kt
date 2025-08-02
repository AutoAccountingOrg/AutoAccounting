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

package net.ankio.auto.http

import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.storage.Logger

object LocalNetwork {
    // 1. 复用同一个 OkHttpClient，按需配置超时 & 禁用代理
    private val client by lazy {
        RequestsUtils()
    }


    /**
     * 发起网络请求：
     * - 可通过 method 强制指定；
     * - headers 用于附加额外请求头。
     */
    /**
     * 发起网络请求
     *
     * @param path        请求路径（自动拼接到 http://127.0.0.1:52045/）
     * @param payload     可选请求体，非空则默认使用 POST（也可通过 method 强制覆盖）
     */
    suspend fun post(
        path: String,
        payload: Any? = null,
    ): String = withContext(Dispatchers.IO) {
        val url = "http://127.0.0.1:52045/${path.trimStart('/')}"
        client.addHeader("Authorize", "")
        try {
            when (payload) {
                is JsonObject -> {
                    return@withContext client.json(url, payload).second
                }

                else -> {
                    return@withContext client.jsonStr(url, payload.toString()).second

                }

            }
        } catch (e: Exception) {
            Logger.e("请求失败：$e", e)
            return@withContext ""
        }
    }
    /**
     * 发起网络请求：
     * - 当 json != null 且非空时，默认 POST；
     * - 否则 GET；
     * - 可通过 method 强制指定；
     * - headers 用于附加额外请求头。
     */
    /**
     * 发起网络请求
     *
     * @param path        请求路径（自动拼接到 http://127.0.0.1:52045/）
     */
    suspend fun get(
        path: String,
    ): String = withContext(Dispatchers.IO) {
        val url = "http://127.0.0.1:52045/${path.trimStart('/')}"
        client.addHeader("Authorize", "")
        try {
            return@withContext client.get(url).second
        } catch (e: Exception) {
            Logger.e("请求失败：$e", e)
            return@withContext ""
        }
    }

    /**
     * 通用请求方法 - 根据是否有payload自动选择GET或POST
     *
     * @param path        请求路径（自动拼接到 http://127.0.0.1:52045/）
     * @param payload     可选请求体，为空则使用GET请求，否则使用POST请求
     */
    suspend fun request(
        path: String,
        payload: String? = null,
    ): String = withContext(Dispatchers.IO) {
        if (payload.isNullOrEmpty()) {
            get(path)
        } else {
            post(path, payload)
        }
    }
}
