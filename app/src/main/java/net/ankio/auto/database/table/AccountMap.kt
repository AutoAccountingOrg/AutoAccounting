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

@Entity
class AccountMap {
    //账户列表
    @PrimaryKey(autoGenerate = true)
    var id = 0
    /**
     * 是否将原始映射的账户名作为正则使用
     */
    var regex = false
    /**
     * 原始获取到的账户名
     */
    var name: String? = null //账户名

    /**
     * 映射到的账户名
     */
    var mapName: String? = null //映射账户名
}
