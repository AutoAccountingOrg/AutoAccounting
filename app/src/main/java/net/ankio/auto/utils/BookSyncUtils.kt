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

package net.ankio.auto.utils

import android.content.Context
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.database.Db
import net.ankio.auto.database.table.BookName
import net.ankio.auto.database.table.Category
import net.ankio.common.model.BookModel

object BookSyncUtils {
    suspend fun syncBook(context: Context) = withContext(Dispatchers.IO) {
        val autoBooks = AutoAccountingServiceUtils.get("auto_books")
        //同步账本数据和分类数据
        if (autoBooks.isNotEmpty()) {
            Db.get().BookNameDao().deleteAll()
            //删除原有的分类
            Db.get().CategoryDao().deleteAll()
            val type = object : TypeToken<List<BookModel>>() {}
            Gson().fromJson(autoBooks, type).forEach {
                val bookModel = it
                val bookName = BookName.fromModel(bookModel)
                val id = Db.get().BookNameDao().insert(bookName)
                Category.importModel(bookModel.category, id)
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(context, R.string.sync_success, Toast.LENGTH_SHORT).show()
            }
         //   AutoAccountingServiceUtils.delete("auto_books")
        }
    }
}