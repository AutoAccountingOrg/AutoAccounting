/*
 * Copyright (C) 2023 ankio(ankio@ankio.net)
 * Licensed under the Apache License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may ob        logger.debug { "分组：开始，bill=${brief(billInfoModel)}" }ain a copy of the License at
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
import org.ezbook.server.constant.BillType
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.BillInfoModel
import org.ezbook.server.tools.SettingUtils
import io.github.oshai.kotlinlogging.KotlinLogging

object BillManager {

    private val logger = KotlinLogging.logger(this::class.java.name)

    // 常量定义，缩短时间窗口，避免大量非重复账单
    private const val TIME_WINDOW_MILLIS = 60 * 1000L
    private const val OTHER_CATEGORY_1 = "其他"
    private const val OTHER_CATEGORY_2 = "其它"

    /**
     * 生成账单简要信息，便于日志定位
     */
    private fun brief(bill: BillInfoModel): String =
        "id=${bill.id}, money=${bill.money}, time=${bill.time}, type=${bill.type}, rule=${bill.ruleName}, ch=${bill.channel}, from=${bill.accountNameFrom}, to=${bill.accountNameTo}"

    /**
     * 检查账单是否重复
     * @param bill1 账单1
     * @param bill2 账单2
     * @return true 表示是重复账单，false 表示不是重复账单
     */
    private suspend fun checkRepeat(bill1: BillInfoModel, bill2: BillInfoModel): Boolean {
        logger.debug { "重复判断开始：b1(${brief(bill1)}), b2(${brief(bill2)})" }
        // 前提：金额相同、类型相同
        // 时间完全相同，是同一笔交易，场景：用户重复打开账单列表识别
        if (bill1.time == bill2.time) {
            logger.debug { "重复判断：时间相同 -> 重复" }
            return true
        }

        //场景：用户通过A渠道支付2元，随后收到A渠道和B渠道的提醒
        //场景：用户通过A渠道支付2元，随后通过B渠道支付2元，且消费账户是同一个，

        //需要判断账户，如果支出或者收入账户完全一致

        // 时间不同，规则也一样,细分渠道也一样，一定不是重复账单：场景，用户多次转账给某人
        if (!bill1.generateByAi() && bill1.ruleName == bill2.ruleName) {
            if (bill1.channel == bill2.channel) {
                logger.debug { "重复判断：规则相同且渠道相同 -> 非重复" }
                return false
            }
        }

        // 有一些支出账户不同的重复账单

        /* // 账户判断依据不准确

         if (bill1.accountNameFrom.isNotEmpty() && bill2.accountNameFrom.isNotEmpty() && bill1.accountNameFrom != bill2.accountNameFrom) {
             logger.debug { "重复判断：支出账户不同 -> 非重复" }
             return false
         }


         if (bill1.accountNameTo.isNotEmpty() && bill2.accountNameTo.isNotEmpty() && bill1.accountNameTo != bill2.accountNameTo) {
             logger.debug { "重复判断：收入账户不同 -> 非重复" }
             return false
         }*/

        logger.debug { "重复判断：未命中排除条件 -> 可能重复" }
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
        val sourceAi = source.generateByAi()
        val targetAi = target.generateByAi()

        // 规则1：来源为AI、目标非AI -> 保留目标资产，直接跳过
        if (sourceAi && !targetAi) {
            logger.debug { "合并账户信息：来源为AI，目标非AI，保留目标资产" }
            return
        }

        // 规则2：来源非AI、目标为AI -> 使用来源资产覆盖（在非空前提下）
        if (!sourceAi && targetAi) {
            if (source.accountNameFrom.isNotEmpty()) {
                target.accountNameFrom = source.accountNameFrom
            }
            if (source.accountNameTo.isNotEmpty()) {
                target.accountNameTo = source.accountNameTo
            }
            logger.debug { "合并账户信息：目标为AI，采用来源资产" }
            return
        }
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
        logger.debug { "分组：开始，bill=${brief(billInfoModel)}" }
        // 检查是否启用自动分组
        if (!SettingUtils.autoGroup()) {
            logger.debug { "分组：未启用，跳过" }
            return null
        }

        // 查找可能重复的账单
        val potentialDuplicates = findPotentialDuplicates(billInfoModel)
        logger.debug { "分组：候选数量=${potentialDuplicates.size}" }

        // 查找并处理重复账单
        return potentialDuplicates
            .find { bill ->
                bill.id != billInfoModel.id && checkRepeat(billInfoModel, bill)
            }
            ?.also { parentBill ->
                logger.debug { "分组：命中父账单 parentId=${parentBill.id}, currentId=${billInfoModel.id}" }
                handleDuplicateBill(billInfoModel, parentBill, context)
            }
    }

    /**
     * 查找指定时间范围和金额的潜在重复账单
     */
    private suspend fun findPotentialDuplicates(bill: BillInfoModel): List<BillInfoModel> {
        val startTime = bill.time - TIME_WINDOW_MILLIS
        val endTime = bill.time + TIME_WINDOW_MILLIS

        logger.debug { "分组：候选查询 id=${bill.id}, 金额=${bill.money}, 时间范围=$startTime-$endTime" }
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
        logger.debug { "分组：合并 parentId=${parentBill.id}, currentId=${currentBill.id}" }

        // 设置分组ID
        currentBill.groupId = parentBill.id

        // 合并账单信息
        mergeRepeatBill(currentBill, parentBill, context)

        // 更新数据库
        Db.get().billInfoDao().run {
            update(parentBill)
            update(currentBill)
        }

        logger.debug { "分组：合并完成 parentId=${parentBill.id}, currentId=${currentBill.id}" }
    }

    /**
     * 获取备注
     * @param billInfoModel 账单信息
     * @param context 上下文
     */
    suspend fun getRemark(billInfoModel: BillInfoModel, context: Context): String {
        val settingBillRemark = SettingUtils.noteFormat()
        // 先按模板替换占位符
        val raw = settingBillRemark
            .replace("【商户名称】", billInfoModel.shopName)
            .replace("【商品名称】", billInfoModel.shopItem)
            .replace("【金额】", billInfoModel.money.toString())
            .replace("【分类】", billInfoModel.cateName)
            .replace("【账本】", billInfoModel.bookName)
            .replace("【来源】", getAppName(billInfoModel.app, context))
            .replace("【原始资产】", billInfoModel.accountNameFrom)
            .replace("【渠道】", billInfoModel.channel)
        // 去除重复片段并规整空白，避免合并后出现重复词（例如 商户/渠道/商品名重复）
        return normalizeRemark(raw)
    }

    /**
     * 备注去重与规整：
     * - 使用空白与连字符作为分隔，将重复片段去重（保留首次出现）
     * - 规整多余空白，返回更简洁的备注
     */
    private fun normalizeRemark(text: String): String {
        // 统一分隔：空白和连字符都作为分隔符
        val tokens = text
            .replace("\u00A0", " ") // 非断行空格
            .trim()
            .split(Regex("[\\s\u3000\\-]+"))
            .filter { it.isNotBlank() }

        val seen = LinkedHashSet<String>()
        for (token in tokens) {
            // 去重：只保留首次出现的片段
            if (!seen.contains(token)) seen.add(token)
        }
        return seen.joinToString(" ")
    }

    /**
     * 获取应用名称
     */
    private suspend fun getAppName(packageName: String, context: Context): String {
        return runCatchingExceptCancel {
            val packageManager = context.packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        }.onFailure {
            logger.error(it) { "获取应用名称失败：${it.message}" }
        }.getOrDefault(packageName)
    }

}
