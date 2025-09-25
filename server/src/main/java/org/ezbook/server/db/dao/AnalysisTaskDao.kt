/*
 * Copyright (C) 2025 ankio(ankio@ankio.net)
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
 *  limitations under the License.
 */

package org.ezbook.server.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import org.ezbook.server.constant.AnalysisTaskStatus
import org.ezbook.server.db.model.AnalysisTaskModel

/**
 * AI分析任务数据访问对象
 */
@Dao
interface AnalysisTaskDao {

    /**
     * 插入新任务
     * @return 插入的任务ID
     */
    @Insert
    suspend fun insert(task: AnalysisTaskModel): Long

    /**
     * 更新任务
     */
    @Update
    suspend fun update(task: AnalysisTaskModel)

    /**
     * 根据ID获取任务
     */
    @Query("SELECT * FROM AnalysisTaskModel WHERE id = :id")
    suspend fun getById(id: Long): AnalysisTaskModel?

    /**
     * 获取所有任务，按创建时间倒序
     */
    @Query("SELECT * FROM AnalysisTaskModel ORDER BY createTime DESC")
    suspend fun getAll(): List<AnalysisTaskModel>

    /**
     * 分页获取任务列表，按创建时间倒序
     */
    @Query("SELECT * FROM AnalysisTaskModel ORDER BY createTime DESC LIMIT :limit OFFSET :offset")
    suspend fun getPage(limit: Int, offset: Int): List<AnalysisTaskModel>

    /**
     * 获取任务总数
     */
    @Query("SELECT COUNT(*) FROM AnalysisTaskModel")
    suspend fun getCount(): Int

    /**
     * 根据状态获取任务列表
     */
    @Query("SELECT * FROM AnalysisTaskModel WHERE status = :status ORDER BY createTime DESC")
    suspend fun getByStatus(status: AnalysisTaskStatus): List<AnalysisTaskModel>

    /**
     * 删除任务
     */
    @Query("DELETE FROM AnalysisTaskModel WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * 更新任务状态
     */
    @Query("UPDATE AnalysisTaskModel SET status = :status, updateTime = :updateTime WHERE id = :id")
    suspend fun updateStatus(
        id: Long,
        status: AnalysisTaskStatus,
        updateTime: Long = System.currentTimeMillis()
    )

    /**
     * 更新任务进度
     */
    @Query("UPDATE AnalysisTaskModel SET progress = :progress, updateTime = :updateTime WHERE id = :id")
    suspend fun updateProgress(
        id: Long,
        progress: Int,
        updateTime: Long = System.currentTimeMillis()
    )

    /**
     * 检查是否存在相同时间范围的任务
     */
    @Query("SELECT COUNT(*) FROM AnalysisTaskModel WHERE startTime = :startTime AND endTime = :endTime")
    suspend fun countByTimeRange(startTime: Long, endTime: Long): Int

    /**
     * 删除指定时间之前的所有任务
     */
    @Query("DELETE FROM AnalysisTaskModel WHERE createTime < :beforeTime")
    suspend fun deleteOldTasks(beforeTime: Long)

    /**
     * 将超时的未完成任务标记为失败
     */
    @Query("UPDATE AnalysisTaskModel SET status = :failedStatus, errorMessage = '任务超时', updateTime = :updateTime WHERE updateTime < :beforeTime AND status IN (:pendingStatus, :processingStatus)")
    suspend fun markTimeoutTasks(
        beforeTime: Long,
        failedStatus: AnalysisTaskStatus = AnalysisTaskStatus.FAILED,
        pendingStatus: AnalysisTaskStatus = AnalysisTaskStatus.PENDING,
        processingStatus: AnalysisTaskStatus = AnalysisTaskStatus.PROCESSING,
        updateTime: Long = System.currentTimeMillis()
    )

    /**
     * 删除所有任务
     */
    @Query("DELETE FROM AnalysisTaskModel")
    suspend fun deleteAll()
} 