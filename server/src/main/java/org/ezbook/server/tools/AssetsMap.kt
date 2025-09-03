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

import org.ezbook.server.Server
import org.ezbook.server.constant.BillType
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.AssetsModel
import org.ezbook.server.db.model.AssetsMapModel
import org.ezbook.server.db.model.BillInfoModel

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
object AssetsMap {

    // 使用线程安全的缓存实现
    private val cache = MemoryCache.instance
    private const val REGEX_MAPPINGS_CACHE_KEY = "assets_regex_mappings"
    private const val ASSETS_LIST_CACHE_KEY = "assets_list"
    private const val CACHE_DURATION_SECONDS = 300L // 5分钟缓存

    /**
     * 清除缓存，在映射规则更新时调用
     *
     * 应在以下情况调用：
     * - 添加、修改或删除资产映射规则时
     * - 系统设置发生变化时
     */
    fun clearCache() {
        cache.remove(REGEX_MAPPINGS_CACHE_KEY)
        cache.remove(ASSETS_LIST_CACHE_KEY)
    }

    /**
     * 获取正则表达式映射规则（带缓存）
     *
     * 使用缓存机制避免频繁的数据库查询，提升性能
     *
     * @return 正则表达式映射规则列表
     */
    private suspend fun getRegexMappings(): List<AssetsMapModel> {
        // 尝试从缓存获取
        cache.get(REGEX_MAPPINGS_CACHE_KEY)?.let { cachedMappings ->
            return cachedMappings as List<AssetsMapModel>
        }

        // 缓存未命中，从数据库重新加载
        return try {
            val regexMappings = Db.get().assetsMapDao().list().filter { it.regex }
            cache.put(REGEX_MAPPINGS_CACHE_KEY, regexMappings, CACHE_DURATION_SECONDS)
            Server.logD("从数据库加载并缓存了 ${regexMappings.size} 条正则映射规则")
            regexMappings
        } catch (e: Exception) {
            Server.log("获取正则映射规则失败: ${e.message}")
            Server.log(e)
            emptyList() // 返回空列表，避免系统崩溃
        }
    }

    /**
     * 获取资产列表（带缓存）
     *
     * 说明：算法映射需要遍历现有资产，使用缓存减少数据库压力。
     */
    private suspend fun getAssetsList(): List<AssetsModel> {
        cache.get(ASSETS_LIST_CACHE_KEY)?.let { cached ->
            return cached as List<AssetsModel>
        }
        return try {
            val assets = Db.get().assetsDao().load()
            cache.put(ASSETS_LIST_CACHE_KEY, assets, CACHE_DURATION_SECONDS)
            Server.logD("从数据库加载并缓存了 ${assets.size} 个资产")
            assets
        } catch (e: Exception) {
            Server.log("获取资产列表失败: ${e.message}")
            Server.log(e)
            emptyList()
        }
    }

    /**
     * 设置资产映射
     *
     * 对账单的来源账户和目标账户进行映射处理，将原始账户名称转换为标准化的资产名称
     *
     * @param billInfoModel 账单信息模型，会直接修改其中的账户名称字段
     *
     * 注意：必须运行在IO线程中，因为涉及数据库操作
     */
    suspend fun setAssetsMap(billInfoModel: BillInfoModel) {
        // 检查资产管理器是否启用
        if (!isAssetManagerEnabled()) {
            Server.logD("资产管理器未启用，跳过映射处理")
            return
        }

        val originalFromAccount = billInfoModel.accountNameFrom
        val originalToAccount = billInfoModel.accountNameTo

        // 处理来源账户映射
        if (shouldMapFromAccount(billInfoModel)) {
            mapAccount(billInfoModel.accountNameFrom, billInfoModel)?.let { mappedName ->
                billInfoModel.accountNameFrom = mappedName
                Server.logD("来源账户映射: '$originalFromAccount' -> '$mappedName'")
            }
        }

        // 处理目标账户映射
        if (shouldMapToAccount(billInfoModel)) {
            mapAccount(billInfoModel.accountNameTo, billInfoModel)?.let { mappedName ->
                billInfoModel.accountNameTo = mappedName
                Server.logD("目标账户映射: '$originalToAccount' -> '$mappedName'")
            }
        }

        Server.logD("资产映射完成: ${billInfoModel.type} | ${billInfoModel.accountNameFrom} -> ${billInfoModel.accountNameTo}")
    }

    /**
     * 检查资产管理器是否启用
     */
    private suspend fun isAssetManagerEnabled(): Boolean {
        return try {
            Db.get().settingDao().query(Setting.SETTING_ASSET_MANAGER)?.value == "true"
        } catch (e: Exception) {
            Server.log("检查资产管理器设置失败: ${e.message}")
            Server.log(e)
            false // 默认禁用，确保系统稳定
        }
    }

    /**
     * 判断是否应该映射来源账户
     * 跳过收入借贷和收入还款类型
     */
    private fun shouldMapFromAccount(billInfoModel: BillInfoModel): Boolean {
        return billInfoModel.accountNameFrom.isNotEmpty() &&
                !listOf(
                    BillType.IncomeLending,
                    BillType.IncomeRepayment
                ).contains(billInfoModel.type)
    }

    /**
     * 判断是否应该映射目标账户
     * 跳过支出借贷和支出还款类型
     */
    private fun shouldMapToAccount(billInfoModel: BillInfoModel): Boolean {
        return billInfoModel.accountNameTo.isNotEmpty() &&
                !listOf(
                    BillType.ExpendLending,
                    BillType.ExpendRepayment
                ).contains(billInfoModel.type)
    }

    /**
     * 映射单个账户
     *
     * 使用责任链模式按优先级顺序处理：
     * 1. 直接资产查找 - 在资产表中查找完全匹配
     * 2. 自定义映射查找 - 使用用户定义的映射规则
     * 3. 正则表达式匹配 - 使用模式匹配进行灵活映射
     * 4. 创建空映射 - 为未知账户创建占位符
     *
     * @param accountName 原始账户名称
     * @param billInfoModel 账单模型，用于判断是否AI生成
     * @return 映射后的账户名称，null表示无法映射
     */
    private suspend fun mapAccount(accountName: String, billInfoModel: BillInfoModel): String? {
        if (accountName.isBlank()) {
            return null
        }

        // 1. 直接资产查找 - 最高优先级
        findDirectAsset(accountName)?.let { return it }

        // 2. 自定义映射查找
        val existingMap = findCustomMapping(accountName)
        if (existingMap?.mapName?.isNotEmpty() == true) {
            return existingMap.mapName
        }

        // 3. 正则表达式匹配
        findRegexMapping(accountName)?.let { return it }

        // 3.5 基于算法的智能匹配（在空映射创建之前尝试）
        findByAlgorithm(accountName)?.let { return it }

        // 4. 创建空映射（仅当非AI生成且不存在映射时）
        if (!billInfoModel.generateByAi() && existingMap == null) {
            createEmptyMapping(accountName)
        }

        return null
    }

    /**
     * 查找直接资产匹配
     */
    private suspend fun findDirectAsset(accountName: String): String? {
        return try {
            Db.get().assetsDao().query(accountName)?.name
        } catch (e: Exception) {
            Server.log("查找直接资产失败 '$accountName': ${e.message}")
            Server.log(e)
            null
        }
    }

    /**
     * 查找自定义映射
     */
    private suspend fun findCustomMapping(accountName: String): AssetsMapModel? {
        return try {
            Db.get().assetsMapDao().query(accountName)
        } catch (e: Exception) {
            Server.log("查找自定义映射失败 '$accountName': ${e.message}")
            Server.log(e)
            null
        }
    }

    /**
     * 查找正则表达式映射
     */
    private suspend fun findRegexMapping(accountName: String): String? {
        return try {
            val regexMappings = getRegexMappings()
            regexMappings.firstOrNull { mapping ->
                accountName.contains(mapping.name)
            }?.mapName
        } catch (e: Exception) {
            Server.log("查找正则映射失败 '$accountName': ${e.message}")
            Server.log(e)
            null
        }
    }

    /**
     * 基于算法的资产匹配
     *
     * 逻辑：
     * 1. 若原始账户名中包含数字（如卡号），优先用数字在资产名中匹配
     * 2. 否则使用“最长连续相似子串”作为相似度指标选取最佳匹配
     *
     * 返回匹配到的资产名称；若无匹配则返回null
     */
    private suspend fun findByAlgorithm(accountName: String): String? {
        val assets = getAssetsList()
        if (assets.isEmpty()) return null

        // 1) 数字优先匹配（如卡号）
        val number = Regex("\\d+").find(accountName)?.value ?: ""
        if (number.isNotEmpty()) {
            assets.firstOrNull { it.name.contains(number.trim()) }?.let { asset ->
                Server.logD("算法映射-卡号命中: ${asset.name}")
                return asset.name
            }
        }

        // 2) 文本相似度匹配（最长连续相似子串）
        val cleanInput = accountName.cleanText(number)
        var bestName: String? = null
        var bestSimilarity = 0
        var bestDiff = Int.MAX_VALUE

        for (asset in assets) {
            val cleanAssetName = asset.name.cleanText()
            val similarity = calculateConsecutiveSimilarity(cleanAssetName, cleanInput)
            if (similarity > 0) {
                val diff = cleanAssetName.length - similarity
                if (similarity > bestSimilarity || (similarity == bestSimilarity && diff < bestDiff)) {
                    // 过滤过短的中文前缀导致的误命中（对齐端侧实现）
                    if (!(similarity == 2 && cleanInput.startsWith("中国"))) {
                        bestName = asset.name
                        bestSimilarity = similarity
                        bestDiff = diff
                    }
                }
            }
        }

        bestName?.let {
            Server.logD("算法映射-文本命中: '$accountName' -> '$it' 相似度=$bestSimilarity 差异=$bestDiff")
            return it
        }
        return null
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

    /**
     * 创建空映射
     * 为未知账户创建占位符映射，方便后续用户手动配置
     */
    private suspend fun createEmptyMapping(accountName: String) {
        try {
            val emptyMapping = AssetsMapModel().apply {
                name = accountName
                mapName = ""
            }
            Db.get().assetsMapDao().insert(emptyMapping)
            Server.logD("为账户 '$accountName' 创建了空映射")
        } catch (e: Exception) {
            // 可能因为并发插入相同记录导致失败，这是正常情况
            Server.logD("创建空映射失败 '$accountName': ${e.message}")
        }
    }
}