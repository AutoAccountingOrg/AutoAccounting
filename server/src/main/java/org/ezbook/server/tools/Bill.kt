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

package org.ezbook.server.tools

import android.content.Context
import org.ezbook.server.Server
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.BillInfoModel

object Bill {
    /**
     * 检查账单是否重复
     * @param bill 账单1
     * @param bill2 账单2
     * @return true 表示是重复账单，false 表示不是重复账单
     */
    private fun checkRepeat(bill: BillInfoModel, bill2: BillInfoModel): Boolean {
        // 前提，上下5分钟，金额相同
        // 首先检查基础条件：类型必须相同
        if (bill.type != bill2.type) {
            return false
        }

        if (bill.accountNameFrom != bill2.accountNameFrom){
            return false
        }
        // 时间完全相同，是同一笔交易
        if (bill.time == bill2.time) {
            return true
        }

        if (bill.ruleName == bill2.ruleName){
            return false
        }


        if (bill.channel != bill2.channel){
            return true
        }


        return false
    }

    /**
     * 合并重复账单,将bill1的信息合并到bill2
     * @param bill 源账单
     * @param bill2 目标账单（将被更新）
     */
    private fun mergeRepeatBill(bill: BillInfoModel, bill2: BillInfoModel, context: Context) {
        bill2.apply {
            // 1. 合并账户信息
            mergeAccountInfo(bill, this)
            
            // 2. 合并商户和商品信息
            mergeShopInfo(bill, this)
            
            // 3. 合并分类信息
            mergeCategoryInfo(bill, this)
            
            // 4. 更新备注
            setRemark(this, context)
        }
    }

    /**
     * 合并账户信息
     */
    private fun mergeAccountInfo(source: BillInfoModel, target: BillInfoModel) {
        // 只在目标账户为空时合并
        if (target.accountNameFrom.isEmpty() && source.accountNameFrom.isNotEmpty()) {
            target.accountNameFrom = source.accountNameFrom
        }
        if (target.accountNameTo.isEmpty() && source.accountNameTo.isNotEmpty()) {
            target.accountNameTo = source.accountNameTo
        }
    }

    /**
     * 合并商户和商品信息
     */
    private fun mergeShopInfo(source: BillInfoModel, target: BillInfoModel) {
        // 合并商户信息
        target.shopName = when {
            target.shopName.isEmpty() -> source.shopName
            source.shopName.isEmpty() -> target.shopName
            target.shopName.contains(source.shopName) -> target.shopName
            else -> "${target.shopName} ${source.shopName}".trim()
        }
        
        // 合并商品信息
        target.shopItem = when {
            target.shopItem.isEmpty() && source.shopItem.isNotEmpty() -> source.shopItem
            target.shopItem.isEmpty() -> target.extendData
            source.shopItem.isEmpty() -> target.shopItem
            target.shopItem.contains(source.shopItem) -> target.shopItem
            else -> "${target.shopItem} ${source.shopItem}".trim()
        }
    }

    /**
     * 合并分类信息
     */
    private fun mergeCategoryInfo(source: BillInfoModel, target: BillInfoModel) {
        // 如果源账单的分类不是"其他"，则使用源账单的分类
        if (source.cateName != "其他") {
            target.cateName = source.cateName
        }
    }

    /**
     * 账单分组，用于检查重复账单
     */
    fun groupBillInfo(
        billInfoModel: BillInfoModel,
        context: Context
    ): BillInfoModel? {
        Server.isRunOnMainThread()
        val settingBillRepeat =
            Db.get().settingDao().query(Setting.AUTO_GROUP)?.value != "false"
        Server.log("settingBillRepeat=$settingBillRepeat")
        if (!settingBillRepeat) return null
        //第一要素，金钱一致，时间在5分钟以内
        val startTime = billInfoModel.time - 5 * 60 * 1000
        val endTime = billInfoModel.time + 5 * 60 * 1000
        val bills = Db.get().billInfoDao().query(billInfoModel.money, startTime, endTime)
        val parentBill = bills.find { billInfoModel.id!=it.id && checkRepeat(billInfoModel, it)  }
        if (parentBill == null)return null

        Server.log("Parent=$parentBill, Bill=$billInfoModel, startTime:$startTime, endTime:${billInfoModel.time}")
        billInfoModel.groupId = parentBill.id
        mergeRepeatBill(billInfoModel, parentBill, context)
        //更新到数据库
        Db.get().billInfoDao().update(parentBill)
        Db.get().billInfoDao().update(billInfoModel)
        Server.log("Convert Parent=$parentBill, Bill=$billInfoModel, startTime:$startTime, endTime:${billInfoModel.time}")
        return parentBill
    }


    /**
     * 获取备注
     * @param billInfoModel 账单信息
     * @param context 上下文
     */
    fun setRemark(billInfoModel: BillInfoModel, context: Context) {
        Server.isRunOnMainThread()
        val settingBillRemark =
            Db.get().settingDao().query(Setting.NOTE_FORMAT)?.value ?: "【商户名称】 - 【商品名称】"
        billInfoModel.remark = settingBillRemark
            .replace("【商户名称】", billInfoModel.shopName)
            .replace("【商品名称】", billInfoModel.shopItem)
            //  .replace("【币种类型】", Currency.valueOf(billInfoModel.currency).name(context))
            .replace("【金额】", billInfoModel.money.toString())
            .replace("【分类】", billInfoModel.cateName)
            .replace("【账本】", billInfoModel.bookName)
            .replace("【来源】", billInfoModel.app)
            .replace("【渠道】", billInfoModel.channel)
    }

    /**
     * 设置默认账本
     */
    fun setBookName(billInfoModel: BillInfoModel) {
        Server.isRunOnMainThread()
        val name = billInfoModel.bookName
        if (name.isEmpty()) {
            billInfoModel.bookName = "默认账本"
        }
        val defaultBookName =
            Db.get().settingDao().query(Setting.DEFAULT_BOOK_NAME)?.value ?: "默认账本"

        if (name == "默认账本") {
            billInfoModel.bookName = defaultBookName
        }
    }
}
