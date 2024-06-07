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
import net.ankio.auto.utils.Logger

class AssetsMap {
    // 账户列表
    var id = 0

    /**
     * 是否将原始映射的账户名作为正则使用
     */
    var regex: Int = 0

    /**
     * 原始获取到的账户名
     */
    var name: String = "" // 账户名

    /**
     * 映射到的账户名
     */
    var mapName: String = "" // 映射账户名

    companion object {
        fun put(map: AssetsMap) {
            AppUtils.getScope().launch {
                AppUtils.getService().sendMsg("asset/map/put", map)
            }
        }

        suspend fun get(): List<AssetsMap> {
            val data = AppUtils.getService().sendMsg("asset/map/get", null)
            return runCatching { Gson().fromJson(data as JsonArray, Array<AssetsMap>::class.java).toList() }.onFailure { Logger.w(
                ("Transfer Error: " + it.message)
            ) }.getOrNull() ?: emptyList()
        }

        suspend fun remove(id: Int)  {
            AppUtils.getService().sendMsg("asset/map/remove", mapOf("id" to id))
        }
    }
}
