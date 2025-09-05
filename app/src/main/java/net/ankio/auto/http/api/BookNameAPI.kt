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
import net.ankio.auto.storage.Logger
import org.ezbook.server.constant.DefaultData
import org.ezbook.server.db.model.BookNameModel
import org.ezbook.server.tools.runCatchingExceptCancel
import java.net.URLEncoder

/**
 * 账本名称API类，提供账本相关的网络请求操作
 * 所有方法都是挂起函数，需要在协程作用域内调用
 */
object BookNameAPI {
    /**
     * 获取所有账本列表
     * @return 账本模型列表，如果请求失败则返回空列表
     */
    suspend fun list(): List<BookNameModel> = withContext(Dispatchers.IO) {

        return@withContext runCatchingExceptCancel {
            val resp = LocalNetwork.post<List<BookNameModel>>("book/list", "{}").getOrThrow()
            resp.data ?: emptyList()
        }.getOrElse {
            Logger.e("list error: ${it.message}", it)
            emptyList()
        }
    }

    /**
     * 根据名称从服务端获取账本（含默认回退逻辑，由后端兜底）。
     */
    suspend fun getBook(bookName: String): BookNameModel = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(bookName, Charsets.UTF_8.name())

        return@withContext runCatchingExceptCancel {
            val resp = LocalNetwork.get<BookNameModel>("book/get?name=$encoded").getOrThrow()
            resp.data ?: BookNameModel().apply {
                name = bookName.ifEmpty { DefaultData.DEFAULT_BOOK_NAME }
            }
        }.getOrElse {
            Logger.e("getBook error: ${it.message}", it)
            BookNameModel().apply {
                name = bookName.ifEmpty { DefaultData.DEFAULT_BOOK_NAME }
            }
        }
    }

    /**
     * 从服务端获取默认账本（含回退逻辑）。
     */
    suspend fun getDefaultBook(): BookNameModel = withContext(Dispatchers.IO) {

        return@withContext runCatchingExceptCancel {
            val resp = LocalNetwork.get<BookNameModel>("book/default").getOrThrow()
            resp.data ?: BookNameModel().apply { name = DefaultData.DEFAULT_BOOK_NAME }
        }.getOrElse {
            Logger.e("getDefaultBook error: ${it.message}", it)
            BookNameModel().apply { name = DefaultData.DEFAULT_BOOK_NAME }
        }
    }


    /**
     * 更新账本列表
     * @param bookList 要更新的账本列表
     * @param md5 账本列表的MD5校验值
     * @return 服务器响应结果
     */
    suspend fun put(bookList: ArrayList<BookNameModel>, md5: String) =
        withContext(Dispatchers.IO) {

            runCatchingExceptCancel {
                LocalNetwork.post<String>("book/put?md5=$md5", Gson().toJson(bookList)).getOrThrow()
            }.getOrElse {
                Logger.e("put error: ${it.message}", it)

            }
        }

    /** 新增单个账本 */
    suspend fun add(book: BookNameModel) = withContext(Dispatchers.IO) {

        runCatchingExceptCancel {
            LocalNetwork.post<String>("book/add", Gson().toJson(book)).getOrThrow()
        }.getOrElse {
            Logger.e("add error: ${it.message}", it)

        }
    }

    /** 更新单个账本 */
    suspend fun update(book: BookNameModel) = withContext(Dispatchers.IO) {

        runCatchingExceptCancel {
            LocalNetwork.post<String>("book/update", Gson().toJson(book)).getOrThrow()
        }.getOrElse {
            Logger.e("update error: ${it.message}", it)

        }
    }

    /**
     * 删除指定账本
     * @param bookId 要删除的账本ID
     * @return 服务器响应结果
     */
    suspend fun delete(bookId: Long) = withContext(Dispatchers.IO) {

        runCatchingExceptCancel {
            val json = JsonObject().apply {
                addProperty("id", bookId)
            }
            LocalNetwork.post<String>("book/delete", Gson().toJson(json)).getOrThrow()
        }.getOrElse {
            Logger.e("delete error: ${it.message}", it)

        }
    }
}
