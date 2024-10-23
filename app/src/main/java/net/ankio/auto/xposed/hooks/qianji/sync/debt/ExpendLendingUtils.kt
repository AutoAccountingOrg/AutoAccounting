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
 * 借出
 */
class ExpendLendingUtils(private val manifest: HookerManifest, private val classLoader: ClassLoader, private val context: Context) :
    BaseDebt(manifest, classLoader, context) {
    override suspend fun sync(billModel: BillInfoModel) = withContext(Dispatchers.IO) {
        // 借出账户
        var accountFrom = getAccountFrom(billModel)
        // 借给谁
        var accountTo = getAccountTo(billModel)
        // 是否是新创建的资产
        val isNewAssets = isNewAssets(accountTo)
        val book = BookUtils(manifest, classLoader, context).getBookByName(billModel.bookName)

        manifest.logD("借出: ${billModel.money} ${billModel.accountNameFrom} -> ${billModel.accountNameTo}, isNewAssets=$isNewAssets")

        // 更新loan
        updateLoan(billModel,accountTo,isNewAssets)
        // 更新资产
        accountTo = updateAsset(accountFrom,accountTo,book,billModel,isNewAssets)

        if (!isNewAssets){
            // 构建账单
            val bill = updateBill(billModel,7,book,accountFrom,accountTo)
            saveBill(bill)
        }

       pushBill()
    }

    /**
     * 获取借入账户
     */
    private suspend fun getAccountTo(billModel: BillInfoModel): AssetAccount = withContext(Dispatchers.IO) {
        return@withContext assetsUtils.getOrCreateAssetByNameWrap(billModel.accountNameTo,5,52)
    }

    /**
     * 获取借款账户
     */
    private suspend fun getAccountFrom(billModel: BillInfoModel): AssetAccount = withContext(Dispatchers.IO) {
        return@withContext assetsUtils.getAssetByNameWrap(billModel.accountNameFrom)?:throw RuntimeException("找不到资产 key=accountname;value=${billModel.accountNameFrom}")
    }


    /**
     * 更新债务
     */
    private suspend fun updateLoan(billModel: BillInfoModel, accountTo: AssetAccount, isNewAssets:Boolean) = withContext(Dispatchers.IO){
        // 债务
        val loan = if (isNewAssets) createLoan(billModel.time) else accountTo.getLoanInfo()

        // {"a":0,"b":"2024-07-17","c":"","e":-12.0,"f":0.0}
        // f=TotalPay 已还金额
        // e=money 待还金额
        //
        loan.setTotalMoney( billModel.money)

        accountTo.setLoanInfo(loan)
        accountTo.addMoney(  billModel.money)
    }
    /**
     * 保存账单
     */
    private suspend fun updateAsset(
        accountFrom: AssetAccount,
        accountTo: AssetAccount,
        book:Any,
        billModel: BillInfoModel,
        isNewAssets:Boolean
    ): AssetAccount = withContext(Dispatchers.IO) {
        val bookId = XposedHelpers.getObjectField(book,"bookId")
        var ret = accountTo
        if (isNewAssets){
            ret = submitAsset(accountTo,book, mapOf(
                "bookId" to bookId,
                "accountId" to accountFrom.getId(),
                "remark" to billModel.remark,
            ))
        }

        accountFrom.addMoney(-billModel.money)

        assetsUtils.updateAsset(accountFrom.get)
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
        accountTo: AssetAccount
    ) = withContext(Dispatchers.IO) {
        val money = billModel.money

        val remark = billModel.remark

        val time = billModel.time / 1000

        val imageList = ArrayList<String>()

        //    bill2 = Bill.newInstance(i12, trim, d12, timeInMillis, imageUrls);
        val bill = XposedHelpers.callStaticMethod(billClazz, "newInstance", type, remark, money, time, imageList)

        // (agent) [385693] Arguments com.mutangtech.qianji.data.db.dbhelper.k.saveOrUpdateBill(_id=null;billid=17264840101334980;userid=200104405e109647c18e9;bookid=-1;timeInSec=1726484010;type=7;remark=;money=2.0;status=2;categoryId=0;platform=0;assetId=1726484010133;fromId=1716722805908;targetId=-1;extra=null)
        // (agent) [385693] Arguments com.mutangtech.qianji.data.db.dbhelper.k.saveOrUpdateBill(_id=null;billid=1726483978004140450;userid=200104405e109647c18e9;bookid=-1;timeInSec=1721215710;type=7;remark=从前慢(**江) ;money=962.0;status=2;categoryId=0;platform=0;assetId=1584352987097;fromId=-1;targetId=1726484010133;extra=null)


        val fromLongId = XposedHelpers.callMethod(accountFrom.getId(),"longValue")
        val toLongId = XposedHelpers.callMethod(accountTo.getId(),"longValue")

        XposedHelpers.setObjectField(bill,"assetid", toLongId)
        XposedHelpers.setObjectField(bill,"fromid", fromLongId)

        val bookId = XposedHelpers.getObjectField(book,"bookId")
        XposedHelpers.setObjectField(bill,"bookId", XposedHelpers.callMethod(bookId,"longValue"))
        XposedHelpers.setObjectField(bill,"descinfo", "${accountFrom.getName()}->${accountTo.getName()}")

        bill

    }
}