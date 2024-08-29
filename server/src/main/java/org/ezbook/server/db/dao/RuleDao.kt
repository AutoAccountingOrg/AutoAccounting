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
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import org.ezbook.server.db.model.RuleModel

@Dao
interface RuleDao {

    //根据条件查询
    @Query("SELECT * FROM RuleModel WHERE app = :app ORDER BY id DESC LIMIT :limit OFFSET :offset")
    fun loadByAppAndType(limit: Int, offset: Int,app: String): List<RuleModel>

    //根据条件查询
    @Query("SELECT * FROM RuleModel WHERE app = :app AND type = :type ORDER BY id DESC LIMIT :limit OFFSET :offset")
    fun loadByAppAndType(limit: Int, offset: Int,app: String, type: String): List<RuleModel>

    //查询所有启用的规则，用于JS执行
    @Query("SELECT * FROM RuleModel WHERE app = :app AND type = :type AND enabled = 1")
    fun loadAllEnabled(app: String, type: String): List<RuleModel>

    // 统计总数
    @Query("SELECT COUNT(*) FROM RuleModel WHERE app = :app ")
    fun count(app: String): Int

    // 统计总数
    @Query("SELECT COUNT(*) FROM RuleModel WHERE app = :app AND type = :type")
    fun count(app: String,type: String): Int

    @Insert
    fun insert(rule:RuleModel): Long

    @Update
    fun update(rule:RuleModel)

    @Query("DELETE FROM RuleModel WHERE id = :id")
    fun delete(id: Int)

    @Query("SELECT app FROM RuleModel")
    fun queryApps():List<String>

    @Query("SELECT * FROM RuleModel WHERE creator = 'system'")
     fun loadAllSystem():List<RuleModel>


    @Query("SELECT * FROM RuleModel WHERE app = :app AND type = :type AND name = :name limit 1")
    fun query(type: String,app:String,name:String):RuleModel?
}