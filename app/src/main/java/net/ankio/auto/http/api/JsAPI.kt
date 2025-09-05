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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.http.LocalNetwork
import net.ankio.auto.storage.Logger
import org.ezbook.server.tools.runCatchingExceptCancel
import org.ezbook.server.constant.DataType
import org.ezbook.server.models.BillResultModel

object JsAPI {

    suspend fun analysis(
        type: DataType,
        data: String,
        appPackage: String,
        fromAppData: Boolean = false
    ): BillResultModel? = withContext(Dispatchers.IO) {

        return@withContext runCatchingExceptCancel {
            val resp = LocalNetwork.post<BillResultModel>(
                "js/analysis?type=${type.name}&app=$appPackage&fromAppData=$fromAppData",
                data
            ).getOrThrow()
            resp.data
        }.getOrElse {
            Logger.e("analysis error: ${it.message}", it)
            null
        }
    }


    suspend fun run(js: String): String = withContext(Dispatchers.IO) {

        return@withContext runCatchingExceptCancel {
            val resp = LocalNetwork.post<String>("js/run", js).getOrThrow()
            resp.data ?: ""
        }.getOrElse {
            Logger.e("run error: ${it.message}", it)
            ""
        }
    }
}