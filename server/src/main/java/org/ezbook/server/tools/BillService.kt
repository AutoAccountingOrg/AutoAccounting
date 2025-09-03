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
import org.ezbook.server.ai.AiManager
import org.ezbook.server.ai.tools.BillTool
import org.ezbook.server.constant.BillState
import org.ezbook.server.constant.BillType
import org.ezbook.server.constant.DataType
import org.ezbook.server.constant.Setting
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
     * @param dnd 横屏勿扰
     */
    private suspend fun startAutoPanel(
        billInfoModel: BillInfoModel,
        parent: BillInfoModel?,
        dnd: Boolean = false
    ) {
        val isLandscape = isLandscapeMode()
        Server.log("横屏状态：$isLandscape, 是否横屏勿扰：$dnd")
        // 检查横屏状态并处理
        if (isLandscape && dnd) {
            Toast.makeText(
                Server.application,
                "账单金额：${billInfoModel.money}，横屏状态下为您自动暂存。",
                Toast.LENGTH_SHORT
            ).show()
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
        Server.log("拉起自动记账悬浮窗口：$intent")

        runCatching {
            Server.application.startActivity(intent)
        }.onFailure { throwable ->
            Server.log("自动记账悬浮窗拉起失败：$throwable")
            Server.log(throwable)
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
    suspend fun analyze(analysisParams: AnalysisParams, context: Context): ResultModel =
        withContext(Dispatchers.IO) {
            // 验证数据类型参数的有效性
            val dataType = runCatching { DataType.valueOf(analysisParams.type) }.getOrElse {
                return@withContext ResultModel.error(400, "Type exception: ${analysisParams.type}")
            }

            var appDataModel: AppDataModel? = null

            if (!analysisParams.fromAppData) {
                //数据不是存储在AppData
                appDataModel = AppDataModel()
                appDataModel.data = analysisParams.data
                appDataModel.app = analysisParams.app
                appDataModel.type = dataType
                appDataModel.id = Db.get().dataDao().insert(appDataModel)
                appDataModel.time = System.currentTimeMillis()
            }

            // 记录分析开始时间，用于性能统计
            val start = System.currentTimeMillis()

            // 根据参数决定使用AI分析还是规则分析
            val billInfo: BillInfoModel? = // 先尝试规则分析，失败则回退到AI分析，不再有指定AI分析的功能
                analyzeWithRule(analysisParams.app, analysisParams.data, dataType)
                    ?: analyzeWithAI(analysisParams.app, analysisParams.data)

            // 如果分析失败，返回错误结果
            if (billInfo == null) {
                return@withContext ResultModel.error(404, "未分析到有效账单。")
            }

            // 对账单进行分类处理
            categorize(billInfo)

            // 执行账单的后续处理逻辑
            val parentBillInfo = process(billInfo, analysisParams.fromAppData, context)

            // 计算并记录分析耗时
            val cost = System.currentTimeMillis() - start
            Server.log("识别用时: $cost ms")

            if (appDataModel != null) {
                appDataModel.match = true
                appDataModel.rule = billInfo.ruleName
                appDataModel.version = ""
                Db.get().dataDao().update(appDataModel)
            }


            // 拉起悬浮窗
            if (!analysisParams.fromAppData) {
                val dnd = Db.get().settingDao().query(Setting.LANDSCAPE_DND)?.value !== "false"
                startAutoPanel(billInfo, parentBillInfo, dnd)
            }

            // 返回成功结果
            ResultModel.ok(
                BillResultModel(
                    billInfo,
                    parentBillInfo
                )
            )
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
        Server.logD("执行规则分析")
        // 获取对应应用和数据类型的规则代码
        val js = ruleGenerator.data(app, dataType)
        if (js.isBlank()) return null

        // 执行规则代码进行分析
        val result = executeJs(js, data)
        if (result.isBlank()) return null
        Server.logD("规则分析结果:$result")
        // 解析分析结果为账单信息对象
        return parseBillInfo(result, app, dataType)
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
    private suspend fun analyzeWithAI(app: String, data: String): BillInfoModel? =
        runCatching {
            if (!Db.get().settingDao().query(Setting.AI_BILL_RECOGNITION)?.value.toBoolean()) {
                Server.logD("AI 功能禁用")
                return@runCatching null
            }

            Server.logD("调用AI分析")
            BillTool().execute(data)
        }.getOrNull()?.apply {
            // 设置AI分析的标识信息
            ruleName = "${AiManager.getInstance().currentProviderName} 生成"
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
        Server.logD("解析数据：$result")
        val json = Gson().fromJson(result, JsonObject::class.java)

        // 使用安全的 JSON 访问扩展函数
        return BillInfoModel().apply {
            type = runCatching {
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
            ruleName = json.safeGetString("ruleName")
            cateName = json.safeGetString("cateName")
            // 根据ruleName判断是否需要自动记录
            val rule = Db.get().ruleDao().query(dataType.name, app, ruleName)
            auto = rule?.autoRecord ?: false
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
        // 检查是否需要重新分类
        if (!bill.needReCategory()) return

        // 构造分类所需的数据对象
        val win = JsonObject().apply {
            addProperty("type", bill.type.name)
            addProperty("money", bill.money)
            addProperty("shopName", bill.shopName)
            addProperty("shopItem", bill.shopItem)
        }

        // 执行分类规则并解析结果
        val categoryJson = runCatching {
            Gson().fromJson(
                executeJs(ruleGenerator.category(), win.toString()),
                JsonObject::class.java
            )
        }.getOrNull()

        // 设置账本名称和分类名称，使用默认值作为回退
        bill.bookName = categoryJson?.safeGetString("book", "默认账本") ?: "默认账本"
        bill.cateName = categoryJson?.safeGetString("category", "其他") ?: "其他"
    }

    /**
     * 处理账单的后续业务逻辑
     *
     * 包括资产映射、分类映射、备注生成、账本设置等操作。
     * 如果不是来自应用数据，会将账单保存到数据库。
     * 最后会将账单加入处理队列进行异步处理。
     *
     * @param bill 要处理的账单信息
     * @param fromAppData 是否来自应用数据（如果是，则不保存到数据库）
     * @param context Android上下文
     * @return 包含父级账单和是否需要用户操作标识的Pair
     */
    private suspend fun process(
        bill: BillInfoModel,
        fromAppData: Boolean,
        context: Context
    ): BillInfoModel? {
        // 设置资产映射，返回是否需要用户操作
        val needAction = AssetsMap.setAssetsMap(bill)

        // 设置分类映射
        setCategoryMap(bill)

        // 生成账单备注
        bill.remark = BillManager.getRemark(bill, context)

        // 设置账本名称
        BillManager.setBookName(bill)

        // 如果不是来自应用数据，则保存到数据库
        if (!fromAppData) {
            bill.id = db.billInfoDao().insert(bill)
        }

        // 将账单加入处理队列并等待自动分组处理完成
        val task = Server.billProcessor.addTask(bill, context)
        task.await()

        // 根据处理结果更新账单状态
        bill.state = if (task.result == null) BillState.Wait2Edit else BillState.Edited
        db.billInfoDao().update(bill)

        return task.result
    }

    //只允许在io线程
    private suspend fun setCategoryMap(billInfoModel: BillInfoModel) {
        val category = billInfoModel.cateName
        Db.get().categoryMapDao().query(category)?.let {
            billInfoModel.cateName = it.mapName
        }
    }


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
        /**
         * 验证账单信息是否有效
         *
         * 有效的账单必须满足以下条件：
         * - 金额不为0
         * - 来源账户名称不为空
         *
         * @param bill 要验证的账单信息
         * @return 账单是否有效
         */
        private fun isValid(bill: BillInfoModel) =
            bill.money != 0.0 && bill.accountNameFrom.isNotEmpty()
    }
}

