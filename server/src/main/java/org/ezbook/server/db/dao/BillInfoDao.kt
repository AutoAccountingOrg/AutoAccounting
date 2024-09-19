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

package org.ezbook.server.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import org.ezbook.server.constant.BillState
import org.ezbook.server.db.model.BillInfoModel

@Dao
interface BillInfoDao {
    @Insert
    fun insert(billInfo: BillInfoModel): Long

    @Query("SELECT * FROM BillInfoModel WHERE money = :money AND time >= :startTime AND time <= :endTime AND groupId = -1")
    fun query(money: Double, startTime: Long, endTime: Long): List<BillInfoModel>

    @Query("SELECT * FROM BillInfoModel WHERE id = :id")
    fun queryId(id: Long): BillInfoModel?

    @Update
    fun update(billInfo: BillInfoModel)

    // 删除策略，365天以前的所有数据（无论是否同步）
    @Query("DELETE FROM BillInfoModel WHERE time < :time")
    fun clearOld(time: Long)

    @Query("SELECT * FROM BillInfoModel WHERE groupId=-1 ORDER BY time DESC LIMIT :limit OFFSET :offset")
    fun loadPage(limit: Int, offset: Int): List<BillInfoModel>

    @Query("DELETE FROM BillInfoModel WHERE groupId = :groupId")
    fun deleteGroup(groupId: Long)

    @Query("DELETE FROM BillInfoModel WHERE id =:id")
    fun deleteId(id: Long)

    @Query("SELECT * FROM BillInfoModel WHERE state = 'Edited' and groupId = -1")
    fun queryNoSync(): List<BillInfoModel>

    @Query("UPDATE BillInfoModel SET state = :status WHERE id = :id")
    fun updateStatus(id: Long, status: BillState)

    @Query("SELECT * FROM BillInfoModel WHERE groupId = :groupId")
    fun queryGroup(groupId: Long): List<BillInfoModel>
}