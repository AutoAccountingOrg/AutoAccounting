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


import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.shiqi.quickjs.JSString
import com.shiqi.quickjs.QuickJS
import io.ktor.application.ApplicationCall
import io.ktor.http.Parameters
import io.ktor.request.receiveText
import org.ezbook.server.Server
import org.ezbook.server.ai.AiManager
import org.ezbook.server.ai.tools.BillTool
import org.ezbook.server.ai.tools.CategoryTool
import org.ezbook.server.constant.BillState
import org.ezbook.server.constant.BillType
import org.ezbook.server.constant.DataType
import org.ezbook.server.constant.DefaultData
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.AppDataModel
import org.ezbook.server.db.model.BillInfoModel
import org.ezbook.server.engine.RuleGenerator
import org.ezbook.server.models.BillResultModel
import org.ezbook.server.models.ResultModel
import org.ezbook.server.tools.Assets
import org.ezbook.server.tools.Bill
import org.ezbook.server.tools.Category


class JsRoute(private val session: ApplicationCall, private val context: Context) {
    private val params: Parameters = session.request.queryParameters




    /**
     * 主函数，负责账单分析的整体流程。
     * 包括接收参数、数据解析、分析、分类处理以及最终结果的返回。
     * @return ResultModel 包含操作结果的模型
     */
    suspend fun analysis(): ResultModel {
        Server.logD("Analysis: init")
        val app = params["app"] ?: ""
        val type = params["type"] ?: ""
        val fromAppData = params["fromAppData"]?.toBoolean() ?: false
        val ai = params["ai"] == "true"
        val data = session.receiveText()

        // 解析字符串为枚举类型 DataType
        val dataType: DataType =
            parseDataType(type) ?: return ResultModel(400, "Type exception: $type")

        Server.logD("账单分析: app=$app ,type=$type ,fromAppData=$fromAppData ,ai=$ai")

        val appDataModel = createAppDataModel(app, data, dataType, fromAppData)

        val t = System.currentTimeMillis()

        // 初始化 BillInfoModel
        val billInfoModel = if (ai) {
            analyzeWithAI(app, data)
        } else {
            analyzeWithoutAI(app, data, dataType, fromAppData)
        }

        if (billInfoModel == null) {
            return ResultModel(404, "未分析到有效账单（FromAi=${ai}）。")
        }

        // 分类账单信息
        categorizeBill(billInfoModel)

        val total = System.currentTimeMillis() - t
        Server.log("识别用时: $total ms")

        // 处理账单信息
        var pair = processBillInfo(billInfoModel, fromAppData)



        if (!fromAppData) {

            if (billInfoModel.auto) {
                billInfoModel.state = BillState.Edited
            }

            if (pair.second) {
                billInfoModel.state = BillState.Wait2Edit

            }
            Db.get().billInfoDao().update(billInfoModel)
            // 更新 AppData 数据
            updateAppDataModel(appDataModel, billInfoModel)


            if (Server.debug) {
                debugIntent(billInfoModel, false, app, pair.first).let {
                    context.startActivity(it)
                }
            }
        }


        return ResultModel(
            200, "OK", BillResultModel(
                billInfoModel,
                pair.first,
                pair.second
            )
        )
    }


    private fun debugIntent(
        billInfoModel: BillInfoModel,
        showTip: Boolean,
        from: String,
        parent: BillInfoModel?
    ): Intent {
        val intent = Intent()
        intent.putExtra("billInfo", Gson().toJson(billInfoModel))
        intent.putExtra("id", billInfoModel.id)
        intent.putExtra("showWaitTip", showTip)
        intent.putExtra("t", System.currentTimeMillis())
        intent.putExtra("intentType", "FloatingIntent")
        if (parent != null) {
            intent.putExtra("parent", Gson().toJson(parent))
        }
        intent.putExtra("from", from)
        intent.setComponent(
            ComponentName(
                Server.packageName,
                "net.ankio.auto.ui.activity.FloatingWindowTriggerActivity"
            )
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        return intent
    }

    /**
     * 解析字符串类型为枚举类型 DataType。
     * @param type 字符串表示的类型
     * @return DataType 或 null（解析失败）
     */
    private fun parseDataType(type: String): DataType? {
        return try {
            DataType.valueOf(type)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    /**
     * 创建 AppDataModel 对象，并根据条件插入数据库。
     * @param app 应用标识
     * @param data 数据内容
     * @param dataType 数据类型
     * @param fromAppData 是否从 App 数据中获取
     * @return AppDataModel 实例
     */
    private suspend fun createAppDataModel(
        app: String,
        data: String,
        dataType: DataType,
        fromAppData: Boolean
    ): AppDataModel {
        val appDataModel = AppDataModel().apply {
            this.app = app
            this.data = data
            this.type = dataType
            this.time = System.currentTimeMillis()
        }
        if (!fromAppData) {
            appDataModel.id = Db.get().dataDao().insert(appDataModel)
        }
        return appDataModel
    }

    /**
     * 不使用 AI 的情况下进行账单分析。
     * 通过规则引擎解析数据或切换到 AI 分析。
     * @param app 应用标识
     * @param data 数据内容
     * @param dataType 数据类型
     * @param fromAppData 是否从 App 数据中获取
     * @return BillInfoModel 或 null
     */
    private suspend fun analyzeWithoutAI(
        app: String,
        data: String,
        dataType: DataType,
        fromAppData: Boolean
    ): BillInfoModel? {
        val js = RuleGenerator.data(app, dataType)
        if (js.isEmpty()) {
            return null
        }
        val result = runJS(js, data)
        if (result.isEmpty()) {
            if (shouldUseAI() && !fromAppData) {
                return analyzeWithAI(app, data)
            }
            return null
        }
        return parseBillInfo(result, app, dataType)
    }

    /**
     * 判断是否应该使用 AI 进行账单分析。
     * @return Boolean 表示是否使用 AI
     */
    private suspend fun shouldUseAI(): Boolean {
        val useAI = Db.get().settingDao().query(Setting.USE_AI)?.value == "true"
        return useAI && Db.get().settingDao().query(Setting.AI_AUXILIARY)?.value == "true"
    }

    /**
     * 使用 AI 分析账单数据。
     * @param app 应用标识
     * @param data 数据内容
     * @return BillInfoModel 或 null
     */
    private suspend fun analyzeWithAI(app: String, data: String): BillInfoModel? {
        return parseBillInfoFromAi(app, data)
    }

    /**
     * 对账单信息进行分类。
     * 确保账单有明确的类别和账本信息。
     * @param billInfoModel 账单信息模型
     */
    private suspend fun categorizeBill(billInfoModel: BillInfoModel) {
        if (!billInfoModel.needReCategory()) return;
        val time = android.text.format.DateFormat.format("HH:mm", billInfoModel.time).toString()
        val categoryJS = RuleGenerator.category()
        val win = JsonObject().apply {
            addProperty("type", billInfoModel.type.name)
            addProperty("money", billInfoModel.money)
            addProperty("shopName", billInfoModel.shopName)
            addProperty("shopItem", billInfoModel.shopItem)
            addProperty("ruleName", billInfoModel.ruleName)
            addProperty("time", time)
        }
        val categoryResult = runCatching {
            runJS(categoryJS, Gson().toJson(win))
        }.getOrDefault("{ book: '默认账本', category: '其他' }")

        val categoryJson = runCatching {
            Gson().fromJson(categoryResult, JsonObject::class.java)
        }.getOrNull()

        billInfoModel.bookName = categoryJson?.get("book")?.asString ?: "默认账本"
        billInfoModel.cateName = categoryJson?.get("category")?.asString ?: "其他"

        // 如果分类结果为"其他"或"其它"，尝试使用AI进行分类
        if (billInfoModel.needReCategory()) {
            val ai = Db.get().settingDao().query(Setting.USE_AI)?.value == "true"
            val useAi =
                Db.get().settingDao().query(Setting.USE_AI_FOR_CATEGORIZATION)?.value == "true"
            if (ai && useAi) {
                val aiCategory = requestAiCategory(billInfoModel)
                billInfoModel.cateName = aiCategory
            }
        }
    }

    /**
     * 处理账单信息。
     * 包括资产映射、分类映射、时间修正和任务调度。
     * @param billInfoModel 账单信息模型
     * @param fromAppData 是否从 App 数据中获取
     */
    private suspend fun processBillInfo(
        billInfoModel: BillInfoModel,
        fromAppData: Boolean
    ): Pair<BillInfoModel?, Boolean> {
        var needUserAction = Assets.setAssetsMap(billInfoModel)
        Category.setCategoryMap(billInfoModel)
        billInfoModel.remark = Bill.getRemark(billInfoModel, context)
        Bill.setBookName(billInfoModel)

        if (!fromAppData) {
            billInfoModel.id = Db.get().billInfoDao().insert(billInfoModel)
        }

        if (billInfoModel.time < 946656000000) {
            billInfoModel.time = System.currentTimeMillis()
        }

        val task = Server.billProcessor.addTask(billInfoModel, context)
        task.await()
        billInfoModel.state = if (task.result == null) BillState.Wait2Edit else BillState.Edited
        val parent = task.result
        val ignoreAsset = Db.get().settingDao().query(Setting.IGNORE_ASSET)?.value == "true"
        if (ignoreAsset && needUserAction) {
            needUserAction = false
        }
        Db.get().billInfoDao().update(billInfoModel)
        return Pair(parent, needUserAction)
    }


    /**
     * 更新 AppDataModel 的匹配状态和规则版本信息。
     * 并存入数据库。
     * @param appDataModel 应用数据模型
     * @param billInfoModel 账单信息模型
     */
    private suspend fun updateAppDataModel(
        appDataModel: AppDataModel,
        billInfoModel: BillInfoModel
    ) {
        appDataModel.match = true
        appDataModel.rule = billInfoModel.ruleName
        appDataModel.version = Db.get().settingDao().query(Setting.RULE_VERSION)?.value ?: ""
        Db.get().dataDao().update(appDataModel)
    }


    /**
     * 使用AI模型解析账单信息
     * @param app 应用名称
     * @param data 待解析的数据
     * @return 解析后的账单信息模型，解析失败返回null
     */
    private suspend fun parseBillInfoFromAi(app: String, data: String): BillInfoModel? {
        // 获取AI模型配置
        val aiModel = AiManager.getInstance().getCurrentModel()

        // 请求AI模型解析数据
        val billInfoModel = requestAiAnalysis(data)
        Server.log("AI($aiModel) 响应: $billInfoModel")

        // 验证并处理解析结果
        return billInfoModel?.let { model ->
            if (!isValidBillInfo(model)) {
                return null
            }

            processBillInfo(model, aiModel, app)
        }
    }

    /**
     * 获取配置的AI模型
     */
    private suspend fun getAiModel(): String =
        Db.get().settingDao().query(Setting.AI_MODEL)?.value ?: DefaultData.AI_MODEL

    private suspend fun requestAiCategory(billInfoModel: BillInfoModel): String {

        val json = Gson().toJson(
            mapOf(
                "shopName" to billInfoModel.shopName,
                "shopItem" to billInfoModel.shopItem,
                "ruleName" to billInfoModel.ruleName
            )
        )

        return CategoryTool().execute(json) ?: "其他"
    }


    /**
     * 请求AI模型解析数据
     */
    private suspend fun requestAiAnalysis(data: String): BillInfoModel? =
        runCatching {
            Gson().fromJson(BillTool().execute(data), BillInfoModel::class.java)
        }.onFailure {
            Server.log(it)
        }.getOrNull()


    /**
     * 验证账单信息是否有效
     */
    private fun isValidBillInfo(billInfo: BillInfoModel): Boolean =
        billInfo.money != 0.0 && billInfo.accountNameFrom.isNotEmpty()

    /**
     * 处理账单信息
     */
    private fun processBillInfo(
        billInfo: BillInfoModel,
        aiModel: String,
        app: String
    ): BillInfoModel = billInfo.apply {
        // 处理负数金额
        if (money < 0) {
            money = -money
        }
        ruleName = "$aiModel 生成"
        state = BillState.Wait2Edit
        this.app = app
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

        // 使用安全调用运算符和默认值
        return BillInfoModel().apply {
            type = runCatching {
                BillType.valueOf(json.get("type")?.asString ?: "Expend")
            }.getOrDefault(BillType.Expend)

            this.app = app
            time = json.get("time")?.asLong ?: System.currentTimeMillis()
            money = json.get("money")?.asDouble ?: 0.0
            fee = json.get("fee")?.asDouble ?: 0.0
            shopName = json.get("shopName")?.asString.orEmpty()
            shopItem = json.get("shopItem")?.asString.orEmpty()
            accountNameFrom = json.get("accountNameFrom")?.asString.orEmpty()
            accountNameTo = json.get("accountNameTo")?.asString.orEmpty()
            currency = json.get("currency")?.asString.orEmpty()
            channel = json.get("channel")?.asString.orEmpty()
            ruleName = json.get("ruleName")?.asString.orEmpty()

            // 根据ruleName判断是否需要自动记录
            val rule = Db.get().ruleDao().query(dataType.name, app, ruleName)
            auto = rule?.autoRecord ?: false
        }
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
            Server.logD("执行Js: $jsCode")
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