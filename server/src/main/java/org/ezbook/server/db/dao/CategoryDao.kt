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
import androidx.room.Update
import org.ezbook.server.db.model.CategoryModel


@Dao
interface CategoryDao {
    //根据条件查询
    @Query("SELECT * FROM CategoryModel WHERE remoteBookId=:book AND type = :type AND remoteParentId=:parent ORDER BY id ")
    suspend fun load(book: String, type: String, parent: String): List<CategoryModel>

    @Query("SELECT * FROM CategoryModel WHERE name =:name AND (:book IS NULL OR  remoteBookId = :book) AND (:type IS NULL OR  type = :type) ORDER BY id DESC LIMIT 1")
    suspend fun getByName(book: String?, type: String?, name: String): CategoryModel?

    @Query("SELECT * FROM CategoryModel WHERE id = :id")
    suspend fun getById(id: Long): CategoryModel?

    @Query("SELECT * FROM CategoryModel WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): CategoryModel?

    @Insert
    suspend fun insert(log: CategoryModel): Long

    @Update
    suspend fun update(category: CategoryModel): Int

    @Query("DELETE FROM CategoryModel WHERE id = :id")
    suspend fun delete(id: Long): Int

    @Query("DELETE FROM CategoryModel")
    suspend fun clear()

    @Transaction
    suspend fun put(data: Array<CategoryModel>) {
        clear()
        data.forEach {
            insert(it)
        }
    }

    @Query("SELECT * FROM CategoryModel")
    suspend fun all(): List<CategoryModel>
}