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

package net.ankio.auto.hooks.qianji.hooks

import android.app.Application
import android.net.Uri
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.BuildConfig
import net.ankio.auto.core.App
import net.ankio.auto.core.api.HookerManifest
import net.ankio.auto.core.api.PartHooker
import net.ankio.auto.hooks.qianji.sync.BaoXiaoUtils
import net.ankio.auto.hooks.qianji.tools.QianJiBillType
import net.ankio.auto.hooks.qianji.tools.QianJiUri
import net.ankio.dex.model.ClazzField
import net.ankio.dex.model.ClazzMethod
import org.ezbook.server.db.model.BillInfoModel

class AutoHooker : PartHooker() {
    lateinit var addBillIntentAct: Class<*>
    override val methodsRule: MutableList<Triple<String, String, ClazzMethod>>
        get() = mutableListOf(
            Triple(
                "com.mutangtech.qianji.bill.auto.AddBillIntentAct", "InsertAutoTask",
                ClazzMethod(
                    parameters =
                    listOf(
                        ClazzField(
                            type = "java.lang.String",
                        ),
                        ClazzField(
                            type = "com.mutangtech.qianji.data.model.AutoTaskLog",
                        ),
                    ),
                    regex = "^\\w{2}$",
                ),
            )
        )

    override fun hook(
        hookerManifest: HookerManifest,
        application: Application?,
        classLoader: ClassLoader
    ) {

        addBillIntentAct = classLoader.loadClass("com.mutangtech.qianji.bill.auto.AddBillIntentAct")

        hookTimeout(hookerManifest)

        hookTaskLog(hookerManifest, classLoader)

        hookTaskLogStatus(hookerManifest, classLoader)
    }

    private fun hookTaskLog(hookerManifest: HookerManifest, classLoader: ClassLoader) {
        XposedHelpers.findAndHookMethod(
            "com.mutangtech.qianji.bill.auto.AddBillIntentAct",
            classLoader,
            method(
                "com.mutangtech.qianji.bill.auto.AddBillIntentAct",
                "InsertAutoTask",
                classLoader
            ),
            "java.lang.String",
            "com.mutangtech.qianji.data.model.AutoTaskLog", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    super.beforeHookedMethod(param)

                    val msg = param.args[0] as String
                    hookerManifest.logD("hookTaskLog: $msg")

                    val error = handleError(msg)
                    val autoTaskLog = param.args[1] as Any


                    val value = XposedHelpers.getObjectField(autoTaskLog, "value") as String
                    val uri = Uri.parse(value)
                    val billInfo = QianJiUri.toAuto(uri)

                    if (billInfo.id < 0) return

                    hookerManifest.logD("hookTaskLog BillInfo: $billInfo")

                    if (error !== msg) {
                        XposedHelpers.callMethod(autoTaskLog, "setStatus", 0)
                        param.args[0] = error
                        App.launch {
                            BillInfoModel.status(billInfo.id, false)
                        }
                        return
                    }



                    XposedHelpers.setObjectField(autoTaskLog, "from", BuildConfig.APPLICATION_ID)


                    when (uri.getQueryParameter("type")?.toInt() ?: 0) {
                        QianJiBillType.Expend.value -> {

                            return
                        }

                        QianJiBillType.ExpendReimbursement.value -> {
                            return
                        }
                        // 支出（借出）
                        QianJiBillType.ExpendLending.value -> {

                        }
                        // 支出（还款销账）
                        QianJiBillType.ExpendRepayment.value -> {

                        }

                        QianJiBillType.Income.value -> {
                            return
                        }
                        // 收入（借入）
                        QianJiBillType.IncomeLending.value -> {

                        }
                        // 收入（还款销账）
                        QianJiBillType.IncomeRepayment.value -> {

                        }
                        // 收入（报销)
                        QianJiBillType.IncomeReimbursement.value -> {
                            App.launch {
                                runCatching {
                                    BaoXiaoUtils(hookerManifest, classLoader).doBaoXiao(billInfo)
                                }.onSuccess {
                                    hookerManifest.logD("报销成功")
                                    App.toast("报销成功")
                                }.onFailure {
                                    BillInfoModel.status(billInfo.id, false)
                                    hookerManifest.logD("报销失败 ${it.message}")
                                    App.toast("报销失败 ${handleError(it.message ?: "")}")
                                    hookerManifest.logE(it)
                                }
                            }
                            param.args[0] = "自动记账正在报销中, 请稍候..."
                            XposedHelpers.callMethod(autoTaskLog, "setStatus", 1)
                        }

                        QianJiBillType.Transfer.value -> {
                            return
                        }
                    }


                }
            }
        )
    }

    private fun handleError(message: String): String {
        if (message.contains("key=accountname;value=")) {
            val assetName = message.split("value=")[1]
            return "钱迹中找不到【${assetName}】资产账户，请先创建。"
        }
        return message
    }

    /*
    * hookTimeout
     */
    private fun hookTimeout(hookerManifest: HookerManifest) {

        XposedHelpers.setStaticLongField(addBillIntentAct, "FREQUENCY_LIMIT_TIME", 0L)

        hookerManifest.logD(
            "hookTimeout =${
                XposedHelpers.getStaticLongField(
                    addBillIntentAct,
                    "FREQUENCY_LIMIT_TIME"
                )
            }"
        )
    }

    private fun hookTaskLogStatus(hookerManifest: HookerManifest, classLoader: ClassLoader) {
        XposedHelpers.findAndHookMethod("com.mutangtech.qianji.data.model.AutoTaskLog",
            classLoader,
            "setStatus",
            Int::class.java,
            object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam?) {
                    val status = param?.args?.get(0) as Int
                    val raw = XposedHelpers.getIntField(param.thisObject, "status")
                    if (raw != 0) {
                        return
                    }

                    XposedHelpers.setIntField(param.thisObject, "status", status)
                }
            }
        )

        XposedHelpers.findAndHookMethod("com.mutangtech.qianji.data.model.AutoTaskLog",
            classLoader,
            "setFrom",
            String::class.java,
            object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam?) {
                    XposedHelpers.setObjectField(
                        param!!.thisObject,
                        "from",
                        BuildConfig.APPLICATION_ID
                    )
                }
            }
        )
    }


}