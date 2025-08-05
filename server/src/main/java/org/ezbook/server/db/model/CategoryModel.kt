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

package org.ezbook.server.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.ezbook.server.constant.BillType

@Entity
class CategoryModel {
    @PrimaryKey(autoGenerate = true)
    var id = 0L

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
    var remoteBookId: String = ""
    var remoteParentId: String = ""


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
    var type: BillType = BillType.Income

    fun isPanel() = remoteId == "-9999"

    fun isAddBtn() = remoteId == "-9998"

    fun isChild() = remoteParentId != "-1"

    override fun toString(): String {
        return "CategoryModel(id=$id, name=$name, icon=$icon, remoteId='$remoteId', remoteBookId='$remoteBookId', remoteParentId='$remoteParentId', book=$book, sort=$sort, type=$type)"
    }
}