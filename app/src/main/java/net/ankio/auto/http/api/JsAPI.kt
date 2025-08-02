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

package net.ankio.auto.http.api

import com.google.gson.Gson
import com.google.gson.JsonObject
import net.ankio.auto.http.LocalNetwork
import org.ezbook.server.constant.DataType
import org.ezbook.server.models.BillResultModel

object JsAPI {

    suspend fun analysis(
        type: DataType,
        data: String,
        appPackage: String,
        fromAppData: Boolean = false
    ): BillResultModel? {
        val result = LocalNetwork.post(
            "js/analysis?type=${type.name}&app=$appPackage&fromAppData=$fromAppData",
            data
        )
            ?: return null

        val json = Gson().fromJson(result, JsonObject::class.java)
        val resultData = json?.getAsJsonObject("data") ?: return null

        return runCatching {
            Gson().fromJson(resultData, BillResultModel::class.java)
        }.getOrNull()
    }


    suspend fun run(js: String): String {
        return LocalNetwork.post(
            "js/run",
            js
        )
    }
}