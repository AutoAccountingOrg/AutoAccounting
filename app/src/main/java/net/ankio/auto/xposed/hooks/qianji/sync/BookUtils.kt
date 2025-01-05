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

package net.ankio.auto.xposed.hooks.qianji.sync

import android.content.Context
import com.google.gson.Gson
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.xposed.core.utils.AppRuntime
import net.ankio.auto.xposed.core.utils.MD5HashTable
import net.ankio.auto.xposed.core.utils.MessageUtils
import net.ankio.auto.xposed.hooks.qianji.models.Book
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.model.BookNameModel
import org.ezbook.server.db.model.SettingModel

class BookUtils(private val context: Context) {
    private val bookManagerInstance by lazy {
        XposedHelpers.callStaticMethod(
            AppRuntime.manifest.clazz("BookManager"),
            "getInstance",
        )
    }

    suspend fun getBooks(): List<*> = withContext(Dispatchers.IO) {
        XposedHelpers.callMethod(
            bookManagerInstance,
            "getAllBooks",
            context,
            true,
            1
        ) as List<*>
    }


    suspend fun getBookByName(name: String): Any {
        val list = getBooks()
        list.find { XposedHelpers.getObjectField(it, "name") == name }?.let {
            return it
        }
        return list.first()!!
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
            list.forEach {
                if (it == null) return@forEach
                val book = Book.fromObject(it)
                bookList.add(BookNameModel().apply {
                    name = book.getName()
                    icon = book.getCover()
                    remoteId = book.getBookId().toString()
                })
            }

            val sync = Gson().toJson(bookList)
            val md5 = MD5HashTable.md5(sync)
            val server = SettingModel.get(Setting.HASH_BOOK, "")
            if (server == md5 && !AppRuntime.debug) {
                AppRuntime.log("No need to Sync Books, Server md5:${server} local md5:${md5}")
                return@withContext bookList
            }
            AppRuntime.logD("Sync Books:$sync")
            BookNameModel.put(bookList, md5)
            withContext(Dispatchers.Main) {
                MessageUtils.toast("已同步账本信息到自动记账")
            }
            bookList
        }
}