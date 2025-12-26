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
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import org.ezbook.server.db.model.AssetsMapModel


@Dao
interface AssetMapDao {

    /**
     * 分页查询资产映射列表
     * 优先按sort排序，sort相同则按id倒序
     */
    @Query("SELECT * FROM AssetsMapModel ORDER BY sort ASC, id DESC limit :limit offset :offset")
    suspend fun load(limit: Int, offset: Int): List<AssetsMapModel>

    /**
     * 获取全部资产映射列表
     * 优先按sort排序，确保规则匹配顺序正确
     */
    @Query("SELECT * FROM AssetsMapModel ORDER BY sort ASC, id DESC")
    suspend fun list(): List<AssetsMapModel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: AssetsMapModel): Long

    @Update
    suspend fun update(log: AssetsMapModel)

    /**
     * 批量更新排序值
     * @param items 包含新排序值的映射列表
     */
    @Update
    suspend fun updateAll(items: List<AssetsMapModel>)

    @Query("DELETE FROM AssetsMapModel")
    suspend fun clear()

    @Query("SELECT * FROM AssetsMapModel WHERE name = :name limit 1")
    suspend fun query(name: String): AssetsMapModel?

    @Query("DELETE FROM AssetsMapModel WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM AssetsMapModel WHERE mapName is null or mapName = ''")
    suspend fun empty(): List<AssetsMapModel>

    /**
     * 获取当前最大排序值
     * 用于新增映射时设置默认排序
     */
    @Query("SELECT MAX(sort) FROM AssetsMapModel")
    suspend fun maxSort(): Int?

    @Transaction
    suspend fun put(data: AssetsMapModel) {
        // 新增时设置默认排序值为最大值+1，确保新规则排在最后
        if (data.id == 0L && data.sort == 0) {
            data.sort = (maxSort() ?: -1) + 1
        }
        // 依赖 name 唯一索引触发 REPLACE，简化逻辑
        insert(data)
    }

}