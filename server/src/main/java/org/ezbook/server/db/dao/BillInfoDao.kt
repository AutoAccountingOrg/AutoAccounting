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
import org.ezbook.server.db.model.TimeBucketStatsModel
import org.ezbook.server.db.model.stat.AmountBucketCountModel
import org.ezbook.server.db.model.stat.AmountCountModel
import org.ezbook.server.db.model.stat.AmountSumModel
import org.ezbook.server.db.model.stat.HeatmapStatsModel

@Dao
interface BillInfoDao {
    @Insert
    suspend fun insert(billInfo: BillInfoModel): Long

    @Query("SELECT * FROM BillInfoModel WHERE money = :money AND time >= :startTime AND time <= :endTime AND groupId = -1 AND type=:type ORDER BY time ASC")
    suspend fun query(
        money: Double,
        startTime: Long,
        endTime: Long,
        type: BillType
    ): List<BillInfoModel>

    @Query("SELECT * FROM BillInfoModel WHERE money = :money AND time >= :startTime AND time <= :endTime AND groupId = -1 ORDER BY time ASC")
    suspend fun queryNoType(
        money: Double,
        startTime: Long,
        endTime: Long,
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


    /**
     * 获取小额支出分类统计（用于拿铁因子）
     * 仅统计支出且金额不超过阈值
     */
    @Query(
        """
        SELECT cateName, SUM(money) as amount, COUNT(*) as count
        FROM BillInfoModel
        WHERE type = 'Expend'
          AND money <= :maxAmount
          AND time >= :startTime AND time <= :endTime
          AND groupId = -1
        GROUP BY cateName
        ORDER BY amount DESC
    """
    )
    suspend fun getSmallExpenseCategoryStats(
        startTime: Long,
        endTime: Long,
        maxAmount: Double
    ): List<CategoryStatsModel>

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

    /**
     * 按星期聚合支出（0=周日...6=周六）
     * 仅统计支出，金额口径使用 money
     */
    @Query(
        """
        SELECT 
            strftime('%w', datetime(time/1000, 'unixepoch', 'localtime')) as bucket,
            SUM(money) as amount,
            COUNT(*) as count
        FROM BillInfoModel
        WHERE type = 'Expend'
          AND time >= :startTime AND time <= :endTime
          AND groupId = -1
        GROUP BY bucket
        ORDER BY bucket ASC
        """
    )
    suspend fun getExpenseWeekdayStats(
        startTime: Long,
        endTime: Long
    ): List<TimeBucketStatsModel>

    /**
     * 按小时聚合支出（00-23）
     * 仅统计支出，金额口径使用 money
     */
    @Query(
        """
        SELECT 
            strftime('%H', datetime(time/1000, 'unixepoch', 'localtime')) as bucket,
            SUM(money) as amount,
            COUNT(*) as count
        FROM BillInfoModel
        WHERE type = 'Expend'
          AND time >= :startTime AND time <= :endTime
          AND groupId = -1
        GROUP BY bucket
        ORDER BY bucket ASC
        """
    )
    suspend fun getExpenseHourStats(
        startTime: Long,
        endTime: Long
    ): List<TimeBucketStatsModel>

    /**
     * 按星期+小时聚合支出（消费生物钟热力图）
     * bucketDay: 0=周日...6=周六
     * bucketHour: 00-23
     */
    @Query(
        """
        SELECT 
            strftime('%w', datetime(time/1000, 'unixepoch', 'localtime')) as bucketDay,
            strftime('%H', datetime(time/1000, 'unixepoch', 'localtime')) as bucketHour,
            SUM(money) as amount
        FROM BillInfoModel
        WHERE type = 'Expend'
          AND time >= :startTime AND time <= :endTime
          AND groupId = -1
        GROUP BY bucketDay, bucketHour
        ORDER BY bucketDay ASC, bucketHour ASC
        """
    )
    suspend fun getExpenseHeatmapStats(
        startTime: Long,
        endTime: Long
    ): List<HeatmapStatsModel>

    /**
     * 支出金额区间分布统计（金额分布柱状图）
     * bucket: 0-50 / 50-100 / 100-200 / 200-500 / 500-1000 / 1000+
     */
    @Query(
        """
        SELECT 
            CASE
                WHEN money < 50 THEN '0-50'
                WHEN money < 100 THEN '50-100'
                WHEN money < 200 THEN '100-200'
                WHEN money < 500 THEN '200-500'
                WHEN money < 1000 THEN '500-1000'
                ELSE '1000+'
            END as bucket,
            COUNT(*) as count
        FROM BillInfoModel
        WHERE type = 'Expend'
          AND time >= :startTime AND time <= :endTime
          AND groupId = -1
        GROUP BY bucket
        """
    )
    suspend fun getExpenseAmountDistribution(
        startTime: Long,
        endTime: Long
    ): List<AmountBucketCountModel>

    /**
     * 小额支出统计（拿铁因子）
     */
    @Query(
        """
        SELECT 
            SUM(money) as amount,
            COUNT(*) as count
        FROM BillInfoModel
        WHERE type = 'Expend'
          AND money < :maxAmount
          AND time >= :startTime AND time <= :endTime
          AND groupId = -1
        """
    )
    suspend fun getSmallExpenseStats(
        startTime: Long,
        endTime: Long,
        maxAmount: Double
    ): AmountCountModel

    /**
     * 大额支出统计
     */
    @Query(
        """
        SELECT 
            SUM(money) as amount,
            COUNT(*) as count
        FROM BillInfoModel
        WHERE type = 'Expend'
          AND money >= :minAmount
          AND time >= :startTime AND time <= :endTime
          AND groupId = -1
        """
    )
    suspend fun getLargeExpenseStats(
        startTime: Long,
        endTime: Long,
        minAmount: Double
    ): AmountCountModel

    /**
     * 周末支出统计（周六/周日）
     */
    @Query(
        """
        SELECT 
            SUM(money) as amount
        FROM BillInfoModel
        WHERE type = 'Expend'
          AND time >= :startTime AND time <= :endTime
          AND groupId = -1
          AND strftime('%w', datetime(time/1000, 'unixepoch', 'localtime')) IN ('0','6')
        """
    )
    suspend fun getWeekendExpenseSum(
        startTime: Long,
        endTime: Long
    ): AmountSumModel

    /**
     * 统计指定时间范围内的总交易数
     */
    @Query(
        """
        SELECT COUNT(*) FROM BillInfoModel
        WHERE groupId = -1
          AND time >= :startTime AND time <= :endTime
        """
    )
    suspend fun getBillsCountByTimeRange(
        startTime: Long,
        endTime: Long
    ): Int

    /**
     * 按类型汇总金额（AI分析用）。
     * 仅使用 money 字段，避免引入 fee 口径差异。
     */
    @Query(
        """
        SELECT SUM(money) FROM BillInfoModel
        WHERE groupId = -1 
          AND type IN (:types)
          AND time >= :startTime AND time <= :endTime
        """
    )
    suspend fun sumAmountByTypes(
        startTime: Long,
        endTime: Long,
        types: List<BillType>
    ): Double?

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

    /** 支出分类统计：直接返回分类名称（有子类则只保留子类） */
    @Query(
        """
        SELECT 
            cateName,
            SUM(money) as amount,
            COUNT(*) as count
        FROM BillInfoModel
        WHERE groupId = -1
          AND type = 'Expend'
          AND time >= :startTime AND time <= :endTime
        GROUP BY cateName
        ORDER BY amount DESC
        """
    )
    suspend fun getExpenseCategoryStats(
        startTime: Long,
        endTime: Long
    ): List<org.ezbook.server.db.model.CategoryStatsModel>

    /** 收入分类统计：直接返回分类名称（有子类则只保留子类） */
    @Query(
        """
        SELECT 
            cateName,
            SUM(money) as amount,
            COUNT(*) as count
        FROM BillInfoModel
        WHERE groupId = -1
          AND type = 'Income'
          AND time >= :startTime AND time <= :endTime
        GROUP BY cateName
        ORDER BY amount DESC
        """
    )
    suspend fun getIncomeCategoryStats(
        startTime: Long,
        endTime: Long
    ): List<org.ezbook.server.db.model.CategoryStatsModel>

    /**
     * 获取收入分类统计（用于 AI 分析，返回 CategoryStatsModel）
     */
    @Query(
        """
        SELECT cateName, SUM(money) as amount, COUNT(*) as count
        FROM BillInfoModel
        WHERE type = 'Income' AND time >= :startTime AND time <= :endTime AND groupId = -1
        GROUP BY cateName
        ORDER BY amount DESC
        """
    )
    suspend fun getIncomeCategoryStatsForAI(
        startTime: Long,
        endTime: Long
    ): List<CategoryStatsModel>
}