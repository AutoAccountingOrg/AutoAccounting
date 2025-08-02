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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.http.LocalNetwork
import org.ezbook.server.db.model.BookNameModel

/**
 * 账本名称API类，提供账本相关的网络请求操作
 * 所有方法都是挂起函数，需要在协程作用域内调用
 */
object BookNameAPI {
    /**
     * 获取所有账本列表
     * @return 账本模型列表，如果请求失败则返回空列表
     */
    suspend fun list(): List<BookNameModel> = withContext(
        Dispatchers.IO
    ) {
        val response = LocalNetwork.post("book/list")

        runCatching {
            val json = Gson().fromJson(response, JsonObject::class.java)
            Gson().fromJson(
                json.getAsJsonArray("data"),
                Array<BookNameModel>::class.java
            ).toList()
        }.getOrNull() ?: emptyList()
    }



    /**
     * 获取默认账本
     * @return 按以下优先级返回账本：
     * 1. 如果账本列表为空，创建新账本
     * 2. 如果找到指定名称的账本，返回该账本
     * 3. 如果请求的是"默认账本"但未找到，返回列表第一个账本
     * 4. 其他情况创建新账本
     */
    const val DEFAULT_BOOK = "默认账本"

    suspend fun getBook(bookName: String): BookNameModel {
        val books = list()

        // ❶ 列表为空 → 直接返回默认账本
        if (books.isEmpty()) return BookNameModel().apply { DEFAULT_BOOK }

        // ❷ 尝试在已有列表里找到同名账本
        books.firstOrNull { it.name == bookName }?.let { return it }

        // ❸ 若请求的是“默认账本”却没找到 → 退而求其次返回第一个账本
        if (bookName == DEFAULT_BOOK) return books.first()

        // ❹ 其余情况：新建一个同名账本
        return BookNameModel().apply { name = bookName }
    }


    /**
     * 获取第一个账本
     * @return 如果存在账本则返回第一个账本，否则返回一个名为"默认账本"的新账本
     */
    suspend fun getFirstBook(): BookNameModel {
        return getBook(DEFAULT_BOOK)
    }

    /**
     * 更新账本列表
     * @param bookList 要更新的账本列表
     * @param md5 账本列表的MD5校验值
     * @return 服务器响应结果
     */
    suspend fun put(bookList: ArrayList<BookNameModel>, md5: String) =
        withContext(Dispatchers.IO) {
            LocalNetwork.post("book/put?md5=$md5", Gson().toJson(bookList))
        }
}
