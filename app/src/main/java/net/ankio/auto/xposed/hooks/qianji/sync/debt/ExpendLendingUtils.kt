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
 *//*


package net.ankio.auto.xposed.hooks.qianji.sync.debt

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.xposed.core.utils.AppRuntime
import net.ankio.auto.xposed.hooks.qianji.impl.AssetPreviewPresenterImpl
import net.ankio.auto.xposed.hooks.qianji.impl.BookManagerImpl
import net.ankio.auto.xposed.hooks.qianji.models.QjAssetAccountModel
import net.ankio.auto.xposed.hooks.qianji.models.QjBillModel
import net.ankio.auto.xposed.hooks.qianji.models.QjBookModel
import org.ezbook.server.db.model.BillInfoModel

*/
/**
 * 借出
 *//*

class ExpendLendingUtils :
    BaseDebt() {
    override suspend fun sync(billModel: BillInfoModel) = withContext(Dispatchers.IO) {
        // 借出账户
        val accountFrom = getAccountFrom(billModel)
        // 借给谁
        var accountTo = getAccountTo(billModel)
        // 是否是新创建的资产
        val isNewAssets = isNewAssets(accountTo)
        val book = BookManagerImpl.getBookByName(billModel.bookName)

        AppRuntime.manifest.logD("借出: ${billModel.money} ${billModel.accountNameFrom} -> ${billModel.accountNameTo}, isNewAssets=$isNewAssets")

        // 更新loan
        updateLoan(billModel, accountTo, isNewAssets)
        // 更新资产
        accountTo = updateAsset(accountFrom, accountTo, book, billModel, isNewAssets)

        if (!isNewAssets) {
            // 构建账单
            val bill = updateBill(billModel, 7, book, accountFrom, accountTo)
            saveBill(bill)
        }

        pushBill()
    }

    */
/**
     * 获取借入账户
 *//*

    private suspend fun getAccountTo(billModel: BillInfoModel): QjAssetAccountModel =
        withContext(Dispatchers.IO) {
            return@withContext AssetPreviewPresenterImpl.getOrCreateAssetByName(
                billModel.accountNameTo,
                5,
                52
            )
        }

    */
/**
     * 获取借款账户
 *//*

    private suspend fun getAccountFrom(billModel: BillInfoModel): QjAssetAccountModel =
        withContext(Dispatchers.IO) {
            return@withContext AssetPreviewPresenterImpl.getAssetByName(billModel.accountNameFrom)
                ?: throw RuntimeException("找不到资产 key=accountname;value=${billModel.accountNameFrom}")
        }


    */
/**
     * 更新债务
 *//*

    private suspend fun updateLoan(
        billModel: BillInfoModel,
        accountTo: QjAssetAccountModel,
        isNewAssets: Boolean
    ) = withContext(Dispatchers.IO) {
        // 债务
        val loan = if (isNewAssets) createLoan(billModel.time) else accountTo.getLoanInfo()

        // {"a":0,"b":"2024-07-17","c":"","e":-12.0,"f":0.0}
        // f=TotalPay 已还金额
        // e=money 待还金额
        //
        loan.setTotalMoney(billModel.money)

        accountTo.setLoanInfo(loan)
        accountTo.addMoney(billModel.money)
    }

    */
/**
     * 保存账单
 *//*

    private suspend fun updateAsset(
        accountFrom: QjAssetAccountModel,
        accountTo: QjAssetAccountModel,
        book: QjBookModel,
        billModel: BillInfoModel,
        isNewAssets: Boolean
    ): QjAssetAccountModel = withContext(Dispatchers.IO) {
        val bookId = book.getBookId()
        var ret = accountTo
        if (isNewAssets) {
            ret = submitAsset(
                accountTo, book, mapOf(
                    "bookId" to bookId,
                    "accountId" to accountFrom.getId(),
                    "remark" to billModel.remark,
                )
            )
        }

        accountFrom.addMoney(-billModel.money)

        updateAssets(accountFrom)
        updateAssets(accountTo)

        return@withContext ret
    }

    */
/**
     * 更新账单
 *//*

    private suspend fun updateBill(
        billModel: BillInfoModel,
        type: Int,
        book: QjBookModel,
        accountFrom: QjAssetAccountModel,
        accountTo: QjAssetAccountModel
    ) = withContext(Dispatchers.IO) {
        val money = billModel.money

        val remark = billModel.remark

        val time = billModel.time / 1000

        val imageList = ArrayList<String>()

        //    bill2 = Bill.newInstance(i12, trim, d12, timeInMillis, imageUrls);
        val bill = QjBillModel.newInstance(
            type,
            remark,
            money,
            time,
            imageList
        )


        // (agent) [385693] Arguments com.mutangtech.qianji.data.db.dbhelper.k.saveOrUpdateBill(_id=null;billid=17264840101334980;userid=200104405e109647c18e9;bookid=-1;timeInSec=1726484010;type=7;remark=;money=2.0;status=2;categoryId=0;platform=0;assetId=1726484010133;fromId=1716722805908;targetId=-1;extra=null)
        // (agent) [385693] Arguments com.mutangtech.qianji.data.db.dbhelper.k.saveOrUpdateBill(_id=null;billid=1726483978004140450;userid=200104405e109647c18e9;bookid=-1;timeInSec=1721215710;type=7;remark=从前慢(**江) ;money=962.0;status=2;categoryId=0;platform=0;assetId=1584352987097;fromId=-1;targetId=1726484010133;extra=null)


        QjBillModel.setZhaiwuCurrentAsset(bill, accountTo)
        QjBillModel.setZhaiwuAboutAsset(bill, accountFrom)
        bill.setBook(book)
        bill.setDescinfo("${accountFrom.getName()}->${accountTo.getName()}")

        bill

    }
}*/
