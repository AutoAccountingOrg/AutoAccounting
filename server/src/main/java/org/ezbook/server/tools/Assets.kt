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
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.AssetsMapModel
import org.ezbook.server.db.model.AssetsModel
import org.ezbook.server.db.model.BillInfoModel

object Assets {
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
    private fun getAssetsByAlgorithm(
        list: List<AssetsModel>,
        raw: String,
    ): String {
        // 提取raw出数字部分
        val regex = Regex("\\d+")
        val number = regex.find(raw)?.value ?: ""
        Server.log("识别到卡号：$number")
        if (number.isNotEmpty()) {
            val find = list.find { it.name.contains(number.trim()) }
            // 包含卡号的一定是对应的数据
            if (find != null) {
                Server.log("找到对应数据：${find.name}")
                return find.name
            }
        }

        Server.log("未通过卡号找到数据，开始使用文本匹配查找。")
        // 去掉所有的括号
        val noNumber = raw.replace(number, "")
        val cleanData = Regex("\\([^(（【】）)]*\\)").replace(noNumber, "").trim()
        var nowSimilarity = 0
        var nowAssets = raw
        list.forEach {
            val wait =
                listOf(
                    "卡",
                    "银行",
                )
            var newName = Regex("\\([^(（【】）)]*\\)").replace(it.name, "").trim()
            var newCleanData = cleanData
            wait.forEach { item ->
                newName = newName.replace(item, "")
                newCleanData = newCleanData.replace(item, "")
            }
            Server.log("尝试匹配:$newCleanData => $newName ")
            calculateConsecutiveSimilarity(newName, newCleanData).let { similarity ->
                Server.log("相似度（$cleanData,${it.name}）：$similarity")
                if (similarity >= nowSimilarity) {
                    val useless =
                        listOf(
                            "储蓄",
                            "借记",
                            "信用",
                        )

                    var newName2 = newName
                    var newCleanData2 = newCleanData

                    useless.forEach { item ->
                        newName2 = newName.replace(item, "")
                        newCleanData2 = newCleanData.replace(item, "")
                    }

                    calculateConsecutiveSimilarity(newName2, newCleanData2).let { similarity2 ->
                        Server.log("二次验证相似度（$newName2,$newCleanData2）：$similarity2")
                        if (similarity2 >= 1) {
                            nowSimilarity = similarity
                            nowAssets = it.name
                        }
                    }
                }
            }
        }
        return nowAssets
    }

    /**
     * 获取资产映射
     * 必须运行在IO线程，不允许在主线程运行
     */
    fun setAssetsMap(billInfoModel: BillInfoModel) {
        //检查是否在主线程
        Server.isRunOnMainThread()
        val maps = Db.get().assetsMapDao().load(9000, 0)
        val rawAccountNameFrom = billInfoModel.accountNameFrom
        val rawAccountNameTo = billInfoModel.accountNameTo
        val assets = Db.get().assetsDao().load(9000, 0)
        //直接命中映射表
        if (assets.find { it.name == rawAccountNameFrom } != null && assets.find { it.name == rawAccountNameTo } != null) {
            return
        }
        val autoAsset =
            Db.get().settingDao().query(Setting.AUTO_IDENTIFY_ASSET)?.value?.toBoolean() ?: false
        billInfoModel.accountNameFrom = processAssets(rawAccountNameFrom, maps, assets, autoAsset)
        billInfoModel.accountNameTo = processAssets(rawAccountNameTo, maps, assets, autoAsset)
    }

    /**
     * 处理资产
     */
    private fun processAssets(
        account: String,
        maps: List<AssetsMapModel>,
        assets: List<AssetsModel>,
        autoAsset: Boolean
    ): String {
        if (account.isEmpty()) {
            return account
        }
        //直接命中原始资产表
        val asset = assets.find { it.name == account }
        if (asset != null) {
            return asset.name
        }
        //命中映射表
        val map = maps.find { it.name == account || it.regex && account.contains(it.name) }
        if (map != null) {
            return map.mapName
        }
        //命中算法
        if (autoAsset) {
            val autoAssetName = getAssetsByAlgorithm(assets, account)
            if (autoAssetName != account && autoAssetName.isNotEmpty()) {
                //将映射结果保存到映射表
                Db.get().assetsMapDao().insert(AssetsMapModel().apply {
                    name = account
                    mapName = autoAssetName
                    regex = false
                })
            }
            return autoAssetName
        } else {
            //将映射结果保存到映射表
            if (account.isNotEmpty()){
                Db.get().assetsMapDao().insert(AssetsMapModel().apply {
                    name = account
                    mapName = account
                    regex = false
                })
            }
        }
        return account
    }
}