/*
 * Copyright (C) 2024 ankio(ankio@ankio.net)
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
package net.ankio.auto.utils.server.model

import com.google.gson.Gson
import com.google.gson.JsonArray
import kotlinx.coroutines.launch
import net.ankio.auto.utils.AppUtils
import java.io.Serializable

class Regular : Serializable {
    var id = 0

    var use = true // 是否启用该规则
    var sort = 0 // 排序
    var auto = false // 是否为自动创建

    var js = ""
    var text = ""

    var element: String = ""

    companion object {
        fun put(regular: Regular) {
            AppUtils.getScope().launch {
                AppUtils.getService().sendMsg("rule/custom/put", regular)
            }
        }

        suspend fun get(limit: Int = 500): List<Regular> {
            val data = AppUtils.getService().sendMsg("rule/custom/get", mapOf("limit" to limit))
            return runCatching { Gson().fromJson(data as JsonArray, Array<Regular>::class.java).toList() }.getOrDefault(emptyList())
        }

        suspend fun getById(id: Int): Regular {
            val data = AppUtils.getService().sendMsg("rule/custom/get/id", mapOf("id" to id))
            return Gson().fromJson(data as String, Regular::class.java)
        }

        suspend fun remove(id: Int) {
            AppUtils.getService().sendMsg("rule/custom/remove", mapOf("id" to id))
        }
    }
}
