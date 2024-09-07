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

import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.core.api.HookerManifest
import org.ezbook.server.db.model.BookBillModel
import java.lang.reflect.Proxy
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class LoanUtils(
    private val manifest: HookerManifest,
    private val classLoader: ClassLoader
) {

    val loanHomeImpl by lazy {
        XposedHelpers.findClass(
            "com.mutangtech.qianji.asset.detail.loanwrapper.LoanHomePresenterImpl",
            classLoader
        )
    }


    private suspend fun getLoanList(debt: Boolean = false): List<*> =
        suspendCoroutine { continuation ->
            var resumed = false
            val constructor = loanHomeImpl.constructors.first()!!
            //  public LoanHomePresenterImpl(z7.b bVar) {
            val param1Clazz = constructor.parameterTypes.first()!!
            val param1Object = Proxy.newProxyInstance(
                classLoader,
                arrayOf(param1Clazz)
            ) { proxy, method, args ->
                when (method.name) {
                    "onGetLoanList" -> {

                        val billList = args[0]
                        val boolean = args[1] as Boolean

                        manifest.log("LoanUtils.getLoanList: $billList , $boolean")

                        if (!resumed) {
                            resumed = true
                            continuation.resume(billList as List<*>)
                        } else {

                        }

                    }

                    "isDebtAsset" -> {
                        return@newProxyInstance debt
                    }

                    else -> {

                    }
                }
            }


            XposedHelpers.callMethod(
                XposedHelpers.newInstance(loanHomeImpl, param1Object),
                "loadAssetList",
                debt,
                0
            )

        }

    suspend fun getDebtList(): List<*> = withContext(Dispatchers.Main) {
        getLoanList(true)
    }

    suspend fun getLoanList(): List<*> = withContext(Dispatchers.Main) {
        getLoanList(false)
    }


    //构建账单：
    /**
     *  Bill bill;
     *         z6.p d10;
     *         int i10;
     *         if (this.V == null) {
     *             d10 = z6.p.d();
     *             i10 = R.string.error_book_belongs_not_set;
     *         } else {
     *             String trim = this.P.getEditText().getText().toString().trim();
     *             Calendar calendar = this.f7682a0;
     *             long timeInMillis = calendar != null ? calendar.getTimeInMillis() / 1000 : z6.b.g();
     *             p pVar = this.f7689h0;
     *             if (pVar == null || pVar.imagePrepared()) {
     *                 p pVar2 = this.f7689h0;
     *                 Bill bill2 = null;
     *                 ArrayList<String> imageUrls = pVar2 != null ? pVar2.getImageUrls() : null;
     *                 boolean K0 = K0();
     *                 int i11 = R.string.error_empty_jiechu_value;
     *                 if (!K0) {
     *                     boolean J0 = J0();
     *                     if (I0()) {
     *                         if (this.f7683b0 <= 0.0d) {
     *                             z6.p d11 = z6.p.d();
     *                             if (J0) {
     *                                 i11 = R.string.error_empty_jieru_value;
     *                             }
     *                             d11.i(this, i11);
     *                             return;
     *                         }
     *                     } else if (this.f7683b0 <= 0.0d && this.f7684c0 <= 0.0d) {
     *                         z6.p.d().i(this, J0 ? R.string.error_empty_pay_debt_value : R.string.error_empty_pay_loan_value);
     *                         return;
     *                     }
     *                     int i12 = J0 ? 9 : 4;
     *                     if (I0()) {
     *                         i12 = J0 ? 6 : 7;
     *                     }
     *                     double d12 = this.f7683b0;
     *                     if (d12 > 0.0d) {
     *                         bill2 = Bill.newInstance(i12, trim, d12, timeInMillis, imageUrls);
     *                         Bill.setZhaiwuCurrentAsset(bill2, this.W);
     *                         Bill.setZhaiwuAboutAsset(bill2, this.Z);
     *                         r9.c.processZhaiwuCurrentAssetData(bill2.getType(), this.f7683b0, this.W, 0.0d);
     *                         r9.c.processZhaiwuBillDescInfo(bill2, this.W, this.Z);
     *                     }
     *                     r9.c.processZhaiwuAboutAssetData(i12, this.f7683b0, this.f7684c0, this.Z, 0.0d, null);
     *                     bill = bill2;
     *                 } else if (this.f7683b0 <= 0.0d) {
     *                     z6.p d13 = z6.p.d();
     *                     if (this.Y.isZhaiwuDebt()) {
     *                         i11 = R.string.error_empty_jieru_value;
     *                     }
     *                     d13.i(this, i11);
     *                     return;
     *                 } else {
     *                     double money = this.Y.getMoney();
     *                     bill = this.Y;
     *                     bill.setMoney(this.f7683b0);
     *                     bill.setRemark(trim);
     *                     bill.setTimeInSec(timeInMillis);
     *                     bill.setImages(imageUrls);
     *                     bill.setStatus(2);
     *                     bill.setUpdateTimeInSec(z6.b.g());
     *                     Bill.setZhaiwuAboutAsset(bill, this.Z);
     *                     r9.c.processZhaiwuCurrentAssetData(bill.getType(), this.f7683b0, this.f7686e0, money);
     *                     r9.c.processZhaiwuAboutAssetData(bill.getType(), this.f7683b0, 0.0d, this.Z, money, this.f7687f0);
     *                     r9.c.processZhaiwuBillDescInfo(bill, this.f7686e0, this.Z);
     *                 }
     *                 com.mutangtech.qianji.data.db.convert.a aVar = new com.mutangtech.qianji.data.db.convert.a();
     *                 AssetAccount assetAccount = this.Z;
     *                 if (assetAccount != null) {
     *                     aVar.insertOrReplace(assetAccount, false);
     *                 }
     *                 AssetAccount assetAccount2 = this.W;
     *                 if (assetAccount2 != null) {
     *                     aVar.insertOrReplace(assetAccount2, false);
     *                 }
     *                 a1(bill, trim);
     *                 return;
     *             }
     *             d10 = z6.p.d();
     *             i10 = R.string.error_image_not_preapred;
     *         }
     *         d10.k(this, i10);
     *     }
     *
     */

    //增加借出账单
    fun addExpand(billModel: BookBillModel){
        // Arguments com.mutangtech.qianji.asset.loanpay.LoanPayAct.a1(_id=null;billid=1725681101123196413;userid=200104405e109647c18e9;bookid=-1;timeInSec=1725681093;type=7;remark=;money=999.0;status=2;categoryId=0;platform=0;assetId=1711369641078;fromId=-1;targetId=-1;extra=null, (none))
    }
    // 借出收款
    fun expandRepayment(billModel: BookBillModel){
        // Arguments com.mutangtech.qianji.asset.loanpay.LoanPayAct.a1(_id=null;billid=1725681101123196413;userid=200104405e109647c18e9;bookid=-1;timeInSec=1725681093;type=7;remark=;money=999.0;status=2;categoryId=0;platform=0;assetId=1711369641078;fromId=-1;targetId=-1;extra=null, (none))
    }

    //增加借款账单
    fun addLoanBill(billModel: BookBillModel){
        // Arguments com.mutangtech.qianji.asset.loanpay.LoanPayAct.a1(_id=null;billid=1725680887791133088;userid=200104405e109647c18e9;bookid=-1;timeInSec=1725680883;type=6;remark=;money=888.0;status=2;categoryId=0;platform=0;assetId=1725641271342;fromId=-1;targetId=-1;extra=null, (none))
    }

    // 借款还款
    fun loanRepayment(billModel: BookBillModel){
        // Arguments com.mutangtech.qianji.asset.loanpay.LoanPayAct.a1(_id=null;billid=1725680857977193424;userid=200104405e109647c18e9;bookid=-1;timeInSec=1725680855;type=9;remark=;money=133.0;status=2;categoryId=0;platform=0;assetId=1725641271342;fromId=-1;targetId=-1;extra=null, (none))
    }

    /**
     *
     *     public static void start(Context context, AssetAccount assetAccount, boolean z10) {
     *         if (assetAccount != null) {
     *             Intent intent = new Intent(context, LoanPayAct.class);
     *             intent.putExtra("extra_asset", assetAccount);
     *             intent.putExtra("extra_append", z10);
     *             context.startActivity(intent);
     *         }
     *     }
     *
     */

    fun buildBill(billModel: BookBillModel){

    }


}