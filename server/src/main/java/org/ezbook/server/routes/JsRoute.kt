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


import android.content.Context
import android.content.Intent
import android.content.res.Configuration
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
import org.ezbook.server.constant.BillAction
import org.ezbook.server.constant.BillState
import org.ezbook.server.constant.BillType
import org.ezbook.server.constant.DataType
import org.ezbook.server.constant.DefaultData
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
            handleUserNotification(billInfoModel, pair.first, pair.second)

            // 更新 AppData 数据
            updateAppDataModel(appDataModel, billInfoModel)
        }

        return ResultModel(200, "OK", billInfoModel)
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
        return (Db.get().settingDao().query(Setting.AI_AUXILIARY)?.value ?: "false") == "true"
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
        if (billInfoModel.cateName.isEmpty() || billInfoModel.cateName == "其它" || billInfoModel.cateName == "其他") {
            val time = android.text.format.DateFormat.format("HH:mm", billInfoModel.time).toString()
            val categoryJS = RuleGenerator.category()
            val win = JsonObject().apply {
                addProperty("type", billInfoModel.type.name)
                addProperty("money", billInfoModel.money)
                addProperty("shopName", billInfoModel.shopName)
                addProperty("shopItem", billInfoModel.shopItem)
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
     * 处理用户通知逻辑。
     * 决定是否弹出提示或执行其他与用户相关的操作。
     * @param billInfoModel 账单信息模型
     */
    private suspend fun handleUserNotification(
        billInfoModel: BillInfoModel,
        parent: BillInfoModel?,
        userAction: Boolean
    ) {
        if (!billInfoModel.auto) {
            val showInLandScape =
                Db.get().settingDao().query(Setting.LANDSCAPE_DND)?.value != "false"
            withContext(Dispatchers.Main) {
                runCatching {
                    startAutoPanel(billInfoModel, parent, showInLandScape)
                }
            }
        } else {
            if (userAction) {
                billInfoModel.state = BillState.Wait2Edit
                Db.get().billInfoDao().update(billInfoModel)
            }
            sync2Book(context)
            val showTip = Db.get().settingDao().query(Setting.SHOW_AUTO_BILL_TIP)?.value == "true"
            if (showTip) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "已自动记录账单(￥${billInfoModel.money})。",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
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
        val aiModel = getAiModel()

        // 请求AI模型解析数据
        val billInfoModel = requestAiAnalysis(aiModel, data)
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
        Db.get().settingDao().query(Setting.AI_MODEL)?.value ?: AIModel.Gemini.name

    /**
     * 请求AI模型解析数据
     */
    private suspend fun requestAiAnalysis(aiModel: String, data: String): BillInfoModel? =
        runCatching {
            when (aiModel) {
                AIModel.Gemini.name -> Gemini().request(data)
                AIModel.QWen.name -> QWen().request(data)
                AIModel.DeepSeek.name -> DeepSeek().request(data)
                AIModel.ChatGPT.name -> ChatGPT().request(data)
                AIModel.OneAPI.name -> handleOneApiRequest(data)
                else -> null
            }
        }.onFailure {
            Server.log(it)
        }.getOrNull()

    /**
     * 处理OneAPI请求
     */
    private suspend fun handleOneApiRequest(data: String): BillInfoModel? {
        var uri = Db.get().settingDao().query(Setting.AI_ONE_API_URI)?.value.orEmpty()
        if (!uri.contains("v1/chat/completions")) {
            uri = "$uri/v1/chat/completions"
        }
        val model = Db.get().settingDao().query(Setting.AI_ONE_API_MODEL)?.value.orEmpty()
        return OneAPI(uri, model).request(data)
    }

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
     * 同步账单到记账应用
     * @param context 应用上下文
     */
    private suspend fun sync2Book(context: Context) {
        // 获取记账应用包名，如果未设置则直接返回
        val packageName = Db.get().settingDao().query(Setting.BOOK_APP_ID)?.value ?: run {
            Server.log("未设置记账应用")
            return
        }

        // 获取同步类型设置
        val syncType = getSyncType()

        // 如果是打开应用时同步，则直接返回
        if (syncType == SyncType.WhenOpenApp.name) {
            Server.log("设置为打开应用时同步")
            return
        }

        // 获取待同步的账单
        val pendingBills = Db.get().billInfoDao().queryNoSync()
        if (pendingBills.isEmpty()) {
            Server.log("无需同步：没有待同步账单")
            return
        }

        // 检查是否需要启动同步
        if (shouldStartSync(syncType, pendingBills.size)) {
            launchBookApp(context, packageName)
        }
    }

    /**
     * 获取同步类型设置
     * @return 同步类型，默认为打开应用时同步
     */
    private suspend fun getSyncType(): String =
        Db.get().settingDao().query(Setting.SYNC_TYPE)?.value ?: SyncType.WhenOpenApp.name

    /**
     * 检查是否应该开始同步
     * @param syncType 同步类型
     * @param pendingCount 待同步账单数量
     * @return 是否应该开始同步
     */
    private fun shouldStartSync(syncType: String, pendingCount: Int): Boolean =
        (syncType == SyncType.BillsLimit10.name && pendingCount >= 10) ||
                (syncType == SyncType.BillsLimit5.name && pendingCount >= 5) ||
                (syncType == SyncType.BillsLimit1.name && pendingCount >= 1)

    /**
     * 启动记账应用
     * @param context 应用上下文
     * @param packageName 记账应用包名
     */
    private suspend fun launchBookApp(context: Context, packageName: String) {
        runCatching {

            var activityName =
                Db.get().settingDao().query(Setting.BOOK_APP_ACTIVITY)?.value
                    ?: DefaultData.BOOK_APP_ACTIVITY

            if (activityName.isEmpty()) {
                activityName = DefaultData.BOOK_APP_ACTIVITY
            }

            if (activityName == DefaultData.BOOK_APP_ACTIVITY && packageName !== DefaultData.BOOK_APP) {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                }
                return
            }
            // val launchIntent = app.packageManager.getLaunchIntentForPackage(packageName)
            val intent = Intent().apply {
                setClassName(packageName, activityName) // 设置目标应用和目标 Activity
                putExtra("from", Server.packageName) // 添加额外参数
                putExtra("action", BillAction.SYNC_BILL) // 传递 action
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // 确保在新任务栈中启动
            }
            context.startActivity(intent)
        }.onFailure { error ->
            Server.log("启动记账应用失败：${error.message}")
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
     * 启动自动记账面板
     * @param billInfoModel 账单信息模型
     * @param parent 父账单信息
     * @param showInLandScape 是否在横屏状态下显示
     */
    private suspend fun startAutoPanel(
        billInfoModel: BillInfoModel,
        parent: BillInfoModel?,
        showInLandScape: Boolean = false
    ) {
        // 检查横屏状态并处理
        if (isLandscapeMode() && !showInLandScape) {
            showToastForLandscapeMode(billInfoModel.money)
            return
        }

        // 创建并启动悬浮窗
        launchFloatingWindow(billInfoModel, parent)
    }

    /**
     * 检查当前设备是否处于横屏模式
     * @return Boolean 如果是横屏返回true，否则返回false
     */
    private fun isLandscapeMode(): Boolean =
        context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    /**
     * 在横屏模式下显示提示信息
     * @param money 账单金额，用于在提示信息中显示
     */
    private fun showToastForLandscapeMode(money: Double) {
        Toast.makeText(
            context,
            "账单金额：$money，横屏状态下为您自动暂存。",
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * 启动悬浮窗口来显示账单信息
     * @param billInfoModel 要显示的账单信息模型
     * @param parent 父账单信息，可能为null，用于关联相关账单
     * @throws SecurityException 如果应用没有必要的权限
     */
    private suspend fun launchFloatingWindow(billInfoModel: BillInfoModel, parent: BillInfoModel?) {
        val intent = FloatingIntent(billInfoModel, true, "JsRoute", parent).toIntent()
        Server.logD("拉起自动记账悬浮窗口：$intent")

        runCatching {
            context.startActivity(intent)
        }.onFailure { throwable ->
            Server.log("自动记账悬浮窗拉起失败：$throwable")
            Server.log(throwable)
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