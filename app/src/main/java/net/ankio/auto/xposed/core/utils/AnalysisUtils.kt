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

package net.ankio.auto.xposed.core.utils

import net.ankio.auto.http.api.JsAPI
import net.ankio.auto.xposed.core.logger.XposedLogger
import org.ezbook.server.constant.DataType
import org.ezbook.server.constant.DefaultData
import org.ezbook.server.constant.Setting
import org.ezbook.server.tools.MD5HashTable

/**
 * AnalysisUtils
 * 封装数据分析上报逻辑。
 */
object AnalysisUtils {
    val mD5HashTable by lazy {
        MD5HashTable(300_000)
    }

    fun analysisData(manifestAppPackage: String, type: DataType, data: String) {
        CoroutineUtils.withIO {
            val hash = MD5HashTable.md5(data)
            if (mD5HashTable.contains(hash)) {
                XposedLogger.d("AnalysisUtils: duplicate skip, pkg=$manifestAppPackage, type=$type")
                return@withIO
            }
            mD5HashTable.put(data)

            val whitelist = DataUtils.configString(Setting.DATA_FILTER, DefaultData.DATA_FILTER)
                .split(",").filter { it.isNotEmpty() }
            if (whitelist.all { !data.contains(it) }) {
                XposedLogger.d("AnalysisUtils: whitelist filter, pkg=$manifestAppPackage, type=$type, data=${data}")
                return@withIO
            }

            val blacklist = DataUtils.configString(
                Setting.DATA_FILTER_BLACKLIST,
                DefaultData.DATA_FILTER_BLACKLIST
            )
                .split(",").filter { it.isNotEmpty() }
            if (blacklist.any { data.contains(it) }) {
                XposedLogger.d("AnalysisUtils: blacklist filter, pkg=$manifestAppPackage, type=$type, data=${data}")
                return@withIO
            }

            XposedLogger.d("AnalysisUtils: submit, pkg=$manifestAppPackage, type=$type, data=${data}")
            val result = JsAPI.analysis(type, data, manifestAppPackage)
            XposedLogger.i("AnalysisUtils: result pkg=$manifestAppPackage, type=$type, ok=${result.data != null}, msg=${result.msg} , result=${result.data}")
        }
    }
}


