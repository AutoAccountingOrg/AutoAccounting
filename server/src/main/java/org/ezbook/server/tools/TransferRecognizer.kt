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

        // 查找候选账单：相反类型 + Transfer类型
        val candidates = Db.get().billInfoDao().queryNoType(
            billInfoModel.money, startTime, endTime
        )

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
     *
     * @param currentBill 当前账单
     * @param transferBill 转账账单（父账单）
     */
    suspend fun process(
        currentBill: BillInfoModel,
        transferBill: BillInfoModel
    ) {
        ServerLog.d("转账处理：合并 parentId=${transferBill.id}(${transferBill.type}), currentId=${currentBill.id}(${currentBill.type})")

        // 保存原始类型用于场景判断（在转换为Transfer之前）
        val originalTransferType = transferBill.type
        val originalCurrentType = currentBill.type

        // 转换为转账类型
        transferBill.type = BillType.Transfer

        // 根据账单类型组合，选择不同的合并策略
        when {
            // 场景1: Income + Expend（传统转账）
            (originalCurrentType == BillType.Income && originalTransferType == BillType.Expend) ||
                    (originalCurrentType == BillType.Expend && originalTransferType == BillType.Income) -> {
                val incomeBill =
                    if (originalCurrentType == BillType.Income) currentBill else transferBill
                val expendBill =
                    if (originalCurrentType == BillType.Expend) currentBill else transferBill
                mergeTransferAccountInfo(incomeBill, expendBill, transferBill)
            }

            // 场景2: Expend + Transfer（信用卡还款等）
            originalCurrentType == BillType.Expend && originalTransferType == BillType.Transfer -> {
                mergeRepaymentAccountInfo(currentBill, transferBill)
            }

            originalTransferType == BillType.Expend && originalCurrentType == BillType.Transfer -> {
                mergeRepaymentAccountInfo(transferBill, currentBill)
            }

            // 场景3: Transfer + Transfer（多个转账通知）
            originalCurrentType == BillType.Transfer && originalTransferType == BillType.Transfer -> {
                mergeTransferToTransfer(currentBill, transferBill)
            }

            else -> {
                ServerLog.w("转账处理：未知的账单类型组合 current=${originalCurrentType}, parent=${originalTransferType}")
            }
        }

        // 合并商户和商品信息
        BillMerger.mergeShopInfo(currentBill, transferBill)

        // 设置去重关系并清空分类
        currentBill.groupId = transferBill.id

        // 更新数据库
        BillMerger.saveBillGroup(currentBill, transferBill)

        ServerLog.d("转账处理：合并完成，父账单类型=${transferBill.type}")
    }

    /**
     * 合并转账账户信息（Income + Expend 场景）
     *
     * 规则：
     * - 转出账户：优先使用支出账单的 accountNameFrom
     * - 转入账户：优先使用收入账单的 accountNameFrom 或 accountNameTo
     */
    private fun mergeTransferAccountInfo(
        incomeBill: BillInfoModel,
        expendBill: BillInfoModel,
        transferBill: BillInfoModel
    ) {
        // 转出账户：从支出账单获取
        transferBill.accountNameFrom = when {
            expendBill.accountNameFrom.isNotEmpty() -> expendBill.accountNameFrom
            incomeBill.accountNameFrom.isNotEmpty() -> incomeBill.accountNameFrom
            else -> ""
        }

        // 转入账户：从收入账单获取
        transferBill.accountNameTo = when {
            incomeBill.accountNameFrom.isNotEmpty() -> incomeBill.accountNameFrom
            incomeBill.accountNameTo.isNotEmpty() -> incomeBill.accountNameTo
            expendBill.accountNameTo.isNotEmpty() -> expendBill.accountNameTo
            else -> ""
        }
    }

    /**
     * 合并还款账户信息（Expend + Transfer 场景）
     *
     * 信用卡还款场景：
     * - expendBill: 借记卡支出（转出账户）
     * - transferBill: 信用卡还款（转入账户）
     *
     * 规则：
     * - 转出账户：优先保留transferBill已有的accountNameFrom，如果为空则使用expendBill的
     * - 转入账户：优先使用transferBill的accountNameTo，如果为空则使用其他可用信息
     */
    private fun mergeRepaymentAccountInfo(
        expendBill: BillInfoModel,
        transferBill: BillInfoModel
    ) {
        // 转出账户：优先保留transferBill已有的accountNameFrom（避免被后续合并覆盖）
        // 只有当transferBill的accountNameFrom为空时，才使用expendBill的
        if (transferBill.accountNameFrom.isEmpty() && expendBill.accountNameFrom.isNotEmpty()) {
            transferBill.accountNameFrom = expendBill.accountNameFrom
        }

        // 转入账户：优先使用transferBill的accountNameTo（信用卡账户）
        // 如果为空，则尝试从其他字段获取
        if (transferBill.accountNameTo.isEmpty()) {
            transferBill.accountNameTo = when {
                transferBill.accountNameFrom.isNotEmpty() -> transferBill.accountNameFrom
                expendBill.accountNameTo.isNotEmpty() -> expendBill.accountNameTo
                else -> ""
            }
        }
    }

    /**
     * 合并两个转账账单（Transfer + Transfer 场景）
     *
     * 多个转账通知场景：
     * - 两个账单都是转账类型
     * - 合并它们的账户信息
     *
     * 规则：
     * - 优先使用非空的账户信息
     * - 尽可能保留更完整的账户信息
     */
    private fun mergeTransferToTransfer(
        bill1: BillInfoModel,
        bill2: BillInfoModel
    ) {
        // 转出账户：优先使用非空的
        bill2.accountNameFrom = when {
            bill1.accountNameFrom.isNotEmpty() -> bill1.accountNameFrom
            bill2.accountNameFrom.isNotEmpty() -> bill2.accountNameFrom
            else -> ""
        }

        // 转入账户：优先使用非空的
        bill2.accountNameTo = when {
            bill1.accountNameTo.isNotEmpty() -> bill1.accountNameTo
            bill2.accountNameTo.isNotEmpty() -> bill2.accountNameTo
            else -> ""
        }
    }
}

