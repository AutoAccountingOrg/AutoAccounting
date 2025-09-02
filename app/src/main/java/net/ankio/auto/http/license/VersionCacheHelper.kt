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
import net.ankio.auto.storage.CacheManager

/**
 * 版本信息缓存助手
 *
 * 统一处理版本信息的缓存逻辑，避免重复代码
 */
object VersionCacheHelper {

    /** 版本信息缓存时间：30分钟 */
    private const val VERSION_CACHE_DURATION = 30 * 60 * 1000L // 30分钟

    /**
     * 获取带缓存的版本信息
     *
     * @param cacheKey 缓存键
     * @param forceRefresh 是否强制刷新缓存（用于强制更新场景）
     * @param networkCall 网络请求函数，返回JSON字符串
     * @return 版本信息JsonObject，失败时返回null
     */
    suspend fun getCachedVersion(
        cacheKey: String,
        forceRefresh: Boolean = false,
        networkCall: suspend () -> String
    ): JsonObject? {
        // 强制刷新时清除缓存
        if (forceRefresh) {
            CacheManager.remove(cacheKey)
        } else {
            // 先尝试从缓存获取
            CacheManager.getString(cacheKey)?.let { cachedJson ->
                return runCatching {
                    Gson().fromJson(cachedJson, JsonObject::class.java)
                }.getOrNull()
            }
        }

        // 缓存未命中或强制刷新，请求网络
        return runCatching {
            val result = networkCall()
            // 只有包含版本信息的响应才缓存
            if (result.contains("\"version\"") || result.contains("\"log\"") || result.contains("\"date\"")) {
                CacheManager.putString(cacheKey, result, VERSION_CACHE_DURATION)
            }

            Gson().fromJson(result, JsonObject::class.java)
        }.getOrNull()
    }

    /**
     * 清除指定缓存
     *
     * @param cacheKey 缓存键
     */
    fun clearCache(cacheKey: String) {
        CacheManager.remove(cacheKey)
    }

    /**
     * 清除所有版本信息缓存
     */
    fun clearAllVersionCache() {
        // 清除应用版本缓存（所有渠道）
        listOf("stable", "beta", "alpha").forEach { channel ->
            CacheManager.remove("app_version_$channel")
        }
        // 清除规则版本缓存
        CacheManager.remove("rule_version")
    }
}
