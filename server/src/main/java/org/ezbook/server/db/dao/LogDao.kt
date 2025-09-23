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
import org.ezbook.server.db.model.LogModel

@Dao
interface LogDao {
    @Query(
        """
        SELECT * FROM LogModel 
        WHERE (:app = '' OR app = :app)
        AND (:levels IS NULL OR level IN (:levels))
        ORDER BY time DESC 
        LIMIT :limit OFFSET :offset
    """
    )
    suspend fun loadPage(
        limit: Int,
        offset: Int,
        app: String = "",
        levels: List<String>? = null
    ): List<LogModel>

    /**
     * 获取所有应用列表
     */
    @Query("SELECT DISTINCT app FROM LogModel ORDER BY app")
    suspend fun getApps(): List<String>

    @Insert
    suspend fun insert(log: LogModel): Long

    /**
     * 批量插入日志
     * 使用Room批量插入以提升性能，返回每条记录的自增ID
     */
    @Insert
    suspend fun insert(logs: List<LogModel>): List<Long>

    @Query("DELETE FROM LogModel")
    suspend fun clear()

    // only keep the latest 10000 records
    @Query("DELETE FROM LogModel WHERE id NOT IN (SELECT id FROM LogModel ORDER BY id DESC LIMIT 10000)")
    suspend fun clearOld()
}