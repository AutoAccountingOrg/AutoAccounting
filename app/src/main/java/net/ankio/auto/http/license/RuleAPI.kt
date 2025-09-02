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

import com.google.gson.JsonObject
import net.ankio.auto.App
import java.io.File

object RuleAPI {

    /**
     * 获取最新版本信息（带30分钟缓存）
     *
     * 缓存策略：
     * - 使用统一的CacheManager进行缓存管理
     * - 缓存30分钟，避免频繁请求服务器
     * - 网络请求失败时仍返回缓存数据（如果存在）
     *
     * @param forceRefresh 是否强制刷新缓存（用于强制更新场景）
     */
    suspend fun lastVer(forceRefresh: Boolean = false): JsonObject? {
        val cacheKey = "rule_version"

        return VersionCacheHelper.getCachedVersion(cacheKey, forceRefresh) {
            App.licenseNetwork.get("/rule/latest")
        }
    }

    suspend fun download(version: String, file: File): Boolean {
        return App.licenseNetwork.download(
            "/rule/download/", file, hashMapOf(
                "version" to version
            )
        )
    }

    suspend fun submit(title: String, body: String): String {
        return App.licenseNetwork.post(
            "/rule/issue",
            hashMapOf(
                "title" to title,
                "body" to body
            )
        )
    }
}