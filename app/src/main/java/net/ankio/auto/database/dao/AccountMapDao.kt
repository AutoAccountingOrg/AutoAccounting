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
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import net.ankio.auto.constant.BillType
import net.ankio.auto.database.table.AccountMap
import net.ankio.auto.database.table.BillInfo

@Dao
interface AccountMapDao {
    @Query("SELECT * FROM AccountMap")
    suspend fun getAll(): List<AccountMap>
    @Insert
    suspend fun insert(accountMap: AccountMap):Long
    @Update
    suspend fun update(accountMap: AccountMap)
    @Delete
    suspend fun delete(accountMap: AccountMap)
}
