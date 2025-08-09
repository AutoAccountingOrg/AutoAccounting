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
import androidx.room.Update
import org.ezbook.server.db.model.TagModel

/**
 * 标签数据访问对象
 *
 * 提供标签的增删改查操作
 */
@Dao
interface TagDao {

    /**
     * 插入标签
     * @param tagModel 标签模型
     * @return 插入后的ID
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(tagModel: TagModel): Long

    /**
     * 批量插入标签
     * @param tagModels 标签模型列表
     * @return 插入后的ID列表
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun batchInsert(tagModels: List<TagModel>): List<Long>

    /**
     * 更新标签
     * @param tagModel 标签模型
     * @return 影响的行数
     */
    @Update
    suspend fun update(tagModel: TagModel): Int

    /**
     * 根据ID删除标签
     * @param id 标签ID
     */
    @Query("DELETE FROM TagModel WHERE id = :id")
    suspend fun delete(id: Long)

    /**
     * 清除所有标签
     * 用于重置标签数据
     */
    @Query("DELETE FROM TagModel")
    suspend fun deleteAll()

    /**
     * 根据ID查询标签
     * @param id 标签ID
     * @return 标签模型，如果不存在返回null
     */
    @Query("SELECT * FROM TagModel WHERE id = :id")
    suspend fun query(id: Long): TagModel?

    /**
     * 根据名称查询标签（用于重复检查）
     * @param name 标签名称
     * @return 标签模型，如果不存在返回null
     */
    @Query("SELECT * FROM TagModel WHERE name = :name")
    suspend fun queryByName(name: String): TagModel?

    /**
     * 获取所有标签列表（按创建时间倒序）
     * @return 标签列表
     */
    @Query("SELECT * FROM TagModel ORDER BY id DESC")
    suspend fun list(): List<TagModel>

    /**
     * 分页查询标签列表
     * @param limit 每页数量
     * @param offset 偏移量
     * @return 标签列表
     */
    @Query("SELECT * FROM TagModel ORDER BY id DESC LIMIT :limit OFFSET :offset")
    suspend fun load(limit: Int, offset: Int): List<TagModel>

    /**
     * 获取标签总数
     * @return 标签总数
     */
    @Query("SELECT COUNT(*) FROM TagModel")
    suspend fun count(): Int

    /**
     * 搜索标签（根据名称模糊匹配）
     * @param keyword 搜索关键词
     * @param limit 限制数量
     * @param offset 偏移量
     * @return 匹配的标签列表
     */
    @Query("SELECT * FROM TagModel WHERE name LIKE '%' || :keyword || '%' ORDER BY id DESC LIMIT :limit OFFSET :offset")
    suspend fun search(keyword: String, limit: Int, offset: Int): List<TagModel>
}
