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
import org.ezbook.server.db.model.BookBillModel

/**
 * BookBillAPI 是一个用于处理账本账单相关网络请求的API类
 * 提供了获取账单列表和上传账单数据的功能
 */
object BookBillAPI {
    /**
     * 获取指定类型的账单列表
     *
     * @param typeName 账单类型名称
     * @return 返回账单模型列表，如果请求失败则返回空列表
     */
    suspend fun list(typeName: String): List<BookBillModel> = withContext(Dispatchers.IO) {

        return@withContext runCatchingExceptCancel {
            val resp = LocalNetwork.get<List<BookBillModel>>("bill/book/list?type=${typeName}")
                .getOrThrow()
            resp.data ?: emptyList()
        }.getOrElse {
            Logger.e("list error: ${it.message}", it)
            emptyList()
        }
    }

    /**
     * 上传账单数据到服务器
     *
     * @param bills 要上传的账单数据列表
     * @param md5 账单数据的MD5校验值
     * @param typeName 账单类型名称
     * @return 返回服务器响应结果
     */
    suspend fun put(bills: ArrayList<BookBillModel>, md5: String, typeName: String) =
        withContext(Dispatchers.IO) {

            runCatchingExceptCancel {
                val json = Gson().toJson(bills)
                LocalNetwork.post<String>("bill/book/put?md5=$md5&type=${typeName}", json)
                    .getOrThrow()
            }.getOrElse {
                Logger.e("put error: ${it.message}", it)

            }
        }

}