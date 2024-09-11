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

package net.ankio.auto.hooks.qianji.sync

import android.content.Context
import com.google.gson.Gson
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.core.App
import net.ankio.auto.core.api.HookerManifest
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.model.BookNameModel
import org.ezbook.server.db.model.SettingModel

class BookUtils(
    private val manifest: HookerManifest,
    private val classLoader: ClassLoader,
    private val context: Context
) {
    private val bookManagerInstance by lazy {
        XposedHelpers.callStaticMethod(
            manifest.clazz("BookManager", classLoader),
            "getInstance",
        )
    }

    private val bookClazz by lazy {
        classLoader.loadClass("com.mutangtech.qianji.data.model.Book")
    }

    suspend fun syncBooks(): ArrayList<BookNameModel> =
        withContext(Dispatchers.IO) {
            val list = XposedHelpers.callMethod(
                bookManagerInstance,
                "getAllBooks",
                context,
                true,
                1
            ) as List<*>
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
            list.forEach { book ->
                if (bookClazz.isInstance(book)) {
                    val bookModel = BookNameModel()
                    // Get all fields of the Book class
                    val fields = bookClazz.declaredFields
                    for (field in fields) {
                        field.isAccessible = true
                        val value = field.get(book)
                        when (field.name) {
                            "name" -> bookModel.name = value as String
                            "cover" -> bookModel.icon = value as String
                            "bookId" -> bookModel.remoteId = (value as Long).toString()
                        }
                    }
                    bookList.add(bookModel)
                }
            }

            val sync = Gson().toJson(bookList)
            val md5 = App.md5(sync)
            val server = SettingModel.get(Setting.HASH_BOOK, "")
            if (server == md5) {
                manifest.log("账本信息未发生变化，无需同步, 服务端md5:${server} 本地md5:${md5}")
                return@withContext bookList
            }
            manifest.log("同步账本信息:$sync")
            BookNameModel.put(bookList, md5)
            withContext(Dispatchers.Main) {
                App.toast("已同步账本信息到自动记账")
            }
            bookList
        }
}