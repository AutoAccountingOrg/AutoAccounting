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
import org.ezbook.server.db.model.AssetsMapModel


@Dao
interface AssetMapDao {

    //根据条件查询
    @Query("SELECT * FROM AssetsMapModel ORDER BY id DESC limit :limit offset :offset")
    fun load(limit: Int, offset: Int): List<AssetsMapModel>

    @Insert
    fun insert(log: AssetsMapModel): Long

    @Update
    fun update(log: AssetsMapModel)

    @Query("DELETE FROM AssetsMapModel")
    fun clear()

    @Query("SELECT * FROM AssetsMapModel WHERE name = :name limit 1")
    fun query(name: String): AssetsMapModel?

    @Query("DELETE FROM AssetsMapModel WHERE id = :id")
    fun delete(id: Long)
}