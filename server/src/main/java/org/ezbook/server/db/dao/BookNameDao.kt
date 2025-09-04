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
import androidx.room.Update
import androidx.room.Query
import androidx.room.Transaction
import org.ezbook.server.db.model.BookNameModel
import androidx.room.OnConflictStrategy


@Dao
interface BookNameDao {

    //根据条件查询
    @Query("SELECT * FROM BookNameModel ORDER BY id DESC")
    suspend fun load(): List<BookNameModel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(book: BookNameModel): Long

    @Update
    suspend fun update(book: BookNameModel): Int

    @Query("DELETE FROM BookNameModel")
    suspend fun clear()

    /**
     * 根据ID删除指定账本
     * @param id 账本ID
     */
    @Query("DELETE FROM BookNameModel WHERE id = :id")
    suspend fun delete(id: Long)

    @Transaction
    suspend fun put(data: Array<BookNameModel>) {
        clear()
        data.forEach {
            insertOrReplace(it)
        }
    }
}