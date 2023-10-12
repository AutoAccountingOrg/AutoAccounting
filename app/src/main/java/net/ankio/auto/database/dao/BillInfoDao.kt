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
package net.ankio.auto.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import net.ankio.auto.constant.BillType
import net.ankio.auto.database.table.BillInfo

@Dao
interface BillInfoDao {

    @Query("SELECT DISTINCT groupId FROM BillInfo WHERE money = :money AND type = :type AND timeStamp >= :timestamp  and groupId != 0")
    fun findDistinctNonZeroGroupIds(money: Float, type: BillType, timestamp: Long): List<Int>

    @Query("SELECT * FROM BillInfo WHERE money = :money AND type = :type AND timeStamp >= :timestamp and groupId=:groupId")
    fun findDuplicateBills(money: Float, type: BillType, timestamp: Long,groupId:Int): List<BillInfo>

    @Query("SELECT * FROM BillInfo WHERE groupId = :groupId")
    fun findParentBill(groupId: Int): BillInfo?

    @Update
    fun update(billInfo: BillInfo)
    @Insert
    fun insert(billInfo: BillInfo):Long
}
