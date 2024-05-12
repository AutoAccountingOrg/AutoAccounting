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
package net.ankio.auto.database.table

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import net.ankio.auto.app.BillUtils
import net.ankio.auto.constant.DataType
import net.ankio.common.constant.BillType
import net.ankio.common.constant.Currency
import net.ankio.common.model.AutoBillModel

@Entity
class BillInfo {
    // 账单列表
    @PrimaryKey(autoGenerate = true)
    var id = 0

    /**
     * 账单类型 只有三种
     */
    var type: BillType = BillType.Expend

    /**
     * 币种类型
     */
    var currency: Currency = Currency.CNY

    /**
     * 金额 大于0
     */
    var money: Int = 0

    /**
     * 手续费
     */
    var fee: Int = 0

    /**
     * 记账时间
     * yyyy-MM-dd HH:mm:ss
     */
    var timeStamp: Long = 0

    /**
     * 商户名称
     */
    var shopName: String = ""

    /**
     * 商品名称
     */
    var shopItem: String = ""

    /**
     * 分类名称
     */
    var cateName: String = "其他"

    /**
     * 拓展数据域，如果是报销或者销账，会对应账单ID
     */
    var extendData: String = ""

    /**
     * 账本名称
     */
    var bookName: String = "默认账本"

    /**
     * 账单所属资产名称（或者转出账户）
     */
    var accountNameFrom: String = ""

    /**
     * 转入账户
     */
    var accountNameTo = ""

    /**
     * 这笔账单的来源
     */
    var from = ""

    /**
     * 来源类型
     */
    var fromType: DataType = DataType.App

    /**
     * 分组id，这个id是指将短时间内捕获到的同等金额进行合并的分组id
     */
    var groupId: Int = 0

    /**
     * 数据渠道，这里指的是更具体的渠道，例如【建设银行】微信公众号，用户【xxxx】这种
     */
    var channel: String = ""

    /**
     * 是否已从App同步
     */
    var syncFromApp: Boolean = false

    /**
     * 备注信息
     */
    var remark: String = ""

    fun toJSON(): String {
        return Gson().toJson(this)
    }

    fun toAutoBillModel(): AutoBillModel {
        return AutoBillModel(
            type = type.ordinal,
            currency = currency,
            amount = BillUtils.getFloatMoney(money),
            fee = BillUtils.getFloatMoney(fee),
            timeStamp = timeStamp,
            cateName = cateName,
            extendData = extendData,
            bookName = bookName,
            accountNameFrom = accountNameFrom,
            accountNameTo = accountNameTo,
            remark = remark,
        )
    }

    override fun toString(): String {
        return """
            BillInfo(
                id=$id, 
                type=$type, 
                currency=$currency, 
                money=$money, 
                fee=$fee, 
                timeStamp=$timeStamp, 
                shopName='$shopName', 
                shopItem='$shopItem', 
                cateName='$cateName', 
                extendData='$extendData', 
                bookName='$bookName', 
                accountNameFrom='$accountNameFrom', 
                accountNameTo='$accountNameTo', 
                from='$from', 
                fromType=$fromType, 
                groupId=$groupId, 
                channel='$channel', 
                syncFromApp=$syncFromApp, 
                remark='$remark'
            )
            """.trimIndent()
    }

    fun copy(): BillInfo {
        val billInfo = BillInfo()
        billInfo.id = id
        billInfo.type = type
        billInfo.currency = currency
        billInfo.money = money
        billInfo.fee = fee
        billInfo.timeStamp = timeStamp
        billInfo.shopName = shopName
        billInfo.shopItem = shopItem
        billInfo.cateName = cateName
        billInfo.extendData = extendData
        billInfo.bookName = bookName
        billInfo.accountNameFrom = accountNameFrom
        billInfo.accountNameTo = accountNameTo
        billInfo.from = from
        billInfo.fromType = fromType
        billInfo.groupId = groupId
        billInfo.channel = channel
        billInfo.syncFromApp = syncFromApp
        billInfo.remark = remark
        return billInfo
    }

    companion object {
        fun fromJSON(json: String): BillInfo {
            return Gson().fromJson(json, BillInfo::class.java)
        }
    }
}
