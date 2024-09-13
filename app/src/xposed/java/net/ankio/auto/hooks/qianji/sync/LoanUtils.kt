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
import org.ezbook.server.db.model.BookBillModel
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class LoanUtils(
    private val manifest: HookerManifest,
    private val classLoader: ClassLoader,
    private val context: Context
) {

    /**
     * 账单类
     */
    val billClazz  by lazy {
        XposedHelpers.findClass(
            "com.mutangtech.qianji.data.model.Bill",
            classLoader
        )
    }


    val loanInfoModelClazz  by lazy {
        XposedHelpers.findClass(
            "com.mutangtech.qianji.asset.model.LoanInfo",
            classLoader
        )
    }

    val billHelpersClazz  by lazy {
        // BillDbHelper
        manifest.clazz("BillDbHelper",classLoader)
    }

    val billToolsClazz  by lazy {
        // BillTools
        manifest.clazz("BillTools",classLoader)
    }

    var assetsUtils: AssetsUtils = AssetsUtils(manifest,classLoader)

    /**
     * 处理资产账户金额的增减
     */
    private suspend fun processAccount(account: Any,money:Double) = withContext(Dispatchers.IO) {
        val total = XposedHelpers.getDoubleField(account,"money")
        XposedHelpers.setObjectField(account,"money",total + money)
        assetsUtils.updateAsset(account)
    }

    // {"accountId":0,"startdate":"2024-09-13","enddate":"","money":28.0,"totalpay":0.0}
    /**
     * 处理债务账单
     * @param account 资产账户(钱迹的资产类）
     * @param billModel 自动记账的账单类
     * @param book 钱迹的账本类
     * @return 资产账户（钱迹的资产类）
     */
    private suspend fun processLoan(account: Any,billModel: BillInfoModel,book: Any):Any = withContext(Dispatchers.IO) {
        var loan = XposedHelpers.getObjectField(account,"loanInfo")
        if (loan == null){
            /**
             * 构建默认的债务账单
             */
            loan = XposedHelpers.newInstance(loanInfoModelClazz)

            XposedHelpers.callMethod(loan,"setStartdate",DateUtils.getTime("yyyy-MM-dd",billModel.time))
            XposedHelpers.callMethod(loan,"setEnddate","")
            XposedHelpers.callMethod(loan,"setTotalMoney",0.00) // 总借款金额
            XposedHelpers.callMethod(loan,"setTotalpay",0.00) // 已收金额
        }
        val total = XposedHelpers.callMethod(loan,"getTotalMoney") as Double

        val totalpay = XposedHelpers.callMethod(loan,"getTotalpay") as Double
        val assetsMoney = XposedHelpers.getDoubleField(account,"money")
        // 借出                                               借入
        if (billModel.type == BillType.ExpendLending || billModel.type == BillType.IncomeLending){
            XposedHelpers.callMethod(loan,"setTotalMoney",total + billModel.money)
            XposedHelpers.setObjectField(account,"money",assetsMoney + billModel.money)
            // 收款                                               还款
        } else if (billModel.type == BillType.IncomeRepayment || billModel.type == BillType.ExpendRepayment){
            // 剩余金额
            var remain = assetsMoney - billModel.money
            remain = if (remain < 0) 0.0 else remain
            XposedHelpers.setObjectField(account,"money",remain)
            XposedHelpers.callMethod(loan,"setTotalpay",totalpay + billModel.money)

        }

        XposedHelpers.setObjectField(account,"loanInfo",loan)




        var retAccount  = account




        val createtime = XposedHelpers.getLongField(account,"createtime")

        if(createtime == 0L){
            retAccount = submitAsset(account,book)
        }

        assetsUtils.updateAsset(retAccount)

        retAccount
    }


    val baseSubmitAssetPresenterImpl by lazy {
        XposedHelpers.findClass("com.mutangtech.qianji.asset.submit.mvp.BaseSubmitAssetPresenterImpl",classLoader)
    }

    val requestInterface by lazy {
        manifest.clazz("RequestInterface",classLoader)
    }

    val assetsInterface by lazy {
        manifest.clazz("AssetsInterface",classLoader)
    }

    /**
     * 提交资产
     * @param assets 钱迹的资产类
     * @param book 钱迹的账本类
     * @return 钱迹的资产类
     */
    private suspend fun submitAsset(assets:Any,book:Any):Any = suspendCoroutine { continuation ->
        val presenter = XposedHelpers.newInstance(baseSubmitAssetPresenterImpl)
        // 构建账本数据
        val json = JSONObject()
        val bookId = XposedHelpers.getObjectField(book,"bookId")
        json.put("bookId",bookId)
        //提交数据给钱迹
        XposedHelpers.callMethod(presenter,"submitAsset",book,assets,json,null)
       Hooker.hookOnce(
           requestInterface,
           "onFinish",
           Object::class.java
       ){  // assetsInterface
           val assetsInstance = it.args[0]
           if (assetsInstance.javaClass == assetsInterface){
                // 提交成功
               manifest.log("提交资产成功")
               val assetsItem = XposedHelpers.callMethod(assetsInstance,"getData")
               continuation.resume(assetsItem)

              return@hookOnce true
           }
           false
       }
    }

    /**
     * 构建账单
     * @param billModel 自动记账的账单类
     * @param type 账单类型（钱迹）
     * @param assetId 资产id（钱迹）
     * @param book 钱迹账本
     */
    private suspend fun buildBill(billModel: BillInfoModel, type:Int, assetId:Any,book: Any) : Any = withContext(Dispatchers.IO){
        // Bill.newInstance(
        // int i10,  //账单类型
        // String str, // 备注
        // double d10, // 金额
        // long j10,  // 时间戳(10位）
        // ArrayList<String> arrayList) {
        val money = billModel.money

        val remark = billModel.remark

        val time = billModel.time / 1000

        val imageList = ArrayList<String>()

        //    bill2 = Bill.newInstance(i12, trim, d12, timeInMillis, imageUrls);
        val bill = XposedHelpers.callStaticMethod(billClazz, "newInstance", type, remark, money, time, imageList)

        // 关联资产

        // com.mutangtech.qianji.data.db.dbhelper.k.saveOrUpdateBill(_id=null;billid=1726209168629173349;userid=200104405e109647c18e9;bookid=-1;timeInSec=1726209156;type=7;remark=;money=28.0;status=2;categoryId=0;platform=0;assetId=1726208819255;fromId=1711425821791;targetId=-1;extra=null)
        val assetLongId = XposedHelpers.callMethod(assetId,"longValue")
        if (billModel.type == BillType.ExpendLending || billModel.type == BillType.IncomeLending){
            XposedHelpers.setObjectField(bill,"assetId", assetLongId)
        } else if (billModel.type == BillType.IncomeRepayment || billModel.type == BillType.ExpendRepayment){
            XposedHelpers.setObjectField(bill,"fromId", assetLongId)
        }

        // 关联bookid
        val bookId = XposedHelpers.getObjectField(book,"bookId")
        XposedHelpers.setObjectField(bill,"bookId", XposedHelpers.callMethod(bookId,"longValue"))

        bill
    }

    /**
     * 借出账单
     * @param billModel 自动记账的账单类
     */
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

        accountTo = processLoan(accountTo,billModel,book)


        val id = XposedHelpers.getObjectField(accountTo,"id")

        //添加借出账单
        // com.mutangtech.qianji.data.db.dbhelper.k.saveOrUpdateBill(_id=null;billid=1726209168629173349;userid=200104405e109647c18e9;bookid=-1;timeInSec=1726209156;type=7;remark=;money=28.0;status=2;categoryId=0;platform=0;assetId=1726208819255;fromId=1711425821791;targetId=-1;extra=null)

        // 构建账单
        val bill = buildBill(billModel,7, id,book)


        saveBill(bill)

        pushBill()
    }

    /**
     * 保存账单并推送
     * @param bill 钱迹账单类
     */
    private fun saveBill(bill:Any){
        val billHelpers = XposedHelpers.newInstance(billHelpersClazz)
        XposedHelpers.callMethod(billHelpers,"saveOrUpdateBill",bill)
    }

    private fun pushBill(){
        val companion = XposedHelpers.getStaticObjectField(billToolsClazz,"Companion")
        val billTools = XposedHelpers.callMethod(companion,"getInstance")
        XposedHelpers.callMethod(billTools,"startPush",context)
    }

    /**
     * 收款账单
     */
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
            saveBill(buildBill(interestBill,11, assetId,book))
        }


        accountFrom = processLoan(accountFrom,billModel,book)
        // 划扣账户 com.mutangtech.qianji.data.db.convert.a.insertOrReplace({"color":"E06966","createtime":1726240094,"icon":"null","id":1726240094257,"incount":1,"lastPayTime":0,"loan":{"accountId":0,"startdate":"2024-09-13","enddate":"","money":12.0,"totalpay":12.0},"money":0.0,"name":"ceshi","sort":0,"status":0,"stype":52,"type":5,"usecount":0,"userid":"200104405e109647c18e9"}, (none))

        val accountTo = assetsUtils.getAssetByName(billModel.accountNameTo)?:throw RuntimeException("收款账户不存在 key=accountname;value=${billModel.accountNameTo}")

        processAccount(accountTo,bill2.money)

        saveBill(buildBill(bill2,4, assetId,book))

        pushBill()
    }

    //增加借款账单
    fun addLoanBill(billModel: BookBillModel) {
        // Arguments com.mutangtech.qianji.asset.loanpay.LoanPayAct.a1(_id=null;billid=1725680887791133088;userid=200104405e109647c18e9;bookid=-1;timeInSec=1725680883;type=6;remark=;money=888.0;status=2;categoryId=0;platform=0;assetId=1725641271342;fromId=-1;targetId=-1;extra=null, (none))
    }

    // 借款还款
    fun loanRepayment(billModel: BookBillModel) {
        // Arguments com.mutangtech.qianji.asset.loanpay.LoanPayAct.a1(_id=null;billid=1725680857977193424;userid=200104405e109647c18e9;bookid=-1;timeInSec=1725680855;type=9;remark=;money=133.0;status=2;categoryId=0;platform=0;assetId=1725641271342;fromId=-1;targetId=-1;extra=null, (none))
    }




}