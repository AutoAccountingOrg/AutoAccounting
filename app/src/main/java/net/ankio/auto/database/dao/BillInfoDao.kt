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
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import net.ankio.auto.database.table.BillInfo
import net.ankio.auto.database.table.BillInfoGroup
import net.ankio.common.constant.BillType

@Dao
interface BillInfoDao {

    @Query("SELECT DISTINCT groupId FROM BillInfo WHERE money = :money AND type = :type AND timeStamp >= :timestamp  and groupId != 0")
    suspend fun findDistinctNonZeroGroupIds(money: Int, type: BillType, timestamp: Long): List<Int>

    @Query("SELECT * FROM BillInfo WHERE money = :money AND type = :type AND timeStamp >= :timestamp and groupId=:groupId")
    suspend fun findDuplicateBills(money: Int, type: BillType, timestamp: Long, groupId:Int): List<BillInfo>

    @Query("SELECT * FROM BillInfo WHERE groupId = :groupId")
    suspend fun findParentBill(groupId: Int): BillInfo?

    @Insert
    suspend fun insert(billInfo: BillInfo):Long
    @Update
    suspend fun update(billInfo: BillInfo)
    @Delete
    suspend fun delete(billInfo: BillInfo)
    @Query("SELECT  * FROM BillInfo where id in (:ids)")
    suspend fun getTotal(ids:List<Int>): Array<BillInfo>
    @Query("DELETE FROM BillInfo")
    suspend fun empty()

    //只保留最近500条数据
    @Query("DELETE FROM BillInfo WHERE id NOT IN (SELECT id FROM BillInfo ORDER BY timeStamp DESC LIMIT 500)")
    suspend fun keepLatest300()

    @Query("SELECT * FROM BillInfo where  groupId == 0  and syncFromApp=0")
    suspend fun getAllParents(): Array<BillInfo>

    @Query("Update BillInfo set syncFromApp=1 where  groupId == 0 and syncFromApp=0"   )
    suspend fun setAllParents()

    @Query("SELECT  strftime('%Y-%m-%d', timeStamp / 1000, 'unixepoch') as date,group_concat(id) as ids FROM BillInfo where  groupId == 0 group by date order by date desc")
    suspend fun getListGroup(): List<BillInfoGroup>
}
