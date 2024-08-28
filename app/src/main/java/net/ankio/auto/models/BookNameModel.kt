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
package net.ankio.auto.models

import android.content.Context
import android.widget.ImageView
import net.ankio.auto.R
import net.ankio.auto.storage.ImageUtils

class BookNameModel {
    // 账本列表
    var id: Int = 0

    /**
     * 账户名
     */
    var name: String = ""

    /**
     * 图标是url或base64编码字符串
     */
    var icon: String = "" // 图标

    companion object {

        suspend fun list(): List<BookNameModel> {
           /* val data = AppUtils.getService().sendMsg("bookname/list", mapOf("size" to 0, "page" to 1))
            return runCatching { Gson().fromJson(data as JsonArray, Array<BookNameModel>::class.java).toList() }
                .getOrNull() ?: emptyList()*/
            return emptyList()
        }



        suspend fun getOne(): BookNameModel? {
            /*val data = AppUtils.getService().sendMsg("bookname/list", mapOf("size" to 1, "page" to 1))
            return runCatching { Gson().fromJson(data as JsonArray, Array<BookNameModel>::class.java).toList() }
                .getOrNull()?.first()*/
            return null

        }

        suspend fun getByName(name: String): BookNameModel {
         /*   val data = AppUtils.getService().sendMsg("bookname/get", mapOf("name" to name))
            return runCatching { Gson().fromJson(data as JsonObject, BookNameModel::class.java) }.getOrNull()
                ?: BookNameModel().apply { this.name = name }*/
            return BookNameModel()
        }



        suspend fun getDefaultBook(bookName: String): BookNameModel {
            if (bookName == "默认账本") {
                var book = getOne()
                if (book == null) {
                    book = BookNameModel()
                    book.name = bookName
                }
                return book
            } else {
                return getByName(bookName)
            }
        }

        suspend fun getDrawable(
            bookName: String,
            context: Context,
            imageView: ImageView,
        ) {
            imageView.setImageDrawable(
                ImageUtils.get(
                    context,
                    getDefaultBook(bookName).icon,
                    R.drawable.default_book,
                ),
            )
        }
    }
}
