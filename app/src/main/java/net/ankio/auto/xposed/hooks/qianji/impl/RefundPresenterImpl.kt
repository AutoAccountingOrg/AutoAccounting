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
import net.ankio.auto.xposed.core.api.HookerClazz
import net.ankio.auto.xposed.core.utils.AppRuntime
import net.ankio.auto.xposed.hooks.qianji.helper.AssetDbHelper
import net.ankio.auto.xposed.hooks.qianji.models.QjBillModel
import org.ezbook.server.db.model.BillInfoModel
import java.lang.reflect.Proxy
import java.util.Calendar
import kotlin.coroutines.suspendCoroutine

/**
 * 通过反射驱动钱迹退款流程：
 * - 定位需退款的原始账单（按 id）
 * - 反射调用宿主 RefundPresenterImpl#submit(...) 完成退款
 * - 回调成功后刷新资产
 */
object RefundPresenterImpl : HookerClazz() {
    private const val CLAZZ = "com.mutangtech.qianji.bill.refund.RefundPresenterImpl"
    private val refundImpl by lazy { clazz() }
    override var rule = net.ankio.dex.model.Clazz(name = this::class.java.name, nameRule = CLAZZ)

    /**
     * 执行退款。
     * @param billInfo 来源于自动记账的退款指令（包含 extendData: 原账单 id、money、time、remark 等）
     */
    suspend fun refund(billInfo: BillInfoModel) = withContext(Dispatchers.Main) {

        // 解析待退款账单 id（宽松逗号分隔，容错空白）
        val billId = billInfo.extendData
            .split(Regex("\\s*,\\s*"))
            .firstOrNull()
        if (billId.isNullOrBlank()) {
            AppRuntime.manifest.i("refund: 退款账单id缺失 extendData=${billInfo.extendData}")
            return@withContext
        }
        // 先获取账单列表
        val bills = SearchPresenterImpl.getLast10DayLists()
        AppRuntime.manifest.i("bills: $bills")
        //查找退款的账单
        val bill = bills.firstOrNull {
            it != null && QjBillModel.fromObject(it).getBillid() == billId.toLong()
        }?.let { QjBillModel.fromObject(it) }
        if (bill == null) {
            AppRuntime.manifest.i("refund: 找不到退款的账单 id=$billId")
            return@withContext
        }
        bill.set_id(null)

        // 获取退款账户（允许为空，由宿主自行处理）
        val assetAccount = AssetPreviewPresenterImpl.getAssetByName(billInfo.accountNameFrom)

        suspendCoroutine { continuation ->
            val money = billInfo.money
            val calendar = Calendar.getInstance().apply { timeInMillis = billInfo.time }
            val remark = billInfo.remark
            val imageUrls = ArrayList<String>()
            val tags = ArrayList<Any>()

            runCatching {
                val constructor = refundImpl.constructors.first()
                val param1Clazz = constructor.parameterTypes.first()

                val viewProxy = Proxy.newProxyInstance(
                    AppRuntime.classLoader,
                    arrayOf(param1Clazz)
                ) { _, method, args ->
                    if (method.name == "onFinished") {
                        val ok = (args.getOrNull(0) as? Boolean) == true
                        if (!ok) {
                            continuation.resumeWith(Result.failure(Throwable("退款失败")))
                        } else {
                            if (assetAccount != null) {
                                AssetDbHelper.newInstance().insertOrReplace(assetAccount)
                            }
                            continuation.resumeWith(Result.success(Unit))
                        }
                    }
                    null
                }

                val presenter = XposedHelpers.newInstance(refundImpl, viewProxy)
                XposedHelpers.callMethod(
                    presenter,
                    "submit",
                    bill.toObject(),
                    null,
                    money,
                    calendar,
                    assetAccount?.toObject(),
                    remark,
                    null,
                    imageUrls,
                    tags
                )
            }.onFailure { e ->
                continuation.resumeWith(Result.failure(e))
            }
        }

    }
}