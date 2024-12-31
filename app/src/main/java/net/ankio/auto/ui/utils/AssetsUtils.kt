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

package net.ankio.auto.ui.utils

import net.ankio.auto.storage.ConfigUtils
import net.ankio.auto.storage.Logger
import org.ezbook.server.constant.BillType
import org.ezbook.server.constant.DefaultData
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.model.AssetsMapModel
import org.ezbook.server.db.model.AssetsModel
import org.ezbook.server.db.model.BillInfoModel

object AssetsUtils {
    /**
     * 计算两个字符串的最长连续相似子串
     */
    private fun calculateConsecutiveSimilarity(a: String, b: String): Int {
        val m = a.length
        val n = b.length
        if (m == 0 || n == 0) return 0

        // 使用两个一维数组来节省空间
        var previousRow = IntArray(n + 1)
        var currentRow = IntArray(n + 1)
        var maxLength = 0

        for (i in 1..m) {
            for (j in 1..n) {
                if (a[i - 1] == b[j - 1]) {
                    currentRow[j] = previousRow[j - 1] + 1
                    maxLength = maxOf(maxLength, currentRow[j])
                } else {
                    currentRow[j] = 0
                }
            }
            // 交换 currentRow 和 previousRow
            val temp = previousRow
            previousRow = currentRow
            currentRow = temp
        }

        return maxLength
    }

    /**
     * 通过算法获取资产
     */
    fun getAssetsByAlgorithm(
        list: List<AssetsModel>,
        raw: String,
    ): String {
        // 1. 首先尝试通过卡号匹配
        val number = Regex("\\d+").find(raw)?.value ?: ""
        Logger.d("识别到卡号：$number")
        if (number.isNotEmpty()) {
            list.find { it.name.contains(number.trim()) }?.let { asset ->
                Logger.d("找到对应数据：${asset.name}")
                return asset.name
            }
        }

        // 2. 如果卡号匹配失败，进行文本相似度匹配
        Logger.d("未通过卡号找到数据，开始使用文本匹配查找。")

        // 预处理输入文本
        val cleanInput = raw.cleanText(number)

        // 记录最佳匹配结果
        var bestMatch = BestMatch(raw)

        list.forEach { asset ->
            val cleanAssetName = asset.name.cleanText()
            Logger.d("尝试匹配: $cleanInput => $cleanAssetName")

            val similarity = calculateConsecutiveSimilarity(cleanAssetName, cleanInput)


            if (similarity >= bestMatch.similarity && similarity > 0) {
                val length = cleanAssetName.length
                val diff = length - similarity
                Logger.d("相似度（$cleanInput,${asset.name}）：$similarity，差异：$diff")
                if (diff < bestMatch.diff) {
                    if (similarity == 2 && cleanInput.startsWith("中国")) return@forEach
                    bestMatch = BestMatch(asset.name, similarity, diff)
                }
            }
        }

        return bestMatch.assetName
    }

    /**
     * 用于存储最佳匹配结果的数据类
     */
    private data class BestMatch(
        val assetName: String,
        val similarity: Int = 0,
        val diff: Int = Int.MAX_VALUE
    )

    /**
     * 字符串清理扩展函数
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
     * 获取需要映射的资产
     */
    suspend fun setMapAssets(billInfoModel: BillInfoModel): MutableList<String> {
        //如果不使用资产管理就不进行映射
        val assetManager =
            ConfigUtils.getBoolean(Setting.SETTING_ASSET_MANAGER, DefaultData.SETTING_ASSET_MANAGER)
        val list = AssetsModel.list()
        if (!assetManager || list.isEmpty()) {
            return mutableListOf()
        }

        val assets = mutableListOf<String>()
        if (billInfoModel.accountNameFrom.isNotEmpty()) {
            val mapName = AssetsMapModel.getByName(billInfoModel.accountNameFrom)
            val assetName = AssetsModel.getByName(billInfoModel.accountNameFrom)
            if (assetName == null) {
                if (mapName == null) {
                    if (!listOf(BillType.IncomeLending, BillType.IncomeRepayment).contains(
                            billInfoModel.type
                        )
                    ) {
                        assets.add(billInfoModel.accountNameFrom)
                    }
                } else {
                    billInfoModel.accountNameFrom = mapName.mapName
                }
            }

        }

        if (billInfoModel.accountNameTo.isNotEmpty()) {
            val mapName = AssetsMapModel.getByName(billInfoModel.accountNameTo)
            val assetName = AssetsModel.getByName(billInfoModel.accountNameTo)
            if (assetName == null) {
                if (mapName == null) {
                    if (!listOf(BillType.ExpendLending, BillType.ExpendRepayment).contains(
                            billInfoModel.type
                        )
                    ) {
                        assets.add(billInfoModel.accountNameTo)
                    }
                } else {
                    billInfoModel.accountNameTo = mapName.mapName
                }
            }
        }



        return assets
    }

}