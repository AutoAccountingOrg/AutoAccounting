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
import net.ankio.auto.database.table.Assets

@Dao
interface AssetsDao {
    @Query("DELETE FROM Assets WHERE id=:id")
    suspend fun del(id: Int)

    @Insert
    suspend fun add(account: Assets)

    @Insert
    suspend fun addList(list: List<Assets>)

    @Query("UPDATE  Assets set sort=:sort WHERE id=:id")
    suspend fun setSort(
        id: Int,
        sort: Int,
    )

    @Query("SELECT * FROM  Assets WHERE name=:account limit 1")
    suspend fun get(account: String): Assets?

    @Query("SELECT * FROM Assets  order by id,sort")
    suspend fun loadAll(): List<Assets>

    @Query("DELETE FROM Assets")
    suspend fun deleteAll()
}
