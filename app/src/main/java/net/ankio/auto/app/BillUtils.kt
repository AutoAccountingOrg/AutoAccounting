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

import net.ankio.auto.App
import net.ankio.auto.database.Db
import net.ankio.auto.database.table.BillInfo
import net.ankio.auto.utils.ActiveUtils
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.SpUtils
import java.lang.Exception

object BillUtils {
    /**
     * 去掉金额中的符号
     */
    fun removeSpecialCharacters(amount: String): String {
        // 使用正则表达式替换逗号和其他特殊字符为空字符串
        val regex = Regex("[^\\d.]")
        return regex.replace(amount, "")
    }

    /**
     * 对重复账单进行分组更新
     */
    private suspend fun updateBillInfo(parentBillInfo:BillInfo, billInfo: BillInfo) {
        if(!parentBillInfo.shopName.contains(billInfo.shopName)){
            parentBillInfo.shopName = billInfo.shopName
        }
        if(!parentBillInfo.shopItem.contains(billInfo.shopItem)){
            parentBillInfo.shopItem += " / " + billInfo.shopItem
        }

        parentBillInfo.cateName = billInfo.cateName

        if(!parentBillInfo.accountNameFrom.contains(billInfo.accountNameFrom)){
            parentBillInfo.accountNameFrom = billInfo.accountNameFrom
        }
        if(!parentBillInfo.accountNameTo.contains(billInfo.accountNameTo)){
            parentBillInfo.accountNameTo = billInfo.accountNameTo
        }
        Db.get().BillInfoDao().update(parentBillInfo)
    }

    /**
     * 重复账单的要素：
     * 1.金额一致
     * 2.来源平台不同  //这个逻辑不对，可能同一个平台可以获取到多个信息，例如多次转账同一金额给同一个人
     * 3.账单时间不超过15分钟
     * 4.账单的类型一致
     * 5.账单的交易账户部分一致（有的交易无法获取完整的账户信息）
     */

    suspend fun groupBillInfo(billInfo: BillInfo) {
        //因为是新账单，所以groupId = 0
        //3分钟之内 重复金额、交易类型的可能是重复订单
        val minutesAgo = billInfo.timeStamp - (3 * 60 * 1000)

        val duplicateIds = Db.get().BillInfoDao().findDistinctNonZeroGroupIds(billInfo.money, billInfo.type, minutesAgo)
        //这边是这个时间段所有重复的GroupId
        var groupId = 0
        if(duplicateIds.isNotEmpty()){
            //循环所有id
            for (id in duplicateIds){
                val duplicateBills = Db.get().BillInfoDao().findDuplicateBills(billInfo.money, billInfo.type, minutesAgo,id)
                //获取到所有重复账单，若来源渠道一致，认为不是重复的
                for (duplicateBill in duplicateBills) {
                    //来源渠道一致，来源于同一个App
                    if(duplicateBill.channel===billInfo.channel && duplicateBill.type===billInfo.type){
                        groupId = duplicateBill.groupId
                        break
                    }
                }
                if(groupId!=0)break
            }
            if(groupId!=0){
                val parentBill = Db.get().BillInfoDao().findParentBill(groupId)
                if (parentBill != null) {
                    // 更新父账单的逻辑，例如更新来源和时间戳
                    updateBillInfo(parentBill, billInfo)
                }
                billInfo.groupId  = groupId
                Db.get().BillInfoDao().insert(billInfo)
                return
            }
        }
        billInfo.groupId  = 0
        val id = Db.get().BillInfoDao().insert(billInfo)

        billInfo.groupId  = id.toInt()
        Db.get().BillInfoDao().insert(billInfo)
    }

     fun getAccountMap(account:String?): String {
        if(account===null)return ""
        val all = ActiveUtils.getAccountMap()
        for (map in all){
            if(map.regex){
                try{
                    val pattern = Regex(map.name!!) // 匹配三个字母的单词
                    if(pattern.matches(account)){
                        return map.mapName?:account
                    }
                }catch (e:Exception){
                    continue
                }
            }else{
                if(map.name===account){
                    return map.mapName?:account
                }
            }
        }
        return account
    }

    /**
     * 生成备注信息
     */
    fun getRemark(billInfo: BillInfo): String {
        return SpUtils.getString("remarkTpl","[商户名称] - [商品名称]")
            .replace("[商户名称]",billInfo.shopName)
            .replace("[币种类型]",billInfo.currency.name(AppUtils.getApplication()))
            .replace("[金额]",billInfo.money.toString())
            .replace("[分类]",billInfo.cateName)
            .replace("[账本]",billInfo.bookName)
            .replace("[来源]",billInfo.from)
    }

    /**
     * 获取分类的显示样式，或者记录方式
     * 例如：父类 - 子类
     * 父类 或 子类
     */
    fun getCategory(category1:String,category2: String?=null): String {
        if(category2===null){
            return category1
        }
        if(SpUtils.getBoolean("setting_category_show_parent",true)){
            return "${category1}-${category2}"
        }
        return "$category2"
    }
}