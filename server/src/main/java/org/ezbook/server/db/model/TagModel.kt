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

/**
 * 标签数据模型
 *
 * 用于存储账单标签信息，每个标签包含名称和分组
 */
@Entity
class TagModel {
    @PrimaryKey(autoGenerate = true)
    var id = 0L

    /**
     * 标签名称，不允许重复
     */
    var name: String = ""

    /**
     * 标签分组名称（如"场景"、"角色"等）
     */
    var group: String = ""

    override fun toString(): String {
        return "TagModel(id=$id, name='$name', group='$group')"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TagModel

        if (id != other.id) return false
        if (name != other.name) return false
        if (group != other.group) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + group.hashCode()
        return result
    }
}
