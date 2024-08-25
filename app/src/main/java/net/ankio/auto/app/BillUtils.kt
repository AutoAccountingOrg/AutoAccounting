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

package net.ankio.auto.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.Logger
import net.ankio.auto.storage.SpUtils
import net.ankio.auto.models.AssetsModel
import net.ankio.auto.models.AssetsMapModel
import net.ankio.auto.models.BillInfoModel
import net.ankio.common.constant.BillType
import net.ankio.common.constant.Currency
import java.text.DecimalFormat

object BillUtils {
    /**
     * 对重复账单进行分组更新
     */
    suspend fun updateBillInfo(
        parentBillInfoModel: BillInfoModel,
        billInfoModel: BillInfoModel,
    ) {
        if (!parentBillInfoModel.shopName.contains(billInfoModel.shopName)) {
            parentBillInfoModel.shopName = billInfoModel.shopName
        }
        if (!parentBillInfoModel.shopItem.contains(billInfoModel.shopItem)) {
            parentBillInfoModel.shopItem += " / " + billInfoModel.shopItem
        }

        parentBillInfoModel.remark =
            getRemark(
                parentBillInfoModel,
                SpUtils.getString("setting_bill_remark", "【商户名称】 - 【商品名称】"),
            )

        parentBillInfoModel.cateName = billInfoModel.cateName

        if (!parentBillInfoModel.accountNameFrom.contains(billInfoModel.accountNameFrom)) {
            parentBillInfoModel.accountNameFrom = billInfoModel.accountNameFrom
        }
        if (!parentBillInfoModel.accountNameTo.contains(billInfoModel.accountNameTo)) {
            parentBillInfoModel.accountNameTo = billInfoModel.accountNameTo
        }
        parentBillInfoModel.syncFromApp = 0
        BillInfoModel.put(parentBillInfoModel)
    }

    fun noNeedFilter(billInfoModel: BillInfoModel): Boolean {
        return !SpUtils.getBoolean("setting_bill_repeat", true) ||
            (
                billInfoModel.type != BillType.Income.value &&
                    billInfoModel.type != BillType.Expend.value
            )
    }

    // 检查是否为重复账单
    fun checkRepeatBill(
        bill: BillInfoModel,
        bills: List<BillInfoModel>,
    ): BillInfoModel? {
        val list = bills.filter { bill.money == it.money && bill.type == it.type }

        if (list.isEmpty())return null

        // 查找金额一致的

        val channel = list.filter { bill.channel != it.channel }

        if (channel.isNotEmpty())
            {
                return null
            }
        return channel.firstOrNull()
    }

    /**
     * 重复账单的要素：
     * 1.金额一致
     * 2.来源平台不同  //这个逻辑不对，可能同一个平台可以获取到多个信息，例如多次转账同一金额给同一个人
     * 来源渠道不同（可以是同一个App，但是可以是不同的公众号
     * 3.账单时间不超过15分钟
     * 4.账单的类型一致，只有收入或者支出需要进行区分
     * 5.账单的交易账户部分一致（有的交易无法获取完整的账户信息）
     */

    suspend fun groupBillInfo(
        billInfoModel: BillInfoModel,
        child: ArrayList<BillInfoModel>?,
    ) {
        if (noNeedFilter(billInfoModel)) {
            BillInfoModel.put(billInfoModel)
            return
        }

        val billId = BillInfoModel.put(billInfoModel)

        child?.forEach {
            it.groupId = billId
            BillInfoModel.put(it)
        }
    }

    private fun getMapName(
        list: List<AssetsMapModel>,
        name: String,
    ): String {
        for (map in list) {
            if (map.regex == 1) {
                try {
                    val pattern = Regex(map.name)
                    if (pattern.matches(name)) {
                        return map.mapName
                    }
                } catch (e: Exception) {
                    Logger.e("正则匹配出错", e)
                    continue
                }
            } else {
                if (map.name == name) {
                    return map.mapName
                }
            }
        }
        return name
    }

// TODO 相似度计算算法优化
    fun calculateConsecutiveSimilarity(
        a: String,
        b: String,
    ): Int {
        val m = a.length
        val n = b.length
        val dp = Array(m + 1) { IntArray(n + 1) }
        var maxLength = 0
        var endIndex = 0

        for (i in 1..m) {
            for (j in 1..n) {
                if (a[i - 1] == b[j - 1]) {
                    dp[i][j] = dp[i - 1][j - 1] + 1
                    if (dp[i][j] > maxLength) {
                        maxLength = dp[i][j]
                        endIndex = i - 1
                    }
                } else {
                    dp[i][j] = 0
                }
            }
        }

        return maxLength
    }

    private fun getAiAssets(
        list: List<AssetsModel>,
        raw: String,
    ): String {
        // 提取raw出数字部分
        val regex = Regex("\\d+")
        val number = regex.find(raw)?.value ?: ""
        Logger.d("识别到卡号：$number")
        if (number.isNotEmpty()) {
            val find = list.find { it.name.contains(number.trim()) }
            if (find != null) {
                Logger.d("找到对应数据：${ find.name}")
                return find.name
            }
        }
        Logger.d("未通过卡号找到数据，开始使用文本匹配查找。")
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
            Logger.d("尝试匹配:$newCleanData => $newName ")
            calculateConsecutiveSimilarity(newName, newCleanData).let { similarity ->
                Logger.d("相似度（$cleanData,${it.name}）：$similarity")
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
                        Logger.d("二次验证相似度（$newName2,$newCleanData2）：$similarity2")
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
     */
    suspend fun setAccountMap(billInfoModel: BillInfoModel) =
        withContext(Dispatchers.IO) {
           /* val list = AssetsMapModel.get()
            val rawAccountNameFrom = billInfoModel.accountNameFrom
            val rawAccountNameTo = billInfoModel.accountNameTo
            billInfoModel.accountNameFrom = getMapName(list, rawAccountNameFrom)
            billInfoModel.accountNameTo = getMapName(list, rawAccountNameTo)

            if (SpUtils.getBoolean("setting_auto_ai_asset", false)) {
                val assets = AssetsModel.get(500, AssetsType.NORMAL)
                if (rawAccountNameTo != "" &&
                    rawAccountNameTo == billInfoModel.accountNameTo &&
                    assets.find { it.name == rawAccountNameTo } == null
                ) {
                    billInfoModel.accountNameTo = getAiAssets(assets, rawAccountNameTo)
                }
                if (rawAccountNameFrom != "" &&
                    rawAccountNameFrom == billInfoModel.accountNameFrom &&
                    assets.find { it.name == rawAccountNameFrom } == null
                ) {
                    billInfoModel.accountNameFrom = getAiAssets(assets, rawAccountNameFrom)
                }
            }*/
        }

    /**
     * 生成备注信息
     * setting_bill_remark
     */
    fun getRemark(
        billInfoModel: BillInfoModel,
        settingBillRemark: String = "【商户名称】 - 【商品名称】",
    ): String {
        val tpl = settingBillRemark.ifEmpty { "【商户名称】 - 【商品名称】" }
        return tpl
            .replace("【商户名称】", billInfoModel.shopName)
            .replace("【商品名称】", billInfoModel.shopItem)
            .replace("【币种类型】", Currency.valueOf(billInfoModel.currency).name(AppUtils.getApplication()))
            .replace("【金额】", billInfoModel.money.toString())
            .replace("【分类】", billInfoModel.cateName)
            .replace("【账本】", billInfoModel.bookName)
            .replace("【来源】", billInfoModel.fromApp)
    }

    /**
     * 获取分类的显示样式，或者记录方式
     * 例如：父类 - 子类
     * 父类 或 子类
     */
    fun getCategory(
        category1: String,
        category2: String? = null,
        settingCategoryShowParent: Boolean = false,
    ): String {
        if (category2 === null) {
            return category1
        }
        if (settingCategoryShowParent) {
            return "$category1-$category2"
        }
        return "$category2"
    }

    /**
     * 获取页面显示颜色
     */
    fun getColor(type: Int): Int {
        val payColor = SpUtils.getInt("setting_pay_color_red", 0)

        return when (type) {
            0 -> if (payColor == 0) R.color.danger else R.color.success
            1 -> if (payColor == 1) R.color.danger else R.color.success
            2 -> R.color.info
            else -> R.color.danger
        }
    }

    /**
     * 将 100 转换为 1.00
     */
    fun getFloatMoney(money: Int): Float {
        val df = DecimalFormat("#.00")
        val amount = df.format(money / 100.0f)
        return amount.toFloat()
    }

    /**
     * 将 1.0023323232 转换为 100
     */
    fun getMoney(money: Float): Int {
        return (money * 100.0f).toInt()
    }
}
