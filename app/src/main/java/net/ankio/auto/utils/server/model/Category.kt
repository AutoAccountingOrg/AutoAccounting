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
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.ImageUtils

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
    var parent: Int = 0

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
        suspend fun getDrawable(
            cateName: String,
            bookID: Int,
            type: Int,
            context: Context,
        ): Drawable {
            var newCateName = cateName
            if (newCateName.contains("-")) {
                newCateName = newCateName.split("-").last()
            }
            val categoryInfo = getByName(newCateName, bookID,type)
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
            return runCatching { Gson().fromJson(data as JsonArray, Array<Category>::class.java).toList() }.getOrDefault(emptyList())
        }

        suspend fun getByName(
            name: String,
            bookID: Int,
            type: Int
        ): Category? {
            val data = AppUtils.getService().sendMsg("cate/get/name", mapOf("cateName" to name, "book" to bookID, "type" to type))
            return runCatching { Gson().fromJson(data as JsonObject, Category::class.java) }.getOrNull()
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
