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
import org.ezbook.server.db.model.CategoryMapModel


@Dao
interface CategoryMapDao {

    @Query("SELECT * FROM CategoryMapModel WHERE  (:searchTerm IS NULL OR name LIKE '%' || :searchTerm || '%') ORDER BY id DESC LIMIT :limit OFFSET :offset")
    suspend fun loadWithLimit(limit: Int, offset: Int, searchTerm: String?): List<CategoryMapModel>

    @Query("SELECT * FROM CategoryMapModel ORDER BY id DESC")
    suspend fun loadWithoutLimit(): List<CategoryMapModel>

    @Insert
    suspend fun insert(log: CategoryMapModel): Long

    @Update
    suspend fun update(log: CategoryMapModel)

    @Query("DELETE FROM CategoryMapModel")
    suspend fun clear()

    @Query("SELECT * FROM CategoryMapModel WHERE name = :name limit 1")
    suspend fun query(name: String): CategoryMapModel?

    @Query("DELETE FROM CategoryMapModel WHERE id = :id")
    suspend fun delete(id: Long)
}