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

import kotlinx.coroutines.launch
import net.ankio.auto.utils.AppUtils

class RuleModel {
    var app = ""
    var js = ""
    var version = ""
    var type = 0

    companion object {
        fun put(rule: RuleModel) {
            AppUtils.getScope().launch {
                AppUtils.getService().sendMsg("rule/put", rule)
            }
        }

        suspend fun get(
            app: String,
            type: Int,
        ): RuleModel {
            val data = AppUtils.getService().sendMsg("rule/get", mapOf("app" to app, "type" to type))
            return data as RuleModel
        }
    }
}
