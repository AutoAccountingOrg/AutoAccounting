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

package org.ezbook.server.routes


import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.shiqi.quickjs.JSString
import com.shiqi.quickjs.QuickJS
import io.ktor.application.ApplicationCall
import io.ktor.http.Parameters
import io.ktor.request.receiveText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ezbook.server.Server
import org.ezbook.server.ai.ChatGPT
import org.ezbook.server.ai.DeepSeek
import org.ezbook.server.ai.Gemini
import org.ezbook.server.ai.OneAPI
import org.ezbook.server.ai.QWen
import org.ezbook.server.constant.AIModel
import org.ezbook.server.constant.BillState
import org.ezbook.server.constant.BillType
import org.ezbook.server.constant.DataType
import org.ezbook.server.constant.Setting
import org.ezbook.server.constant.SyncType
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.AppDataModel
import org.ezbook.server.db.model.BillInfoModel
import org.ezbook.server.engine.RuleGenerator
import org.ezbook.server.models.ResultModel
import org.ezbook.server.tools.Assets
import org.ezbook.server.tools.Bill
import org.ezbook.server.tools.Category
import org.ezbook.server.tools.FloatingIntent


class JsRoute(private val session: ApplicationCall, private val context: android.content.Context) {
    private val params: Parameters = session.request.queryParameters

    /**
     * 获取设置
     */
    suspend fun analysis(): ResultModel {
        Server.log("Analysis: init")
        val app = params["app"] ?: ""
        val type = params["type"] ?: ""
        val fromAppData = params["fromAppData"]?.toBoolean() ?: false
        val ai = params["ai"] == "true"
        val data = session.receiveText()
        //将string转换为枚举类型
        val dataType: DataType
        try {
            dataType = DataType.valueOf(type)
        } catch (e: IllegalArgumentException) {
            return ResultModel(400, "Type exception: $type")
        }

        Server.log("Analysis: $app $type $fromAppData $ai")
        val appDataModel = AppDataModel()
        appDataModel.app = app
        appDataModel.data = data
        appDataModel.type = dataType
        appDataModel.time = System.currentTimeMillis()


        // 判断是否需要插入AppData
        Server.log("isFromAppData: $fromAppData")
        if (!fromAppData) {
            appDataModel.id = Db.get().dataDao().insert(appDataModel)
        }

        val t = System.currentTimeMillis()

        var billInfoModel: BillInfoModel? = null

        if (!ai) {
            val js = RuleGenerator.data(app, dataType)

            if (js === "") {
                return ResultModel(404, "JS无效，请更新规则。")
            }

            val result = runJS(js, data)

            if (result == "") {

                val useAi =
                    (Db.get().settingDao().query(Setting.AI_AUXILIARY)?.value ?: "false") == "true"

                if (useAi && !fromAppData) {
                    billInfoModel = parseBillInfoFromAi(app, data)
                    if (billInfoModel == null) {
                        return ResultModel(404, "未分析到有效账单（大模型也识别不到）。")
                    }
                } else return ResultModel(404, "未分析到有效账单（可以试试用大模型识别）。")
            } else {
                billInfoModel = parseBillInfo(result, app, dataType)
            }
        } else {
            billInfoModel = parseBillInfoFromAi(app, data)
            if (billInfoModel == null) {
                return ResultModel(404, "未分析到有效账单（大模型也识别不到）。")
            }
        }


        /**
         *  {
         *       type: 1,
         *       money: 0.01,
         *       fee: 0,
         *       shopName: '来自从前慢',
         *       shopItem: '普通红包',
         *       accountNameFrom: '支付宝余额',
         *       accountNameTo: '',
         *       currency: 'CNY',
         *       time: 1702972951000,
         *       channel: '支付宝[收红包]',
         *       ruleName：'xxxx'
         *     }
         */


        if (billInfoModel.cateName.isEmpty() || billInfoModel.cateName == "其它" || billInfoModel.cateName == "其他") {
            //将time从时间戳，转换为h:i的格式
            val time = android.text.format.DateFormat.format("HH:mm", billInfoModel.time).toString()


            val categoryJS = RuleGenerator.category()

            val win = JsonObject()
            win.addProperty("type", billInfoModel.type.name)
            win.addProperty("money", billInfoModel.money)
            win.addProperty("shopName", billInfoModel.shopName)
            win.addProperty("shopItem", billInfoModel.shopItem)
            win.addProperty("time", time)
            var categoryResult = runJS(categoryJS, Gson().toJson(win))
            // { book: '默认账本', category: '早茶' }
            if (categoryResult == "") {
                categoryResult = "{ book: '默认账本', category: '其他' }"
            }

            val categoryJson = runCatching {
                Gson().fromJson(categoryResult, JsonObject::class.java)
            }.onFailure {
                Server.logW("Failed to analyze categories：$categoryResult")
            }.getOrNull()

            billInfoModel.bookName = categoryJson?.get("book")?.asString ?: "默认账本"
            billInfoModel.cateName = categoryJson?.get("category")?.asString ?: "其他"
        }


        val total = System.currentTimeMillis() - t
        // 识别用时
        Server.log("识别用时: $total ms")

        //  资产映射
        Assets.setAssetsMap(billInfoModel)

        // 分类映射
        Category.setCategoryMap(billInfoModel)

        billInfoModel.remark = Bill.getRemark(billInfoModel, context)
        //  备注生成
        //  设置默认账本
        Bill.setBookName(billInfoModel)
        // app数据里面识别的数据不着急插入数据库，等用户选择，其他情况下先插入数据库，再判断是否需要去重
        if (!fromAppData) {
            //存入数据库
            billInfoModel.id = Db.get().billInfoDao().insert(billInfoModel)
        }
        // 账单分组，用于检查重复账单
        val task = Server.billProcessor.addTask(billInfoModel, context)
        task.await()
        if (task.result == null) {
            billInfoModel.state = BillState.Wait2Edit
        } else {
            billInfoModel.state = BillState.Edited
        }
        if (billInfoModel.auto) {
            billInfoModel.state = BillState.Edited
        }

        // 时间容错：部分规则可能识别的时间准确，需要容错，小于2000年之前的时间认为是错误的
        if (billInfoModel.time < 946656000000) {
            billInfoModel.time = System.currentTimeMillis()
        }

        Db.get().billInfoDao().update(billInfoModel)
        Server.log("账单: $billInfoModel")
        if (!fromAppData) {
            // 切换到主线程
            if (!billInfoModel.auto) {
                Server.log("自动记录 - 关闭: $billInfoModel")
                val showInLandScape =
                    Db.get().settingDao().query(Setting.LANDSCAPE_DND)?.value != "false"
                withContext(Dispatchers.Main) {
                    runCatching {
                        startAutoPanel(billInfoModel, task.result,showInLandScape)
                    }.onFailure {
                        Server.log(it)
                    }
                }
            } else {
                // try
                sync2Book(context)
                val showTip =
                    Db.get().settingDao().query(Setting.SHOW_AUTO_BILL_TIP)?.value == "true"
                if (showTip) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "已自动记录账单，可在账单列表中查看。",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            // 更新AppData
            appDataModel.match = true
            appDataModel.rule = billInfoModel.ruleName
            appDataModel.version = Db.get().settingDao().query(Setting.RULE_VERSION)?.value ?: ""
            Db.get().dataDao().update(appDataModel)
        }
        return ResultModel(200, "OK", billInfoModel)
    }


    private suspend fun parseBillInfoFromAi(app: String, data: String): BillInfoModel? {
        val aiModel = Db.get().settingDao().query(Setting.AI_MODEL)?.value ?: AIModel.Gemini
        val billInfoModel: BillInfoModel? = runCatching {
            when (aiModel) {
                AIModel.Gemini.name -> Gemini().request(data)
                AIModel.QWen.name -> QWen().request(data)
                AIModel.DeepSeek.name -> DeepSeek().request(data)
                AIModel.ChatGPT.name -> ChatGPT().request(data)
                AIModel.OneAPI.name -> {
                    var uri = Db.get().settingDao().query(Setting.AI_ONE_API_URI)?.value ?: ""
                    if (!uri.contains("v1/chat/completions")) {
                        uri = "$uri/v1/chat/completions"
                    }
                    val model = Db.get().settingDao().query(Setting.AI_ONE_API_MODEL)?.value ?: ""
                    OneAPI(uri, model).request(data)
                }

                else -> {
                    null
                }
            }
        }.onFailure {
            Server.log(it)
        }.getOrNull()
        Server.log("AI($aiModel) 响应: ${billInfoModel}")
        if (billInfoModel == null) return null
        if (billInfoModel.money == 0.0 || billInfoModel.accountNameFrom.isEmpty()) {
            return null
        }
        if (billInfoModel.money < 0) {
            billInfoModel.money = -billInfoModel.money
        }
        billInfoModel.ruleName = "$aiModel 生成"
        billInfoModel.state = BillState.Wait2Edit
        billInfoModel.app = app
        return billInfoModel
    }

    private suspend fun sync2Book(context: android.content.Context) {
        val packageName = Db.get().settingDao().query(Setting.BOOK_APP_ID)?.value ?: return
        val syncType =
            Db.get().settingDao().query(Setting.SYNC_TYPE)?.value ?: SyncType.WhenOpenApp.name

        if (syncType == SyncType.WhenOpenApp.name) {
            return
        }

        val wait = Db.get().billInfoDao().queryNoSync()

        if (wait.isEmpty()) {
            Server.log("No need to sync")
            return
        }

        if ((syncType == SyncType.BillsLimit10.name && wait.size >= 10) || (syncType == SyncType.BillsLimit5.name && wait.size >= 5)) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                context.startActivity(launchIntent)
            }
        }

    }

    /**
     * 解析账单信息
     * @param result 解析结果
     * @param app 应用
     * @param dataType 数据类型
     * @return 账单信息
     */
    private suspend fun parseBillInfo(
        result: String,
        app: String,
        dataType: DataType
    ): BillInfoModel {
        val json = Gson().fromJson(result, JsonObject::class.java)

        val money = json.get("money")?.asDouble ?: 0.0
        val fee = json.get("fee")?.asDouble ?: 0.0
        val shopName = json.get("shopName")?.asString ?: ""
        val shopItem = json.get("shopItem")?.asString ?: ""
        val accountNameFrom = json.get("accountNameFrom")?.asString ?: ""
        val accountNameTo = json.get("accountNameTo")?.asString ?: ""
        val currency = json.get("currency")?.asString ?: ""
        val time = json.get("time")?.asLong ?: System.currentTimeMillis()
        val channel = json.get("channel")?.asString ?: ""
        val ruleName = json.get("ruleName")?.asString ?: ""
        val type = json.get("type")?.asString ?: "Expend"
        // 根据ruleName判断是否需要自动记录
        val rule = Db.get().ruleDao().query(dataType.name, app, ruleName)
        val autoRecord = rule != null && rule.autoRecord


        val billInfoModel = BillInfoModel()
        billInfoModel.type = BillType.valueOf(type)
        billInfoModel.app = app
        billInfoModel.time = time
        billInfoModel.money = money
        billInfoModel.fee = fee
        billInfoModel.shopName = shopName
        billInfoModel.shopItem = shopItem
        billInfoModel.accountNameFrom = accountNameFrom
        billInfoModel.accountNameTo = accountNameTo
        billInfoModel.currency = currency
        billInfoModel.channel = channel
        billInfoModel.ruleName = ruleName
        billInfoModel.auto = autoRecord

        return billInfoModel
    }

    /**
     * 启动自动记账面板
     */
    private suspend fun startAutoPanel(billInfoModel: BillInfoModel, parent: BillInfoModel?, showInLandScape: Boolean = false) {

        // 判断当前手机是否为横屏状态
        if (!showInLandScape && context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
           Toast.makeText(context, "账单金额：${billInfoModel.money}，横屏状态下为您自动暂存。", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = FloatingIntent(billInfoModel, true, "JsRoute", parent).toIntent()
        Server.log("拉起自动记账悬浮窗口：$intent")
        try {
            context.startActivity(intent)
        } catch (t: Throwable) {
            Server.log("自动记账悬浮窗拉起失败：$t")
            Server.log(t)
        }
        /* if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
             context.startForegroundService(intent)
         } else {
             context.startService(intent)
         }*/
    }


    /**
     * 运行js代码
     */
    private suspend fun runJS(jsCode: String, data: String): String {
        try {
            val quickJS = QuickJS.Builder().build()
            val runtime = quickJS.createJSRuntime()
            val context = runtime.createJSContext()

            val stringBuilder = StringBuilder()
            val print = context.createJSFunction { itemContext, args ->
                for (i in args.indices) {
                    val arg = args[i].cast(JSString::class.java).string
                    stringBuilder.append(arg).append(" ")
                }
                itemContext.createJSUndefined()
            }
            Server.log("执行Js: $jsCode")
            Server.log("执行数据: $data")
            context.globalObject.setProperty("print", print)
            context.globalObject.setProperty("data", context.createJSString(data))
            context.globalObject.setProperty(
                "currentTime",
                context.createJSString(
                    android.text.format.DateFormat.format(
                        "HH:mm",
                        System.currentTimeMillis()
                    ).toString()
                )
            )
            var result = context.evaluate(jsCode, "fibonacci.js", String::class.java)
            if (stringBuilder.isNotEmpty()) {
                result = stringBuilder.toString()
            }
            Server.log("执行结果:$result")
            context.close()
            runtime.close()
            return result ?: ""
        } catch (e: Throwable) {
            Server.log(e)
            return ""
        }
    }

    suspend fun run(): ResultModel {
        val js = session.receiveText()
        val result = runJS(js, "")
        return ResultModel(200, "OK", result)
    }


}