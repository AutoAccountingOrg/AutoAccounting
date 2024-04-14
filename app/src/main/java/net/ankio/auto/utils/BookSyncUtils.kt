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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hjq.toast.Toaster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.database.Db
import net.ankio.auto.database.table.Assets
import net.ankio.auto.database.table.BookName
import net.ankio.auto.database.table.Category
import net.ankio.auto.events.AutoServiceErrorEvent
import net.ankio.auto.exceptions.AutoServiceException
import net.ankio.auto.utils.event.EventBus
import net.ankio.common.model.AssetsModel
import net.ankio.common.model.BookModel

object BookSyncUtils {
    suspend fun sync(context: Context) =
        withContext(Dispatchers.IO) {
            syncBook(context)
            syncAssets(context)
        }

    private suspend fun syncBook(context: Context) =
        withContext(Dispatchers.IO) {
            runCatching {
                val autoBooks = AutoAccountingServiceUtils.get("auto_books", context)
                // 同步账本数据和分类数据
                if (autoBooks.isNotEmpty()) {
                    Db.get().BookNameDao().deleteAll()
                    // 删除原有的分类
                    Db.get().CategoryDao().deleteAll()
                    val type = object : TypeToken<List<BookModel>>() {}
                    Gson().fromJson(autoBooks, type).forEach {
                        val bookModel = it
                        val bookName = BookName.fromModel(bookModel)
                        val id = Db.get().BookNameDao().insert(bookName)
                        Category.importModel(bookModel.category, id)
                    }
                    Logger.i("从记账软件同步【 账本和分类数据 】 成功")
                    AutoAccountingServiceUtils.delete("auto_books", context)
                    Toaster.show(R.string.sync_book_success)
                }
            }.onFailure {
                if (it is AutoServiceException) {
                    EventBus.post(AutoServiceErrorEvent(it))
                }
                Logger.e("从记账软件同步【 账本和分类数据 】 失败", it)
            }
        }

    private suspend fun syncAssets(context: Context) =
        withContext(Dispatchers.IO) {
            runCatching {
                val autoAssets = AutoAccountingServiceUtils.get("auto_assets", context)
                if (autoAssets.isNotEmpty()) {
                    val type = object : TypeToken<List<AssetsModel>>() {}
                    val assets = Gson().fromJson(autoAssets, type)
                    Db.get().AssetsDao().deleteAll()
                    assets.forEach {
                        Db.get().AssetsDao().add(Assets.fromModel(it))
                    }
                    Logger.i("从记账软件同步【 资产数据 】 成功")

                    AutoAccountingServiceUtils.delete("auto_assets", context)
                    Toaster.show(R.string.sync_assets_success)
                }
            }.onFailure {
                if (it is AutoServiceException) {
                    EventBus.post(AutoServiceErrorEvent(it))
                }
                Logger.e("从记账软件同步【 资产数据 】 失败", it)
            }
        }
}
