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
            Toast.makeText(
                Server.application,
                "账单金额：${billInfoModel.money}，横屏状态下为您自动暂存。",
                Toast.LENGTH_SHORT
            ).show()
            // 记录横屏免打扰触发，便于排查为何未拉起悬浮窗
            ServerLog.d("横屏免打扰开启，自动暂存账单并返回：money=${billInfoModel.money}, app=${billInfoModel.app}")
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
        Server.application.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE


    /**
     * 启动悬浮窗口来显示账单信息
     * @param billInfoModel 要显示的账单信息模型
     * @param parent 父账单信息，可能为null，用于关联相关账单
     * @throws SecurityException 如果应用没有必要的权限
     */
    private suspend fun launchFloatingWindow(billInfoModel: BillInfoModel, parent: BillInfoModel?) {
        val intent = BillInfoIntent(billInfoModel, "JsRoute", parent).toIntent()
        // 调起悬浮窗（调试用日志）
        ServerLog.d("拉起自动记账悬浮窗口：$intent")

        runCatchingExceptCancel {
            Server.application.startActivity(intent)
        }.onFailure { throwable ->
            ServerLog.e("自动记账悬浮窗拉起失败：$throwable", throwable)
        }
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
            hash.put(key)
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

            // 4) 分析：保持“先规则，后AI”的顺序；当 forceAI=true 时仅跳过 AI 开关检查
            val start = System.currentTimeMillis()
            val billInfo: BillInfoModel =
                analyzeWithRule(analysisParams.app, analysisParams.data, dataType)
                    ?: analyzeWithAI(
                        analysisParams.app,
                        analysisParams.data,
                        force = analysisParams.forceAI
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
            // 设置资产映射
            AssetsMap().setAssetsMap(billInfo)
            // 记录资产映射摘要
            ServerLog.d("资产映射完成：from=${billInfo.accountNameFrom}, to=${billInfo.accountNameTo}")

            // 生成账单备注（仅当模板非空时）
            BillManager.getRemark(billInfo, context).let { generated ->
                billInfo.remark = generated
            }
            // 记录备注生成结果
            ServerLog.d("备注生成完成：remark=${billInfo.remark}")

            // 如果不是来自应用数据，则保存到数据库
            if (!analysisParams.fromAppData) {
                billInfo.id = db.billInfoDao().insert(billInfo)
                // 记录账单入库主键
                ServerLog.d("账单入库成功：billId=${billInfo.id}")
            }


            // 对账单类型进行检查，这里如果没有开启资产管理，是没有转账类型的


            // 将账单加入处理队列并等待自动去重处理完成
            val task = Server.billProcessor.addTask(billInfo, context)
            val parent = task.await()
            // 记录自动去重处理的结果摘要
            if (parent == null) {
                ServerLog.d("自动去重未找到父账单，待用户编辑")
                categorize(billInfo)
                ServerLog.d("进行分类之后的账单 $billInfo")
            } else {
                ServerLog.d("自动去重找到父账单：parentId=${parent.id}")
                // 父账单设置特殊规则名称
                parent.ruleName = formatParentBillRuleName()
                if (analysisParams.fromAppData) {
                    ServerLog.d("用户手动启动分析，重新生成分类")
                    categorize(billInfo)
                    ServerLog.d("进行分类之后的账单 $billInfo")
                } else {
                    categorize(parent)
                    ServerLog.d("进行分类之后的账单 $parent")
                }
            }
            // 根据处理结果更新账单状态
            billInfo.state = if (parent == null) BillState.Wait2Edit else BillState.Edited
            db.billInfoDao().update(billInfo)
            // 记录账单最终状态
            ServerLog.d("账单状态更新：state=${billInfo.state}")


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

            // 9) 拉起悬浮窗（仅外部数据）
            if (!analysisParams.fromAppData) startAutoPanel(billInfo, parent)
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
        creator: String
    ): BillInfoModel? {
        val src = if ("system" == creator) "系统" else "用户"
        val js = ruleGenerator.data(app, dataType, creator)
        if (js.isBlank()) {
            ServerLog.d("${src}规则数据为空，跳过")
            return null
        }
        val result = executeJs(js, data)
        return parseBillInfo(result, app, dataType)?.also {

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
    ): BillInfoModel? {
        ServerLog.d("使用规则进行分析：$data")
        //为了避免部分用户的错误规则影响自动记账整体规则的可用性，拆分成2部分处理
        for (creator in arrayOf("system", "user")) {
            analyzeWithCreator(app, data, dataType, creator)?.let { return it }
        }
        ServerLog.d("系统与用户规则均未解析出有效结果")
        return null
    }

    /**
     * 使用AI分析账单数据
     *
     * 调用AI管理器的账单工具来分析数据，并将结果转换为账单信息对象。
     * 只有当分析结果有效时才会返回账单信息。
     *
     * @param app 应用名称
     * @param data 要分析的原始数据
     * @return 分析得到的账单信息，如果分析失败则返回null
     */
    private suspend fun analyzeWithAI(
        app: String,
        data: String,
        force: Boolean = false
    ): BillInfoModel? {

        if (!force && !SettingUtils.aiBillRecognition()) {
            ServerLog.d("AI分析账单功能禁用，跳过账单分析")
            return null
        }
        ServerLog.d("AI分析中，$data")
        val result = BillTool().execute(data) ?: run {
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
            time = json.safeGetLong("time", System.currentTimeMillis())
            money = json.safeGetDouble("money", 0.0)
            fee = json.safeGetDouble("fee", 0.0)
            shopName = json.safeGetString("shopName")
            shopItem = json.safeGetString("shopItem")
            accountNameFrom = json.safeGetString("accountNameFrom")
            accountNameTo = json.safeGetString("accountNameTo")
            currency = json.safeGetString("currency")
            channel = json.safeGetString("channel")

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
    private suspend fun categorize(bill: BillInfoModel) {
        if (!bill.needReCategory()) {
            ServerLog.d("之前账单已有有效分类，无需重新分析")
            return
        }

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
        ServerLog.d("规则处理后的账单信息：$bill")
        if (bill.needReCategory() && SettingUtils.aiCategoryRecognition()) {
            bill.cateName = CategoryTool().execute(
                win.toString()
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
    }
}

