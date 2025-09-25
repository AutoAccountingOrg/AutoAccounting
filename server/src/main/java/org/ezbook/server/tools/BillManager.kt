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
import org.ezbook.server.constant.BillType
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.BillInfoModel

object BillManager {

    // 常量定义，缩短时间窗口，避免大量非重复账单
    private const val TIME_WINDOW_MILLIS = 3 * 60 * 1000L
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
        ServerLog.d("重复判断开始：b1(${brief(bill1)}), b2(${brief(bill2)})")
        // 前提：金额相同、类型相同
        // 时间完全相同，是同一笔交易，场景：用户重复打开账单列表识别
        if (bill1.time == bill2.time) {
            ServerLog.d("重复判断：时间相同 -> 重复")
            return true
        }

        //场景：用户通过A渠道支付2元，随后收到A渠道和B渠道的提醒
        //场景：用户通过A渠道支付2元，随后通过B渠道支付2元，且消费账户是同一个，

        //需要判断账户，如果支出或者收入账户完全一致
// TODO 后续需要根据用户判断反馈去掉这里
        // 时间不同，规则也一样,细分渠道也一样，一定不是重复账单：场景，用户多次转账给某人
        if (!bill1.generateByAi() && bill1.ruleName == bill2.ruleName) {
            if (bill1.channel == bill2.channel) {
                ServerLog.d("重复判断：规则相同(${bill1.ruleName})且渠道(${bill1.channel})相同 -> 非重复")
                return false
            }
        }

        // 有一些支出账户不同的重复账单

        /* // 账户判断依据不准确

         if (bill1.accountNameFrom.isNotEmpty() && bill2.accountNameFrom.isNotEmpty() && bill1.accountNameFrom != bill2.accountNameFrom) {
             ServerLog.d("重复判断：支出账户不同 -> 非重复")
             return false
         }


         if (bill1.accountNameTo.isNotEmpty() && bill2.accountNameTo.isNotEmpty() && bill1.accountNameTo != bill2.accountNameTo) {
             ServerLog.d("重复判断：收入账户不同 -> 非重复")
             return false
         }*/

        ServerLog.d("重复判断：未命中排除条件 -> 可能重复")
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

            // 更新备注：若模板为空，返回空串；仅当两者都非空且不同才覆盖，避免用户主动清空被回写
            val currentRemark = getRemark(this, context)
            if (currentRemark.isNotEmpty() && remark.isNotEmpty() && remark != currentRemark) {
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
            ServerLog.d("合并账户信息：来源为AI，目标非AI，保留目标资产")
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
            ServerLog.d("合并账户信息：目标为AI，采用来源资产")
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
     * 规范化名称：去除名称中连续重复的子串（长度≥3），例如：
     * - "京东自营京东自营旗舰店" -> "京东自营旗舰店"
     * - "苹果苹果旗舰店旗舰店" -> "苹果旗舰店"
     *
     * 规则源于需求：去掉重复内容，并移除长度超过 2 个字的叠词（等价于长度≥3 的重复片段）。
     */
    private fun normalizeName(name: String): String {
        var result = name.trim()
        if (result.isEmpty()) return result

        // 连续重复子串压缩：(.{3,}?)\1+ => \1
        // 使用循环直至不再发生替换，处理嵌套/多层重复
        val regex = Regex("(.{3,}?)\\1+")
        while (true) {
            val replaced = regex.replace(result, "\\1")
            if (replaced == result) break
            result = replaced
        }
        return result
    }

    /**
     * 移除跨字段重复：若 A 包含 B，则从 A 中移除 B；若 B 包含 A，则从 B 中移除 A。
     * 用于避免诸如 商户=“京东自营”、商品=“京东自营旗舰店” 的冗余，处理后商品变为“旗舰店”。
     */
    private fun removeCrossDuplicate(a: String, b: String): Pair<String, String> {
        var left = a
        var right = b
        if (left.isNotEmpty() && right.isNotEmpty()) {
            when {
                right.contains(left) -> right = right.replace(left, "").trim()
                left.contains(right) -> left = left.replace(right, "").trim()
            }
        }
        return left to right
    }

    /**
     * 清理备注（极简版）：只处理首尾
     * - 去掉首尾空白
     * - 去掉首尾分隔符（常见连接符/分隔符）
     */
    private fun cleanupRemark(text: String): String {
        if (text.isEmpty()) return text
        val sep = "[/|、,，;；:：-—~]"
        var t = text.trim()
        // 去掉首尾分隔符
        t = t.replace(Regex("^$sep+"), "")
        t = t.replace(Regex("$sep+$"), "")
        return t.trim()
    }

    /**
     * 账单去重，用于检查重复账单
     * @return 返回匹配到的父账单，如果没有匹配则返回null
     */
    suspend fun groupBillInfo(
        billInfoModel: BillInfoModel,
        context: Context
    ): BillInfoModel? {
        ServerLog.d("去重：开始，bill=${brief(billInfoModel)}")
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
                bill.id != billInfoModel.id && checkRepeat(billInfoModel, bill)
            }
            ?.also { parentBill ->
                ServerLog.d("去重：命中父账单 parentId=${parentBill.id}, currentId=${billInfoModel.id}")
                handleDuplicateBill(billInfoModel, parentBill, context)
            }
    }

    /**
     * 查找指定时间范围和金额的潜在重复账单
     */
    private suspend fun findPotentialDuplicates(bill: BillInfoModel): List<BillInfoModel> {
        val startTime = bill.time - TIME_WINDOW_MILLIS
        val endTime = bill.time + TIME_WINDOW_MILLIS

        ServerLog.d("去重：候选查询 id=${bill.id}, 金额=${bill.money}, 时间范围=$startTime-$endTime")
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
        ServerLog.d("去重：合并 parentId=${parentBill.id}, currentId=${currentBill.id}")

        // 设置去重ID
        currentBill.groupId = parentBill.id

        // 合并账单信息
        mergeRepeatBill(currentBill, parentBill, context)

        // 更新数据库
        Db.get().billInfoDao().run {
            update(parentBill)
            update(currentBill)
        }

        ServerLog.d("去重：合并完成 parentId=${parentBill.id}, currentId=${currentBill.id}")
    }

    /**
     * 获取备注
     * @param billInfoModel 账单信息
     * @param context 上下文
     */
    suspend fun getRemark(billInfoModel: BillInfoModel, context: Context): String {
        val settingBillRemark = SettingUtils.noteFormat()

        // 在合并备注之前，清洗商户与商品名称的重复（含长度≥3的叠词）
        var normalizedShopName = normalizeName(billInfoModel.shopName).trim()
        var normalizedShopItem = normalizeName(billInfoModel.shopItem).trim()

        // 跨字段去重：如果一方包含另一方，从较长的一方移除重复片段
        val (finalShopName, finalShopItem) = removeCrossDuplicate(
            normalizedShopName,
            normalizedShopItem
        )
        normalizedShopName = finalShopName
        normalizedShopItem = finalShopItem

        // 先按模板替换占位符
        val raw = settingBillRemark
            .replace("【商户名称】", normalizedShopName)
            .replace("【商品名称】", normalizedShopItem)
            .replace("【金额】", billInfoModel.money.toString())
            .replace("【分类】", billInfoModel.cateName)
            .replace("【账本】", billInfoModel.bookName)
            .replace("【来源】", getAppName(billInfoModel.app, context))
            .replace("【原始资产】", billInfoModel.accountNameFrom)
            .replace("【渠道】", billInfoModel.channel)
        return cleanupRemark(raw)
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
            ServerLog.e("获取应用名称失败：${it.message}", it)
        }.getOrDefault(packageName)
    }

}
