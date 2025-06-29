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

package net.ankio.auto.utils

import com.google.gson.JsonObject

object VersionUtils {
    const val CHANNEL_CANARY = "Canary"
    const val CHANNEL_BETA = "Beta"
    const val CHANNEL_STABLE = "Stable"

    private fun replaceChannel(string: String): String {
        return string.replace("-${CHANNEL_STABLE}", "")
            .replace("-${CHANNEL_BETA}", "")
            .replace("-${CHANNEL_CANARY}", "")
    }

    /**
     * 检查两个版本号哪个大
     * @param localVersion 本地版本号
     * @param cloudVersion 云端版本号
     */
    fun checkVersionLarge(localVersion: String, cloudVersion: String): Boolean {
        val localParts = replaceChannel(localVersion).replace("_", "").split(".")
        val cloudParts = replaceChannel(cloudVersion).replace("_", "").split(".")
        // 找出较长的版本号长度，补齐较短版本号的空位
        val maxLength = maxOf(localParts.size, cloudParts.size)

        for (i in 0 until maxLength) {
            val localPart = localParts.getOrNull(i)?.toLongOrNull() ?: 0  // 如果某个部分不存在，默认视为0
            val cloudPart = cloudParts.getOrNull(i)?.toLongOrNull() ?: 0
            if (cloudPart > localPart) {
                return true
            }
        }
        return false
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

data class UpdateModel(val log: String, val date: String, val version: String)