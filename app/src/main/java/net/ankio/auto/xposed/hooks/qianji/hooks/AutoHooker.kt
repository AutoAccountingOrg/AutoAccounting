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

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.net.toUri
import de.robv.android.xposed.XposedBridge
import net.ankio.auto.BuildConfig
import net.ankio.auto.http.api.BillAPI
import net.ankio.auto.xposed.core.api.PartHooker
import net.ankio.auto.xposed.core.hook.Hooker
import net.ankio.auto.xposed.core.ui.ViewUtils
import net.ankio.auto.xposed.core.utils.AppRuntime
import net.ankio.auto.xposed.core.utils.CoroutineUtils
import net.ankio.auto.xposed.core.utils.MessageUtils
import net.ankio.auto.xposed.hooks.qianji.activity.AddBillIntentAct
import net.ankio.auto.xposed.hooks.qianji.debt.ExpendLendingUtils
import net.ankio.auto.xposed.hooks.qianji.helper.BillDbHelper
import net.ankio.auto.xposed.hooks.qianji.impl.AssetPreviewPresenterImpl
import net.ankio.auto.xposed.hooks.qianji.impl.BookManagerImpl
import net.ankio.auto.xposed.hooks.qianji.impl.BxPresenterImpl
import net.ankio.auto.xposed.hooks.qianji.impl.CateInitPresenterImpl
import net.ankio.auto.xposed.hooks.qianji.impl.RefundPresenterImpl
import net.ankio.auto.xposed.hooks.qianji.impl.SearchPresenterImpl
import net.ankio.auto.xposed.hooks.qianji.models.AutoTaskLogModel
import net.ankio.auto.xposed.hooks.qianji.models.BillExtraModel
import net.ankio.auto.xposed.hooks.qianji.models.QjBillModel
import net.ankio.auto.xposed.hooks.qianji.models.UserModel
import net.ankio.auto.xposed.hooks.qianji.debt.IncomeLendingUtils
import net.ankio.auto.xposed.hooks.qianji.debt.ExpendRepaymentUtils
import net.ankio.auto.xposed.hooks.qianji.debt.IncomeRepaymentUtils
import net.ankio.auto.xposed.hooks.qianji.tools.QianJiBillType
import net.ankio.auto.xposed.hooks.qianji.tools.QianJiUri
import net.ankio.auto.xposed.hooks.qianji.utils.BroadcastUtils
import net.ankio.auto.xposed.hooks.qianji.utils.TimeRecordUtils
import org.ezbook.server.constant.BillAction
import org.ezbook.server.db.model.BillInfoModel
import org.ezbook.server.tools.runCatchingExceptCancel


/**
 * 钱迹模块主 Hook 入口。
 *
 * 职责：
 * - 拦截入口 Intent 按 action 分发任务，并在处理后终止原流程；
 * - 拦截自动任务日志，补齐来源并分发到具体同步器；
 * - 初始化界面细节（透明背景）；
 * - 定向放行宿主的超时检查以保证任务不中断。
 *
 * 约束：
 * - 仅在自动记账场景阻断宿主方法，其他场景完全不干预；
 * - UI 在主线程执行，网络/IO 使用 withIO；
 * - 简洁优先，避免过度设计。
 */
class AutoHooker : PartHooker() {
    private lateinit var addBillIntentAct: Class<*>

    /**
     * 注册所有 Hook，按职责拆分为视图、Intent、日志与超时处理。
     */
    override fun hook() {

        addBillIntentAct = AddBillIntentAct.clazz()
        // 拦截intent
        hookDoIntent()
        // 超时调用pass
        hookTimeout()


        hookTaskLog()

        hookView()

    }


    /**
     * 视图初始化：将内容视图背景设为透明，避免遮挡或闪烁。
     */
    private fun hookView() {
        Hooker.after(addBillIntentAct, "onCreate", Bundle::class.java) {
            val act = it.thisObject as Activity
            val background = ViewUtils.getViewById(
                "com.mutangtech.qianji.R\$id",
                act,
                AppRuntime.classLoader,
                "content_view"
            )
            // 设置透明背景
            background.setBackgroundColor(android.graphics.Color.TRANSPARENT)

            if (!UserModel.isLogin()) {
                MessageUtils.toast("未登录的用户无法进行自动记账（钱迹限制）")
            }
        }
    }

    private fun hookBroadcast(activity: Activity, uri: Uri) {
        Hooker.onceBefore(
            BroadcastUtils.clazz(),
            "onAddBill",
            QjBillModel.clazz(),
            Boolean::class.javaPrimitiveType!!
        ) {
            val billModel = QjBillModel.fromObject(it.args[0])
            //传入的是负值表示优惠
            val discount = uri.getQueryParameter("discount")?.toDoubleOrNull()
            if (discount != null && discount > 0 && (billModel.isSpend() || billModel.isTransfer())) {
                //只有支出或者还款的账户才需要记录优惠
                val extraModel = BillExtraModel.newInstance()
                extraModel.setTransfee(-discount)
                billModel.setExtra(extraModel)
                it.args[0] = billModel.toObject()

            }
            //TODO 对于币种的处理
            // 先获取本位币
            // 根据本位币对目的币种的汇率计算本位币的实际支出金额
            // 给Extra填充汇率等信息


            BillDbHelper.newInstance().saveOrUpdateBill(billModel)
            XposedBridge.log("保存的自动记账账单：${it.args[0]}, 当前的URi: ${uri}")




            true
        }
    }


    /**
     * 拦截入口 Intent：
     * - 仅处理自动记账 action；匹配成功则由我们在主线程处理并关闭页面；
     * - 匹配失败不干预，保持宿主原逻辑。
     */
    private fun hookDoIntent() {
        Hooker.before(
            addBillIntentAct,
            AddBillIntentAct.doIntent(),
            Intent::class.java
        ) {
            val intent = it.args[0] as Intent
            val data = intent.data ?: return@before
            hookBroadcast(it.thisObject as Activity, data)
            // 只处理来自自动记账的账单
            val action = data.getQueryParameter("action") ?: return@before
            // 阻断宿主原实现，改由自动记账逻辑接管
            it.result = null
            val actionItem = BillAction.valueOf(action)
            CoroutineUtils.withMain {
                when (actionItem) {

                    BillAction.SYNC_BOOK_CATEGORY_ASSET -> {
                        MessageUtils.toast("正在同步资产信息")
                        //同步资产、账本、分类等数据的请求
                        AssetPreviewPresenterImpl.syncAssets()
                        val books = BookManagerImpl.syncBooks()
                        CateInitPresenterImpl.syncCategory(books)
                        MessageUtils.toast("资产信息同步完成")
                    }

                    BillAction.SYNC_REIMBURSE_BILL -> {
                        //同步报销账单的请求，不需要频繁请求
                        BxPresenterImpl.syncBaoXiao()
                    }

                    BillAction.SYNC_RECENT_EXPENSE_BILL -> {
                        //同步最近10天的支出账单, 不需要频繁请求
                        SearchPresenterImpl.syncBills()
                    }
                }
                // 完成后关闭当前任务栈，跳过宿主原流程

                AddBillIntentAct.fromObj(it.thisObject).finishAffinity()
            }
        }
    }

    /**
     * 拦截自动任务日志：
     * - 标记来源为本应用包名；
     * - 解析 URI 得到账单并分发到具体同步器；
     * - 在 IO 线程汇报状态并在完成后关闭页面。
     */
    private fun hookTaskLog() {


        Hooker.before(
            AddBillIntentAct.CLAZZ,
            AddBillIntentAct.insertAutoTask(),
            "java.lang.String",
            AutoTaskLogModel.CLAZZ
        ) { param ->
            val msg = param.args[0] as String
            val autoTaskLog = AutoTaskLogModel.fromObject(param.args[1])
            autoTaskLog.setFrom(BuildConfig.APPLICATION_ID)
            param.args[1] = autoTaskLog.toObject()
            val value = autoTaskLog.getValue()
            val uri = value?.toUri()
            manifest.i("hookTaskLog: $value")
            val addBillIntentAct = AddBillIntentAct.fromObj(param.thisObject)
            if (uri == null) {
                addBillIntentAct.finishAffinity()
                return@before
            }
            val billInfo = QianJiUri.toAuto(uri)
            if (billInfo.id <= 0) {
                addBillIntentAct.finishAffinity()
                return@before
            }

            CoroutineUtils.withIO {
                BillAPI.status(billInfo.id, false)
            }


            when (uri.getQueryParameter("type")?.toInt() ?: 0) {
                QianJiBillType.Expend.value,
                QianJiBillType.Transfer.value,
                QianJiBillType.Income.value,
                QianJiBillType.ExpendReimbursement.value
                    -> {
                    manifest.i("Qianji Error: $msg")

                }

                // 支出（借出）
                QianJiBillType.ExpendLending.value -> {
                    // 阻断宿主实现，采用异步处理并在完成后关闭页面
                    param.result = null
                    handleExpendLending(billInfo, AddBillIntentAct.fromObj(param.thisObject))

                }
                // 支出（还款）
                QianJiBillType.ExpendRepayment.value -> {
                    // 阻断宿主实现，采用异步处理并在完成后关闭页面
                    param.result = null
                    handleExpendRepayment(billInfo, AddBillIntentAct.fromObj(param.thisObject))
                }

                // 收入（借入）
                QianJiBillType.IncomeLending.value -> {
                    // 阻断宿主实现，采用异步处理并在完成后关闭页面
                    param.result = null
                    handleIncomeLending(billInfo, AddBillIntentAct.fromObj(param.thisObject))


                }
                // 收入（收款）,OK
                QianJiBillType.IncomeRepayment.value -> {
                    // 阻断宿主实现，采用异步处理并在完成后关闭页面
                    param.result = null
                    handleIncomeRepayment(billInfo, AddBillIntentAct.fromObj(param.thisObject))

                }
                // 收入（报销)
                QianJiBillType.IncomeReimbursement.value -> {
                    // 阻断宿主实现，采用异步处理并在完成后关闭页面
                    param.result = null
                    CoroutineUtils.withIO {
                        runCatchingExceptCancel {
                            BxPresenterImpl.doBaoXiao(billInfo)
                        }.onSuccess {
                            MessageUtils.toast("报销成功")
                            BillAPI.status(billInfo.id, true)
                        }.onFailure {
                            manifest.e("报销失败 ${it.message}", it)
                            MessageUtils.toast("报销失败 ${it.message ?: ""}")
                        }
                        addBillIntentAct.finishAffinity()
                    }
                }
                // 收入（退款),OK
                QianJiBillType.IncomeRefund.value -> {
                    // 阻断宿主实现，采用异步处理并在完成后关闭页面
                    param.result = null
                    CoroutineUtils.withIO {
                        runCatching {
                            RefundPresenterImpl.refund(billInfo)
                        }.onSuccess {
                            MessageUtils.toast("退款成功")
                            BillAPI.status(billInfo.id, true)
                        }.onFailure {
                            manifest.e("退款失败 ${it.message}", it)
                            MessageUtils.toast("退款失败 ${it.message ?: ""}")
                        }
                        AddBillIntentAct.fromObj(param.thisObject).finishAffinity()
                    }
                }


            }

        }
    }

    private fun handleExpendLending(billModel: BillInfoModel, act: AddBillIntentAct) {
        CoroutineUtils.withIO {
            runCatching {
                ExpendLendingUtils().sync(billModel)
            }.onSuccess {
                MessageUtils.toast("借出成功")
                BillAPI.status(billModel.id, true)
            }.onFailure {
                manifest.d("借出失败 ${it.message}")
                manifest.e(it)
                MessageUtils.toast("借出失败 ${it.message ?: ""}")
            }
            act.finishAffinity()

        }
    }

    private fun handleIncomeRepayment(billModel: BillInfoModel, act: AddBillIntentAct) {
        CoroutineUtils.withIO {
            runCatching {
                IncomeRepaymentUtils().sync(billModel)
            }.onSuccess {
                MessageUtils.toast("收款成功")
                BillAPI.status(billModel.id, true)
            }.onFailure {
                manifest.e(it)
                manifest.d("收款失败 ${it.message}")
                MessageUtils.toast("收款失败 ${it.message ?: ""}")
            }
            act.finishAffinity()

        }
    }

    private fun handleExpendRepayment(billModel: BillInfoModel, act: AddBillIntentAct) {
        CoroutineUtils.withIO {
            runCatching {
                ExpendRepaymentUtils().sync(billModel)
            }.onSuccess {
                MessageUtils.toast("还款成功")
                BillAPI.status(billModel.id, true)
            }.onFailure {
                manifest.e(it)
                manifest.d("还款失败 ${it.message}")
                MessageUtils.toast("还款失败 ${it.message ?: ""}")
            }
            act.finishAffinity()

        }
    }


    //收入（借入）
    private fun handleIncomeLending(billModel: BillInfoModel, act: AddBillIntentAct) {
        CoroutineUtils.withIO {

            // 1. 为该账单创建一个Bill
            // 2. 新增或创建来源
            // 3. 为收入账户新增资金

            runCatching {
                IncomeLendingUtils().sync(billModel)
            }.onSuccess {
                MessageUtils.toast("借入成功")
                BillAPI.status(billModel.id, true)
            }.onFailure {
                manifest.e(it)
                manifest.d("借入失败 ${it.message}")
                MessageUtils.toast("借入失败 ${it.message ?: ""}")
            }
            act.finishAffinity()
        }
    }

    /**
     * 宿主超时策略：
     * - 对 key 为 "auto_task_last_time" 的检测直接放行，避免任务被中断；
     * - 其他键不做修改。
     */
    private fun hookTimeout() {
        Hooker.after(
            TimeRecordUtils.clazz(),
            "timeoutApp",
            String::class.java,
            Long::class.java
        ) { param ->
            val prop = param.args[0] as String
            val timeout = param.args[1] as Long
            if (prop == "auto_task_last_time") {
                manifest.d("hookTimeout: $prop $timeout")
                param.result = true
            }
        }
    }


}