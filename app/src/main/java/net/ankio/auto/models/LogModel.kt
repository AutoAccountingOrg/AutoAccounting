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

class LogModel: BaseModel(){
    var date = ""
    var app = ""
    var hook = 0
    var thread = ""
    var line = ""
    var level = 0
    var log = ""

    companion object {
        const val LOG_LEVEL_DEBUG = 0
        const val LOG_LEVEL_INFO = 1
        const val LOG_LEVEL_WARN = 2
        const val LOG_LEVEL_ERROR = 3

       suspend fun put(logModel: LogModel) {
          /* AppUtils.getService().sendMsg("log/add", logModel)*/
        }


        suspend fun clear() {
          /*  AppUtils.getService().sendMsg("log/clear", null)*/
        }

        suspend fun list(page:Int,limit:Int):List<LogModel>{
           /* val data = runCatching {
                AppUtils.getService().sendMsg("log/list", mapOf("page" to page, "limit" to limit)) as List<LogModel>
            }.getOrNull()?: emptyList()
            return data*/
            return emptyList()
        }


    }
}
