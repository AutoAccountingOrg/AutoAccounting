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

package net.ankio.auto.hooks.qianji.sync

import android.content.Context
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.core.api.HookerManifest
import net.ankio.auto.core.xposed.Hooker
import net.ankio.auto.utils.DateUtils
import org.ezbook.server.constant.BillType
import org.ezbook.server.db.model.BillInfoModel
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class LoanUtils(
    private val manifest: HookerManifest,
    private val classLoader: ClassLoader,
    private val context: Context
) {


  /*
    *//**
     * 借出账单
     * @param billModel 自动记账的账单类
     *//*
    suspend fun doExpendLending(billModel: BillInfoModel) = withContext(Dispatchers.IO) {

        val book = BookUtils(manifest, classLoader, context).getBookByName(billModel.bookName)

        // 划扣账户 com.mutangtech.qianji.data.db.convert.a.insertOrReplace({"color":"529AF8","createtime":1711425821,"currency":"CNY","extra":{"ftime":-1,"initmoney":21.0},"icon":"http://res3.qianjiapp.com/assetv2/asset_icon_cash.png","id":1711425821791,"incount":1,"lastPayTime":0,"money":45.56,"name":"现金","sort":0,"status":0,"stype":11,"type":1,"usecount":1,"userid":"200104405e109647c18e9"}, (none))

        val accountFrom = assetsUtils.getAssetByName(billModel.accountNameFrom)?:throw RuntimeException("找不到资产 key=accountname;value=${billModel.accountNameFrom}")
        // 扣钱
        processAccount(accountFrom,-billModel.money)

        // 借出人
        // com.mutangtech.qianji.data.db.convert.a.insertOrReplace({"color":"E06966","createtime":1726208819,"currency":"CNY","icon":"null","id":1726208819255,"incount":1,"lastPayTime":0,"loan":{"accountId":0,"startdate":"2024-09-13","enddate":"","money":28.0,"totalpay":0.0},"money":28.0,"name":"123","sort":0,"status":0,"stype":52,"type":5,"usecount":0,"userid":"200104405e109647c18e9"}, (none))

        var accountTo = assetsUtils.getOrCreateAssetByName(billModel.accountNameTo,5,52)!!

        // {"accountId":0,"startdate":"2024-09-13","enddate":"","money":28.0,"totalpay":0.0}

        accountTo = processLoan(accountTo,billModel,book,0)


        val id = XposedHelpers.getObjectField(accountTo,"id")

        //添加借出账单
        // com.mutangtech.qianji.data.db.dbhelper.k.saveOrUpdateBill(_id=null;billid=1726209168629173349;userid=200104405e109647c18e9;bookid=-1;timeInSec=1726209156;type=7;remark=;money=28.0;status=2;categoryId=0;platform=0;assetId=1726208819255;fromId=1711425821791;targetId=-1;extra=null)

        // 构建账单
        val bill = buildBill(billModel,7, id,0L,book)


        saveBill(bill)

        pushBill()
    }


    *//**
     * 收款账单
     *//*
    suspend fun doIncomeRepayment(billModel: BillInfoModel) = withContext(Dispatchers.IO) {

        val book = BookUtils(manifest, classLoader, context).getBookByName(billModel.bookName)


        var accountFrom = assetsUtils.getAssetByName(billModel.accountNameFrom)?:throw RuntimeException("欠债人不存在 key=accountname;value=${billModel.accountNameFrom}")

        var bill2 = billModel
        //账单拆分，超过需要还款的部分记作利息

        val assetMoney = XposedHelpers.getDoubleField(accountFrom,"money")

        val assetId = XposedHelpers.getObjectField(accountFrom,"id")

        if (assetMoney < billModel.money){ //还入的钱大于需要还的钱，记作利息
            val interest = billModel.money - assetMoney
            // 实际需要记录的账单
            bill2 = billModel.copy().apply {
                money = assetMoney
            }
            // 利息账单
            val interestBill = billModel.copy().apply {
                money = interest
                remark = "债务利息"
            }

            // _id=null;billid=1726240048328133877;userid=200104405e109647c18e9;bookid=-1;timeInSec=1726240037;type=11;remark=债务利息;mon08.0;status=2;categoryId=0;platform=0;assetId=-1;fromId=1726240094257;targetId=-1;extra=null
            saveBill(buildBill(interestBill,11, assetId,0L,book))
        }


        accountFrom = processLoan(accountFrom,billModel,book,0)
        // 划扣账户 com.mutangtech.qianji.data.db.convert.a.insertOrReplace({"color":"E06966","createtime":1726240094,"icon":"null","id":1726240094257,"incount":1,"lastPayTime":0,"loan":{"accountId":0,"startdate":"2024-09-13","enddate":"","money":12.0,"totalpay":12.0},"money":0.0,"name":"ceshi","sort":0,"status":0,"stype":52,"type":5,"usecount":0,"userid":"200104405e109647c18e9"}, (none))

        val accountTo = assetsUtils.getAssetByName(billModel.accountNameTo)?:throw RuntimeException("收款账户不存在 key=accountname;value=${billModel.accountNameTo}")

        processAccount(accountTo,bill2.money)

        saveBill(buildBill(bill2,4, assetId,0L,book))

        pushBill()
    }

    *//**
     * 还款账单
     *//*
    suspend fun doExpendRepayment(billModel: BillInfoModel) = withContext(Dispatchers.IO) {

        val book = BookUtils(manifest, classLoader, context).getBookByName(billModel.bookName)


        var accountFrom = assetsUtils.getAssetByName(billModel.accountNameFrom)?:throw RuntimeException("债主不存在 key=accountname;value=${billModel.accountNameFrom}")

        var bill2 = billModel
        //账单拆分，超过需要还款的部分记作利息

        val assetMoney = XposedHelpers.getDoubleField(accountFrom,"money")

        val assetId = XposedHelpers.getObjectField(accountFrom,"id")

        if (assetMoney < billModel.money){ //还入的钱大于需要还的钱，记作利息
            val interest = billModel.money - assetMoney
            // 实际需要记录的账单
            bill2 = billModel.copy().apply {
                money = assetMoney
            }
            // 利息账单
            val interestBill = billModel.copy().apply {
                money = interest
                remark = "债务利息"
            }

            // _id=null;billid=1726240048328133877;userid=200104405e109647c18e9;bookid=-1;timeInSec=1726240037;type=11;remark=债务利息;mon08.0;status=2;categoryId=0;platform=0;assetId=-1;fromId=1726240094257;targetId=-1;extra=null
            saveBill(buildBill(interestBill,10, assetId,0L,book))
        }


        accountFrom = processLoan(accountFrom,billModel,book,0)
        // 划扣账户 com.mutangtech.qianji.data.db.convert.a.insertOrReplace({"color":"E06966","createtime":1726240094,"icon":"null","id":1726240094257,"incount":1,"lastPayTime":0,"loan":{"accountId":0,"startdate":"2024-09-13","enddate":"","money":12.0,"totalpay":12.0},"money":0.0,"name":"ceshi","sort":0,"status":0,"stype":52,"type":5,"usecount":0,"userid":"200104405e109647c18e9"}, (none))

        val accountTo = assetsUtils.getAssetByName(billModel.accountNameTo)?:throw RuntimeException("还款账户不存在 key=accountname;value=${billModel.accountNameTo}")

        processAccount(accountTo,bill2.money)

        saveBill(buildBill(bill2,9, assetId,0L,book))

        pushBill()
    }

*/
}