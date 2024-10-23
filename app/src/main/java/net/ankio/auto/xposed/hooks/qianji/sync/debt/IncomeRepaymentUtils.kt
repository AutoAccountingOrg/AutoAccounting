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

package net.ankio.auto.xposed.hooks.qianji.sync.debt

import android.content.Context
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.xposed.core.api.HookerManifest
import net.ankio.auto.xposed.hooks.qianji.sync.BookUtils
import net.ankio.auto.xposed.hooks.qianji.tools.AssetAccount
import org.ezbook.server.db.model.BillInfoModel

/**
 * 收款
 */
class IncomeRepaymentUtils(private val manifest: HookerManifest, private val classLoader: ClassLoader, private val context: Context) :
    BaseDebt(manifest, classLoader, context) {
    override suspend fun sync(billModel: BillInfoModel) = withContext(Dispatchers.IO) {
        // 谁付钱
        val accountFrom = getAccountFrom(billModel)
        // 资产
        val accountTo = getAccountTo(billModel)

        val book = BookUtils(manifest, classLoader, context).getBookByName(billModel.bookName)

        manifest.logD("收款: ${billModel.money} ${billModel.accountNameFrom} -> ${billModel.accountNameTo}")

        //拆分账单

        val (bill1,bill2) = splitBill(billModel,accountFrom)

        if (bill2!= null){
            val bill = updateBill(bill2,11,book,accountFrom,accountTo)
            saveBill(bill)
        }

        // 更新loan
        updateLoan(bill1!!, accountFrom)
        // 更新资产
        updateAsset(accountFrom,accountTo,bill1)

        if (bill1.money > 0) {
            // 构建账单
            val bill = updateBill(bill1,4,book,accountFrom,accountTo)
            saveBill(bill)
        }



       pushBill()
    }

    private suspend fun splitBill(billModel: BillInfoModel, accountFrom: AssetAccount): List<BillInfoModel?> = withContext(Dispatchers.IO) {
        val assetMoney = accountFrom.getMoney()
        if (assetMoney < billModel.money) {
            val interest = billModel.money - assetMoney
            val bill1 = billModel.copy().apply {
                money = assetMoney
            }
            val bill2 = billModel.copy().apply {
                money = interest
                remark = "债务利息"
            }
            return@withContext listOf(bill1,bill2)
        }
        return@withContext listOf(billModel,null)
    }

    /**
     * 获取借入账户
     */
    private suspend fun getAccountTo(billModel: BillInfoModel): AssetAccount = withContext(Dispatchers.IO) {
        return@withContext assetsUtils.getAssetByNameWrap(billModel.accountNameTo)?:throw RuntimeException("收款账户不存在 key=accountname;value=${billModel.accountNameTo}")
    }

    /**
     * 获取借款账户
     */
    private suspend fun getAccountFrom(billModel: BillInfoModel): AssetAccount = withContext(Dispatchers.IO) {
        return@withContext assetsUtils.getAssetByNameWrap(billModel.accountNameFrom)?:throw RuntimeException("欠款人不存在 key=accountname;value=${billModel.accountNameFrom}")
    }


    /**
     * 更新债务
     */
    private suspend fun updateLoan(billModel: BillInfoModel, accountTo: AssetAccount) = withContext(Dispatchers.IO){
        // 债务
        val loan = accountTo.getLoanInfo()

        // {"a":0,"b":"2024-07-17","c":"","e":-12.0,"f":0.0}
        // f=TotalPay 已还金额
        // e=money 待还金额
        //
        loan.setTotalpay( billModel.money)

        accountTo.setLoanInfo(loan)
        accountTo.addMoney(  billModel.money)
    }
    /**
     * 保存账单
     */
    private suspend fun updateAsset(
        accountFrom: AssetAccount,
        accountTo: AssetAccount,
        billModel: BillInfoModel,
    ) = withContext(Dispatchers.IO) {

        accountTo.addMoney(billModel.money)

        assetsUtils.updateAsset(accountTo.get)
        assetsUtils.updateAsset(accountFrom.get)
    }

    /**
     * 更新账单
     */
    private suspend fun updateBill(
        billModel: BillInfoModel,
        type:Int,
        book:Any,
        accountFrom: AssetAccount,
        accountTo: AssetAccount
    ) = withContext(Dispatchers.IO) {
        val money = billModel.money

        val remark = billModel.remark

        val time = billModel.time / 1000

        val imageList = ArrayList<String>()

        //    bill2 = Bill.newInstance(i12, trim, d12, timeInMillis, imageUrls);
        val bill = XposedHelpers.callStaticMethod(billClazz, "newInstance", type, remark, money, time, imageList)


        // _id=null;billid=1726484778608150253;userid=200104405e109647c18e9;bookid=-1;timeInSec=1726484773;type=11;remark=债务利息;money=12.0;status=2egoryId=0;platform=0;assetId=-1;fromId=1726484010133;targetId=-1;extra=null

        // _id=null;billid=1726485028243184123;userid=200104405e109647c18e9;bookid=-1;timeInSec=1726485025;type=4;remark=;money=19.0;status=2;categoryId=0;platform=0;assetId=1726484010133;fromId=-1;targetId=-1;extra=null


        val fromLongId = XposedHelpers.callMethod(accountFrom.getId(),"longValue")
        val toLongId = XposedHelpers.callMethod(accountTo.getId(),"longValue")

        if (billModel.remark.contains("利息")){
            XposedHelpers.setObjectField(bill,"fromid", fromLongId)
        }else{
            XposedHelpers.setObjectField(bill,"assetid", fromLongId)
        }



        val bookId = XposedHelpers.getObjectField(book,"bookId")
        XposedHelpers.setObjectField(bill,"bookId", XposedHelpers.callMethod(bookId,"longValue"))
        XposedHelpers.setObjectField(bill,"descinfo", "${accountFrom.getName()}->${accountTo.getName()}")

        bill

    }
}