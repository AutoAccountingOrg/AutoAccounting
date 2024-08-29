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

package net.ankio.auto.app.js

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ezbook.server.db.model.BillInfoModel
import net.ankio.auto.utils.AppTimeMonitor

object Engine {
    suspend fun analyze(
        dataType: Int, // 类型
        app: String, // 来自哪个App或者手机号
        data: String, // 具体的数据
        call: Boolean = true,
    ): BillInfoModel? =
        withContext(Dispatchers.IO) {
            AppTimeMonitor.startMonitoring("规则识别")

         /*   val json =
                AppUtils.getService().sendMsg(
                    "analyze",
                    JsonObject().apply {
                        addProperty("type", dataType)
                        addProperty("app", app)
                        addProperty("data", data)
                        addProperty("call", if (call) 1 else 0)
                    },
                )

            val billInfoModel = runCatching { Gson().fromJson(json as JsonObject, BillInfoModel::class.java) }.getOrNull()

            AppTimeMonitor.stopMonitoring("规则识别")
            billInfoModel*/
            null
        }
}
