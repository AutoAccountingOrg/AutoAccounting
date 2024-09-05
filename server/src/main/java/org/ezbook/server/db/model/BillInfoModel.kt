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

package org.ezbook.server.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ezbook.server.Server
import org.ezbook.server.constant.BillType

@Entity
class BillInfoModel {
    @PrimaryKey(autoGenerate = true)
    var id = 0L

    /**
     * 账单类型 只有三种
     */
    var type: BillType = BillType.Income

    /**
     * 币种类型
     */
    var currency: String = ""

    /**
     * 金额 大于0
     */
    var money: Double = 0.00

    /**
     * 手续费
     */
    var fee: Double = 0.00

    /**
     * 记账时间
     * yyyy-MM-dd HH:mm:ss
     */
    var time: Long = 0

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
     * 这笔账单的来源,例如是微信还是支付宝
     */
    var app = ""
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

    /**
     * 是否为自动记录的账单
     */
    var auto:Boolean = false

    /**
     * 规则名称
     */
    var ruleName:String = ""

    fun copy(): BillInfoModel {
        val billInfoModel = BillInfoModel()
        billInfoModel.id = id
        billInfoModel.type = type
        billInfoModel.currency = currency
        billInfoModel.money = money
        billInfoModel.fee = fee
        billInfoModel.time = time
        billInfoModel.shopName = shopName
        billInfoModel.shopItem = shopItem
        billInfoModel.cateName = cateName
        billInfoModel.extendData = extendData
        billInfoModel.bookName = bookName
        billInfoModel.accountNameFrom = accountNameFrom
        billInfoModel.accountNameTo = accountNameTo
        billInfoModel.app = app
        billInfoModel.groupId = groupId
        billInfoModel.channel = channel
        billInfoModel.syncFromApp = syncFromApp
        billInfoModel.remark = remark
        billInfoModel.auto = auto
        return billInfoModel
    }



    companion object {
        suspend  fun put(billInfoModel: BillInfoModel) = withContext(Dispatchers.IO) {
            Server.request("bill/put", Gson().toJson(billInfoModel))
        }

        suspend fun remove(id: Long)  = withContext(Dispatchers.IO) {
            Server.request("bill/remove?id=$id")
        }


        suspend fun list(page: Int, pageSize: Int) : List<BillInfoModel> = withContext(Dispatchers.IO) {
            val response = Server.request("bill/list?page=$page&limit=$pageSize")
            val json = Gson().fromJson(response, JsonObject::class.java)

            runCatching { Gson().fromJson(json.getAsJsonArray("data"), Array<BillInfoModel>::class.java).toList() }.getOrNull() ?: emptyList()
        }



    }

    override fun toString(): String {
        return "BillInfoModel(id=$id, type=$type, currency='$currency', money=$money, fee=$fee, time=$time, shopName='$shopName', shopItem='$shopItem', cateName='$cateName', extendData='$extendData', bookName='$bookName', accountNameFrom='$accountNameFrom', accountNameTo='$accountNameTo', app='$app', groupId=$groupId, channel='$channel', syncFromApp=$syncFromApp, remark='$remark', auto=$auto, ruleName='$ruleName')"
    }
}