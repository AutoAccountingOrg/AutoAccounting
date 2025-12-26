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
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 资产名称映射表
 * 按 name 建唯一索引，便于使用 INSERT ... REPLACE 进行幂等写入
 */
@Entity(
    indices = [Index(value = ["name"], unique = true)]
)
class AssetsMapModel {
    @PrimaryKey(autoGenerate = true)
    var id = 0L

    /**
     * 是否将原始映射的账户名作为正则使用
     */
    var regex: Boolean = false

    /**
     * 原始获取到的账户名
     */
    var name: String = "" // 账户名

    /**
     * 映射到的账户名
     */
    var mapName: String = "" // 映射账户名

    /**
     * 排序优先级，值越小优先级越高
     * 用于控制规则匹配顺序，更具体的规则应排在前面
     */
    var sort: Int = 0

    override fun toString(): String {
        return "AssetsMapModel(id=$id, regex=$regex, name='$name', mapName='$mapName', sort=$sort)"
    }
}