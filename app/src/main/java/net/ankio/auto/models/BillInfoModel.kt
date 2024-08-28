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
package net.ankio.auto.models

import com.google.gson.Gson

class BillInfoModel {
    // 账单列表
    var id = 0

    /**
     * 账单类型 只有三种
     */
    var type: Int = 0

    /**
     * 币种类型
     */
    var currency: String = ""

    /**
     * 金额 大于0
     */
    var money: Float = 0F

    /**
     * 手续费
     */
    var fee: Float = 0F

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
    var fromApp = ""


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
    var syncFromApp: Int = 0

    /**
     * 备注信息
     */
    var remark: String = ""

    var auto:Int=0

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
        billInfoModel.fromApp = fromApp
        billInfoModel.groupId = groupId
        billInfoModel.channel = channel
        billInfoModel.syncFromApp = syncFromApp
        billInfoModel.remark = remark
        billInfoModel.auto = auto
        return billInfoModel
    }

    fun toJson(): String {
        return  Gson().toJson(this)
    }

    override fun toString(): String {
        return toJson()
    }

    companion object {
        suspend fun put(billInfoModel: BillInfoModel): Int {
            //判断时间戳是否为10位，10位则使用13位的
            if (billInfoModel.time.toString().length == 10) {
                billInfoModel.time *= 1000
            }
           /* if (billInfoModel.id == 0) {
                return (AppUtils.getService().sendMsg("bill/add", billInfoModel) as JsonPrimitive).asInt
            }else {
                AppUtils.getService().sendMsg("bill/update", billInfoModel)
                return billInfoModel.id
            }*/
            return 0
        }

        suspend fun del(id: Int) {
          /*  AppUtils.getService().sendMsg("bill/del", mapOf("id" to id))
     */   }

        suspend fun list(page: Int, size: Int): Array<BillInfoModel>? {
           /* val list = AppUtils.getService().sendMsg("bill/list",
                hashMapOf(
                    "page" to page,
                    "size" to size,

                )
            )
            return runCatching {
                Gson().fromJson(list as JsonArray, Array<BillInfoModel>::class.java)
            }.onFailure {
                Logger.e(it.message ?: "", it)
            }.getOrNull()*/
            return null
        }

        suspend fun clear(){
         /*   AppUtils.getService().sendMsg("bill/clear", mapOf<String, Int>())
       */ }

    }
}
