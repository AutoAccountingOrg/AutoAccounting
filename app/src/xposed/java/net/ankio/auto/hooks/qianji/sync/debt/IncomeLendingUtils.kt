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

package net.ankio.auto.hooks.qianji.sync.debt

import android.content.Context
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.core.api.HookerManifest
import net.ankio.auto.hooks.qianji.sync.BookUtils
import net.ankio.auto.hooks.qianji.tools.AssetAccount
import org.ezbook.server.db.model.BillInfoModel

/**
 * 借款
 */
class IncomeLendingUtils(private val manifest: HookerManifest,private val classLoader: ClassLoader,private val context: Context) :
    BaseDebt(manifest, classLoader, context) {
    override suspend fun sync(billModel: BillInfoModel) = withContext(Dispatchers.IO) {
        // 借入账户
        val accountTo = getAccountTo(billModel)
        // 向谁借款
        var accountFrom = getAccountFrom(billModel)
        // 是否是新创建的资产
        val isNewAssets = isNewAssets(accountFrom)
        val book = BookUtils(manifest, classLoader, context).getBookByName(billModel.bookName)

        manifest.logD("借入: ${billModel.money} ${billModel.accountNameFrom} -> ${billModel.accountNameTo}, isNewAssets=$isNewAssets")

        // 更新loan
        updateLoan(billModel,accountFrom,isNewAssets)
        // 更新资产
        accountFrom = updateAsset(accountFrom,accountTo,book,billModel,isNewAssets)

        if (!isNewAssets){
            // 构建账单
            val bill = updateBill(billModel,6,book,accountFrom,accountTo)

            manifest.logD("bill: $bill")

            saveBill(bill)
        }

       pushBill()
    }

    /**
     * 获取借入账户
     */
    private suspend fun getAccountTo(billModel: BillInfoModel): AssetAccount = withContext(Dispatchers.IO) {
        return@withContext assetsUtils.getAssetByNameWrap(billModel.accountNameTo)?:throw RuntimeException("找不到资产 key=accountname;value=${billModel.accountNameTo}")
    }

    /**
     * 获取借款账户
     */
    private suspend fun getAccountFrom(billModel: BillInfoModel): AssetAccount = withContext(Dispatchers.IO) {
        return@withContext assetsUtils.getOrCreateAssetByNameWrap(billModel.accountNameFrom,5,51)
    }


    /**
     * 更新债务
     */
    private suspend fun updateLoan(billModel: BillInfoModel,accountFrom: AssetAccount,isNewAssets:Boolean) = withContext(Dispatchers.IO){
        // 债务
        val loan = if (isNewAssets) createLoan(billModel.time) else accountFrom.getLoanInfo()
        manifest.logD("LoanInfo: ${loan.get}")
        // {"a":0,"b":"2024-07-17","c":"","e":-12.0,"f":0.0}
        // f=TotalPay 已还金额
        // e=money 待还金额
        //
        loan.setTotalMoney(- billModel.money)

        accountFrom.setLoanInfo(loan)
        accountFrom.addMoney( - billModel.money)
    }
    /**
     * 保存账单
     */
    private suspend fun updateAsset(
        accountFrom: AssetAccount,
        accountTo:AssetAccount,
        book:Any,
        billModel: BillInfoModel,
        isNewAssets:Boolean
    ):AssetAccount = withContext(Dispatchers.IO) {
        val bookId = XposedHelpers.getObjectField(book,"bookId")
        var ret = accountFrom
        if (isNewAssets){


            ret = submitAsset(accountFrom,book, mapOf(
                "bookId" to bookId,
                "accountId" to accountTo.getId(),
                "remark" to billModel.remark,
            ))
        }

        accountTo.addMoney(billModel.money)
        assetsUtils.updateAsset(accountTo.get)

        return@withContext ret
    }

    /**
     * 更新账单
     */
    private suspend fun updateBill(
        billModel: BillInfoModel,
        type:Int,
        book:Any,
        accountFrom: AssetAccount,
        accountTo:AssetAccount
    ) = withContext(Dispatchers.IO) {
        val money = billModel.money

        val remark = billModel.remark

        val time = billModel.time / 1000

        val imageList = ArrayList<String>()

        //    bill2 = Bill.newInstance(i12, trim, d12, timeInMillis, imageUrls);
        val bill = XposedHelpers.callStaticMethod(billClazz, "newInstance", type, remark, money, time, imageList)

        val fromLongId = XposedHelpers.callMethod(accountFrom.getId(),"longValue")
        val toLongId = XposedHelpers.callMethod(accountTo.getId(),"longValue")

        //  Arguments com.mutangtech.qianji.data.db.dbhelper.k.saveOrUpdateBill(_id=null;billid=1726484445302133106;userid=200104405e109647c18e9;bookid=-1;timeInSec=1726484437;type=6;remark=;money=12.0;status=2;categoryId=0;platform=0;assetId=1726474557467;fromId=-1;targetId=1584352475380;extra=null)
        XposedHelpers.setObjectField(bill,"assetid", fromLongId)
        XposedHelpers.setObjectField(bill,"targetid", toLongId)

        val bookId = XposedHelpers.getObjectField(book,"bookId")
        XposedHelpers.setObjectField(bill,"bookId", XposedHelpers.callMethod(bookId,"longValue"))


        XposedHelpers.setObjectField(bill,"descinfo", "${accountFrom.getName()}->${accountTo.getName()}")

        bill

    }
}