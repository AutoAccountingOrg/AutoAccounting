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
import kotlinx.coroutines.launch
import net.ankio.auto.utils.AppUtils

class AssetsMap {
    // 账户列表
    var id = 0

    /**
     * 是否将原始映射的账户名作为正则使用
     */
    var regex: Boolean = false

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
            return Gson().fromJson(Gson().toJson(data), Array<AssetsMap>::class.java).toList()
        }

        suspend fun remove(id: Int)  {
            AppUtils.getService().sendMsg("asset/map/remove", mapOf("id" to id))
        }
    }
}
