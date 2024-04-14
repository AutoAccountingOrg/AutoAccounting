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
import net.ankio.auto.database.table.Category

@Dao
interface CategoryDao {
    @Query("SELECT * FROM Category where book = :book and parent = :parent and type = :type order by id,sort")
    suspend fun loadAll(
        book: Int,
        type: Int = 0,
        parent: Int = -1,
    ): Array<Category?>?

    @Query("SELECT count(*) FROM Category where book=:book and parent = :parent and type = :type")
    suspend fun count(
        book: Int,
        parent: Int,
        type: Int = 0,
    ): Int

    @Insert
    suspend fun add(data: Category)

    @Query("SELECT * FROM Category where name=:cateName and book=:book limit 1")
    suspend fun get(
        cateName: String,
        book: Long,
    ): Category?

    @Query("SELECT * FROM Category where remoteId=:remote and book=:book limit 1")
    suspend fun getRemote(
        remote: String,
        book: Int,
    ): Category?

    @Query("DELETE FROM Category")
    suspend fun deleteAll()
}
