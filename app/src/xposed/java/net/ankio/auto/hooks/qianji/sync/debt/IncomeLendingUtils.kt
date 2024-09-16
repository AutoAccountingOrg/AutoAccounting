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

        manifest.logD("借款: ${billModel.money} ${billModel.accountNameFrom} -> ${billModel.accountNameTo}, isNewAssets=$isNewAssets")

        // 更新loan
        updateLoan(billModel,accountFrom,isNewAssets)
        // 更新资产
        accountFrom = updateAsset(accountFrom,accountTo,book,billModel,isNewAssets)

        if (!isNewAssets){
            // 构建账单
            val bill = updateBill(billModel,6,book,accountFrom,accountTo)
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
     * 是否为新账单
     */
    private suspend fun isNewAssets(account: AssetAccount): Boolean = withContext(Dispatchers.IO) {
        return@withContext account.getId() == -1L
    }
    /**
     * 更新债务
     */
    private suspend fun updateLoan(billModel: BillInfoModel,accountFrom: AssetAccount,isNewAssets:Boolean) = withContext(Dispatchers.IO){
        // 债务
        val loan = if (isNewAssets) createLoan(billModel.time) else accountFrom.getLoanInfo()

        // {"a":0,"b":"2024-07-17","c":"","e":-12.0,"f":0.0}
        // f=TotalPay 已还金额
        // e=money 待还金额
        val totalMoney = loan.getTotalMoney()
        //
        loan.setTotalMoney(totalMoney - billModel.money)

        manifest.logD("LoanInfo: ${loan.get}")

        accountFrom.setLoanInfo(loan)

        val money = accountFrom.getMoney()

        accountFrom.addMoney(money - billModel.money)
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

        val assetLongId = XposedHelpers.callMethod(accountFrom.getId(),"longValue")
        val targetLongId = XposedHelpers.callMethod(accountTo.getId(),"longValue")

        XposedHelpers.setObjectField(bill,"assetid", assetLongId)
        XposedHelpers.setObjectField(bill,"targetid", targetLongId)

        val bookId = XposedHelpers.getObjectField(book,"bookId")
        XposedHelpers.setObjectField(bill,"bookId", XposedHelpers.callMethod(bookId,"longValue"))

        bill

    }
}