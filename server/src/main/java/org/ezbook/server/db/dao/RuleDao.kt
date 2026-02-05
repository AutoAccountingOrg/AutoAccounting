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
import androidx.room.Update
import org.ezbook.server.db.model.RuleModel

@Dao
interface RuleDao {

    @Query(
        """
    SELECT * FROM RuleModel 
    WHERE (:app IS NULL OR app = :app )
    AND (:type IS NULL OR type = :type)
    AND (:searchTerm IS NULL OR name LIKE '%' || :searchTerm || '%')
    AND (:creator IS NULL OR :creator = '' OR creator = :creator)
    ORDER BY enabled DESC,id DESC 
    LIMIT :limit 
    OFFSET :offset
"""
    )
    suspend fun loadByAppAndFilters(
        limit: Int,
        offset: Int,
        app: String? = null,
        type: String? = null,
        searchTerm: String? = null,
        creator: String? = null
    ): List<RuleModel>

    //查询所有启用的规则，用于JS执行
    @Query("SELECT * FROM RuleModel WHERE app = :app AND type = :type AND enabled = 1")
    suspend fun loadAllEnabled(app: String, type: String): List<RuleModel>

    //查询所有禁用的规则，用于命中即丢弃的场景
    @Query("SELECT * FROM RuleModel WHERE app = :app AND type = :type AND enabled = 0")
    suspend fun loadAllDisabled(app: String, type: String): List<RuleModel>

    //按创建者查询所有启用的规则（系统/用户），用于JS执行
    @Query("SELECT * FROM RuleModel WHERE app = :app AND type = :type AND enabled = 1 AND creator = :creator")
    suspend fun loadAllEnabledByCreator(app: String, type: String, creator: String): List<RuleModel>

    //按创建者查询所有禁用的规则（系统/用户），用于命中即丢弃的场景
    @Query("SELECT * FROM RuleModel WHERE app = :app AND type = :type AND enabled = 0 AND creator = :creator")
    suspend fun loadAllDisabledByCreator(
        app: String,
        type: String,
        creator: String
    ): List<RuleModel>


    @Insert
    suspend fun insert(rule: RuleModel): Long

    @Update
    suspend fun update(rule: RuleModel)

    @Query("DELETE FROM RuleModel WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("SELECT app FROM RuleModel")
    suspend fun queryApps(): List<String>

    @Query("SELECT * FROM RuleModel WHERE creator = 'system'")
    suspend fun loadAllSystem(): List<RuleModel>


    @Query("SELECT * FROM RuleModel WHERE app = :app AND type = :type AND name = :name limit 1")
    suspend fun query(type: String, app: String, name: String): RuleModel?

    @Query("SELECT * FROM RuleModel WHERE name = :name AND creator = 'system' limit 1")
    suspend fun loadSystemRule(name: String): RuleModel?

    @Query("DELETE FROM RuleModel WHERE creator = 'system' AND updateAt < :time")
    suspend fun deleteSystemRule(time: Long)
}