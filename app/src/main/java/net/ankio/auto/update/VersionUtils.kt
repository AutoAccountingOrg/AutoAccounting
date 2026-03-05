/*
 * Copyright (C) 2026 ankio(ankio@ankio.net)
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

package net.ankio.auto.update

import com.google.gson.JsonObject

object VersionUtils {
    const val CHANNEL_CANARY = "Canary"
    const val CHANNEL_BETA = "Beta"
    const val CHANNEL_STABLE = "Stable"

    private fun removeChannelSuffix(version: String): String {
        return version.replace("-${CHANNEL_STABLE}", "")
            .replace("-${CHANNEL_BETA}", "")
            .replace("-${CHANNEL_CANARY}", "")
            .replace("v", "")
    }

    /**
     * 检查云端版本是否比本地版本更新
     * @param localVersion 本地版本号
     * @param cloudVersion 云端版本号
     * @return true表示云端版本更新，需要升级；false表示本地版本相同或更新
     */
    fun isCloudVersionNewer(localVersion: String, cloudVersion: String): Boolean {
        val localParts = removeChannelSuffix(localVersion).replace("_", "").split(".")
        val cloudParts = removeChannelSuffix(cloudVersion).replace("_", "").split(".")
        
        val maxLength = maxOf(localParts.size, cloudParts.size)

        for (i in 0 until maxLength) {
            val localPart = localParts.getOrNull(i)?.toLongOrNull() ?: 0
            val cloudPart = cloudParts.getOrNull(i)?.toLongOrNull() ?: 0

            when {
                cloudPart > localPart -> return true   // 云端版本更大
                cloudPart < localPart -> return false  // 本地版本更大
                // 相等时继续比较下一部分
            }
        }
        return false  // 版本完全相同
    }


    fun fromJSON(jsonObject: JsonObject?): UpdateModel? {
        if (jsonObject == null) return null
        return UpdateModel(
            jsonObject.get("log").asString,
            jsonObject.get("date").asString,
            jsonObject.get("version").asString
        )
    }
}