/*
 * Copyright (C) 2023 ankio(ankio@ankio.net)
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

package org.ezbook.server.tools

import org.ezbook.server.db.Db
import org.ezbook.server.db.model.BillInfoModel
import org.ezbook.server.log.ServerLog

/**
 * 重复账单检测器
 *
 * 职责：识别并合并重复账单（同一笔交易的不同通知源）
 *
 * 重复账单的定义：同一笔交易通过不同渠道产生的多条记录
 */
object DuplicateDetector {

    /**
     * 检测重复账单
     *
     * @param billInfoModel 当前账单
     * @return 匹配到的重复账单（父账单），如果没有匹配则返回null
     */
    suspend fun detect(billInfoModel: BillInfoModel): BillInfoModel? {
        ServerLog.d("去重：开始，bill=${billInfoModel}")

        // 检查是否启用自动去重
        if (!SettingUtils.autoGroup()) {
            ServerLog.d("去重：未启用，跳过")
            return null
        }

        // 查找可能重复的账单
        val potentialDuplicates = findPotentialDuplicates(billInfoModel)
        ServerLog.d("去重：候选数量=${potentialDuplicates.size}")

        // 查找并处理重复账单
        return potentialDuplicates
            .find { bill ->
                bill.id != billInfoModel.id && isDuplicate(billInfoModel, bill)
            }
            ?.also { parentBill ->
                ServerLog.d("去重：命中父账单 parentId=${parentBill.id}, currentId=${billInfoModel.id}")
                process(parentBill, billInfoModel)
            }
    }

    /**
     * 查找指定时间范围和金额的潜在重复账单
     */
    private suspend fun findPotentialDuplicates(bill: BillInfoModel): List<BillInfoModel> {
        // 从设置中读取时间阈值（秒），转换为毫秒
        val timeThresholdSeconds = SettingUtils.autoGroupTimeThreshold()
        val timeWindowMillis = timeThresholdSeconds * 1000L

        val startTime = bill.time - timeWindowMillis
        val endTime = bill.time + timeWindowMillis

        ServerLog.d("去重：候选查询 id=${bill.id}, 金额=${bill.money}, 时间窗口=${timeThresholdSeconds}秒, 时间范围=$startTime-$endTime")
        return Db.get().billInfoDao().query(bill.money, startTime, endTime, bill.type)
    }

    /**
     * 判断两个账单是否重复
     *
     * 判断规则：
     * 1. 时间完全相同 → 一定是重复（用户重复识别同一条通知/短信）
     * 2. 双方都是AI生成 → 不是重复（AI识别不稳定，易误判）
     * 3. 规则相同且渠道相同 → 不是重复（场景：用户多次转账给同一个人）
     * 4. 规则相同但渠道不同 → 可能是重复（同一笔交易的不同通知源）
     *
     * @param bill1 账单1
     * @param bill2 账单2
     * @return true 表示是重复账单，false 表示不是重复账单
     */
    private fun isDuplicate(bill1: BillInfoModel, bill2: BillInfoModel): Boolean {
        ServerLog.d("重复判断开始：b1(${bill1}), b2(${bill2})")

        // 规则1：时间完全相同 → 一定是重复
        if (bill1.time == bill2.time) {
            ServerLog.d("重复判断：时间完全相同 -> 重复")
            return true
        }

        // 规则2：如果两个都是AI生成，不做去重（AI识别不稳定，易误判）
        if (bill1.generateByAi() && bill2.generateByAi()) {
            ServerLog.d("重复判断：双方都是AI生成 -> 非重复")
            return false
        }

        // 规则3：规则相同 && 渠道相同 → 一定不是重复（场景：用户多次转账给同一个人）
        if (bill1.channel == bill2.channel && bill1.channel.isNotEmpty()) {
            ServerLog.d("重复判断：规则相同且渠道相同(${bill1.channel}) -> 非重复")
            return false
        }

        // 规则4：规则相同但渠道不同 → 可能是重复（同一笔交易的不同通知源）
        // 例如：微信支付通知 + 银行卡扣款通知
        ServerLog.d("重复判断：规则相同(${bill1.ruleName})但渠道不同 -> 可能重复")
        return true
    }

    /**
     * 处理重复账单
     *
     * 职责：标记去重关系、合并基础数据、持久化
     * 不负责：分类决策（返回后由 BillService 统一处理）
     */
    private suspend fun process(
        parentBill: BillInfoModel,
        currentBill: BillInfoModel
    ) {
        ServerLog.d("去重：合并 parentId=${parentBill.id}, currentId=${currentBill.id}")

        // 设置去重关系
        currentBill.groupId = parentBill.id

        // 合并账单基础数据（不含分类）
        BillMerger.mergeBillData(currentBill, parentBill)

        // 更新数据库
        BillMerger.saveBillGroup(currentBill, parentBill)



        ServerLog.d("去重：合并完成，父账单 $parentBill , 子账单 $currentBill")
    }
}

