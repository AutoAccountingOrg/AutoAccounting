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
import net.ankio.auto.database.table.AppData
import net.ankio.auto.database.table.BookName
import net.ankio.auto.database.table.Category

@Dao
interface CategoryDao {
    @Query("SELECT * FROM Category where book=:book and parent = :id")
    suspend fun loadAll(book:Int,id:Int = -1): Array<Category?>?

    @Query("SELECT count(*) FROM Category where book=:book and parent = :id")
    suspend fun count(book:Int,id: Int):Int

    @Insert
    suspend fun add(data: Category)
    @Query("SELECT * FROM Category where name=:cateName limit 1")
    suspend fun get(cateName: String): Category?
}
