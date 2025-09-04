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

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.storage.Logger
//import net.ankio.auto.ui.dialog.BillAssetsMapDialog
import org.ezbook.server.constant.BillType
import org.ezbook.server.constant.DefaultData
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.AssetsMapModel
import org.ezbook.server.db.model.AssetsModel
import org.ezbook.server.db.model.BillInfoModel
import net.ankio.auto.http.api.AssetsAPI
import net.ankio.auto.http.api.AssetsMapAPI
import org.ezbook.server.db.model.CategoryModel
import java.io.IOException
import java.io.InputStreamReader

object AssetsUtils {
    suspend fun setMapAssets(
        context: Context,
        float: Boolean,
        billInfoModel: BillInfoModel,
        callback: () -> Unit
    ) =
        withContext(Dispatchers.Main) {
            val ignoreAssets = PrefManager.ignoreAsset
            if (ignoreAssets) {
                callback()
                return@withContext
            }
            val empty = AssetsMapAPI.empty()
            if (empty.isEmpty()) {
                callback()
                return@withContext
            }
            val assetItems = AssetsAPI.list()
            /*  BillAssetsMapDialog(context, float, empty.toMutableList(), assetItems) { items ->
                  runCatching {

                      items.first { billInfoModel.accountNameFrom.isNotEmpty() && billInfoModel.accountNameFrom == it.name }
                          .let {
                              billInfoModel.accountNameFrom = it.mapName
                          }
                  }
                  runCatching {
                      items.first { billInfoModel.accountNameTo.isNotEmpty() && billInfoModel.accountNameTo == it.name }
                          .let {
                              billInfoModel.accountNameTo = it.mapName
                          }
                  }
                  callback()
              }.show(cancel = true)
          */
        }
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
            Logger.d("尝试匹配：$cleanInput => $cleanAssetName")

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

    private suspend fun handleEmptyMapping(accountName: String): String? {
        val list = AssetsMapAPI.list(1, 10000)
        Logger.d("处理空映射：账户名=$accountName，匹配列表=$list")
        return list.firstOrNull { it.regex && accountName.contains(it.name) }?.mapName
    }
    /**
     * 获取需要映射的资产
     */
    suspend fun setMapAssets(billInfoModel: BillInfoModel): MutableList<String> {
        //如果不使用资产管理就不进行映射
        val assetManager = PrefManager.featureAssetManage
        val list = AssetsAPI.list()
        if (!assetManager || list.isEmpty()) {
            return mutableListOf()
        }

        val assets = mutableListOf<String>()
        if (billInfoModel.accountNameFrom.isNotEmpty()) {
            val mapName = AssetsMapAPI.getByName(billInfoModel.accountNameFrom)
            val assetName = AssetsAPI.getByName(billInfoModel.accountNameFrom)

            Logger.d("资产映射：账单=$billInfoModel，映射名=$mapName，资产名=$assetName")
            if (assetName == null) {
                if (mapName == null) {
                    if (!listOf(BillType.IncomeLending, BillType.IncomeRepayment).contains(
                            billInfoModel.type
                        )
                    ) {
                        val mapNameStr = handleEmptyMapping(billInfoModel.accountNameFrom)
                        if (mapNameStr != null) {
                            billInfoModel.accountNameFrom = mapNameStr
                        } else {
                            assets.add(billInfoModel.accountNameFrom)
                        }
                    }
                } else {
                    billInfoModel.accountNameFrom = mapName.mapName
                }
            }

        }

        if (billInfoModel.accountNameTo.isNotEmpty()) {
            val mapName = AssetsMapAPI.getByName(billInfoModel.accountNameTo)
            val assetName = AssetsAPI.getByName(billInfoModel.accountNameTo)
            Logger.d("资产映射：账单=$billInfoModel，映射名=$mapName，资产名=$assetName")
            if (assetName == null) {
                if (mapName == null) {
                    if (!listOf(BillType.ExpendLending, BillType.ExpendRepayment).contains(
                            billInfoModel.type
                        )
                    ) {
                        val mapNameStr = handleEmptyMapping(billInfoModel.accountNameTo)
                        if (mapNameStr != null) {
                            billInfoModel.accountNameTo = mapNameStr
                        } else {
                            assets.add(billInfoModel.accountNameTo)
                        }
                    }
                } else {
                    billInfoModel.accountNameTo = mapName.mapName
                }
            }
        }



        return assets
    }


    /**
     * 从 raw/category.json 文件中读取分类数据并解析为 CategoryItem 列表
     * @param context 上下文对象
     * @return 分类项目列表
     */
    fun list(context: Context): List<AssetsModel> {
        return try {
            // 打开 raw 资源文件
            val inputStream = context.resources.openRawResource(R.raw.assets)
            val reader = InputStreamReader(inputStream)

            // 使用 gson 解析 JSON 数据
            val gson = Gson()
            val listType = object : TypeToken<List<AssetsModel>>() {}.type
            val assets: List<AssetsModel> = gson.fromJson(reader, listType)

            // 关闭资源
            reader.close()
            inputStream.close()

            assets

        } catch (e: Exception) {
            Logger.e(e.message ?: "", e)
            // 处理其他异常，返回空列表
            emptyList()
        }
    }
}