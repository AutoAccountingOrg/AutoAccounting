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


import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.ezbook.server.Server
import org.ezbook.server.constant.BillState
import org.ezbook.server.constant.BillType
import org.ezbook.server.constant.DataType
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.AppDataModel
import org.ezbook.server.db.model.BillInfoModel
import org.ezbook.server.engine.RuleGenerator
import org.ezbook.server.tools.Assets
import org.ezbook.server.tools.Bill
import org.ezbook.server.tools.Category
import org.mozilla.javascript.BaseFunction
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.nanohttpd.protocols.http.IHTTPSession
import org.nanohttpd.protocols.http.response.Response


class JsRoute(private val session: IHTTPSession, private val context: android.content.Context) {
    /**
     * 获取设置
     */
    fun analysis(): Response {
        val params = session.parameters
        val app = params["app"]?.firstOrNull()?.toString() ?: ""
        val type = params["type"]?.firstOrNull()?.toString() ?: ""
        val fromAppData = params["fromAppData"]?.firstOrNull()?.toBoolean() ?: false
        val data = Server.reqData(session)
        //将string转换为枚举类型
        val dataType: DataType
        try {
            dataType = DataType.valueOf(type)
        } catch (e: IllegalArgumentException) {
            return Server.json(400, "Type exception: $type")
        }

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

        val js = RuleGenerator.data(app, dataType)


        if (js === "") {
            return Server.json(404, "JS无效，请更新规则。")
        }

        val result = runJS(js, data)

        if (result === "") {
            return Server.json(404, "未分析到有效账单。")
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

        var billInfoModel = parseBillInfo(result, app, dataType)


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


        val total = System.currentTimeMillis() - t
        // 识别用时
        Server.log("Time Usage: $total ms")

        //  资产映射
        Assets.setAssetsMap(billInfoModel)

        // 分类映射
        Category.setCategoryMap(billInfoModel)

        Bill.setRemark(billInfoModel, context)
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
        val parent = task.result
        if (parent == null){
            billInfoModel.state = BillState.Wait2Edit
        }else{
            billInfoModel.state = BillState.Edited
        }
        if (billInfoModel.auto){
            billInfoModel.state =  BillState.Edited
        }
        Db.get().billInfoDao().update(billInfoModel)
        Server.log("BillInfoModel: $billInfoModel")
        if (!fromAppData) {
            // 切换到主线程
            if(!billInfoModel.auto){
                Server.log("Auto record is off: $billInfoModel")
                Server.runOnMainThread {
                    startAutoPanel(billInfoModel, parent)
                }
            }

            // 更新AppData
            appDataModel.match = true
            appDataModel.rule = billInfoModel.ruleName
            appDataModel.version = Db.get().settingDao().query(Setting.RULE_VERSION)?.value ?: ""
            Db.get().dataDao().update(appDataModel)
        }
        return Server.json(200, "OK", billInfoModel)
    }

    /**
     * 解析账单信息
     * @param result 解析结果
     * @param app 应用
     * @param dataType 数据类型
     * @return 账单信息
     */
    private fun parseBillInfo(result: String, app: String, dataType: DataType): BillInfoModel {
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
        val rule = Db.get().ruleDao().query( dataType.name,app, ruleName)
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
    private fun startAutoPanel(billInfoModel: BillInfoModel, parent: BillInfoModel?) {
        val intent = Intent()
        intent.putExtra("billInfo", Gson().toJson(billInfoModel))
        intent.putExtra("id", billInfoModel.id)
        intent.putExtra("showWaitTip", true)
        if (parent != null) {
            intent.putExtra("parent", Gson().toJson(parent))
        }
        intent.putExtra("from","JsRoute")
        intent.setComponent(
            ComponentName(
                "net.ankio.auto.xposed",
                "net.ankio.auto.service.FloatingWindowService"
            )
        )

        Server.log("Calling auto server：$intent")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    class CustomPrintFunction(private val output: StringBuilder) : BaseFunction() {
        override fun call(
            cx: Context,
            scope: Scriptable,
            thisObj: Scriptable,
            args: Array<Any>
        ): Any {
            args.forEach { arg ->
                output.append(Context.toString(arg)).append("\n")
            }
            return Context.getUndefinedValue()
        }
    }

    /**
     * 运行js代码
     */
    private fun runJS(jsCode: String, data: String): String {
        val rhino: Context = Context.enter()
        rhino.setOptimizationLevel(-1)
        val outputBuilder = StringBuilder()
        var result: Any? = null
        try {
            Server.log("RunJs: $jsCode")
            Server.log("RunData: $data")
            val scope: Scriptable = rhino.initStandardObjects()
            val printFunction = CustomPrintFunction(outputBuilder)
            ScriptableObject.putProperty(scope, "print", printFunction)
            ScriptableObject.putProperty(scope, "data", data)
            ScriptableObject.putProperty(scope, "currentTime", android.text.format.DateFormat.format("HH:mm", System.currentTimeMillis()).toString())
            rhino.evaluateString(scope, jsCode, "JavaScript", 1, null)
            result = outputBuilder.toString()
        } catch (e: Throwable) {
            Server.log(e)
            result = e.message
        } finally {
            Context.exit()
            Server.log("RuleData: $result")
        }
        if (result != null) return result.toString()
        return ""
    }

    fun run(): Response {
        val js = Server.reqData(session)
        val result = runJS(js, "")
        return Server.json(200, "OK", result)
    }


}