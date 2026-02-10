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
            // 记录横屏免打扰触发，便于排查为何未拉起悬浮窗
            ServerLog.d("横屏免打扰开启，自动暂存账单并返回：money=${billInfoModel.money}, app=${billInfoModel.app}")
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
            ServerLog.d("==============开始执行账单分析===============")
            // 1) 校验数据类型
            val dataType = runCatchingExceptCancel { DataType.valueOf(analysisParams.type) }
                .getOrElse {
                    ServerLog.d("账单数据类型错误\n==============账单分析结束===============")
                    return@withContext ResultModel<BillResultModel>(
                        400,
                        "Type exception: ${analysisParams.type}",
                        null
                    )
                }

            // 2) 仅对外部数据做重复触发过滤
            val key = MD5HashTable.md5(analysisParams.data)
            if (!analysisParams.fromAppData && hash.contains(key)) {
                ServerLog.d("检测到重复触发分析(同一个数据)\n==============账单分析结束===============")
                return@withContext ResultModel<BillResultModel>(400, "检测到重复触发分析", null)
            }
            if (!analysisParams.fromAppData) hash.put(key)
            ServerLog.d("1. 分析初始化数据：$analysisParams")
            // 3) 如有需要，先持久化原始数据
            val appDataModel: AppDataModel? = if (!analysisParams.fromAppData) {
                AppDataModel().apply {
                    data = analysisParams.data
                    app = analysisParams.app
                    type = dataType
                    time = System.currentTimeMillis()
                    id = Db.get().dataDao().insert(this)
                    // 记录原始数据持久化的主键与摘要，方便追溯
                    ServerLog.d("原始数据持久化成功：id=$id, app=$app, type=$type")
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
                    // 记录禁用规则命中摘要，便于排查
                    ServerLog.d("命中禁用规则并丢弃：id=${model.id}, rule=${model.rule}")
                }
                ServerLog.d("命中禁用规则，已丢弃\n==============账单分析结束===============")
                return@withContext ResultModel<BillResultModel>(
                    404,
                    "命中禁用规则，已忽略。",
                    null
                )
            }
            val billInfo: BillInfoModel =
                ruleMatchResult.billInfo
                    ?: analyzeWithAI(
                        analysisParams.app,
                        analysisParams.data,
                        dataType
                    )
                    ?: run {
                        ServerLog.d("AI和规则的解析结果都为NULL\n==============账单分析结束===============")
                        return@withContext ResultModel<BillResultModel>(
                            404,
                            "未分析到有效账单。",
                            null
                        )
                    }
            ServerLog.d("初步解析的账单结果 $billInfo")
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
            // 记录资产映射摘要
            ServerLog.d("资产映射完成：from=${billInfo.accountNameFrom}, to=${billInfo.accountNameTo}")
            // 先根据已有的信息进行分类
            categorize(billInfo, dataType)
            if (billInfo.remark.isEmpty()) {
                billInfo.remark = BillManager.getRemark(billInfo, context)
            }
            // 🔒 关键区间：账单入库+去重+分类+保存+拉起悬浮窗全流程串行执行
            // 防止并发竞态：确保账单处理的完整生命周期严格按序执行，避免悬浮窗乱序
            val parent = deduplicationMutex.withLock {
                // 如果不是来自应用数据，则保存到数据库
                if (!analysisParams.fromAppData) {
                    billInfo.id = db.billInfoDao().insert(billInfo)
                    // 记录账单入库主键
                    ServerLog.d("账单入库成功：billId=${billInfo.id}")
                }

                // 对账单类型进行检查，这里如果没有开启资产管理，是没有转账类型的

                // 自动去重处理（来自App的数据跳过去重）
                val parentBill = if (analysisParams.fromAppData) {
                    ServerLog.d("来自App的数据，跳过去重处理")
                    null
                } else {
                    // 直接调用去重逻辑，不需要任务队列
                    BillManager.groupBillInfo(billInfo)
                }

                // 确定最终要分类和保存的账单
                val finalBill = if (parentBill != null) {
                    ServerLog.d("自动去重找到父账单：parentId=${parentBill.id}")
                    // 父账单设置特殊规则名称
                    parentBill.ruleName = formatParentBillRuleName()
                    ServerLog.d("使用父账单作为最终账单，准备重新分类")
                    parentBill
                } else {
                    ServerLog.d("自动去重未找到父账单，使用当前账单")
                    billInfo
                }
                // 根据合并后的账单重新分类
                categorize(finalBill, dataType)
                ServerLog.d("分类完成后的账单：$finalBill")

                // 生成账单备注（在分类之后，因为备注可能依赖分类信息）
                if (finalBill.remark.isEmpty()) {
                    finalBill.remark = BillManager.getRemark(finalBill, context)
                    ServerLog.d("备注生成完成：remark=${finalBill.remark}")
                }



                // 保存最终账单（包含分类、备注等完整信息）
                db.billInfoDao().update(finalBill)

                // 如果有父账单，需要额外更新子账单状态
                if (parentBill != null) {
                    // 确保子账单的groupId正确指向父账单（防御性编程，避免被覆盖）
                    billInfo.groupId = parentBill.id
                    billInfo.state = BillState.Edited
                    db.billInfoDao().update(billInfo)
                    ServerLog.d("子账单状态更新为已编辑：billId=${billInfo.id}, groupId=${billInfo.groupId}")
                } else {
                    // 无父账单，更新当前账单状态为等待编辑
                    billInfo.state = BillState.Wait2Edit
                }
                // 记录账单最终状态
                ServerLog.d("账单状态更新：state=${billInfo.state}")

                // 拉起悬浮窗（仅外部数据）
                if (!analysisParams.fromAppData) startAutoPanel(billInfo, parentBill)

                // 返回父账单供后续使用
                parentBill
            }

            // 7) 统计耗时
            val cost = System.currentTimeMillis() - start
            ServerLog.d("识别用时: $cost ms")

            // 8) 更新原始数据存档
            appDataModel?.let {
                it.match = true
                it.rule = billInfo.ruleName
                it.version = ""
                Db.get().dataDao().update(it)
                // 记录原始数据与规则的关联情况
                ServerLog.d("原始数据归档更新：id=${it.id}, match=${it.match}, rule=${it.rule}")
            }
            ServerLog.d("==============账单分析结束===============")
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
        val src = if ("system" == creator) "系统" else "用户"
        val js = ruleGenerator.data(app, dataType, creator, scope)
        if (js.isBlank()) {
            ServerLog.d("${src}规则数据为空，跳过")
            return null
        }
        var result = executeJs(js, data)
        var billInfo = parseBillInfo(result, app, dataType);
        if (billInfo == null && creator == "user") {
            result = executeJs(js, DataConvert.convert(data))
            billInfo = parseBillInfo(result, app, dataType);
        }

        return billInfo?.also {

            ServerLog.d("${src}规则解析成功：type=${it.type}, money=${it.money}")
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
        ServerLog.d("使用规则进行分析：$data")
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
        ServerLog.d("系统与用户规则均未解析出有效结果")
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
     *
     * 调用AI管理器的账单工具来分析数据，并将结果转换为账单信息对象。
     * 只有当分析结果有效时才会返回账单信息。
     *
     * @param app 应用名称
     * @param data 要分析的原始数据
     * @param dataType 数据来源类型
     * @return 分析得到的账单信息，如果分析失败则返回null
     */
    private suspend fun analyzeWithAI(
        app: String,
        data: String,
        dataType: DataType
    ): BillInfoModel? {

        // AI功能总开关关闭时，直接跳过AI分析
        if (!SettingUtils.featureAiAvailable()) {
            ServerLog.d("AI功能总开关关闭，跳过账单AI分析")
            return null
        }

        // AI识别账单开关关闭时不调用 AI
        if (!SettingUtils.aiBillRecognition()) {
            ServerLog.d("AI识别账单已关闭，跳过账单分析")
            return null
        }
        ServerLog.d("AI分析中，$data")
        val result = BillTool().execute(data, app, dataType) ?: run {
            // 记录AI未返回有效结果
            ServerLog.d("AI未返回有效账单结果")
            return null
        }
        return result.apply {
            // 设置AI分析的标识信息
            ruleName = "${SettingUtils.apiProvider()} 生成"
            state = BillState.Wait2Edit
            this.app = app
            // 记录AI解析成功的关键信息
            ServerLog.d("AI解析成功：type=$type, money=$money, shop=$shopName")
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
    ): BillInfoModel? {
        ServerLog.d("根据AI或者JS结果解析数据：$result")
        val json =
            runCatchingExceptCancel { Gson().fromJson(result, JsonObject::class.java) }.getOrNull()
                ?: run {
                    // 记录JSON解析失败信息
                    ServerLog.d("结果JSON解析失败，返回空")
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
                ServerLog.d("时间为0,解析失败，尝试从string解析")
                if (timeText.isEmpty()) {
                    ServerLog.d("时间string,解析失败，使用当前时间")
                    time = System.currentTimeMillis()
                } else {
                    runCatchingExceptCancel {
                        time = DateUtils.toEpochMillis(timeText)
                    }.onFailure {
                        ServerLog.e(it)
                        time = System.currentTimeMillis()
                    }
                }
            }
            //  DateUtils.toEpochMillis(timeText)


            money = json.safeGetDouble("money", 0.0)
            fee = json.safeGetDouble("fee", 0.0)
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
                // 记录规则驱动的自动记账标记
                ServerLog.d("规则匹配：rule=${ruleName}, auto=$auto")
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
        ServerLog.d("规则分类结果：$categoryJson")
        // 设置账本名称与分类（优先规则结果，否则默认值）
        bill.bookName = categoryJson.safeGetStringNonBlank("bookName", SettingUtils.bookName())
        bill.cateName = categoryJson.safeGetStringNonBlank("category", "其他")
        bill.remark = categoryJson.safeGetStringNonBlank("remark", "")
        ServerLog.d("规则处理后的账单信息：$bill")
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
            ServerLog.d("AI分析的账单分类结果：${bill.cateName}")
        }

        // 设置分类映射、查找
        CategoryProcessor().setCategoryMap(bill)
        // 记录分类映射摘要
        ServerLog.d("分类映射完成：book=${bill.bookName}, cate=${bill.cateName}")
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

                    // 调起悬浮窗（调试用日志）
                    ServerLog.d("拉起自动记账悬浮窗口：$intent")
                    runCatchingExceptCancel {
                        Server.application.startActivity(intent.toIntent())
                    }.onFailure { throwable ->
                        ServerLog.e("自动记账悬浮窗拉起失败：$throwable", throwable)
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

