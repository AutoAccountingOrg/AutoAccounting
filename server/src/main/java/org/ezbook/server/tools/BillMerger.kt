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
import org.ezbook.server.db.model.AssetsModel
import org.ezbook.server.db.model.BillInfoModel
import org.ezbook.server.log.ServerLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 账单合并器
 *
 * 职责：
 * 1. 合并账单数据（账户、商户等信息）
 * 2. 生成账单备注
 * 3. 管理账单分组关系
 */
object BillMerger {

    /**
     * 合并账单数据
     *
     * 职责：仅合并账户、商户等基础数据
     * 不负责：分类决策、备注生成（这些由 BillService 统一处理）
     */
    suspend fun mergeBillData(sourceBill: BillInfoModel, targetBill: BillInfoModel) {
        mergeAccountInfo(sourceBill, targetBill)
        mergeShopInfo(sourceBill, targetBill)
    }

    /**
     * 合并账户信息
     *
     * 合并规则：
     * 1. AI产生的账单直接忽略（不参与合并）
     * 2. 同位置的资产优先选择在已知资产里面的
     * 3. 通用名称（如"银行卡"）不应覆盖具体名称（如"交通银行（工资）"）
     * 4. 选择名字更长（更具体）的资产
     * 5. 非转账账单：如果 accountNameFrom 为空但 accountNameTo 有值，移到 accountNameFrom
     */
    suspend fun mergeAccountInfo(source: BillInfoModel, target: BillInfoModel) {
        val knownAssets = getKnownAssets()
        // 检查父账单或子账单任一是转账类型
        val isTransfer = source.type == BillType.Transfer || target.type == BillType.Transfer

        // 合并转出账户：只要任一方有值就尝试选择更好的
        if (source.accountNameFrom.isNotEmpty() || target.accountNameFrom.isNotEmpty()) {
            target.accountNameFrom = selectBetterAccount(
                source.accountNameFrom,
                target.accountNameFrom,
                knownAssets
            )
        }

        // 合并转入账户：只要任一方有值就尝试选择更好的
        if (source.accountNameTo.isNotEmpty() || target.accountNameTo.isNotEmpty()) {
            target.accountNameTo = selectBetterAccount(
                source.accountNameTo,
                target.accountNameTo,
                knownAssets
            )
        }

        // 非转账账单：如果 accountNameFrom 为空但 accountNameTo 有值，移到 accountNameFrom
        if (!isTransfer && target.accountNameFrom.isEmpty() && target.accountNameTo.isNotEmpty()) {
            target.accountNameFrom = target.accountNameTo
            target.accountNameTo = ""
        }
    }

    /**
     * 获取已知资产列表（缓存）
     */
    private var cachedAssets: List<AssetsModel>? = null

    private suspend fun getKnownAssets(): List<AssetsModel> {
        if (cachedAssets == null) {
            cachedAssets = Db.get().assetsDao().load()
        }
        return cachedAssets ?: emptyList()
    }

    /**
     * 选择更好的资产名称
     *
     * 核心规则：三层优先级
     * 1. 在资产列表中的 > 不在列表的
     * 2. 名字长的（更具体）> 名字短的（更模糊）
     * 3. 原值（target）> 新值（source）
     */
    private fun selectBetterAccount(
        source: String,
        target: String,
        knownAssets: List<AssetsModel>
    ): String {
        if (source.isEmpty()) return target
        if (target.isEmpty()) return source

        val sourceKnown = knownAssets.any { it.name == source }
        val targetKnown = knownAssets.any { it.name == target }

        val decision = when {
            // 第一层：资产列表优先
            sourceKnown && !targetKnown -> source
            !sourceKnown && targetKnown -> target
            // 第二层：都在列表或都不在列表时，名字长的更具体
            source.length != target.length -> if (source.length > target.length) source else target
            // 第三层：长度相同时，保留原值
            else -> target
        }

        // 记录决策路径，方便现场取证
        ServerLog.d(
            "账户合并选择: src='$source'(known=$sourceKnown,len=${source.length}), " +
                    "tgt='$target'(known=$targetKnown,len=${target.length}), " +
                    "choose='$decision'"
        )

        return decision
    }

    /**
     * 合并商户和商品信息
     */
    fun mergeShopInfo(source: BillInfoModel, target: BillInfoModel) {
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
     * 设置账单分组关系并清空分类
     */

    /**
     * 保存账单分组到数据库
     */
    suspend fun saveBillGroup(currentBill: BillInfoModel, parentBill: BillInfoModel) {
        ServerLog.d("保存账单分组：parent=${parentBill.id}, child=${currentBill.id}, groupId=${currentBill.groupId}")
        ServerLog.d("父账单合并后数据：accountFrom=${parentBill.accountNameFrom}, accountTo=${parentBill.accountNameTo}, shop=${parentBill.shopName}")
        Db.get().billInfoDao().run {
            update(parentBill)
            update(currentBill)
        }
        ServerLog.d("账单分组保存完成")
    }

    /**
     * 获取备注
     *
     * @param billInfoModel 账单信息
     * @param context 上下文
     * @return 格式化后的备注字符串
     */
    suspend fun getRemark(billInfoModel: BillInfoModel, context: Context): String {
        val template = SettingUtils.noteFormat()
        if (template.isEmpty()) return ""

        // 规范化商户和商品名称（去除重复内容）
        val normalizedShopName = normalizeName(billInfoModel.shopName).trim()
        val normalizedShopItem = normalizeName(billInfoModel.shopItem).trim()

        // 使用特殊分隔符合并后再规范化，避免商户和商品名称重复
        val separator = "/=@=/"
        val combined = "$normalizedShopName$separator$normalizedShopItem"
        val (shopName, shopItem) = normalizeName(combined).trim().split(separator)

        // 替换占位符
        return template
            // 基础信息
            .replace("【商户名称】", shopName)
            .replace("【商品名称】", shopItem)
            .replace("【金额】", billInfoModel.money.toString())
            .replace("【分类】", billInfoModel.cateName)
            .replace("【账本】", billInfoModel.bookName)
            .replace("【来源】", getAppName(billInfoModel.app, context))
            .replace("【原始资产】", billInfoModel.accountNameFrom)
            .replace("【目标资产】", billInfoModel.accountNameTo)
            .replace("【渠道】", billInfoModel.channel)
            // 扩展信息
            .replace("【规则名称】", billInfoModel.ruleName)
            .replace("【AI】", getAIProvider(billInfoModel))
            .replace("【货币类型】", billInfoModel.currencyCode())
            .replace("【手续费】", billInfoModel.fee.toString())
            .replace("【标签】", billInfoModel.tags)
            .replace("【交易类型】", billInfoModel.type.toChineseString())
            .replace("【时间】", formatTime(billInfoModel.time))
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

    /**
     * 获取AI提供商名称
     *
     * @param billInfoModel 账单信息
     * @return AI提供商名称，如果不是AI生成则返回空字符串
     */
    private fun getAIProvider(billInfoModel: BillInfoModel): String {
        val ruleName = billInfoModel.ruleName
        // AI生成的账单格式："{提供商名称} 生成"
        return if (ruleName.endsWith(" 生成")) {
            ruleName.substringBeforeLast(" 生成")
        } else {
            ""
        }
    }

    /**
     * 格式化时间为字符串
     *
     * @param timeMillis 时间戳（毫秒）
     * @return 格式化后的时间字符串（yyyy-MM-dd HH:mm:ss）
     */
    private fun formatTime(timeMillis: Long): String {
        return try {
            val date = Date(timeMillis)
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            sdf.format(date)
        } catch (e: Exception) {
            ServerLog.e("格式化时间失败：${e.message}", e)
            ""
        }
    }
}

