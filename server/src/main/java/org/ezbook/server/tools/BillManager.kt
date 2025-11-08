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
     *
     * 重复账单的定义：同一笔交易通过不同渠道产生的多条记录
     * 
     * @param bill1 账单1
     * @param bill2 账单2
     * @return true 表示是重复账单，false 表示不是重复账单
     */
    private suspend fun checkRepeat(bill1: BillInfoModel, bill2: BillInfoModel): Boolean {
        ServerLog.d("重复判断开始：b1(${brief(bill1)}), b2(${brief(bill2)})")

        // 规则1：时间完全相同 → 一定是重复（用户重复识别同一条通知/短信）
        if (bill1.time == bill2.time) {
            ServerLog.d("重复判断：时间完全相同 -> 重复")
            return true
        }

        // 规则2：如果两个都是AI生成，不做去重（AI识别不稳定，易误判）
        if (bill1.generateByAi() && bill2.generateByAi()) {
            ServerLog.d("重复判断：双方都是AI生成 -> 非重复")
            return false
        }


        // 规则4：规则相同 && 渠道相同 → 一定不是重复（场景：用户多次转账给同一个人）
        if (bill1.channel == bill2.channel && bill1.channel.isNotEmpty()) {
            ServerLog.d("重复判断：规则相同且渠道相同(${bill1.channel}) -> 非重复")
            return false
        }

        // 规则5：规则相同但渠道不同 → 可能是重复（同一笔交易的不同通知源）
        // 例如：微信支付通知 + 银行卡扣款通知
        ServerLog.d("重复判断：规则相同(${bill1.ruleName})但渠道不同 -> 可能重复")
        return true
    }

    /**
     * 合并重复账单数据
     *
     * 职责：仅合并账户、商户等基础数据
     * 不负责：分类决策、备注生成（这些由 BillService 统一处理）
     * 
     * @param sourceBill 源账单
     * @param targetBill 目标账单（将被更新）
     */
    private suspend fun mergeRepeatBill(
        sourceBill: BillInfoModel,
        targetBill: BillInfoModel
    ) {
        targetBill.apply {
            // 只合并基础数据字段
            mergeAccountInfo(sourceBill, this)
            mergeShopInfo(sourceBill, this)
            // 不再合并分类信息 - 由 BillService 统一处理
            // 不再生成备注 - 由 BillService 在分类后统一处理
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
     * 规范化名称：去除名称中连续重复的子串（长度≥3），例如：
     * - "京东自营京东自营旗舰店" -> "京东自营旗舰店"
     * - "苹果苹果旗舰店旗舰店" -> "苹果旗舰店"
     *
     * 规则源于需求：去掉重复内容，并移除长度超过 2 个字的叠词（等价于长度≥3 的重复片段）。
     */
    private fun normalizeName(name: String): String {
        var result = name.trim()
        if (result.isEmpty()) return result

        while (true) {
            var found = false

            // 从长子串开始，避免短子串干扰
            outer@ for (len in result.length downTo 2) {
                for (i in 0..result.length - len) {
                    val sub = result.substring(i, i + len)
                    val regex = Regex(Regex.escape(sub))
                    val matches = regex.findAll(result).toList()
                    if (matches.size > 1) {
                        // 删除所有重复，只保留第一个
                        val sb = StringBuilder(result)
                        // 从后往前删，避免索引错乱
                        matches.drop(1).reversed().forEach { m ->
                            sb.delete(m.range.first, m.range.last + 1)
                        }
                        result = sb.toString()
                        found = true
                        break@outer
                    }
                }
            }

            if (!found) break
        }

        return result
    }


    /**
     * 清理备注（极简版）：只处理首尾
     * - 去掉首尾空白
     * - 去掉首尾分隔符（常见连接符/分隔符），不使用正则
     */
    private fun cleanupRemark(text: String): String {
        if (text.isEmpty()) return text
        val seps = charArrayOf('/', '|', '、', ',', '，', ';', '；', ':', '：', '-', '—', '~')
        return text
            .trim()                 // 先去首尾空白
            .trimStart(*seps)       // 去首部分隔符
            .trimEnd(*seps)         // 去尾部分隔符
            .trim()                 // 再次去空白（以防分隔符后有空格）
    }

    /**
     * 账单去重，用于检查重复账单
     *
     * 返回父账单表示发现重复，返回 null 表示无重复
     * 注意：返回的父账单分类信息已被清空，需要调用方重新分类
     * 
     * @return 返回匹配到的父账单，如果没有匹配则返回null
     */
    suspend fun groupBillInfo(
        billInfoModel: BillInfoModel
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
                handleDuplicateBill(billInfoModel, parentBill)
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
     *
     * 职责：标记去重关系、合并基础数据、持久化
     * 不负责：分类决策（返回后由 BillService 统一处理）
     */
    private suspend fun handleDuplicateBill(
        currentBill: BillInfoModel,
        parentBill: BillInfoModel
    ) {
        ServerLog.d("去重：合并 parentId=${parentBill.id}, currentId=${currentBill.id}")

        // 设置去重关系
        currentBill.groupId = parentBill.id

        // 合并账单基础数据（不含分类）
        mergeRepeatBill(currentBill, parentBill)

        // 清空父账单的分类信息，标记需要重新分类
        parentBill.cateName = ""
        parentBill.bookName = ""

        // 更新数据库
        Db.get().billInfoDao().run {
            update(parentBill)
            update(currentBill)
        }

        ServerLog.d("去重：合并完成，父账单已清空分类等待重新处理")
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

        val key = "/=@=/"

        val normal = "$normalizedShopName$key$normalizedShopItem"
        val (shopName, shopItem) = normalizeName(normal).trim().split(key)


        // 先按模板替换占位符
        val raw = settingBillRemark
            .replace("【商户名称】", shopName)
            .replace("【商品名称】", shopItem)
            .replace("【金额】", billInfoModel.money.toString())
            .replace("【分类】", billInfoModel.cateName)
            .replace("【账本】", billInfoModel.bookName)
            .replace("【来源】", getAppName(billInfoModel.app, context))
            .replace("【原始资产】", billInfoModel.accountNameFrom)
            .replace("【渠道】", billInfoModel.channel)
        return raw
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
