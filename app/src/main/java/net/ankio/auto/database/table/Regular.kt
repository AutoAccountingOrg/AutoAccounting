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
class Regular {

    //自动分类规则
    @PrimaryKey(autoGenerate = true)
    var id = 0

    /**
     * 分类结果
     */
    var category: String? = null

    /**
     * 账本结果，为空表示使用默认账本
     */
    var bookName: String? = null

    /**
     * 来源App，为空表示任意App
     */
    var app: String? = null

    /**
     * 商户名称，为空表示任意商户
     */
    var shopName:String? = null

    /**
     * 商品名称，为空表示任意商品
     */
    var shopitem:String? = null

    /**
     * 起始时间，为空表示任意时间
     */
    var timeStart :String? = null
    var timeEnd :String? = null

    /**
     * 金额区间，0.00表示任意金额
     */
    var moneyStart:Float = 0.00F
    var moneyEnd:Float = 0.00F
    var use = true //是否启用该规则
    var sort = 0 //排序
    var auto = false //是否为自动创建
}
