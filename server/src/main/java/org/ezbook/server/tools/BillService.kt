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
import org.ezbook.server.ai.tools.CategoryTool
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
        Server.logD("拉起自动记账悬浮窗口：$intent")

        runCatchingExceptCancel {
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
            // 1) 校验数据类型
            val dataType = runCatchingExceptCancel { DataType.valueOf(analysisParams.type) }
                .getOrElse {
                    return@withContext ResultModel.error(
                        400,
                        "Type exception: ${analysisParams.type}"
                    )
                }

            // 2) 仅对外部数据做重复触发过滤
            val key = MD5HashTable.md5(analysisParams.data)
            if (!analysisParams.fromAppData && hash.contains(key)) {
                return@withContext ResultModel.error(400, "检测到重复触发分析")
            }
            hash.put(key)

            // 3) 如有需要，先持久化原始数据
            val appDataModel: AppDataModel? = if (!analysisParams.fromAppData) {
                AppDataModel().apply {
                    data = analysisParams.data
                    app = analysisParams.app
                    type = dataType
                    time = System.currentTimeMillis()
                    id = Db.get().dataDao().insert(this)
                }
            } else null

            // 4) 分析（先规则，后 AI）
            val start = System.currentTimeMillis()
            val billInfo: BillInfoModel =
                analyzeWithRule(analysisParams.app, analysisParams.data, dataType)
                    ?: analyzeWithAI(analysisParams.app, analysisParams.data)
                    ?: return@withContext ResultModel.error(404, "未分析到有效账单。")

            //这里也不加bookName, bookName在分类里面处理
            // 5) 分类（规则 → 可选 AI）
            categorize(billInfo)

            // 设置资产映射
            AssetsMap().setAssetsMap(billInfo)
            // 设置分类映射、查找
            CategoryProcessor().setCategoryMap(billInfo)

            // 生成账单备注
            billInfo.remark = BillManager.getRemark(billInfo, context)

            // 如果不是来自应用数据，则保存到数据库
            if (!analysisParams.fromAppData) {
                billInfo.id = db.billInfoDao().insert(billInfo)
            }

            // 将账单加入处理队列并等待自动分组处理完成
            val task = Server.billProcessor.addTask(billInfo, context)
            task.await()

            // 根据处理结果更新账单状态
            billInfo.state = if (task.result == null) BillState.Wait2Edit else BillState.Edited
            db.billInfoDao().update(billInfo)


            // 7) 统计耗时
            val cost = System.currentTimeMillis() - start
            Server.log("识别用时: $cost ms")

            // 8) 更新原始数据存档
            appDataModel?.let {
                it.match = true
                it.rule = billInfo.ruleName
                it.version = ""
                Db.get().dataDao().update(it)
            }

            // 9) 拉起悬浮窗（仅外部数据）
            if (!analysisParams.fromAppData) startAutoPanel(billInfo, task.result)

            // 10) 返回
            ResultModel.ok(BillResultModel(billInfo, task.result))
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
        // 执行规则代码进行分析
        val result = executeJs(js, data)
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
    private suspend fun analyzeWithAI(app: String, data: String): BillInfoModel? {
        if (!SettingUtils.aiBillRecognition()) {
            Server.logD("AI 功能禁用")
            return null
        }
        Server.logD("调用AI分析")
        val result = BillTool().execute(data) ?: return null
        return result.apply {
            // 设置AI分析的标识信息
            ruleName = "${SettingUtils.apiProvider()} 生成"
            state = BillState.Wait2Edit
            this.app = app
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
        Server.logD("解析数据：$result")
        val json =
            runCatchingExceptCancel { Gson().fromJson(result, JsonObject::class.java) }.getOrNull()
                ?: return null
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
            ruleName = json.safeGetString("ruleName")
            cateName = json.safeGetString("cateName")
            // 这个地方不要带上bookName，因为这里的数据来源是JS生成的，Js里面不会输出bookName和cateName，但是AI会携带cateName
            if (!this.generateByAi()) {
                val rule = Db.get().ruleDao().query(dataType.name, app, ruleName)
                auto = rule?.autoRecord ?: false
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
        if (!bill.needReCategory()) return

        val win = JsonObject().apply {
            addProperty("type", bill.type.name)
            addProperty("money", bill.money)
            addProperty("shopName", bill.shopName)
            addProperty("shopItem", bill.shopItem)
        }

        val categoryJson = runCatchingExceptCancel {
            Gson().fromJson(
                executeJs(ruleGenerator.category(), win.toString()),
                JsonObject::class.java
            )
        }.getOrNull()
        Server.logD("categoryJson $categoryJson")
        // 设置账本名称与分类（优先规则结果，否则默认值）
        bill.bookName = categoryJson.safeGetStringNonBlank("bookName", SettingUtils.bookName())
        bill.cateName = categoryJson.safeGetStringNonBlank("category", "其他")

        if (bill.needReCategory() && SettingUtils.aiCategoryRecognition()) {
            bill.cateName = CategoryTool().execute(
                win.toString()
            ).takeUnless { it.isNullOrEmpty() } ?: "其他"
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
        private val hash = MD5HashTable(300_000)
    }
}

