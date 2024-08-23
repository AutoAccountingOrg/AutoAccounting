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

class RuleModel {
    var id = 0
    var app = ""
    var type = 0
    var use = 0
    var auto_record = 0
    var name = ""
    var js = ""

    companion object {
        suspend fun put(rule: RuleModel) {
            if (rule.id == 0)
                AppUtils.getService().sendMsg("rule/add", rule)
            else
                AppUtils.getService().sendMsg("rule/update", rule)
            }


        suspend fun clear() {
        AppUtils.getService().sendMsg("rule/clear", mapOf("id" to 0))
        }


        suspend fun delete(id: Int) {
            AppUtils.getService().sendMsg("rule/del", mapOf("id" to id))
        }

        suspend fun list(page:Int,limit:Int,app: String,type: Int):List<RuleModel>{
            val data = runCatching {
                AppUtils.getService().sendMsg("rule/list", mapOf("page" to page, "limit" to limit, "app" to app, "type" to type)) as List<RuleModel>
            }.getOrNull()?: emptyList()
            return data

        }
    }
}
