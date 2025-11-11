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

/**
 * 转账识别器
 *
 * 职责：将短时间内发生的收入和支出账单识别为转账
 *
 * 识别规则：
 * - 在时间窗口内找到金额相同但类型相反（一个收入，一个支出）的账单
 * - 将它们合并为一个转账账单
 */
object TransferRecognizer {

    /**
     * 识别转账账单
     *
     * @param billInfoModel 当前账单
     * @return 匹配到的转账账单（父账单），如果没有匹配则返回null
     */
    suspend fun recognize(billInfoModel: BillInfoModel): BillInfoModel? {
        // 只处理收入和支出类型
        if (billInfoModel.type != BillType.Income && billInfoModel.type != BillType.Expend) {
            ServerLog.d("转账识别：账单类型不是收入或支出，跳过")
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

        // 查找相反类型的账单
        val oppositeType =
            if (billInfoModel.type == BillType.Income) BillType.Expend else BillType.Income

        ServerLog.d("转账识别：查询 id=${billInfoModel.id}, 金额=${billInfoModel.money}, 类型=${billInfoModel.type}, 时间窗口=${timeThresholdSeconds}秒")

        val candidates = Db.get().billInfoDao().query(
            billInfoModel.money,
            startTime,
            endTime,
            oppositeType
        )

        // 找到匹配的转账账单（金额相同、类型相反、未被去重）
        return candidates.find { candidate ->
            candidate.id != billInfoModel.id
        }
    }

    /**
     * 处理转账账单
     *
     * 将收入和支出账单合并为一个转账账单
     *
     * @param currentBill 当前账单
     * @param transferBill 转账账单（父账单）
     */
    suspend fun process(
        currentBill: BillInfoModel,
        transferBill: BillInfoModel
    ) {
        ServerLog.d("转账处理：合并 parentId=${transferBill.id}, currentId=${currentBill.id}")

        // 确定收入和支出账单
        val incomeBill = if (currentBill.type == BillType.Income) currentBill else transferBill
        val expendBill = if (currentBill.type == BillType.Expend) currentBill else transferBill

        // 转换为转账类型
        transferBill.type = BillType.Transfer

        // 合并账户信息
        mergeTransferAccountInfo(incomeBill, expendBill, transferBill)

        // 合并商户和商品信息
        BillMerger.mergeShopInfo(currentBill, transferBill)

        // 设置去重关系并清空分类
        BillMerger.setupBillGroup(currentBill, transferBill)

        // 更新数据库
        BillMerger.saveBillGroup(currentBill, transferBill)

        ServerLog.d("转账处理：合并完成，父账单已转换为转账类型")
    }

    /**
     * 合并转账账户信息
     *
     * 规则：
     * - 转出账户：优先使用收入账单的 accountNameFrom，否则使用支出账单的 accountNameFrom
     * - 转入账户：使用支出账单的 accountNameFrom
     */
    private fun mergeTransferAccountInfo(
        incomeBill: BillInfoModel,
        expendBill: BillInfoModel,
        transferBill: BillInfoModel
    ) {
        // 转出账户
        transferBill.accountNameFrom = when {
            incomeBill.accountNameFrom.isNotEmpty() -> incomeBill.accountNameFrom
            expendBill.accountNameFrom.isNotEmpty() -> expendBill.accountNameFrom
            else -> ""
        }

        // 转入账户
        transferBill.accountNameTo = expendBill.accountNameFrom.ifEmpty { expendBill.accountNameTo }
    }
}

