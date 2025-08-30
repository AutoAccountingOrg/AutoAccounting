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
import net.ankio.auto.utils.PrefManager
import java.io.File

object AppAPI {

    /** 版本信息缓存时间：30分钟 */
    private const val VERSION_CACHE_DURATION = 30 * 60 * 1000L // 30分钟 = 1800000毫秒

    /**
     * 获取最新版本信息（带30分钟缓存）
     *
     * 缓存策略：
     * - 缓存键基于当前更新渠道生成，不同渠道独立缓存
     * - 缓存30分钟，避免频繁请求服务器
     * - 网络请求失败时仍返回缓存数据（如果存在）
     */
    suspend fun lastVer(): JsonObject? {
        val channel = PrefManager.appChannel
        val cacheKey = "app_version_$channel"

        // 先尝试从缓存获取
        CacheManager.getString(cacheKey)?.let { cachedJson ->
            return runCatching {
                Gson().fromJson(cachedJson, JsonObject::class.java)
            }.getOrNull()
        }

        // 缓存未命中，请求网络
        return runCatching {
            val result = App.licenseNetwork.get(
                "/app/latest", hashMapOf(
                    "channel" to channel
                )
            )

            // 缓存响应结果
            CacheManager.putString(cacheKey, result, VERSION_CACHE_DURATION)

            Gson().fromJson(result, JsonObject::class.java)
        }.getOrNull()
    }

    suspend fun download(version: String, file: File): Boolean {
        return App.licenseNetwork.download(
            "/app/download/", file, hashMapOf(
                "channel" to PrefManager.appChannel,
                "version" to version
            )
        )
    }
}