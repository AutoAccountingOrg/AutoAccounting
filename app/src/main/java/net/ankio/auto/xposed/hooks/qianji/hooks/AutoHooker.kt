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

package net.ankio.auto.xposed.hooks.qianji.hooks

import android.content.Intent
import android.net.Uri
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.BuildConfig
import net.ankio.auto.xposed.core.api.PartHooker
import net.ankio.auto.xposed.core.hook.Hooker
import net.ankio.auto.xposed.core.utils.AppRuntime
import net.ankio.auto.xposed.core.utils.AppRuntime.application
import net.ankio.auto.xposed.core.utils.AppRuntime.manifest
import net.ankio.auto.xposed.core.utils.MessageUtils
import net.ankio.auto.xposed.core.utils.ThreadUtils
import net.ankio.auto.xposed.hooks.qianji.impl.AssetPreviewPresenterImpl
import net.ankio.auto.xposed.hooks.qianji.impl.BookManagerImpl
import net.ankio.auto.xposed.hooks.qianji.impl.BxPresenterImpl
import net.ankio.auto.xposed.hooks.qianji.impl.CateInitPresenterImpl
import net.ankio.auto.xposed.hooks.qianji.impl.RefundPresenterImpl
import net.ankio.auto.xposed.hooks.qianji.impl.SearchPresenterImpl
import net.ankio.auto.xposed.hooks.qianji.models.AutoTaskLog
import net.ankio.auto.xposed.hooks.qianji.sync.SyncBillUtils
import net.ankio.auto.xposed.hooks.qianji.sync.debt.ExpendLendingUtils
import net.ankio.auto.xposed.hooks.qianji.sync.debt.ExpendRepaymentUtils
import net.ankio.auto.xposed.hooks.qianji.sync.debt.IncomeLendingUtils
import net.ankio.auto.xposed.hooks.qianji.sync.debt.IncomeRepaymentUtils
import net.ankio.auto.xposed.hooks.qianji.tools.QianJiAction
import net.ankio.auto.xposed.hooks.qianji.tools.QianJiBillType
import net.ankio.auto.xposed.hooks.qianji.tools.QianJiUri
import org.ezbook.server.db.model.BillInfoModel

class AutoHooker : PartHooker() {
    lateinit var addBillIntentAct: Class<*>

    val className = "com.mutangtech.qianji.bill.auto.AddBillIntentAct"



    override fun hook() {

        addBillIntentAct = manifest.clazz("AddBillIntentAct")
        // 拦截intent
        hookDoIntent()
        // 超时调用pass
        hookTimeout()


        hookTaskLog()

    }






    private fun hookDoIntent() {
        Hooker.before(
            addBillIntentAct,
            manifest.method("AddBillIntentAct", "doIntent"),
            Intent::class.java
        ) {
            val intent = it.args[0] as Intent
            val from = intent.getStringExtra("from")
            if (from != BuildConfig.APPLICATION_ID) return@before
            // 只处理来自自动记账的账单
            val action = intent.getStringExtra("action") ?: return@before
            it.result = null
            val actionItem = QianJiAction.valueOf(action)
            ThreadUtils.launch(Dispatchers.Main) {
                when (actionItem) {
                    QianJiAction.SYNC_BILL -> {
                        // 同步账单的请求
                        SyncBillUtils().sync(application!!)
                    }

                    QianJiAction.SYNC_BOOK_CATEGORY_ASSET -> {
                        //同步资产、账本、分类等数据的请求
                        AssetPreviewPresenterImpl.syncAssets()
                        val books = BookManagerImpl.syncBooks()
                        CateInitPresenterImpl.syncCategory(books)
                    }

                    QianJiAction.SYNC_REIMBURSE_BILL -> {
                        //同步报销账单的请求，不需要频繁请求
                        BxPresenterImpl.syncBaoXiao()
                    }

                    QianJiAction.SYNC_RECENT_EXPENSE_BILL -> {
                        //同步最近10天的支出账单, 不需要频繁请求
                        SearchPresenterImpl.syncBills()
                    }
                }
                // 跳过原始方法执行

                XposedHelpers.callMethod(it.thisObject, "finish")
            }
        }
    }



    private fun hookTaskLog() {


        Hooker.before(
            "com.mutangtech.qianji.bill.auto.AddBillIntentAct",
            manifest.method(
                "AddBillIntentAct",
                "InsertAutoTask",
            ),
            "java.lang.String",
            "com.mutangtech.qianji.data.model.AutoTaskLog"
        ){ param ->
            val msg = param.args[0] as String
            val autoTaskLog = AutoTaskLog.fromObject(param.args[1])
            autoTaskLog.setFrom(BuildConfig.APPLICATION_ID)
            param.args[1] = autoTaskLog.toObject()
            val value = autoTaskLog.getValue() ?: return@before
            val uri = Uri.parse(value)
            AppRuntime.log("hookTaskLog: $value")
            val billInfo = QianJiUri.toAuto(uri)
            if (billInfo.id < 0) return@before

            ThreadUtils.launch {
                BillInfoModel.status(billInfo.id, false)
            }


            when (uri.getQueryParameter("type")?.toInt() ?: 0) {
                QianJiBillType.Expend.value,
                QianJiBillType.Transfer.value,
                QianJiBillType.Income.value,
                QianJiBillType.ExpendReimbursement.value
                    -> {
                    manifest.log("Qianji Error: $msg")
                    return@before
                }

                // 支出（借出）
                QianJiBillType.ExpendLending.value -> {
                    param.result = null
                    ThreadUtils.launch {
                        runCatching {
                            ExpendLendingUtils().sync(billInfo)
                        }.onSuccess {
                            MessageUtils.toast("借出成功")
                            BillInfoModel.status(billInfo.id, true)
                        }.onFailure {

                            manifest.logD("借出失败 ${it.message}")
                            manifest.logE(it)
                            MessageUtils.toast("借出失败 ${it.message ?: ""}")
                        }
                        XposedHelpers.callMethod(param.thisObject, "finish")
                    }

                }
                // 支出（还款）
                QianJiBillType.ExpendRepayment.value -> {
                    param.result = null
                    ThreadUtils.launch {
                        runCatching {
                            ExpendRepaymentUtils().sync(billInfo)
                        }.onSuccess {
                            MessageUtils.toast("还款成功")
                            BillInfoModel.status(billInfo.id, true)
                        }.onFailure {
                            manifest.logE(it)
                            manifest.logD("还款失败 ${it.message}")
                            MessageUtils.toast("还款失败 ${it.message ?: ""}")
                        }
                        XposedHelpers.callMethod(param.thisObject, "finish")
                    }
                }

                // 收入（借入）,OK
                QianJiBillType.IncomeLending.value -> {
                    param.result = null
                    ThreadUtils.launch {
                        runCatching {
                            IncomeLendingUtils().sync(billInfo)
                        }.onSuccess {
                            MessageUtils.toast("借入成功")
                            BillInfoModel.status(billInfo.id, true)
                        }.onFailure {
                            manifest.logE(it)
                            manifest.logD("借入失败 ${it.message}")
                            MessageUtils.toast("借入失败 ${it.message ?: ""}")
                        }
                        XposedHelpers.callMethod(param.thisObject, "finish")
                    }

                }
                // 收入（收款）,OK
                QianJiBillType.IncomeRepayment.value -> {
                    param.result = null
                    ThreadUtils.launch {
                        runCatching {
                            IncomeRepaymentUtils().sync(billInfo)
                        }.onSuccess {
                            MessageUtils.toast("收款成功")
                            BillInfoModel.status(billInfo.id, true)
                        }.onFailure {
                            manifest.logE(it)
                            manifest.logD("收款失败 ${it.message}")
                            MessageUtils.toast("收款失败 ${it.message ?: ""}")
                        }
                        XposedHelpers.callMethod(param.thisObject, "finish")
                    }
                }
                // 收入（报销)
                QianJiBillType.IncomeReimbursement.value -> {
                    param.result = null
                    ThreadUtils.launch {
                        runCatching {
                            BxPresenterImpl.doBaoXiao(billInfo)
                        }.onSuccess {
                            MessageUtils.toast("报销成功")
                            BillInfoModel.status(billInfo.id, true)
                        }.onFailure {
                            manifest.logD("报销失败 ${it.message}")
                            MessageUtils.toast("报销失败 ${it.message ?: ""}")
                            manifest.logE(it)
                        }
                        XposedHelpers.callMethod(param.thisObject, "finish")
                    }
                }
                // 收入（退款)
                QianJiBillType.IncomeRefund.value -> {
                    param.result = null
                    ThreadUtils.launch {
                        runCatching {
                            RefundPresenterImpl.refund(billInfo)
                        }.onSuccess {
                            MessageUtils.toast("退款成功")
                            BillInfoModel.status(billInfo.id, true)
                        }.onFailure {
                            manifest.logE(it)
                            manifest.logD("退款失败 ${it.message}")
                            MessageUtils.toast("退款失败 ${it.message ?: ""}")
                        }
                        XposedHelpers.callMethod(param.thisObject, "finish")
                    }
                }


            }

        }
    }


    /*
    * hookTimeout
     */
    private fun hookTimeout() {

        val clazz by lazy {
            manifest.clazz("TimeoutApp")
        }

        Hooker.after(
            clazz,
            "timeoutApp",
            String::class.java,
            Long::class.java
        ) { param ->
            val prop = param.args[0] as String
            val timeout = param.args[1] as Long
            if (prop == "auto_task_last_time") {
                manifest.logD("hookTimeout: $prop $timeout")
                param.result = true
            }
        }
    }




}