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

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.database.Db
import net.ankio.auto.database.table.AssetsMap
import net.ankio.auto.database.table.BillInfo
import net.ankio.auto.events.AutoServiceErrorEvent
import net.ankio.auto.exceptions.AutoServiceException
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.Logger
import net.ankio.auto.utils.SpUtils
import net.ankio.auto.utils.event.EventBus
import net.ankio.common.constant.BillType
import net.ankio.common.model.AutoBillModel
import java.text.DecimalFormat

object BillUtils {

    /**
     * 对重复账单进行分组更新
     */
    private suspend fun updateBillInfo(parentBillInfo: BillInfo, billInfo: BillInfo) {
        if (!parentBillInfo.shopName.contains(billInfo.shopName)) {
            parentBillInfo.shopName = billInfo.shopName
        }
        if (!parentBillInfo.shopItem.contains(billInfo.shopItem)) {
            parentBillInfo.shopItem += " / " + billInfo.shopItem
        }

        parentBillInfo.remark = getRemark(
            parentBillInfo,
            SpUtils.getString("setting_bill_remark", "【商户名称】 - 【商品名称】")
        )

        parentBillInfo.cateName = billInfo.cateName

        if (!parentBillInfo.accountNameFrom.contains(billInfo.accountNameFrom)) {
            parentBillInfo.accountNameFrom = billInfo.accountNameFrom
        }
        if (!parentBillInfo.accountNameTo.contains(billInfo.accountNameTo)) {
            parentBillInfo.accountNameTo = billInfo.accountNameTo
        }
        parentBillInfo.syncFromApp = false
        Db.get().BillInfoDao().update(parentBillInfo)
    }

    /**
     * 将自动记账的账单与待同步区域的账单进行合并
     */
    private suspend fun syncBillInfo() = withContext(Dispatchers.IO) {
        val bills = Db.get().BillInfoDao().getAllParents()
        Db.get().BillInfoDao().setAllParents()
       val it = AppUtils.getService().get("auto_bills")
        val list = runCatching {
            Gson().fromJson(it, Array<AutoBillModel>::class.java).toMutableList()
        }.getOrElse {
            Logger.e("解析自动记账出错", it)
            mutableListOf()
        }
        // 添加或更新list中的元素
        bills.forEach { bill ->
            val index = list.indexOfFirst { it.id == bill.id }
            if (index != -1) {
                list[index] = bill.toAutoBillModel()
            } else {
                list.add(bill.toAutoBillModel())
            }
        }

        val json = Gson().toJson(list)
        AppUtils.getService().set("auto_bills", json)
        //如果需要同步的数据过多，则尝试自动跳转
        if (list.size > 20) {
            AppUtils.startBookApp()
        }
    }

     fun noNeedFilter(billInfo: BillInfo): Boolean {
         return  !SpUtils.getBoolean("setting_bill_repeat", true) ||
                  (
                   billInfo.type != BillType.Income &&
                   billInfo.type != BillType.Expend
                  )
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

    suspend fun groupBillInfo(billInfo: BillInfo) {
        if (noNeedFilter(billInfo) ) {
            Db.get().BillInfoDao().insert(billInfo)
            syncBillInfo()
            return
        }
        //因为是新账单，所以groupId = 0
        //3分钟之内 重复金额、交易类型的可能是重复订单
        val minutesAgo = billInfo.timeStamp - (3 * 60 * 1000)
        //只遍历金额和类型重复的，时间在10分钟之内
        val duplicateIds = Db.get().BillInfoDao()
            .findDistinctNonZeroGroupIds(billInfo.money, billInfo.type, minutesAgo)
        //这边是这个时间段所有重复的GroupId
        var groupId = 0

        Logger.i("重复GroupId：$duplicateIds")

        if (duplicateIds.isNotEmpty()) {
            //循环所有id
            for (id in duplicateIds) {
                val duplicateBills = Db.get().BillInfoDao()
                    .findDuplicateBills(billInfo.money, billInfo.type, minutesAgo, id)
                //这里的重复账单有两种：1. 本身重复 2.
                //获取到所有重复账单，若来源渠道一致，认为不是重复的
                for (duplicateBill in duplicateBills) {

                    //无论如何重复，如果发生时间（毫秒）完全一致，可以认定是重复的
                    if (duplicateBill.timeStamp == billInfo.timeStamp) {
                        groupId = duplicateBill.groupId
                        break
                    }
                    val t = billInfo.timeStamp - duplicateBill.timeStamp

                    // 不同应用在5分钟内发出的账单
                    if ((duplicateBill.fromType != billInfo.fromType || duplicateBill.from != billInfo.from) && t > 0 && t < 1000 * 60 * 2) {
                        groupId = duplicateBill.groupId
                        break
                    }

                    // 金额一致，2分钟内由同一个App不同渠道发出的账单
                    if (duplicateBill.from == billInfo.from && duplicateBill.channel != billInfo.channel && t > 0 && t < 1000 * 60 * 1) {
                        groupId = duplicateBill.groupId
                        break
                    }

                }
                if (groupId != 0) break
            }
            if (groupId != 0) {
                val parentBill = Db.get().BillInfoDao().findParentBill(groupId)
                if (parentBill != null) {
                    // 更新父账单的逻辑，例如更新来源和时间戳
                    updateBillInfo(parentBill, billInfo)
                }
                billInfo.groupId = groupId
                Db.get().BillInfoDao().insert(billInfo)
                syncBillInfo()
                return
            }
        }
        billInfo.groupId = 0
        val id = Db.get().BillInfoDao().insert(billInfo)

        billInfo.groupId = id.toInt()
        Db.get().BillInfoDao().insert(billInfo)
        syncBillInfo()
    }

    /**
     * 获取资产映射
     */
    suspend fun setAccountMap(billInfo: BillInfo) = withContext(Dispatchers.IO) {
        fun getMapName(list: List<AssetsMap>, name: String): String {
            for (map in list) {
                if (map.regex) {
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
                    if (map.name === name) {
                        return map.mapName
                    }
                }
            }
            return name
        }

        AppUtils.getService().get("assets_map").let {
            runCatching {
                val list = Gson().fromJson(it, Array<AssetsMap>::class.java).toList()
                billInfo.accountNameFrom = getMapName(list, billInfo.accountNameFrom)
                billInfo.accountNameTo = getMapName(list, billInfo.accountNameTo)
            }.onFailure {
                Logger.e("获取资产映射出错", it)
            }
        }
    }

    /**
     * 生成备注信息
     * setting_bill_remark
     */
    fun getRemark(
        billInfo: BillInfo,
        settingBillRemark: String = "【商户名称】 - 【商品名称】"
    ): String {
        val tpl = settingBillRemark.ifEmpty { "【商户名称】 - 【商品名称】" }
        return tpl
            .replace("【商户名称】", billInfo.shopName)
            .replace("【商品名称】", billInfo.shopItem)
            .replace("【币种类型】", billInfo.currency.name(AppUtils.getApplication()))
            .replace("【金额】", billInfo.money.toString())
            .replace("【分类】", billInfo.cateName)
            .replace("【账本】", billInfo.bookName)
            .replace("【来源】", billInfo.from)
    }

    /**
     * 获取分类的显示样式，或者记录方式
     * 例如：父类 - 子类
     * 父类 或 子类
     */
    fun getCategory(
        category1: String,
        category2: String? = null,
        settingCategoryShowParent: Boolean = false
    ): String {
        if (category2 === null) {
            return category1
        }
        if (settingCategoryShowParent) {
            return "${category1}-${category2}"
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
    /**
     * 同步自定义规则到远程目录
     */
    suspend fun syncRules() = withContext(Dispatchers.IO){
        Logger.i("同步自定义规则到远程目录")
        val rule = StringBuilder()
        Db.get().RegularDao().loadAll()?.forEach {
            if (it != null) {
                rule.append(it.js).append("\n")
            }
        }
        AppUtils.getService().set("auto_category_custom", rule.toString())
    }
}