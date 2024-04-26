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
import androidx.room.Update
import net.ankio.auto.database.table.AppData

@Dao
interface AppDataDao {
    @Query("DELETE FROM AppData WHERE id=:id")
    suspend fun del(id: Int)

    @Insert
    suspend fun add(appData: AppData)

    @Insert
    suspend fun addList(list: List<AppData>)

    @Update
    suspend fun update(appData: AppData)

    @Query("SELECT * FROM AppData  order by id")
    suspend fun loadAll(): List<AppData>

    @Query("DELETE FROM AppData")
    suspend fun deleteAll()

    @Query("DELETE FROM AppData WHERE id NOT IN (SELECT id FROM AppData ORDER BY id DESC LIMIT 500) ")
    suspend fun deleteLast500()
}
