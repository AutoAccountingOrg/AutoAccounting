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

package org.ezbook.server.tools

import android.content.Context
import android.content.res.Configuration
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ezbook.server.Server
import org.ezbook.server.ai.tools.BillTool
import org.ezbook.server.ai.tools.CategoryTool
import org.ezbook.server.constant.BillState
import org.ezbook.server.constant.BillType
import org.ezbook.server.constant.DataType
import org.ezbook.server.db.AppDatabase
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.AppDataModel
import org.ezbook.server.db.model.BillInfoModel
import org.ezbook.server.db.model.CurrencyModel
import org.ezbook.server.engine.JsExecutor
import org.ezbook.server.engine.RuleGenerator
import org.ezbook.server.intent.BillInfoIntent
import org.ezbook.server.models.BillResultModel
import org.ezbook.server.models.ResultModel
import org.ezbook.server.server.AnalysisParams
import org.ezbook.server.server.resolveBookByNameOrDefault
import java.io.Closeable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.ezbook.server.log.ServerLog


/**
 * 账单核心业务服务类
 *
 * 该类封装了账单分析、处理和管理的核心业务逻辑，包括：
 * - 基于规则引擎的账单分析
 * - 基于AI的账单分析
 * - 账单分类和处理
 * - JavaScript代码执行
 *
 * 所有与数据库、AI、JS引擎交互的细节都被隐藏在这里，
 * 使得Route层能够保持轻量和简洁。
 *
 * @param db 数据库实例，用于账单数据的存储和查询
 * @param ruleGenerator 规则生成器实例，用于生成JavaScript规则代码
 * @param jsExecutor JavaScript执行器实例，用于执行规则代码
 */
class BillService(
    private val db: AppDatabase = Db.get(),
    private val ruleGenerator: RuleGenerator = RuleGenerator,
    private val jsExecutor: JsExecutor = JsExecutor()
) : Closeable {

    /**
     * 启动自动记账面板
     * @param billInfoModel 账单信息模型
     * @param parent 父账单信息
     */
    private suspend fun startAutoPanel(
        billInfoModel: BillInfoModel,
        parent: BillInfoModel?,
    ) {
        val dnd = SettingUtils.landscapeDnd()
        val isLandscape = isLandscapeMode()
        // 检查横屏状态并处理
        if (isLandscape && dnd) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    Server.application,
                    "账单金额：${billInfoModel.money}，横屏状态下为您自动暂存。",
                    Toast.LENGTH_SHORT
                ).show()
            }
            ServerLog.d("Landscape DND: auto-stashed, money=${billInfoModel.money}, app=${billInfoModel.app}")
            return
        }

        // 创建并启动悬浮窗（改为入队串行处理）
        launchFloatingWindow(billInfoModel, parent)
    }

    /**
     * 检查当前设备是否处于横屏模式
     * @return Boolean 如果是横屏返回true，否则返回false
     */
    private fun isLandscapeMode(): Boolean =
        Server.application.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE


    /**
     * 启动悬浮窗口来显示账单信息
     * @param billInfoModel 要显示的账单信息模型
     * @param parent 父账单信息，可能为null，用于关联相关账单
     * @throws SecurityException 如果应用没有必要的权限
     */
    private suspend fun launchFloatingWindow(billInfoModel: BillInfoModel, parent: BillInfoModel?) {
        val intent = BillInfoIntent(billInfoModel, "JsRoute", parent)
        // 入队，由全局主线程消费者串行拉起，确保队列式显示
        floatingIntentChannel.send(intent)
    }

    /**
     * 分析账单数据的主要入口方法
     *
     * 根据参数配置，可以选择使用规则引擎或AI进行账单分析。
     * 如果规则分析失败，会自动回退到AI分析。
     * 分析完成后会进行分类处理和后续业务逻辑处理。
     *
     * @param analysisParams 分析参数，包含应用名、数据类型、是否使用AI等配置
     * @param context Android上下文，用于访问系统资源
     * @return 分析结果，包含账单信息、父级账单和是否需要用户操作的标识
     */
    suspend fun analyze(
        analysisParams: AnalysisParams,
        context: Context
    ): ResultModel<BillResultModel> =
        withContext(Dispatchers.IO) {
            val dataType = runCatchingExceptCancel { DataType.valueOf(analysisParams.type) }
                .getOrElse {
                    ServerLog.w("Invalid data type: ${analysisParams.type}")
                    return@withContext ResultModel<BillResultModel>(
                        400,
                        "Type exception: ${analysisParams.type}",
                        null
                    )
                }

            // 2) 仅对外部数据做重复触发过滤（OCR/图片不参与，每次截图均视为新数据）
            val skipDedup = analysisParams.fromAppData || dataType == DataType.OCR
            if (!skipDedup) {
                val key = MD5HashTable.md5(analysisParams.data)
                if (hash.contains(key)) {
                    ServerLog.d("Duplicate analysis skipped, key=$key")
                    return@withContext ResultModel<BillResultModel>(400, "检测到重复触发分析", null)
                }
                hash.put(key)
            }
            // 3) 如有需要，先持久化原始数据
            val appDataModel: AppDataModel? = if (!analysisParams.fromAppData) {
                AppDataModel().apply {
                    data = analysisParams.data
                    app = analysisParams.app
                    type = dataType
                    time = System.currentTimeMillis()
                    image = analysisParams.image  // 必须在 insert 前设置，否则入库时 image 为空
                    id = Db.get().dataDao().insert(this)
                    ServerLog.d("AppData saved: id=$id, app=$app, type=$type")
                }
            } else null

            // 4) 分析：保持“先规则，后AI”的顺序
            val start = System.currentTimeMillis()
            val ruleMatchResult = analyzeWithRule(analysisParams.app, analysisParams.data, dataType)
            if (ruleMatchResult.matchedDisabled) {
                // 命中禁用规则：直接丢弃，避免触发 AI 与后续账单流程
                appDataModel?.let { model ->
                    model.match = true
                    model.rule = ruleMatchResult.ruleName
                    model.version = SettingUtils.ruleVersion()
                    Db.get().dataDao().update(model)
                    ServerLog.d("Disabled rule matched, discarded: id=${model.id}, rule=${model.rule}")
                }
                return@withContext ResultModel<BillResultModel>(
                    404,
                    "命中禁用规则，已忽略。",
                    null
                )
            }
            val (aiBill, aiError) = analyzeWithAI(
                analysisParams.app,
                analysisParams.data,
                dataType,
                analysisParams.image,
                analysisParams.manual
            )
            val billInfo: BillInfoModel = ruleMatchResult.billInfo ?: aiBill ?: run {
                ServerLog.w("No valid bill from rule or AI")
                return@withContext ResultModel<BillResultModel>(
                    404,
                    aiError ?: "未分析到有效账单。",
                    null
                )
            }
            ServerLog.d("Parsed bill: type=${billInfo.type}, money=${billInfo.money}, shop=${billInfo.shopName}")
            //这里也不加bookName, bookName在分类里面处理
            if (appDataModel != null) {
                appDataModel.version = SettingUtils.ruleVersion()
                Db.get().dataDao().update(appDataModel)
            }
            // 保存映射前的原始账户名，供编辑器"记住资产映射"使用
            billInfo.rawAccountNameFrom = billInfo.accountNameFrom
            billInfo.rawAccountNameTo = billInfo.accountNameTo
            // 设置资产映射
            AssetsMap().setAssetsMap(billInfo)
            // 先根据已有的信息进行分类
            categorize(billInfo, dataType)
            if (billInfo.remark.isEmpty()) {
                billInfo.remark = BillManager.getRemark(billInfo, context)
            }
            // 🔒 关键区间：账单入库+去重+分类+保存+拉起悬浮窗全流程串行执行
            // 防止并发竞态：确保账单处理的完整生命周期严格按序执行，避免悬浮窗乱序
            val parent = deduplicationMutex.withLock {
                // 如果不是来自应用数据，则保存到数据库（保存前统一金额 2 位小数）
                if (!analysisParams.fromAppData) {
                    billInfo.money = billInfo.money.roundAmount()
                    billInfo.fee = billInfo.fee.roundAmount()
                    billInfo.id = db.billInfoDao().insert(billInfo)
                    ServerLog.d("Bill saved: id=${billInfo.id}")
                }

                // 对账单类型进行检查，这里如果没有开启资产管理，是没有转账类型的

                // 自动去重处理（来自App的数据跳过去重）
                val parentBill = if (analysisParams.fromAppData) {
                    null
                } else {
                    // 直接调用去重逻辑，不需要任务队列
                    BillManager.groupBillInfo(billInfo)
                }

                // 确定最终要分类和保存的账单
                val finalBill = if (parentBill != null) {
                    parentBill.ruleName = formatParentBillRuleName()
                    ServerLog.d("Dedup: merged to parent id=${parentBill.id}")
                    parentBill
                } else {
                    billInfo
                }
                categorize(finalBill, dataType)
                if (finalBill.remark.isEmpty()) {
                    finalBill.remark = BillManager.getRemark(finalBill, context)
                }


                // 保存最终账单（包含分类、备注等完整信息，保存前统一金额 2 位小数）
                finalBill.money = finalBill.money.roundAmount()
                finalBill.fee = finalBill.fee.roundAmount()
                db.billInfoDao().update(finalBill)

                // 如果有父账单，需要额外更新子账单状态
                if (parentBill != null) {
                    // 确保子账单的groupId正确指向父账单（防御性编程，避免被覆盖）
                    billInfo.groupId = parentBill.id
                    billInfo.state = BillState.Edited
                    billInfo.money = billInfo.money.roundAmount()
                    billInfo.fee = billInfo.fee.roundAmount()
                    db.billInfoDao().update(billInfo)
                } else {
                    billInfo.state = BillState.Wait2Edit
                }
                // 拉起悬浮窗（仅外部数据）
                if (!analysisParams.fromAppData) startAutoPanel(billInfo, parentBill)

                // 返回父账单供后续使用
                parentBill
            }

            val cost = System.currentTimeMillis() - start
            ServerLog.d("Analysis done: ${cost}ms")

            appDataModel?.let {
                it.match = true
                it.rule = billInfo.ruleName
                it.version = ""
                Db.get().dataDao().update(it)
            }
            // 10) 返回
            ResultModel.ok(BillResultModel(billInfo, parent))
        }

    /**
     * 执行JavaScript代码
     *
     * @param code 要执行的JavaScript代码
     * @param data 传递给JavaScript的数据参数，默认为空字符串
     * @return JavaScript执行结果
     */
    suspend fun executeJs(code: String, data: String = ""): String = jsExecutor.run(code, data)

    // region --- 私有辅助方法 ---

    /**
     * 使用指定来源（系统/用户）的规则进行一次解析尝试
     * @param app 应用名称
     * @param data 原始数据
     * @param dataType 数据类型
     * @param creator 规则来源（system/user）
     * @return 解析成功返回账单，失败返回 null
     */
    private suspend fun analyzeWithCreator(
        app: String,
        data: String,
        dataType: DataType,
        creator: String,
        scope: RuleGenerator.RuleScope
    ): BillInfoModel? {
        val src = if ("system" == creator) "system" else "user"
        val js = ruleGenerator.data(app, dataType, creator, scope)
        if (js.isBlank()) {
            ServerLog.d("$src rule empty, skip")
            return null
        }
        var result = executeJs(js, data)
        var billInfo = parseBillInfo(result, app, dataType);
        if (billInfo == null && creator == "user") {
            result = executeJs(js, DataConvert.convert(data))
            billInfo = parseBillInfo(result, app, dataType);
        }

        return billInfo?.also {
            ServerLog.d("$src rule matched: type=${it.type}, money=${it.money}")
        }
    }

    /**
     * 使用规则引擎分析账单数据
     *
     * 通过规则生成器获取对应应用和数据类型的JavaScript规则代码，
     * 然后执行该代码来分析账单数据。
     *
     * @param app 应用名称
     * @param data 要分析的原始数据
     * @param dataType 数据类型（如短信、通知等）
     * @return 分析得到的账单信息，如果分析失败则返回null
     */
    private suspend fun analyzeWithRule(
        app: String,
        data: String,
        dataType: DataType
    ): RuleMatchResult {
        ServerLog.d("Rule analysis: data=${data.take(100)}...")
        //为了避免部分用户的错误规则影响自动记账整体规则的可用性，拆分成2部分处理
        // 优先使用用户规则，随后使用系统规则兜底
        for (creator in arrayOf("user", "system")) {
            analyzeWithCreator(app, data, dataType, creator, RuleGenerator.RuleScope.Enabled)
                ?.let { return RuleMatchResult(it, matchedDisabled = false) }
        }
        // 若开启“禁用规则参与匹配”，尝试禁用规则命中以规避 AI
        if (SettingUtils.ruleMatchIncludeDisabled()) {
            for (creator in arrayOf("user", "system")) {
                analyzeWithCreator(app, data, dataType, creator, RuleGenerator.RuleScope.Disabled)
                    ?.let { return RuleMatchResult(it, matchedDisabled = true) }
            }
        }
        ServerLog.d("No rule match from system or user")
        return RuleMatchResult(null, matchedDisabled = false)
    }

    /**
     * 规则匹配结果封装。
     * @param billInfo 匹配到的账单信息
     * @param matchedDisabled 是否命中禁用规则
     */
    private data class RuleMatchResult(
        val billInfo: BillInfoModel?,
        val matchedDisabled: Boolean
    ) {
        // 便捷输出规则名称，避免外层重复取空
        val ruleName: String = billInfo?.ruleName ?: ""
    }

    /**
     * 使用AI分析账单数据
     * 当 aiVisionRecognition 开启且 image 非空时，将图片直接发给大模型进行视觉识别。
     * @return 成功时返回 BillInfoModel，失败时返回 null 且 errorMsg 会反映 AI 错误信息
     */
    private suspend fun analyzeWithAI(
        app: String,
        data: String,
        dataType: DataType,
        image: String = "",
        manual: Boolean = false
    ): Pair<BillInfoModel?, String?> {
        if (!SettingUtils.featureAiAvailable()) {
            ServerLog.d("AI disabled, skip")
            return null to null
        }
        // 手动触发时跳过 AI 识别开关检查，用户主动发起的请求应尽力返回结果
        if (!manual && !SettingUtils.aiBillRecognition()) {
            ServerLog.d("AI bill recognition disabled, skip")
            return null to null
        }
        val useVision = SettingUtils.aiVisionRecognition() && image.isNotBlank()
        ServerLog.d("AI analyzing, useVision=$useVision")
        val result = BillTool().execute(data, app, dataType, if (useVision) image else "")
        return result.fold(
            onSuccess = { bill ->
                if (bill == null) {
                    ServerLog.d("AI returned no valid bill")
                    null to null
                } else {
                    bill.apply {
                        ruleName = "${SettingUtils.apiProvider()} 生成"
                        state = BillState.Wait2Edit
                        this.app = app
                        ServerLog.d("AI parsed: type=$type, money=$money, shop=$shopName")
                    } to null
                }
            },
            onFailure = { e ->
                ServerLog.e("AI analysis failed: ${e.message}", e)
                null to (e.message ?: "Unknown error")
            }
        )
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
    ): BillInfoModel? {
        ServerLog.d("Parse result: ${result.take(80)}...")
        val json =
            runCatchingExceptCancel { Gson().fromJson(result, JsonObject::class.java) }.getOrNull()
                ?: run {
                    ServerLog.d("JSON parse failed, return null")
                    return null
                }
        // 使用安全的 JSON 访问扩展函数
        return BillInfoModel().apply {
            type = runCatchingExceptCancel {
                BillType.valueOf(json.safeGetString("type", "Expend"))
            }.getOrDefault(BillType.Expend)

            this.app = app
            time = json.safeGetLong("time", 0)
            val timeText = json.safeGetString("timeText", "")
            if (time == 0L) {
                ServerLog.d("Time=0, try parse from timeText")
                if (timeText.isEmpty()) {
                    time = System.currentTimeMillis()
                } else {
                    runCatchingExceptCancel {
                        time = DateUtils.toEpochMillis(timeText)
                    }.onFailure {
                        ServerLog.e("Time parse failed, use now", it)
                        time = System.currentTimeMillis()
                    }
                }
            }
            //  DateUtils.toEpochMillis(timeText)


            money = json.safeGetDouble("money", 0.0).roundAmount()
            fee = json.safeGetDouble("fee", 0.0).roundAmount()
            shopName = json.safeGetString("shopName")
            shopItem = json.safeGetString("shopItem")
            accountNameFrom = json.safeGetString("accountNameFrom")
            accountNameTo = json.safeGetString("accountNameTo")
            channel = json.safeGetString("channel")

            // 构造 CurrencyModel：获取币种代码并查询汇率
            val rawCurrency = json.safeGetString("currency").uppercase().ifEmpty { "CNY" }
            val multiCurrency = SettingUtils.featureMultiCurrency()
            val baseCurrency = SettingUtils.baseCurrency()
            currency = if (multiCurrency && rawCurrency != baseCurrency) {
                // 多币种启用且币种不同，获取汇率
                CurrencyService.buildCurrencyModel(rawCurrency, baseCurrency).toJson()
            } else {
                // 未开启多币种或同币种，直接构造默认模型
                CurrencyModel(
                    code = rawCurrency,
                    baseCurrency = baseCurrency,
                    rate = 1.0,
                    timestamp = System.currentTimeMillis()
                ).toJson()
            }

            // 格式化规则名称 - 添加数据类型前缀
            val rawRuleName = json.safeGetString("ruleName")
            ruleName = formatRuleName(rawRuleName, dataType)
            
            cateName = json.safeGetString("cateName")
            // 这个地方不要带上bookName，因为这里的数据来源是JS生成的，Js里面不会输出bookName和cateName，但是AI会携带cateName
            if (!this.generateByAi()) {
                val rule = Db.get().ruleDao().query(dataType.name, app, rawRuleName)
                auto = rule?.autoRecord ?: false
                ServerLog.d("Rule matched: rule=$ruleName, auto=$auto")
            }
        }
    }

    /**
     * 对账单进行自动分类处理
     *
     * 如果账单需要重新分类，则使用分类规则引擎来确定账本和分类。
     * 分类规则基于账单的类型、金额、商家名称和商品信息。
     *
     * @param bill 需要分类的账单信息
     */
    private suspend fun categorize(bill: BillInfoModel, dataType: DataType) {

        val win = JsonObject().apply {
            addProperty("type", bill.type.name)
            addProperty("money", bill.money)
            addProperty("shopName", bill.shopName)
            addProperty("shopItem", bill.shopItem)
            // 注入格式化后的实际账单时间（24小时制：HH:mm），供分类规则使用
            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(bill.time))
            addProperty("time", timeStr)
            addProperty("ruleName", bill.ruleName)
        }

        val js = ruleGenerator.category()
        val categoryJson = runCatchingExceptCancel {
            Gson().fromJson(
                executeJs(js, win.toString()),
                JsonObject::class.java
            )
        }.getOrNull()
        ServerLog.d(
            "Category result: book=${
                categoryJson.safeGetStringNonBlank(
                    "book",
                    ""
                )
            }, cate=${categoryJson.safeGetStringNonBlank("category", "")}"
        )
        // 设置账本名称与分类（优先规则结果，否则默认值）
        // 将账本指针（如"默认账本"）解析为数据库中真实存在的账本名称
        val rawBookName = categoryJson.safeGetStringNonBlank("book", SettingUtils.bookName())
        bill.bookName = resolveBookByNameOrDefault(rawBookName).name
        bill.cateName = categoryJson.safeGetStringNonBlank("category", "其他")
        bill.remark = categoryJson.safeGetStringNonBlank("remark", "")
        // AI分类识别需要总开关和分类开关同时开启
        if (!bill.hasValidCategory() &&
            SettingUtils.featureAiAvailable() &&
            SettingUtils.aiCategoryRecognition()
        ) {
            bill.cateName = CategoryTool().execute(
                win.toString(),
                bill.app,
                dataType
            ).takeUnless { it.isNullOrEmpty() } ?: "其他"
            ServerLog.d("AI category: ${bill.cateName}")
        }

        // 设置分类映射、查找
        CategoryProcessor().setCategoryMap(bill)
    }

    /**
     * 格式化规则名称 - 添加数据类型前缀
     * @param rawRuleName 原始规则名称
     * @param dataType 数据类型
     * @return 格式化后的规则名称
     */
    private fun formatRuleName(rawRuleName: String, dataType: DataType): String {
        if (rawRuleName.isEmpty()) return rawRuleName

        val prefix = when (dataType) {
            DataType.NOTICE -> "通知"
            DataType.DATA -> "数据"
            DataType.OCR -> "OCR"
        }

        return "$prefix·$rawRuleName"
    }

    /**
     * 格式化父账单规则名称
     * @return 父账单专用的规则名称
     */
    private fun formatParentBillRuleName(): String = "由多个账单合并生成"

    /**
     * 关闭服务，释放资源
     *
     * 主要是关闭JavaScript执行器以释放相关资源
     */
    override fun close() {
        jsExecutor.close()
    }

    // endregion

    companion object {
        private val hash = MD5HashTable(300_000)

        /**
         * 去重锁：确保账单入库和去重查询串行执行，避免并发竞态
         *
         * 并发场景下的问题：
         * - 账单A入库 → 查询重复 → 没找到
         * - 账单B入库 → 查询重复 → 找到A
         * - 结果：A和B应该去重但A先入库时还找不到B
         *
         * 解决方案：用锁保护"入库+去重查询"这个关键区间
         */
        private val deduplicationMutex = Mutex()

        /**
         * 悬浮窗启动全局队列：确保多次触发时严格按序执行，避免并发拉起
         */
        private val floatingIntentChannel = Channel<BillInfoIntent>(Channel.UNLIMITED)
        private val floatingIntentScope =
            CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

        private var lastStart = 0L
        init {
            // 在主线程上顺序消费队列，保证顺序与 UI 线程安全
            floatingIntentScope.launch {
                for (intent in floatingIntentChannel) {

                    runCatchingExceptCancel {
                        Server.application.startActivity(intent.toIntent())
                    }.onFailure { throwable ->
                        ServerLog.e("Floating window failed: $throwable", throwable)
                    }
                    if (System.currentTimeMillis() - lastStart <= 300) {
                        delay(300)
                    }
                    lastStart = System.currentTimeMillis()
                }
            }
        }
    }
}

