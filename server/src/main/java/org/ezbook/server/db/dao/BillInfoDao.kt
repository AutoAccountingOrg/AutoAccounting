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
import org.ezbook.server.constant.BillState
import org.ezbook.server.constant.BillType
import org.ezbook.server.db.model.BillInfoModel
import org.ezbook.server.db.model.BillSummaryModel
import org.ezbook.server.db.model.CategoryStatsModel
import org.ezbook.server.db.model.ShopStatsModel

@Dao
interface BillInfoDao {
    @Insert
    suspend fun insert(billInfo: BillInfoModel): Long

    @Query("SELECT * FROM BillInfoModel WHERE money = :money AND time >= :startTime AND time <= :endTime AND groupId = -1 AND type=:type")
    suspend fun query(
        money: Double,
        startTime: Long,
        endTime: Long,
        type: BillType
    ): List<BillInfoModel>

    @Query("SELECT * FROM BillInfoModel WHERE id = :id")
    suspend fun queryId(id: Long): BillInfoModel?

    @Update
    suspend fun update(billInfo: BillInfoModel)

    // 删除策略，365天以前的所有数据（无论是否同步）
    @Query("DELETE FROM BillInfoModel WHERE time < :time")
    suspend fun clearOld(time: Long)

    @Query("DELETE FROM BillInfoModel")
    suspend fun clear()

    @Query("SELECT * FROM BillInfoModel WHERE state = 'Wait2Edit' and groupId = -1")
    suspend fun loadWaitEdit(): List<BillInfoModel>

    @Query("SELECT * FROM BillInfoModel WHERE groupId=-1 and state in(:state) ORDER BY time DESC LIMIT :limit OFFSET :offset")
    suspend fun loadPage(limit: Int, offset: Int, state: List<String>): List<BillInfoModel>

    /**
     * 按时间范围分页查询账单（用于按月份筛选）。
     * 使用 [startTime, endTime) 半开区间，避免跨月边界的重复或遗漏。
     */
    @Query("SELECT * FROM BillInfoModel WHERE groupId = -1 AND state IN(:state) AND time >= :startTime AND time < :endTime ORDER BY time DESC LIMIT :limit OFFSET :offset")
    suspend fun loadPageByTimeRange(
        limit: Int,
        offset: Int,
        state: List<String>,
        startTime: Long,
        endTime: Long
    ): List<BillInfoModel>

    @Query("DELETE FROM BillInfoModel WHERE groupId = :groupId")
    suspend fun deleteGroup(groupId: Long)

    @Query("DELETE FROM BillInfoModel WHERE groupId != -1 AND id NOT IN (SELECT id FROM BillInfoModel)")
    suspend fun deleteNoGroup()

    @Query("UPDATE BillInfoModel SET groupId = -1 WHERE id = :id")
    suspend fun unGroup(id: Long)

    @Query("DELETE FROM BillInfoModel WHERE id =:id")
    suspend fun deleteId(id: Long)

    @Query("SELECT * FROM BillInfoModel WHERE state = 'Edited' and groupId = -1")
    suspend fun queryNoSync(): List<BillInfoModel>

    @Query("UPDATE BillInfoModel SET state = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: BillState)

    @Query("SELECT * FROM BillInfoModel WHERE groupId = :groupId")
    suspend fun queryGroup(groupId: Long): List<BillInfoModel>

    @Query("SELECT SUM(money) FROM BillInfoModel WHERE type = 'Income' AND time >= :startTime AND time < :endTime AND groupId = -1")
    suspend fun getMonthlyIncome(startTime: Long, endTime: Long): Double?

    @Query("SELECT SUM(money) FROM BillInfoModel WHERE type = 'Expend' AND time >= :startTime AND time < :endTime AND groupId = -1")
    suspend fun getMonthlyExpense(startTime: Long, endTime: Long): Double?

    /**
     * 汇总：收入 sum(money+fee)
     */
    @Query(
        """
        SELECT SUM(money + fee) FROM BillInfoModel
        WHERE groupId = -1 
          AND type IN ('Income','IncomeLending','IncomeRepayment','IncomeReimbursement','IncomeRefund')
          AND time >= :startTime AND time <= :endTime
        """
    )
    suspend fun sumIncomeWithFee(startTime: Long, endTime: Long): Double?

    /**
     * 汇总：支出 sum(money+fee)
     */
    @Query(
        """
        SELECT SUM(money + fee) FROM BillInfoModel
        WHERE groupId = -1 
          AND type IN ('Expend','ExpendReimbursement','ExpendLending','ExpendRepayment')
          AND time >= :startTime AND time <= :endTime
        """
    )
    suspend fun sumExpenseWithFee(startTime: Long, endTime: Long): Double?

    /**
     * 趋势聚合：按天聚合收入与支出金额（金额口径按 money+fee）。
     * 这里使用 SQLite 的 strftime 计算本地日期键。
     */
    @Query(
        """
        SELECT 
            strftime('%Y-%m-%d', datetime(time/1000, 'unixepoch', 'localtime')) as day,
            SUM(CASE WHEN type IN ('Income','IncomeLending','IncomeRepayment','IncomeReimbursement','IncomeRefund') THEN (money + fee) ELSE 0 END) as income,
            SUM(CASE WHEN type IN ('Expend','ExpendReimbursement','ExpendLending','ExpendRepayment') THEN (money + fee) ELSE 0 END) as expense
        FROM BillInfoModel
        WHERE groupId = -1 AND time >= :startTime AND time <= :endTime
        GROUP BY day
        ORDER BY day ASC
        """
    )
    suspend fun getDailyTrend(
        startTime: Long,
        endTime: Long
    ): List<org.ezbook.server.db.model.TrendRowModel>


    @Query("SELECT * FROM BillInfoModel WHERE groupId = -1 LIMIT :limit OFFSET :offset")
    suspend fun getBillsBatch(limit: Int, offset: Int): List<BillInfoModel>

    @Query("SELECT COUNT(*) FROM BillInfoModel WHERE groupId = -1")
    suspend fun getBillsCount(): Int

    @Query("SELECT * FROM BillInfoModel WHERE groupId = -1 AND time >= :startTime LIMIT :limit OFFSET :offset")
    suspend fun getRecentBillsBatch(limit: Int, offset: Int, startTime: Long): List<BillInfoModel>

    @Query("SELECT COUNT(*) FROM BillInfoModel WHERE groupId = -1 AND time >= :startTime")
    suspend fun getRecentBillsCount(startTime: Long): Int

    @Query("SELECT * FROM BillInfoModel WHERE groupId = -1 AND time >= :startTime AND time <= :endTime ORDER BY time DESC")
    suspend fun getBillsByTimeRange(startTime: Long, endTime: Long): List<BillInfoModel>

    // AI摘要相关查询

    /** 获取分类统计（AI摘要专用，完整统计数据，不能限制） */
    @Query(
        """
        SELECT cateName, SUM(money) as amount, COUNT(*) as count 
        FROM BillInfoModel 
        WHERE type = 'Expend' AND time >= :startTime AND time <= :endTime AND groupId = -1 
        GROUP BY cateName 
        ORDER BY amount DESC
    """
    )
    suspend fun getExpenseCategoryStatsForAI(
        startTime: Long,
        endTime: Long
    ): List<CategoryStatsModel>

    /** 获取商户统计（完整统计数据，不能限制） */
    @Query(
        """
        SELECT shopName, SUM(money) as amount, COUNT(*) as count 
        FROM BillInfoModel 
        WHERE type = 'Expend' AND time >= :startTime AND time <= :endTime AND groupId = -1 
        GROUP BY shopName 
        ORDER BY amount DESC
    """
    )
    suspend fun getExpenseShopStats(startTime: Long, endTime: Long): List<ShopStatsModel>

    /** 获取大额交易样本（必须限制数量，避免过多） */
    @Query(
        """
        SELECT time, type, money, cateName, shopName, shopItem 
        FROM BillInfoModel 
        WHERE money >= :threshold AND time >= :startTime AND time <= :endTime AND groupId = -1 
        ORDER BY money DESC 
        LIMIT :limit
    """
    )
    suspend fun getLargeTransactions(
        startTime: Long,
        endTime: Long,
        threshold: Double,
        limit: Int
    ): List<BillSummaryModel>

    /** 获取账单样本（必须限制数量，避免过多） */
    @Query(
        """
        SELECT time, type, money, cateName, shopName, shopItem 
        FROM BillInfoModel 
        WHERE time >= :startTime AND time <= :endTime AND groupId = -1 
        ORDER BY time DESC 
        LIMIT :limit
    """
    )
    suspend fun getBillSamples(startTime: Long, endTime: Long, limit: Int): List<BillSummaryModel>

    /** 获取指定时间范围内各类型账单数量 */
    @Query("SELECT COUNT(*) FROM BillInfoModel WHERE type = 'Income' AND time >= :startTime AND time <= :endTime AND groupId = -1")
    suspend fun getIncomeCount(startTime: Long, endTime: Long): Int

    @Query("SELECT COUNT(*) FROM BillInfoModel WHERE type = 'Expend' AND time >= :startTime AND time <= :endTime AND groupId = -1")
    suspend fun getExpenseCount(startTime: Long, endTime: Long): Int

    @Query("SELECT COUNT(*) FROM BillInfoModel WHERE type = 'Transfer' AND time >= :startTime AND time <= :endTime AND groupId = -1")
    suspend fun getTransferCount(startTime: Long, endTime: Long): Int

    /** 简化的按天趋势统计：只统计Income和Expend，只使用money字段 */
    @Query(
        """
        SELECT 
            strftime('%Y-%m-%d', datetime(time/1000, 'unixepoch', 'localtime')) as day,
            SUM(CASE WHEN type = 'Income' THEN money ELSE 0 END) as income,
            SUM(CASE WHEN type = 'Expend' THEN money ELSE 0 END) as expense
        FROM BillInfoModel
        WHERE groupId = -1 AND time >= :startTime AND time <= :endTime
        GROUP BY day
        ORDER BY day ASC
        """
    )
    suspend fun getSimpleDailyTrend(
        startTime: Long,
        endTime: Long
    ): List<org.ezbook.server.db.model.TrendRowModel>

    /** 简化的按月趋势统计：只统计Income和Expend，只使用money字段 */
    @Query(
        """
        SELECT 
            strftime('%Y-%m', datetime(time/1000, 'unixepoch', 'localtime')) as day,
            SUM(CASE WHEN type = 'Income' THEN money ELSE 0 END) as income,
            SUM(CASE WHEN type = 'Expend' THEN money ELSE 0 END) as expense
        FROM BillInfoModel
        WHERE groupId = -1 AND time >= :startTime AND time <= :endTime
        GROUP BY day
        ORDER BY day ASC
        """
    )
    suspend fun getSimpleMonthlyTrend(
        startTime: Long,
        endTime: Long
    ): List<org.ezbook.server.db.model.TrendRowModel>

    /** 支出分类统计：优先按子类统计，包含金额和计数 */
    @Query(
        """
        SELECT 
            TRIM(CASE WHEN instr(cateName, '-') <= 0 THEN cateName ELSE substr(cateName, 1, instr(cateName, '-') - 1) END) as parent,
            TRIM(CASE 
                    WHEN instr(cateName, '-') > 1 AND instr(cateName, '-') < length(cateName) THEN substr(cateName, instr(cateName, '-') + 1)
                    ELSE ''
                END) as child,
            SUM(money) as amount,
            COUNT(*) as count
        FROM BillInfoModel
        WHERE groupId = -1 AND type = 'Expend' AND time >= :startTime AND time <= :endTime
        GROUP BY parent, child
        ORDER BY amount DESC
        """
    )
    suspend fun getExpenseCategoryStats(
        startTime: Long,
        endTime: Long
    ): List<org.ezbook.server.db.model.CategoryAggregateRow>

    /** 收入分类统计：优先按子类统计，包含金额和计数 */
    @Query(
        """
        SELECT 
            TRIM(CASE WHEN instr(cateName, '-') <= 0 THEN cateName ELSE substr(cateName, 1, instr(cateName, '-') - 1) END) as parent,
            TRIM(CASE 
                    WHEN instr(cateName, '-') > 1 AND instr(cateName, '-') < length(cateName) THEN substr(cateName, instr(cateName, '-') + 1)
                    ELSE ''
                END) as child,
            SUM(money) as amount,
            COUNT(*) as count
        FROM BillInfoModel
        WHERE groupId = -1 AND type = 'Income' AND time >= :startTime AND time <= :endTime
        GROUP BY parent, child
        ORDER BY amount DESC
        """
    )
    suspend fun getIncomeCategoryStats(
        startTime: Long,
        endTime: Long
    ): List<org.ezbook.server.db.model.CategoryAggregateRow>
}