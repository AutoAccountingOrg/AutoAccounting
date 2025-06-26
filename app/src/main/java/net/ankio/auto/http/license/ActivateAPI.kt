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
import net.ankio.auto.utils.DateUtils
import net.ankio.auto.utils.PrefManager

object ActivateAPI {
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
        // 用户进入设置页会刷新，其他情况不刷新
        val result = App.licenseNetwork.get("/info")

        return runCatching {
            val json = Gson().fromJson(result, JsonObject::class.java)
            if (json.get("code").asInt != 200) {
                return hashMapOf(
                    "error" to json.get("msg").asString
                )
            } else {
                val data = json.get("data").asJsonObject
                return hashMapOf(
                    "count" to data.get("active_count").asInt.toString(), //本月激活次数
                    "time" to DateUtils.stampToDate(data.get("bind_time").asLong) //激活时间
                )
            }
        }.getOrElse { hashMapOf() }
    }
}