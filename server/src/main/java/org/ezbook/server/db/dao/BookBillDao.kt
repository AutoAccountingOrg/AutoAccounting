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
import org.ezbook.server.db.model.BookBillModel

@Dao
interface BookBillDao {
    @Insert
    fun insert(bookBill: BookBillModel): Long

    @Query("SELECT * FROM BookBillModel")
    fun list(): List<BookBillModel>

    @Query("DELETE FROM BookBillModel")
    fun clear()


    @Transaction
    fun put(list: List<BookBillModel>) {
        clear()
        list.forEach {
            insert(it)
        }
    }

}