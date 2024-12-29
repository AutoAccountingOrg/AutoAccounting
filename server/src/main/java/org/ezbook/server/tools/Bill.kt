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

import android.content.Context
import org.ezbook.server.Server
import org.ezbook.server.constant.BillType
import org.ezbook.server.constant.DefaultData
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.BillInfoModel

object Bill {
    /**
     * 检查账单是否重复
     * @param bill 账单1
     * @param bill2 账单2
     * @return true 表示是重复账单，false 表示不是重复账单
     */
    private fun checkRepeat(bill: BillInfoModel, bill2: BillInfoModel): Boolean {
        // 前提，上下5分钟，金额相同
        // 如果有一个是转账类型，另一个是收入或者支出，可能是重复账单

        if (bill.type != bill2.type) {
            if (bill.type == BillType.Income && bill2.type == BillType.Expend) {
                return false
            }
            if (bill.type == BillType.Expend && bill2.type == BillType.Income) {
                return false
            }
        }

        // 时间完全相同，是同一笔交易
        if (bill.time == bill2.time) {
            return true
        }
        // 规则相同，不是重复账单
        if (bill.ruleName == bill2.ruleName) {
            return false
        }
        // 渠道不同，是重复账单
        if (bill.channel != bill2.channel) {
            return true
        }


        return false
    }

    /**
     * 合并重复账单,将bill1的信息合并到bill2
     * @param bill 源账单
     * @param bill2 目标账单（将被更新）
     */
    private suspend fun mergeRepeatBill(
        bill: BillInfoModel,
        bill2: BillInfoModel,
        context: Context
    ) {
        bill2.apply {
            // 1. 合并账户信息
            mergeAccountInfo(bill, this)

            // 2. 合并商户和商品信息
            mergeShopInfo(bill, this)

            // 3. 合并分类信息
            mergeCategoryInfo(bill, this)

            val target = bill2.remark
            if (target != getRemark(bill2, context)) {
                // 4. 更新备注
                this.remark = getRemark(this, context)
            }

            // 5. 更新类型，如果有转账，以转账为准
            if (bill.type == BillType.Transfer && accountNameTo.isNotEmpty()) {
                this.type = BillType.Transfer
            }

        }
    }

    /**
     * 合并账户信息
     */
    private fun mergeAccountInfo(source: BillInfoModel, target: BillInfoModel) {
        // 只在目标账户为空时合并
        if (target.accountNameFrom.isEmpty() && source.accountNameFrom.isNotEmpty()) {
            target.accountNameFrom = source.accountNameFrom
        }
        if (target.accountNameTo.isEmpty() && source.accountNameTo.isNotEmpty()) {
            target.accountNameTo = source.accountNameTo
        }
    }

    /**
     * 合并商户和商品信息
     */
    private fun mergeShopInfo(source: BillInfoModel, target: BillInfoModel) {
        // 合并商户信息
        target.shopName = when {
            target.shopName.isEmpty() -> source.shopName
            source.shopName.isEmpty() -> target.shopName
            target.shopName.contains(source.shopName) -> target.shopName
            else -> "${target.shopName} ${source.shopName}".trim()
        }

        // 合并商品信息
        target.shopItem = when {
            target.shopItem.isEmpty() && source.shopItem.isNotEmpty() -> source.shopItem
            target.shopItem.isEmpty() -> target.extendData
            source.shopItem.isEmpty() -> target.shopItem
            target.shopItem.contains(source.shopItem) -> target.shopItem
            else -> "${target.shopItem} ${source.shopItem}".trim()
        }
    }

    /**
     * 合并分类信息
     */
    private fun mergeCategoryInfo(source: BillInfoModel, target: BillInfoModel) {
        // 如果源账单的分类不是"其他"，则使用源账单的分类
        if (target.cateName == "其他" || target.cateName == "其它") {
            target.cateName = source.cateName
        }
    }

    /**
     * 账单分组，用于检查重复账单
     * @return 返回匹配到的父账单，如果没有匹配则返回null
     */
    suspend fun groupBillInfo(
        billInfoModel: BillInfoModel,
        context: Context
    ): BillInfoModel? {


        // 1. 检查是否启用自动分组
        if (!isAutoGroupEnabled()) {
            return null
        }

        // 2. 查找可能重复的账单
        val potentialDuplicates = findPotentialDuplicates(billInfoModel)

        Server.log("潜在重复账单数量: ${potentialDuplicates.size}")
        // 3. 查找并处理重复账单
        return potentialDuplicates
            .find { bill ->
                bill.id != billInfoModel.id && checkRepeat(billInfoModel, bill)
            }
            ?.also { parentBill ->
                handleDuplicateBill(billInfoModel, parentBill, context)
            }
    }

    /**
     * 检查是否启用自动分组功能
     */
    private suspend fun isAutoGroupEnabled(): Boolean {
        val setting = Db.get().settingDao().query(Setting.AUTO_GROUP)?.value
        val enabled = setting != "false"
        Server.log("自动分组功能状态: $enabled")
        return enabled
    }

    /**
     * 查找指定时间范围和金额的潜在重复账单
     */
    private suspend fun findPotentialDuplicates(bill: BillInfoModel): List<BillInfoModel> {
        val timeWindow = 5 * 60 * 1000L // 5分钟
        val startTime = bill.time - timeWindow
        val endTime = bill.time + timeWindow

        Server.log("查找潜在重复账单 - 金额: ${bill.money}, 时间范围: $startTime - $endTime")

        return Db.get().billInfoDao().query(bill.money, startTime, endTime)
    }

    /**
     * 处理重复账单
     */
    private suspend fun handleDuplicateBill(
        currentBill: BillInfoModel,
        parentBill: BillInfoModel,
        context: Context
    ) {
        Server.log("发现重复账单 - 父账单: $parentBill, 当前账单: $currentBill")

        // 1. 设置分组ID
        currentBill.groupId = parentBill.id

        // 2. 合并账单信息
        mergeRepeatBill(currentBill, parentBill, context)

        // 3. 更新数据库
        Db.get().billInfoDao().apply {
            update(parentBill)
            update(currentBill)
        }

        Server.log("账单合并完成 - 父账单: $parentBill, 当前账单: $currentBill")
    }

    /**
     * 获取备注
     * @param billInfoModel 账单信息
     * @param context 上下文
     */
    suspend fun getRemark(billInfoModel: BillInfoModel, context: Context): String {
        val settingBillRemark =
            Db.get().settingDao().query(Setting.NOTE_FORMAT)?.value ?: DefaultData.NOTE_FORMAT
        return settingBillRemark
            .replace("【商户名称】", billInfoModel.shopName)
            .replace("【商品名称】", billInfoModel.shopItem)
            //  .replace("【币种类型】", Currency.valueOf(billInfoModel.currency).name(context))
            .replace("【金额】", billInfoModel.money.toString())
            .replace("【分类】", billInfoModel.cateName)
            .replace("【账本】", billInfoModel.bookName)
            .replace("【来源】", pkgName(billInfoModel.app, context))
            .replace("【原始资产】", billInfoModel.accountNameFrom)
            .replace("【渠道】", billInfoModel.channel)
    }


    private fun pkgName(pkg: String, context: Context): String {
        return runCatching {
            val packageManager = context.packageManager
            val applicationInfo = packageManager.getApplicationInfo(pkg, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        }.onFailure {
            it.printStackTrace()
        }.getOrNull() ?: pkg

    }

    /**
     * 设置账单的账本名称
     * 如果账本名称为空或为"默认账本"，则使用系统设置的默认账本名称
     */
    suspend fun setBookName(billInfoModel: BillInfoModel) {

        // 获取系统设置的默认账本名称
        val defaultBookName = getDefaultBookName()

        // 更新账本名称
        billInfoModel.bookName = when (billInfoModel.bookName) {
            "" -> defaultBookName         // 空名称使用默认账本
            "默认账本" -> defaultBookName  // "默认账本"替换为系统设置的默认账本
            else -> billInfoModel.bookName // 其他情况保持不变
        }
    }

    /**
     * 获取系统设置的默认账本名称
     * 如果未设置，返回"默认账本"
     */
    private suspend fun getDefaultBookName(): String {
        return Db.get().settingDao()
            .query(Setting.DEFAULT_BOOK_NAME)
            ?.value
            ?: "默认账本"
    }
}
