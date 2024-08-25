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
package net.ankio.auto.models

import net.ankio.auto.utils.AppUtils

class AssetsMapModel {
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
        suspend fun put(map: AssetsMapModel) {
          /*  if (map.id == 0)
                AppUtils.getService().sendMsg("assets_map/add", map)
            else
                AppUtils.getService().sendMsg("assets_map/update", map)*/
        }

        suspend fun clear(){
    /*        AppUtils.getService().sendMsg("assets_map/clear", mapOf<String, Int>())
    */    }

        suspend fun del(id: Int)  {
       /*     AppUtils.getService().sendMsg("assets_map/del", mapOf("id" to id))
      */  }

        suspend fun list(): List<AssetsMapModel> {
            /*val data = runCatching {
                AppUtils.getService().sendMsg("assets_map/list", mapOf("page" to 0, "size" to 0)) as List<AssetsMapModel>
            }.getOrNull()?: emptyList()*/
            return emptyList()
        }
    }
}
