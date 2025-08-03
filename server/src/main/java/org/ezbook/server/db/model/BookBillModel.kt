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
 * 记账软件里面的账单
 * 主要是 需要报销的、欠别人钱的、别人欠自己钱的
 */
@Entity
class BookBillModel {


    @PrimaryKey(autoGenerate = true)
    var id = 0L

    var money: Double = 0.00

    var time: Long = 0

    var remark: String = ""

    var remoteId: String = ""

    var remoteBookId: String = ""

    var category: String = ""
    var type: String = ""

    override fun toString(): String {
        return "BookBillModel(id=$id, money=$money, time=$time, remark='$remark', remoteId='$remoteId', remoteBookId='$remoteBookId', category='$category', type='$type')"
    }
}