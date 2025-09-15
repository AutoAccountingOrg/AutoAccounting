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

package net.ankio.auto.http.license

import com.google.gson.Gson
import com.google.gson.JsonObject
import net.ankio.auto.App
import net.ankio.auto.storage.CacheManager
import net.ankio.auto.utils.DateUtils
import net.ankio.auto.utils.PrefManager

object ActivateAPI {
    /** 缓存键 */
    private const val CACHE_KEY_INFO = "activate_api_info"

    /** 缓存时间：30分钟（毫秒） */
    private const val CACHE_DURATION_MS = 24 * 60 * 60 * 1000L
    suspend fun active(code: String): String? {
        if (code.isEmpty() || !Regex("^ak-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$").matches(
                code
            )
        ) {
            return "Invalid activation code format"
        }

        val result = App.licenseNetwork.post(
            "/activate", hashMapOf(
                "activationCode" to code
            )
        )

        return runCatching {
            val json = Gson().fromJson(result, JsonObject::class.java)
            if (json.get("code").asInt != 200) {
                return json.get("msg").asString
            } else {
                PrefManager.token = json.get("data").asString
                return null
            }
        }.getOrNull()
    }

    suspend fun info(): HashMap<String, String> {
        // 先尝试从缓存获取数据
        val cachedData = CacheManager.getString(CACHE_KEY_INFO)
        if (cachedData != null) {
            return runCatching {
                @Suppress("UNCHECKED_CAST")
                Gson().fromJson(cachedData, HashMap::class.java) as HashMap<String, String>
            }.getOrElse { hashMapOf() }
        }

        // 缓存未命中，请求网络数据
        val result = App.licenseNetwork.get("/info")

        return runCatching {
            val json = Gson().fromJson(result, JsonObject::class.java)
            val responseData = if (json.get("code").asInt != 200) {
                hashMapOf(
                    "error" to json.get("msg").asString
                )
            } else {
                val data = json.get("data").asJsonObject
                hashMapOf(
                    "count" to data.get("active_count").asInt.toString(), //本月激活次数
                    "time" to DateUtils.stampToDate(data.get("bind_time").asLong * 1000) //激活时间
                )
            }

            // 将成功的响应数据缓存30分钟
            if (!responseData.containsKey("error")) {
                CacheManager.putString(
                    CACHE_KEY_INFO,
                    Gson().toJson(responseData),
                    CACHE_DURATION_MS
                )
            }

            responseData
        }.getOrElse { hashMapOf() }
    }

    /**
     * 清除激活信息缓存
     * 用于强制刷新数据，比如用户手动刷新时调用
     */
    fun clearInfoCache() {
        CacheManager.remove(CACHE_KEY_INFO)
    }
}