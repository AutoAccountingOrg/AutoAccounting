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
import androidx.room.Transaction
import androidx.room.Update
import org.ezbook.server.db.model.CategoryRuleModel


@Dao
interface CategoryRuleDao {
    /**
     * 根据条件查询分类规则
     * 按 sort 升序排序（用户自定义顺序），sort 相同时按 id 降序排序（新规则在前）
     */
    @Query("SELECT * FROM CategoryRuleModel ORDER BY sort ASC, id DESC LIMIT :limit OFFSET :offset")
    suspend fun load(limit: Int, offset: Int): List<CategoryRuleModel>


    @Update
    suspend fun update(log: CategoryRuleModel): Int

    @Insert
    suspend fun insert(log: CategoryRuleModel): Long

    @Query("DELETE FROM CategoryRuleModel")
    suspend fun clear()

    @Transaction
    suspend fun put(data: Array<CategoryRuleModel>) {
        clear()
        data.forEach {
            insert(it)
        }
    }

    @Query("DELETE FROM CategoryRuleModel WHERE id = :id")
    suspend fun delete(id: Long)

    /**
     * 查询所有分类规则
     * 按 sort 升序排序（用户自定义顺序），sort 相同时按 id 降序排序（新规则在前）
     */
    @Query("SELECT * FROM CategoryRuleModel ORDER BY sort ASC, id DESC")
    suspend fun loadAll(): List<CategoryRuleModel>

    /**
     * 按创建者筛选规则
     * 按 sort 升序排序（用户自定义顺序），sort 相同时按 id 降序排序（新规则在前）
     */
    @Query("SELECT * FROM CategoryRuleModel WHERE creator = :creator ORDER BY sort ASC, id DESC")
    suspend fun loadByCreator(creator: String): List<CategoryRuleModel>
}