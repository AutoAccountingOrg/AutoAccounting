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

package net.ankio.auto.xposed.hooks.qianji.impl

import com.google.gson.Gson
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.xposed.core.utils.AppRuntime
import org.ezbook.server.tools.MD5HashTable
import net.ankio.auto.xposed.core.utils.MessageUtils
import net.ankio.auto.xposed.hooks.qianji.models.Book
import org.ezbook.server.constant.Setting
import net.ankio.auto.http.api.BookNameAPI
import net.ankio.auto.http.api.SettingAPI
import org.ezbook.server.db.model.BookNameModel
import org.ezbook.server.db.model.SettingModel

object BookManagerImpl {
    private val bookManagerInstance by lazy {
        XposedHelpers.callStaticMethod(
            AppRuntime.clazz("BookManager"),
            "getInstance",
        )
    }

    suspend fun getBooks(): List<*> = withContext(Dispatchers.IO) {
        XposedHelpers.callMethod(
            bookManagerInstance,
            "getAllBooks",
            AppRuntime.application!!,
            true,
            1
        ) as List<*>
    }


    suspend fun getBookByName(name: String): Book {
        val list = getBooks()
        list.find { XposedHelpers.getObjectField(it, "name") == name }?.let {
            return Book.fromObject(it)
        }
        return Book.fromObject(list.first()!!)
    }

    suspend fun syncBooks(): ArrayList<BookNameModel> =
        withContext(Dispatchers.IO) {
            val list = getBooks()
            val bookList = arrayListOf<BookNameModel>()


            /**
             * [
             *     {
             *         "bookId":-1,
             *         "cover":"http://res.qianjiapp.com/headerimages2/daniela-cuevas-t7YycgAoVSw-unsplash20_9.jpg!headerimages2",
             *         "createtimeInSec":0,
             *         "expired":0,
             *         "memberCount":1,
             *         "name":"日常账本",
             *         "sort":0,
             *         "type":-1,
             *         "updateTimeInSec":0,
             *         "userid":"",
             *         "visible":1
             *     }
             * ]
             */



            /**
             * [
             *     {
             *         "bookId":-1,
             *         "cover":"http://res.qianjiapp.com/headerimages2/daniela-cuevas-t7YycgAoVSw-unsplash20_9.jpg!headerimages2",
             *         "createtimeInSec":0,
             *         "expired":0,
             *         "memberCount":1,
             *         "name":"日常账本",
             *         "sort":0,
             *         "type":-1,
             *         "updateTimeInSec":0,
             *         "userid":"",
             *         "visible":1
             *     }
             * ]
             */
            list.forEach {
                if (it == null) return@forEach
                val book = Book.fromObject(it)
                bookList.add(BookNameModel().apply {
                    name = book.getName()
                    icon = book.getCover() ?: ""
                    remoteId = book.getBookId().toString()
                })
            }

            val sync = Gson().toJson(bookList)
            val md5 = MD5HashTable.md5(sync)
            val server = SettingAPI.get(Setting.HASH_BOOK, "")
            if (server == md5 && !AppRuntime.debug) {
                AppRuntime.log("No need to Sync Books, Server md5:${server} local md5:${md5}")
                return@withContext bookList
            }
            AppRuntime.logD("Sync Books:$sync")
            BookNameAPI.put(bookList, md5)
            withContext(Dispatchers.Main) {
                MessageUtils.toast("已同步账本信息到自动记账")
            }
            bookList
        }
}