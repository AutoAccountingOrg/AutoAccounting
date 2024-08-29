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
import org.ezbook.server.db.model.AppDataModel

@Dao
interface AppDataDao {

    //根据条件查询
    @Query("SELECT * FROM AppDataModel WHERE app = :app ORDER BY id DESC LIMIT :limit OFFSET :offset")
    fun loadByAppAndType(limit: Int, offset: Int,app: String): List<AppDataModel>

    //根据条件查询
    @Query("SELECT * FROM AppDataModel WHERE app = :app AND type = :type ORDER BY id DESC LIMIT :limit OFFSET :offset")
    fun loadByAppAndType(limit: Int, offset: Int,app: String, type: String): List<AppDataModel>


    // 统计总数
    @Query("SELECT COUNT(*) FROM AppDataModel WHERE app = :app AND type = :type")
    fun count(app: String,type: String): Int

    // 统计总数
    @Query("SELECT COUNT(*) FROM AppDataModel WHERE app = :app ")
    fun count(app: String): Int
    @Insert
    fun insert(log: AppDataModel): Long

    @Query("DELETE FROM AppDataModel")
    fun clear()
}