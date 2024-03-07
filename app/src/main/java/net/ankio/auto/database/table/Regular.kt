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

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import net.ankio.auto.database.data.ElementConverters
import net.ankio.auto.database.data.FlowElementList
import java.io.Serializable

@Entity
@TypeConverters(ElementConverters::class)
class Regular:Serializable {

    //自动分类规则
    @PrimaryKey(autoGenerate = true)
    var id = 0

    var use = true //是否启用该规则
    var sort = 0 //排序
    var auto = false //是否为自动创建

    var js = ""
    var text = ""

    var element: FlowElementList? = null
        get() {
            if (field == null) {
                field = FlowElementList(mutableListOf())
            }
            return field
        }
}
