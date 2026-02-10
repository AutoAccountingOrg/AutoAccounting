/*
 * Copyright (C) 2024 ankio(ankio@ankio.net)
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

import org.ezbook.server.ai.tools.AssetTool
import org.ezbook.server.constant.BillType
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.AssetsMapModel
import org.ezbook.server.db.model.AssetsModel
import org.ezbook.server.db.model.BillInfoModel
import org.ezbook.server.log.ServerLog

/**
 * 资产映射工具类
 *
 * 负责将账单中的原始账户名称映射为标准化的资产名称，支持多种映射方式：
 * - 直接资产查找：在资产表中查找完全匹配的名称
 * - 自定义映射：使用用户定义的映射规则
 * - 正则表达式匹配：使用模式匹配进行灵活映射
 * - 空映射创建：为未知账户创建占位符映射
 *
 * 设计特性：
 * - 线程安全的缓存机制，避免重复数据库查询
 * - 责任链模式的映射查找，逻辑清晰易维护
 * - 智能的错误处理，确保系统稳定性
 */
class AssetsMap {

    // 懒加载缓存：首次访问时从数据库加载，后续复用内存数据
    private var assets: List<AssetsModel> = emptyList()
    private var assetsMap: List<AssetsMapModel> = emptyList()

    /**
     * 懒加载获取资产列表，读取一次后缓存到内存
     */
    private suspend fun getAssets(): List<AssetsModel> {
        if (assets.isEmpty()) {
            assets = Db.get().assetsDao().load()
            ServerLog.d("资产列表已加载: ${assets.size} 条")
        }
        return assets
    }

    /**
     * 懒加载获取资产映射列表，读取一次后缓存到内存
     */
    private suspend fun getAssetsMap(): List<AssetsMapModel> {
        if (assetsMap.isEmpty()) {
            assetsMap = Db.get().assetsMapDao().list()
            ServerLog.d("资产映射列表已加载: ${assetsMap.size} 条")
        }
        return assetsMap
    }

    /**
     * 设置资产映射
     *
     * 对账单的来源账户和目标账户进行映射处理，将原始账户名称转换为标准化的资产名称
     *
     * @param billInfoModel 账单信息模型，会直接修改其中的账户名称字段
     * @param useAi 调用方是否允许AI参与映射（路由或批处理可强制关闭）
     *
     * 注意：必须运行在IO线程中，因为涉及数据库操作
     */
    suspend fun setAssetsMap(billInfoModel: BillInfoModel, useAi: Boolean = true) {
        // 检查资产管理器是否启用
        if (!SettingUtils.featureAssetManager()) {
            ServerLog.d("资产管理未启用，跳过映射处理")
            return
        }

        // 始终使用 raw 字段做查找和创建，不可用时回退到当前值（兼容历史数据重映射）
        val rawFrom = billInfoModel.rawAccountNameFrom.ifBlank { billInfoModel.accountNameFrom }
        val rawTo = billInfoModel.rawAccountNameTo.ifBlank { billInfoModel.accountNameTo }
        ServerLog.d("开始资产映射: type=${billInfoModel.type} rawFrom='$rawFrom' rawTo='$rawTo'")

        // 处理来源账户映射
        if (shouldMap(rawFrom, billInfoModel.type, isTo = false)) {
            mapAccount(rawFrom, rawTo, billInfoModel, useAi = useAi)?.let { mappedName ->
                billInfoModel.accountNameFrom = mappedName
                ServerLog.d("来源账户映射: '$rawFrom' -> '$mappedName'")
            }
        } else {
            ServerLog.d("跳过来源账户映射: '$rawFrom'")
        }

        // 处理目标账户映射
        if (shouldMap(rawTo, billInfoModel.type, isTo = true)) {
            mapAccount(
                rawTo,
                rawFrom,
                billInfoModel,
                isAccountName2 = true,
                useAi = useAi
            )?.let { mappedName ->
                billInfoModel.accountNameTo = mappedName
                ServerLog.d("目标账户映射: '$rawTo' -> '$mappedName'")
            }
        } else {
            ServerLog.d("跳过目标账户映射: '$rawTo'")
        }

        ServerLog.d("资产映射完成: ${billInfoModel.type} | ${billInfoModel.accountNameFrom} -> ${billInfoModel.accountNameTo}")
    }

    /**
     * 判断是否需要映射
     * @param rawName 原始账户名
     * @param type 账单类型
     * @param isTo true=目标账户, false=来源账户
     */
    private fun shouldMap(rawName: String, type: BillType, isTo: Boolean): Boolean {
        if (rawName.isEmpty()) return false
        val skipTypes = if (isTo) {
            listOf(BillType.ExpendLending, BillType.ExpendRepayment)
        } else {
            listOf(BillType.IncomeLending, BillType.IncomeRepayment)
        }
        return type !in skipTypes
    }

    /**
     * 映射单个账户
     *
     * 责任链按优先级顺序处理：
     * 1. 直接资产查找 - 在资产表中查找完全匹配
     * 2. 自定义映射查找 - 使用用户定义的映射规则
     * 3. 正则表达式匹配 - 使用模式匹配进行灵活映射
     * 4. 创建空映射占位符
     * 5. AI映射
     * 6. 算法匹配
     *
     * @param rawName 原始账户名称（映射前）
     * @param rawOtherName 对方原始账户名称（传给AI做上下文）
     * @param billInfoModel 账单模型，用于判断是否AI生成
     * @param isAccountName2 是否为目标账户名（避免双向AI映射冲突）
     * @param useAi 调用方是否允许AI参与映射
     * @return 映射后的账户名称，null表示无法映射
     */
    private suspend fun mapAccount(
        rawName: String,
        rawOtherName: String,
        billInfoModel: BillInfoModel,
        isAccountName2: Boolean = false,
        useAi: Boolean = true
    ): String? {
        ServerLog.d("映射账户开始: rawName='$rawName' isAccountName2=$isAccountName2")
        if (rawName.isBlank() || rawName.endsWith("支付")) {
            ServerLog.d("映射账户跳过: 账户名为空或者过于宽泛")
            return null
        }

        // 1. 直接资产查找 - 最高优先级
        getAssets().firstOrNull { it.name == rawName }?.let { asset ->
            ServerLog.d("直接资产查找命中: '${asset.name}'")
            return asset.name
        }

        // 2. 自定义映射查找（若 mapName 为空则跳过继续后续策略）
        getAssetsMap().firstOrNull { it.name == rawName && it.mapName.isNotBlank() }?.let {
            ServerLog.d("自定义映射命中: '${it.name}' -> '${it.mapName}'")
            return it.mapName
        }

        // 3. 正则表达式匹配（若 mapName 为空则跳过）
        getAssetsMap().filter { it.regex && it.mapName.isNotBlank() }.firstOrNull { mapping ->
            runCatchingExceptCancel { Regex(mapping.name).containsMatchIn(rawName) }.getOrElse { false }
        }?.let { mapping ->
            ServerLog.d("正则映射命中: /${mapping.name}/ -> '${mapping.mapName}'")
            return mapping.mapName
        }

        // 4. 创建空白占位符（在 AI 之前，避免 AI 不稳定导致错过创建）
        if (!billInfoModel.generateByAi() && SettingUtils.autoAssetMap()) {
            runCatchingExceptCancel {
                val emptyMapping = AssetsMapModel().apply {
                    name = rawName
                    mapName = ""
                }
                ServerLog.d("创建空资产映射占位符: '$rawName'")
                Db.get().assetsMapDao().insert(emptyMapping)
            }
        }

        // 5. AI映射（仅来源账户触发，避免双向冲突）
        if (useAi &&
            SettingUtils.featureAiAvailable() &&
            SettingUtils.aiAssetMapping() &&
            !isAccountName2
        ) {
            ServerLog.d("通过AI进行资产映射")
            val json = AssetTool().execute(
                rawOtherName,
                rawName,
                billInfoModel.app,
                billInfoModel.type
            )
            if (json != null) {
                val aiTo = json.safeGetString("asset1")
                val aiFrom = json.safeGetString("asset2")
                billInfoModel.accountNameTo = aiTo.ifBlank { billInfoModel.accountNameTo }
                billInfoModel.accountNameFrom = aiFrom.ifBlank { billInfoModel.accountNameFrom }
                ServerLog.d("AI建议: to='$aiTo' from='$aiFrom'")
                return null
            } else {
                ServerLog.d("AI未返回有效结果")
            }
        }

        // 6. 基于算法的保守匹配（数字优先 → 最长连续相似子串）
        findByAlgorithm(rawName)?.let { return it }

        return null
    }



    /**
     * 基于算法的资产匹配
     *
     * 策略：
     * 1) 若原始账户名中包含数字（如卡号），优先用数字在资产名中匹配
     * 2) 否则使用“最长连续相似子串”作为相似度指标选取最佳匹配
     *
     * 返回匹配到的资产名称；若无匹配则返回 null
     */
    private suspend fun findByAlgorithm(accountName: String): String? {
        // 提取原始账户名中的银行标识（如“招商银行”、“北京银行”），用于信用卡银行一致性校验
        val inputBank = extractBankName(accountName)

        // 1) 数字优先匹配（如卡号）
        val number = Regex("\\d+").find(accountName)?.value ?: ""
        if (number.isNotEmpty()) {
            assets.firstOrNull { candidate ->
                val hit = candidate.name.contains(number.trim())
                if (!hit) return@firstOrNull false
                // 若候选为信用卡且识别到输入中的银行名，则要求银行一致，避免不同银行信用卡混淆
                if (candidate.name.contains("信用卡") && inputBank != null) {
                    val candidateBank = extractBankName(candidate.name)
                    return@firstOrNull candidateBank == null || candidateBank == inputBank
                }
                true
            }?.let { asset ->
                ServerLog.d("算法映射-卡号命中: ${asset.name}")
                return asset.name
            }
        }

        // 2) 文本相似度匹配（最长连续相似子串）
        val cleanInput = accountName.cleanText(number)
        var bestName: String? = null
        var bestSimilarity = 0
        var bestDiff = Int.MAX_VALUE

        for (asset in assets) {
            // 若候选为信用卡且识别到输入中的银行名，但银行不一致，则跳过该候选
            if (asset.name.contains("信用卡") && inputBank != null) {
                val candidateBank = extractBankName(asset.name)
                if (candidateBank != null && candidateBank != inputBank) {
                    continue
                }
            }
            val cleanAssetName = asset.name.cleanText()
            val similarity = calculateConsecutiveSimilarity(cleanAssetName, cleanInput)
            if (similarity > 0) {
                val diff = cleanAssetName.length - similarity
                if (similarity > bestSimilarity || (similarity == bestSimilarity && diff < bestDiff)) {
                    // 过滤过短的中文前缀导致的误命中（与端侧行为对齐）
                    if (!(similarity == 2 && cleanInput.startsWith("中国"))) {
                        bestName = asset.name
                        bestSimilarity = similarity
                        bestDiff = diff
                    }
                }
            }
        }

        bestName?.let {
            ServerLog.d("算法映射-文本命中: '$accountName' -> '$it' 相似度=$bestSimilarity 差异=$bestDiff")
            return it
        }
        ServerLog.d("算法映射未命中: '$accountName'")
        return null
    }

    /**
     * 从文本中提取银行名称。
     * 仅做保守提取：匹配“[若干中文]银行”的最早出现，用于信用卡银行一致性校验。
     */
    private fun extractBankName(text: String): String? {
        val match = Regex("([\\u4e00-\\u9fa5]{2,10}银行)").find(text)
        return match?.value
    }

    /**
     * 计算两个字符串的最长连续相似子串长度
     */
    private fun calculateConsecutiveSimilarity(a: String, b: String): Int {
        val m = a.length
        val n = b.length
        if (m == 0 || n == 0) return 0

        var previousRow = IntArray(n + 1)
        var currentRow = IntArray(n + 1)
        var maxLength = 0

        for (i in 1..m) {
            for (j in 1..n) {
                if (a[i - 1] == b[j - 1]) {
                    currentRow[j] = previousRow[j - 1] + 1
                    if (currentRow[j] > maxLength) maxLength = currentRow[j]
                } else {
                    currentRow[j] = 0
                }
            }
            val temp = previousRow
            previousRow = currentRow
            currentRow = temp
        }
        return maxLength
    }

    /**
     * 字符串清理（去除卡号、常见无效词）
     */
    private fun String.cleanText(numberToRemove: String = ""): String {
        return this
            .replace(numberToRemove, "")
            .replace(Regex("\\([^(（【】）)]*\\)"), "")
            .replace(Regex("[卡银行储蓄借记]"), "")
            .replace("支付", "")
            .trim()
    }


}