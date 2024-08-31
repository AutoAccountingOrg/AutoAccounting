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
import org.ezbook.server.db.model.BookNameModel


@Dao
interface BookNameDao {

    //根据条件查询
    @Query("SELECT * FROM BookNameModel ORDER BY id DESC")
    fun load(): List<BookNameModel>

    @Insert
    fun insert(log: BookNameModel): Long

    @Query("DELETE FROM BookNameModel")
    fun clear()

    @Transaction
    fun put(data: Array<BookNameModel>) {
        clear()
        data.forEach {
            insert(it)
        }
    }
}