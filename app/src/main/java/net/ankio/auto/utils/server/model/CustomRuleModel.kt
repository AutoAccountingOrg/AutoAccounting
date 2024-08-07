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

class CustomRuleModel  {
    var id = 0

    var use = 1 // 是否启用该规则
    var sort = 0 // 排序
    var auto_create = 0 // 是否为自动创建

    var js = ""
    var text = ""

    var element: String = ""

    companion object {
        suspend fun clear(){
            AppUtils.getService().sendMsg("custom/clear", mapOf<String, Int>())
        }

        suspend fun list(page:Int,limit:Int):List<CustomRuleModel>{
            val data = runCatching {
                AppUtils.getService().sendMsg("custom/list", mapOf("page" to page, "limit" to limit)) as List<CustomRuleModel>
            }.getOrNull()?: emptyList()
            return data
        }

        suspend fun put(model: CustomRuleModel){
            if(model.id == 0)
                AppUtils.getService().sendMsg("custom/add", model)
            else
                AppUtils.getService().sendMsg("custom/update", model)
        }

        suspend fun del(id:Int){
            AppUtils.getService().sendMsg("custom/del", mapOf("id" to id))
        }

    }
}
