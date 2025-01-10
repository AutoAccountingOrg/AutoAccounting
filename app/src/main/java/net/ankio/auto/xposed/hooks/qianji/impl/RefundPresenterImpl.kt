/*
 * Copyright (C) 2025 ankio(ankio@ankio.net)
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

package net.ankio.auto.xposed.hooks.qianji.impl

import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.xposed.core.hook.Hooker
import net.ankio.auto.xposed.core.utils.AppRuntime
import net.ankio.auto.xposed.hooks.qianji.impl.AssetPreviewPresenterImpl.assetPreviewPresenterImplClazz
import net.ankio.auto.xposed.hooks.qianji.models.Bill
import org.ezbook.server.db.model.BillInfoModel
import java.lang.reflect.Proxy
import java.util.Calendar
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object RefundPresenterImpl {
    val CLAZZ = "com.mutangtech.qianji.bill.refund.RefundPresenterImpl"
    val refundImpl = Hooker.loader(CLAZZ)

    suspend fun refund(billInfo: BillInfoModel): Boolean = withContext(Dispatchers.Main) {


        val billId = billInfo.extendData.split(", ").firstOrNull()
        if (billId == null) {
            AppRuntime.logE(Throwable("找不到退款的账单id"))
            return@withContext false
        }
        // 先获取账单列表
        val bills = SearchPresenterImpl.getLast10DayLists()
        //AppRuntime.log("bills: $bills")
        //查找退款的账单
        val bill = bills.firstOrNull {
            it != null && Bill.fromObject(it).getBillid() == billId.toLong()
        }?.let { Bill.fromObject(it) }
        if (bill == null) {
            AppRuntime.logE(Throwable("找不到退款的账单"))
            return@withContext false
        }
        bill.set_id(null)

        //   val refundBill = Bill.newInstance()

        // 获取退款账户
        val assetAccount = AssetPreviewPresenterImpl.getAssetByName(billInfo.accountNameFrom)

        return@withContext suspendCoroutine { continuation ->
            val money = billInfo.money
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = billInfo.time
            val remark = billInfo.remark
            val stringList = ArrayList<String>()
            val tagList = ArrayList<Any>()

            val constructor = refundImpl.constructors.firstOrNull()
            if (constructor == null) {
                AppRuntime.logE(NoSuchMethodException("构造函数未找到"))
                continuation.resume(false)
                return@suspendCoroutine
            }

            val parameterTypes: Array<Class<*>> = constructor.parameterTypes
            val param1Clazz = parameterTypes[0]

            val param1Object = Proxy.newProxyInstance(
                AppRuntime.classLoader,
                arrayOf(param1Clazz)
            ) { _, method, args ->
                if (method.name == "onFinished") {
                    val result = args[0] as Boolean
                    if (!result) {
                        AppRuntime.logE(Throwable("退款失败"))
                    }
                    continuation.resume(result)
                }
                null
            }

            val refundImplObj = XposedHelpers.newInstance(refundImpl, param1Object)
            XposedHelpers.callMethod(
                refundImplObj,
                "submit",
                bill.toObject(),
                null,
                money,
                calendar,
                assetAccount?.toObject(),
                remark,
                null,
                stringList,
                tagList
            )
        }
    }
}