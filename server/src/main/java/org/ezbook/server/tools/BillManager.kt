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

object BillManager {

    // 常量定义，缩短时间窗口，避免大量非重复账单
    private const val TIME_WINDOW_MINUTES = 2
    private const val TIME_WINDOW_MILLIS = TIME_WINDOW_MINUTES * 60 * 1000L
    private const val DEFAULT_BOOK_NAME = "默认账本"
    private const val OTHER_CATEGORY_1 = "其他"
    private const val OTHER_CATEGORY_2 = "其它"

    /**
     * 检查账单是否重复
     * @param bill1 账单1
     * @param bill2 账单2
     * @return true 表示是重复账单，false 表示不是重复账单
     */
    private suspend fun checkRepeat(bill1: BillInfoModel, bill2: BillInfoModel): Boolean {
        // 前提：金额相同、类型相同
        // 时间完全相同，是同一笔交易，场景：用户重复打开账单列表识别
        if (bill1.time == bill2.time) {
            Server.logD("重复账单判据：金额相同、类型相同、时间完全相同，一定是重复账单")
            return true
        }

        //场景：用户通过A渠道支付2元，随后收到A渠道和B渠道的提醒
        //场景：用户通过A渠道支付2元，随后通过B渠道支付2元，且消费账户是同一个，

        //需要判断账户，如果支出或者收入账户完全一致

        // 时间不同，规则也一样,细分渠道也一样，一定不是重复账单：场景，用户多次转账给某人
        if (bill1.ruleName == bill2.ruleName) {

            if (bill1.channel == bill2.channel) {
                Server.logD("重复账单判据：金额相同、类型相同、时间不同、规则一样、细分渠道一样，一定不是重复账单")
                return false
            }
        }

        if (bill1.accountNameFrom.isNotEmpty() && bill2.accountNameFrom.isNotEmpty() && bill1.accountNameFrom != bill2.accountNameFrom) {
            Server.logD("重复账单判据：金额相同、类型相同、时间不同、规则一样、支出账户不一样，一定不是重复账单")
            return false
        }


        if (bill1.accountNameTo.isNotEmpty() && bill2.accountNameTo.isNotEmpty() && bill1.accountNameTo != bill2.accountNameTo) {
            Server.logD("重复账单判据：金额相同、类型相同、时间不同、规则一样、收入账户不一样，一定不是重复账单")
            return false
        }

        return true

    }

    /**
     * 合并重复账单,将bill1的信息合并到bill2
     * @param sourceBill 源账单
     * @param targetBill 目标账单（将被更新）
     */
    private suspend fun mergeRepeatBill(
        sourceBill: BillInfoModel,
        targetBill: BillInfoModel,
        context: Context
    ) {
        targetBill.apply {
            // 合并各类信息
            mergeAccountInfo(sourceBill, this)
            mergeShopInfo(sourceBill, this)
            mergeCategoryInfo(sourceBill, this)

            // 更新备注
            val currentRemark = getRemark(this, context)
            if (remark != currentRemark) {
                remark = currentRemark
            }
        }
    }

    /**
     * 合并账户信息
     */
    private fun mergeAccountInfo(source: BillInfoModel, target: BillInfoModel) {
        val isTransfer = source.type == BillType.Transfer

        // 合并源账户
        if (source.accountNameFrom.isNotEmpty() &&
            (isTransfer || target.accountNameFrom.isEmpty())
        ) {
            target.accountNameFrom = source.accountNameFrom
        }

        // 合并目标账户
        if (target.accountNameTo.isEmpty() && source.accountNameTo.isNotEmpty()) {
            target.accountNameTo = source.accountNameTo
        }
    }

    /**
     * 合并商户和商品信息
     */
    private fun mergeShopInfo(source: BillInfoModel, target: BillInfoModel) {
        // 合并商户信息
        target.shopName = mergeStringField(target.shopName, source.shopName)
        
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
     * 合并字符串字段的通用方法
     */
    private fun mergeStringField(target: String, source: String): String {
        return when {
            target.isEmpty() -> source
            source.isEmpty() -> target
            target.contains(source) -> target
            else -> "$target $source".trim()
        }
    }

    /**
     * 合并分类信息
     */
    private fun mergeCategoryInfo(source: BillInfoModel, target: BillInfoModel) {
        // 如果目标账单的分类是"其他"，则使用源账单的分类
        if (target.cateName in listOf(OTHER_CATEGORY_1, OTHER_CATEGORY_2)) {
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

        // 检查是否启用自动分组
        if (!SettingUtils.autoGroup()) {
            Server.logD("未启用自动去重")
            return null
        }

        // 查找可能重复的账单
        val potentialDuplicates = findPotentialDuplicates(billInfoModel)
        Server.log("潜在重复账单数量: ${potentialDuplicates.size}")

        // 查找并处理重复账单
        return potentialDuplicates
            .find { bill ->
                bill.id != billInfoModel.id && checkRepeat(billInfoModel, bill)
            }
            ?.also { parentBill ->
                handleDuplicateBill(billInfoModel, parentBill, context)
            }
    }

    /**
     * 查找指定时间范围和金额的潜在重复账单
     */
    private suspend fun findPotentialDuplicates(bill: BillInfoModel): List<BillInfoModel> {
        val startTime = bill.time - TIME_WINDOW_MILLIS
        val endTime = bill.time + TIME_WINDOW_MILLIS

        Server.log("查找潜在重复账单 - 金额: ${bill.money}, 时间范围: $startTime - $endTime")
        return Db.get().billInfoDao().query(bill.money, startTime, endTime, bill.type)
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

        // 设置分组ID
        currentBill.groupId = parentBill.id

        // 合并账单信息
        mergeRepeatBill(currentBill, parentBill, context)

        // 更新数据库
        Db.get().billInfoDao().run {
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
        val settingBillRemark = SettingUtils.noteFormat()

        return settingBillRemark
            .replace("【商户名称】", billInfoModel.shopName)
            .replace("【商品名称】", billInfoModel.shopItem)
            .replace("【金额】", billInfoModel.money.toString())
            .replace("【分类】", billInfoModel.cateName)
            .replace("【账本】", billInfoModel.bookName)
            .replace("【来源】", getAppName(billInfoModel.app, context))
            .replace("【原始资产】", billInfoModel.accountNameFrom)
            .replace("【渠道】", billInfoModel.channel)
    }

    /**
     * 获取应用名称
     */
    private fun getAppName(packageName: String, context: Context): String {
        return runCatching {
            val packageManager = context.packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        }.onFailure {
            it.printStackTrace()
        }.getOrDefault(packageName)
    }

}
