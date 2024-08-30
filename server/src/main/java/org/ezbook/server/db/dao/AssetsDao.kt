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
import androidx.room.Transaction
import org.ezbook.server.db.model.AssetsModel


@Dao
interface AssetsDao {

    //根据条件查询
    @Query("SELECT * FROM AssetsModel ORDER BY id DESC LIMIT :limit OFFSET :offset")
    fun load(limit: Int, offset: Int): List<AssetsModel>

    //根据条件查询
    @Query("SELECT * FROM AssetsModel WHERE type = :type ORDER BY id DESC LIMIT :limit OFFSET :offset")
    fun load(limit: Int, offset: Int, type: String): List<AssetsModel>

    // 统计总数
    @Query("SELECT COUNT(*) FROM AssetsModel WHERE type = :type")
    fun count(type: String): Int

    // 统计总数
    @Query("SELECT COUNT(*) FROM AssetsModel")
    fun count(): Int
    @Insert
    fun insert(log: AssetsModel): Long

    @Query("DELETE FROM AssetsModel")
    fun clear()

    @Transaction
    fun put(data: Array<AssetsModel>) {
        clear()
        data.forEach {
            insert(it)
        }
    }
}