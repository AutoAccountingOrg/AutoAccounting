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

package net.ankio.auto.xposed.hooks.qianji.debt

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.xposed.core.utils.AppRuntime
import net.ankio.auto.xposed.hooks.qianji.impl.AssetPreviewPresenterImpl
import net.ankio.auto.xposed.hooks.qianji.impl.BookManagerImpl
import net.ankio.auto.xposed.hooks.qianji.models.QjAssetAccountModel
import net.ankio.auto.xposed.hooks.qianji.models.QjBillModel
import net.ankio.auto.xposed.hooks.qianji.models.QjBookModel
import org.ezbook.server.db.model.BillInfoModel

/**
 * 借款
 */
class IncomeLendingUtils :
    BaseDebt() {
    override suspend fun sync(billModel: BillInfoModel) = withContext(Dispatchers.IO) {
        // 借入账户
        val accountTo = getAccountTo(billModel)
        // 向谁借款
        var accountFrom = getAccountFrom(billModel)
        // 是否是新创建的资产
        val isNewAssets = isNewAssets(accountFrom)
        val book = BookManagerImpl.getBookByName(billModel.bookName)

        AppRuntime.manifest.logD("借入: ${billModel.money} ${billModel.accountNameFrom} -> ${billModel.accountNameTo}, isNewAssets=$isNewAssets")

        // 更新loan
        updateLoan(billModel, accountFrom, isNewAssets)
        // 更新资产
        accountFrom = updateAsset(accountFrom, accountTo, book, billModel, isNewAssets)

        if (!isNewAssets) {
            // 构建账单
            val bill = updateBill(billModel, 6, book, accountFrom, accountTo)

            AppRuntime.manifest.logD("bill: $bill")

            saveBill(bill)
        }

        pushBill()
    }

    /**
     * 获取借入账户
     */
    private suspend fun getAccountTo(billModel: BillInfoModel): QjAssetAccountModel =
        withContext(Dispatchers.IO) {
            return@withContext AssetPreviewPresenterImpl.getAssetByName(billModel.accountNameTo)
                ?: throw RuntimeException("找不到资产 key=accountname;value=${billModel.accountNameTo}")
        }

    /**
     * 获取借款账户
     */
    private suspend fun getAccountFrom(billModel: BillInfoModel): QjAssetAccountModel =
        withContext(Dispatchers.IO) {
            return@withContext AssetPreviewPresenterImpl.getOrCreateAssetByName(
                billModel.accountNameFrom,
                5,
                51
            )
        }


    /**
     * 更新债务
     */
    private suspend fun updateLoan(
        billModel: BillInfoModel,
        accountFrom: QjAssetAccountModel,
        isNewAssets: Boolean
    ) = withContext(Dispatchers.IO) {
        // 债务
        val loan = if (isNewAssets) createLoan(billModel.time) else accountFrom.getLoanInfo()
        AppRuntime.manifest.logD("LoanInfo: ${loan}")
        // {"a":0,"b":"2024-07-17","c":"","e":-12.0,"f":0.0}
        // f=TotalPay 已还金额
        // e=money 待还金额
        //
        loan.setTotalMoney(-billModel.money)

        accountFrom.setLoanInfo(loan)
        accountFrom.addMoney(-billModel.money)
    }

    /**
     * 保存账单
     */
    private suspend fun updateAsset(
        accountFrom: QjAssetAccountModel,
        accountTo: QjAssetAccountModel,
        book: QjBookModel,
        billModel: BillInfoModel,
        isNewAssets: Boolean
    ): QjAssetAccountModel = withContext(Dispatchers.IO) {
        val bookId = book.getBookId()
        var ret = accountFrom
        if (isNewAssets) {


            ret = submitAsset(
                accountFrom, book, mapOf(
                    "bookId" to bookId,
                    "accountId" to accountTo.getId(),
                    "remark" to billModel.remark,
                )
            )
        }

        accountTo.addMoney(billModel.money)
        updateAssets(accountTo)

        return@withContext ret
    }

    /**
     * 更新账单
     */
    private suspend fun updateBill(
        billModel: BillInfoModel,
        type: Int,
        book: QjBookModel,
        accountFrom: QjAssetAccountModel,
        accountTo: QjAssetAccountModel
    ): QjBillModel = withContext(Dispatchers.IO) {
        val money = billModel.money

        val remark = billModel.remark

        val time = billModel.time / 1000

        val imageList = ArrayList<String>()

        val bill = QjBillModel.newInstance(
            type,
            remark,
            money,
            time,
            imageList
        )
        QjBillModel.setZhaiwuCurrentAsset(bill, accountFrom)
        QjBillModel.setZhaiwuAboutAsset(bill, accountTo)

        bill.setBook(book)
        bill.setDescinfo("${accountFrom.getName()}->${accountTo.getName()}")

        bill

    }
}