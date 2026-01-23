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

import org.ezbook.server.constant.BillType
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.BillInfoModel
import org.ezbook.server.log.ServerLog

/**
 * 转账识别器
 *
 * 职责：将短时间内发生的金额相同的账单识别并合并为转账
 *
 * 支持场景：
 * 1. Income + Expend → Transfer（账户间转账：A账户收入 + B账户支出）
 * 2. Expend + Transfer → Transfer（信用卡还款：借记卡支出 + 信用卡还款）
 * 3. Transfer + Transfer → Transfer（多个转账通知合并）
 *
 * 识别规则：
 * - 在时间窗口内查找金额相同的账单
 * - 支持 Income/Expend/Transfer 类型的配对
 * - 合并后统一转换为 Transfer 类型
 */
object TransferRecognizer {

    /**
     * 识别转账账单
     *
     * @param billInfoModel 当前账单
     * @return 匹配到的转账账单（父账单），如果没有匹配则返回null
     */
    suspend fun recognize(billInfoModel: BillInfoModel): BillInfoModel? {
        // 只处理收入、支出和转账类型
        if (billInfoModel.type != BillType.Income &&
            billInfoModel.type != BillType.Expend &&
            billInfoModel.type != BillType.Transfer
        ) {
            ServerLog.d("转账识别：账单类型不支持，跳过")
            return null
        }

        // 检查是否启用转账识别
        if (!SettingUtils.featureAssetManager() || !SettingUtils.autoTransferRecognition()) {
            ServerLog.d("转账识别：未启用，跳过")
            return null
        }

        // 获取时间窗口
        val timeThresholdSeconds = SettingUtils.autoTransferTimeThreshold()
        val timeWindowMillis = timeThresholdSeconds * 1000L
        val startTime = billInfoModel.time - timeWindowMillis
        val endTime = billInfoModel.time + timeWindowMillis

        ServerLog.d("转账识别：查询 id=${billInfoModel.id}, 金额=${billInfoModel.money}, 类型=${billInfoModel.type}, 时间窗口=${timeThresholdSeconds}秒")

        /**
         *      * 1. Income + Expend → Transfer（账户间转账）
         *      * 2. Expend + Transfer → Transfer（信用卡还款等）
         *      * 3. Transfer + Transfer → Transfer（多个转账通知合并）
         */
        val candidates = mutableListOf<BillInfoModel>()

        if (billInfoModel.type == BillType.Income) {
            candidates.addAll(
                Db.get().billInfoDao().query(
                    billInfoModel.money, startTime, endTime, BillType.Expend
                )
            )
            candidates.addAll(
                Db.get().billInfoDao().query(
                    billInfoModel.money, startTime, endTime, BillType.Transfer
                )
            )
        }
        if (billInfoModel.type == BillType.Expend) {
            candidates.addAll(
                Db.get().billInfoDao().query(
                    billInfoModel.money, startTime, endTime, BillType.Income
                )
            )
            candidates.addAll(
                Db.get().billInfoDao().query(
                    billInfoModel.money, startTime, endTime, BillType.Transfer
                )
            )
        }
        if (billInfoModel.type == BillType.Transfer) {
            candidates.addAll(
                Db.get().billInfoDao().queryNoType(
                    billInfoModel.money, startTime, endTime
                )
            )
        }

        // 找到匹配的转账账单（金额相同、类型匹配、未被去重）
        return candidates.find { candidate ->
            candidate.id != billInfoModel.id
        }
    }

    /**
     * 处理转账账单
     *
     * 将收入和支出账单合并为一个转账账单
     * 支持场景：
     * 1. Income + Expend → Transfer（账户间转账）
     * 2. Expend + Transfer → Transfer（信用卡还款等）
     * 3. Transfer + Transfer → Transfer（多个转账通知合并）
     *
     * @param currentBill 当前账单
     * @param transferBill 转账账单（父账单）
     */
    suspend fun process(
        currentBill: BillInfoModel,
        transferBill: BillInfoModel
    ) {
        ServerLog.d("转账处理：合并 parentId=${transferBill.id}(${transferBill.type}), currentId=${currentBill.id}(${currentBill.type})")

        // 合并前先规范化账户位置：
        // Income 的 accountNameFrom 实际是转入账户，需要移到 accountNameTo
        // Expend 的 accountNameFrom 实际是转出账户，保持不变
        normalizeAccountPosition(currentBill)
        normalizeAccountPosition(transferBill)

        // 转换为转账类型
        transferBill.type = BillType.Transfer

        // 使用 BillMerger 统一合并账户信息（支持已知资产优先、名字长度优先等规则）
        BillMerger.mergeAccountInfo(currentBill, transferBill)

        // 合并商户和商品信息
        BillMerger.mergeShopInfo(currentBill, transferBill)

        // 设置去重关系并清空分类
        currentBill.groupId = transferBill.id
        currentBill.cateName = ""
        transferBill.cateName = ""

        // 更新数据库
        BillMerger.saveBillGroup(currentBill, transferBill)

        ServerLog.d("转账处理：合并完成，父账单类型=${transferBill.type}")
    }

    /**
     * 规范化账户位置
     *
     * Income 账单的 accountNameFrom 实际是"收到钱的账户"，即转账的目标账户
     * Expend 账单的 accountNameFrom 实际是"花钱的账户"，即转账的来源账户
     *
     * 为了正确合并，需要把 Income 的 accountNameFrom 移到 accountNameTo
     */
    private fun normalizeAccountPosition(bill: BillInfoModel) {
        if (bill.type == BillType.Income && bill.accountNameTo.isEmpty() && bill.accountNameFrom.isNotEmpty()) {
            ServerLog.d("规范化账户位置：Income 账单 ${bill.id}，将 accountNameFrom='${bill.accountNameFrom}' 移到 accountNameTo")
            bill.accountNameTo = bill.accountNameFrom
            bill.accountNameFrom = ""
        }
    }
}

