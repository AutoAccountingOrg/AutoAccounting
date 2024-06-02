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
package net.ankio.auto.utils.server.model

import android.content.Context
import android.graphics.drawable.Drawable
import com.google.gson.Gson
import com.google.gson.JsonNull
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.ImageUtils
import net.ankio.common.model.CategoryModel

class Category {
    var id = 0

    /**
     * 分类名称
     */
    var name: String? = null

    /**
     * 分类图标Url或base64
     */
    var icon: String? = null

    /**
     * 远程id
     */
    var remoteId: String = ""

    /**
     * 父类id
     */
    var parent: Int = -1

    /**
     * 所属账本
     */
    var book: Int = 1

    /**
     * 排序
     */
    var sort: Int = 0 // 排序

    /**
     * 分类类型，0：支出，1：收入

     */
    var type: Int = 0

    companion object {
        suspend fun importModel(
            model: List<CategoryModel>,
            bookID: Long,
        ) {
            // 排序
            val sortedModel =
                model.sortedWith(compareBy<CategoryModel> { it.parent != "-1" }.thenBy { it.sort })
            sortedModel.forEach {
                Category().apply {
                    name = it.name
                    icon = it.icon
                    book = bookID.toInt()
                    sort = it.sort
                    type = it.type
                    remoteId = it.id
                    if (it.parent != "-1") {
                        getByRemote(it.parent, book)?.let { it2 ->
                            parent = it2.id
                        }
                    }

                    put(this)
                }
            }
        }

        suspend fun getDrawable(
            cateName: String,
            bookID: Int,
            context: Context,
        ): Drawable {
            var newCateName = cateName
            if (newCateName.contains("-")) {
                newCateName = newCateName.split("-").last()
            }
            val categoryInfo = getByName(newCateName, bookID)
            return ImageUtils.get(context, categoryInfo?.icon ?: "", R.drawable.default_cate)
        }

        fun put(cate: Category) {
            AppUtils.getScope().launch {
                AppUtils.getService().sendMsg("cate/put", cate)
            }
        }

        suspend fun getAll(
            bookID: Int,
            type: Int,
            parent: Int,
        ): List<Category> {
            val data = AppUtils.getService().sendMsg("cate/get/all", mapOf("book" to bookID, "type" to type, "parent" to parent))
            return if (data !is JsonNull) {
                Gson().fromJson(Gson().toJson(data), Array<Category>::class.java).toList()
            } else {
                emptyList()
            }
        }

        suspend fun getByName(
            name: String,
            bookID: Int,
        ): Category? {
            val data = AppUtils.getService().sendMsg("cate/get/name", mapOf("name" to name, "book" to bookID))
            return runCatching { Gson().fromJson(data as String, Category::class.java) }.getOrNull()
        }

        suspend fun getByRemote(
            remoteId: String,
            book: Int,
        ): Category?  {
            val data = AppUtils.getService().sendMsg("cate/get/remote", mapOf("remoteId" to remoteId, "book" to book))
            return runCatching { Gson().fromJson(data as String, Category::class.java) }.getOrNull()
        }

        suspend fun remove(id: Int) {
            AppUtils.getService().sendMsg("cate/remove", mapOf("id" to id))
        }
    }

    fun isPanel(): Boolean {
        return remoteId === "-9999"
    }

    override fun toString(): String {
        return "Category(id=$id, name=$name, icon=$icon, remoteId='$remoteId', parent=$parent, book=$book, sort=$sort, type=$type)"
    }
}
