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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.http.LocalNetwork
import net.ankio.auto.storage.Logger
import org.ezbook.server.constant.DataType
import org.ezbook.server.models.BillResultModel
import org.ezbook.server.models.ResultModel
import org.ezbook.server.tools.runCatchingExceptCancel

object JsAPI {

    /**
     * 分析数据（规则匹配 + AI识别）
     * @return 完整的 ResultModel，调用方可通过 code/msg/data 区分成功和各种失败原因
     */
    suspend fun analysis(
        type: DataType,
        data: String,
        appPackage: String,
        fromAppData: Boolean = false
    ): ResultModel<BillResultModel> = withContext(Dispatchers.IO) {

        return@withContext runCatchingExceptCancel {
            LocalNetwork.post<BillResultModel>(
                "js/analysis?type=${type.name}&app=$appPackage&fromAppData=$fromAppData",
                data
            ).getOrThrow()
        }.getOrElse {
            Logger.e("analysis error: ${it.message}", it)
            ResultModel<BillResultModel>(500, it.message ?: "未知错误", null)
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
