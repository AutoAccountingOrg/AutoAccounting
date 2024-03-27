/*
 * Copyright (C) 2023 ankio(ankio@ankio.net)
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
package net.ankio.auto.database.table

import android.content.Context
import android.widget.ImageView
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.database.Db
import net.ankio.auto.utils.ImageUtils
import net.ankio.common.model.BookModel

@Entity
class BookName {
    //账本列表
    @PrimaryKey(autoGenerate = true)
    var id = 0
    /**
     * 账户名
     */
    var name: String = ""

    /**
     * 图标是url或base64编码字符串
     */
    var icon: String = "" //图标

    companion object{
        fun fromModel(model: BookModel): BookName {
            return BookName().apply {
                name = model.name
                icon = model.icon
            }
        }

        suspend fun getByName(name:String) = withContext(Dispatchers.IO) {
            var book =  Db.get().BookNameDao().getByName(name)
            if (book == null) {
                book = BookName()
                book.name  = name
            }
            return@withContext book
        }
        suspend fun getDrawable(bookName: String, context: Context, imageView: ImageView) {
            imageView.setImageDrawable(
                ImageUtils.get(
                    context,
                    Db.get().BookNameDao().getByName(bookName)?.icon ?:"",
                    R.drawable.default_book
                )
            )
        }

    }
}
