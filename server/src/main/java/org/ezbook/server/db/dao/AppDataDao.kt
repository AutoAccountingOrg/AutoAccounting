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
    @Query("SELECT * FROM AppDataModel WHERE app = :app " +
            "AND (:type IS NULL OR type = :type)" +
            "AND (:search IS NULL OR data LIKE '%' || :search || '%')" +
            "ORDER BY id DESC LIMIT :limit OFFSET :offset")
    fun load(limit: Int, offset: Int,app: String,type: String?,search: String?): List<AppDataModel>


    @Insert
    fun insert(log: AppDataModel): Long

    @Query("DELETE FROM AppDataModel")
    fun clear()

    //只保留最近的2000条数据
    @Query("DELETE FROM AppDataModel WHERE id NOT IN (SELECT id FROM AppDataModel ORDER BY id DESC LIMIT 2000)")
    fun clearOld()

    //查询所有app
    @Query("SELECT app FROM AppDataModel")
     fun queryApps(): List<String>
}