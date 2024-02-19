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

import androidx.room.Entity
import androidx.room.PrimaryKey
import net.ankio.auto.database.Db
import net.ankio.auto.utils.Logger
import net.ankio.common.model.CategoryModel

@Entity
class Category {


    @PrimaryKey(autoGenerate = true)
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
    var sort: Int = 0 //排序

    /**
     * 分类类型，0：支出，1：收入

     */
    var type: Int = 0
    companion object{
        suspend fun importModel(model: List<CategoryModel>,bookID: Long) {
            //排序
            val sortedModel = model.sortedWith(compareBy<CategoryModel> { it.parent != "-1" }.thenBy { it.sort })
           sortedModel.forEach {
                Category().apply {
                    name = it.name
                    icon = it.icon
                    book = bookID.toInt()
                    sort = it.sort
                    type = it.type
                    remoteId = it.id
                    if (it.parent != "-1") {
                        Db.get().CategoryDao().getRemote(it.parent,book)?.let { it2 ->
                            parent = it2.id
                        }
                    }
                   Db.get().CategoryDao().add(this)
                }
            }

        }
    }

    fun isPanel(): Boolean {
        return remoteId === "-9999"
    }

    override fun toString(): String {
        return "Category(id=$id, name=$name, icon=$icon, remoteId='$remoteId', parent=$parent, book=$book, sort=$sort, type=$type)"
    }

}
