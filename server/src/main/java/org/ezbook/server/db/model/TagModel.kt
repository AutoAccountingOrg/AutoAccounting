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
 * 用于存储账单标签信息，每个标签包含名称和颜色
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
     * 标签颜色，使用16进制颜色值（如#FF0000）
     */
    var color: String = "#2196F3"

    /**
     * 创建时间戳
     */
    var createTime: Long = System.currentTimeMillis()

    /**
     * 更新时间戳
     */
    var updateTime: Long = System.currentTimeMillis()

    override fun toString(): String {
        return "TagModel(id=$id, name='$name', color='$color', createTime=$createTime, updateTime=$updateTime)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TagModel

        if (id != other.id) return false
        if (name != other.name) return false
        if (color != other.color) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + color.hashCode()
        return result
    }
}
