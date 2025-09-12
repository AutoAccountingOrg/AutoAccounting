/*
 * Copyright (C) 2023 ankio(ankio@ankio.net)
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

package net.ankio.auto.ui.dialog.components

import net.ankio.auto.databinding.ComponentActionButtonsBinding
import net.ankio.auto.ui.api.BaseComponent
import net.ankio.auto.utils.PrefManager
import org.ezbook.server.db.model.BillInfoModel
import com.google.gson.Gson
import net.ankio.auto.App
import net.ankio.auto.http.api.CategoryRuleAPI
import net.ankio.auto.storage.Logger
import org.ezbook.server.db.model.CategoryRuleModel
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.http.api.AssetsMapAPI
import org.ezbook.server.db.model.AssetsMapModel
import org.ezbook.server.constant.BillType
import org.ezbook.server.tools.runCatchingExceptCancel
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * 操作按钮组件 - 专用于账单编辑对话框
 *
 * 职责：
 * - 提供取消和确认操作按钮
 * - 处理按钮点击事件
 *
 * 使用方式：
 * ```kotlin
 * val actionButtons: ActionButtonsComponent = binding.actionButtons.bindAs()
 * actionButtons.setBillInfo(billInfoModel)
 * actionButtons.setOnCancelClickListener {
 *     // 处理取消逻辑
 * }
 * actionButtons.setOnConfirmClickListener {
 *     // 处理确认逻辑
 * }
 * ```
 */
class ActionButtonsComponent(
    binding: ComponentActionButtonsBinding
) : BaseComponent<ComponentActionButtonsBinding>(binding) {

    private var onCancelClickListener: (() -> Unit)? = null
    private var onConfirmClickListener: (() -> Unit)? = null

    private lateinit var billInfoModel: BillInfoModel
    private lateinit var rawBillInfoModel: BillInfoModel

    override fun onComponentCreate() {
        super.onComponentCreate()
        setupClickListeners()
    }

    /**
     * 设置账单信息
     */
    fun setBillInfo(billInfoModel: BillInfoModel) {
        this.billInfoModel = billInfoModel
        this.rawBillInfoModel = billInfoModel.copy()
        refresh()
    }

    /**
     * 刷新显示 - 根据当前账单信息更新UI
     */
    fun refresh() {
        // 操作按钮组件通常不需要根据数据刷新UI
        // 保持方法以维持一致性
    }

    /**
     * 设置取消按钮点击监听器
     *
     * @param listener 取消点击回调
     */
    fun setOnCancelClickListener(listener: () -> Unit) {
        onCancelClickListener = listener
    }

    /**
     * 设置确认按钮点击监听器
     *
     * @param listener 确认点击回调
     */
    fun setOnConfirmClickListener(listener: () -> Unit) {
        onConfirmClickListener = listener
    }


    /**
     * 设置点击事件监听器
     */
    private fun setupClickListeners() {
        binding.cancelButton.setOnClickListener {
            onCancelClickListener?.invoke()
        }

        binding.confirmButton.setOnClickListener {
            if (PrefManager.rememberCategory) {
                // 仅当分类被用户明确修改，且当前账单不需要再自动分类时，才执行自动记忆
                if (rawBillInfoModel.cateName != billInfoModel.cateName && !billInfoModel.needReCategory()) {
                    rememberCategoryAuto()
                }
            }

            if (PrefManager.autoAssetMapping) {
                rememberAssetMap()
            }

            onConfirmClickListener?.invoke()
        }
    }

    private fun rememberAssetMap() {
        // 通过 getMap 接口，使用原始资产名称获取资产映射模型
        // 如果没有就返回；如果有，则将映射的目的资产替换为当前编辑值，并更新到远程
        if (!::billInfoModel.isInitialized || !::rawBillInfoModel.isInitialized) return

        // 构建需要处理的“原始资产名 → 当前资产名”对，仅限账户字段（人员字段不参与映射）
        val pairs: List<Pair<String, String>> = when (billInfoModel.type) {
            // 单账户：仅来源账户
            BillType.Expend, BillType.Income, BillType.ExpendReimbursement,
            BillType.IncomeRefund, BillType.IncomeReimbursement -> listOf(
                rawBillInfoModel.accountNameFrom to billInfoModel.accountNameFrom
            )

            // 转账：来源与目标账户
            BillType.Transfer -> listOf(
                rawBillInfoModel.accountNameFrom to billInfoModel.accountNameFrom,
                rawBillInfoModel.accountNameTo to billInfoModel.accountNameTo
            )

            // 借出/还款：仅来源账户（目标为人员）
            BillType.ExpendLending, BillType.ExpendRepayment -> listOf(
                rawBillInfoModel.accountNameFrom to billInfoModel.accountNameFrom
            )

            // 借入/收款：仅目标账户（来源为人员）
            BillType.IncomeLending, BillType.IncomeRepayment -> listOf(
                rawBillInfoModel.accountNameTo to billInfoModel.accountNameTo
            )
        }
            .filter { (orig, curr) -> orig.isNotBlank() && curr.isNotBlank() && orig != curr }

        if (pairs.isEmpty()) return

        App.launchIO {
            pairs.forEach { (originalName, currentName) ->
                // 拉取现有映射；按需求：没有则不创建，直接跳过
                val model: AssetsMapModel = AssetsMapAPI.getByName(originalName) ?: return@forEach
                // 更新目的资产
                if (model.mapName != currentName) {
                    model.mapName = currentName
                    runCatchingExceptCancel {
                        AssetsMapAPI.put(model)
                    }.onFailure { e ->
                        logger.debug { "更新资产映射失败: ${e.message}" }
                    }
                }
            }
        }
    }

    /**
     * 自动记住分类
     * 规则：
     * - 使用 shopItem 与 shopName 作为匹配条件（包含匹配）
     * - 两者都为空则不保存
     * - 去重逻辑在服务端执行：同键规则将被替换
     */
    private fun rememberCategoryAuto() {
        val shopItem = sanitizeForRule(billInfoModel.shopItem.trim())
        val shopName = sanitizeForRule(billInfoModel.shopName.trim())
        if (shopItem.isEmpty() && shopName.isEmpty()) return

        // 构建待保存的规则模型（自动生成）
        val model = CategoryRuleModel().apply {
            creator = "system"
            enabled = true
            sort = 0
        }

        // 生成 element 列表（与编辑组件保持同一结构）
        val elements = mutableListOf<MutableMap<String, Any>>()
        if (shopName.isNotEmpty()) {
            elements.add(
                mutableMapOf(
                    "type" to "shopName",
                    "js" to "shopName.indexOf(\"${escapeForJs(shopName)}\")!==-1",
                    "text" to "商家包含【$shopName】",
                    "select" to 0,
                    "content" to shopName
                )
            )
        }
        if (shopItem.isNotEmpty()) {
            elements.add(
                mutableMapOf(
                    "type" to "shopItem",
                    "js" to "shopItem.indexOf(\"${escapeForJs(shopItem)}\")!==-1",
                    "text" to "商品包含【$shopItem】",
                    "select" to 0,
                    "content" to shopItem
                )
            )
        }
        elements.add(
            mutableMapOf(
                "book" to billInfoModel.bookName,
                "category" to billInfoModel.cateName,
                "id" to "-1"
            )
        )
        model.element = Gson().toJson(elements)

        // 生成 js 条件表达式
        val cond = buildString {
            var first = true
            if (shopName.isNotEmpty()) {
                append("shopName.indexOf(\"${escapeForJs(shopName)}\")!==-1")
                first = false
            }
            if (shopItem.isNotEmpty()) {
                if (!first) append(" && ")
                append("shopItem.indexOf(\"${escapeForJs(shopItem)}\")!==-1")
            }
        }
        model.js =
            "if(${cond}){ return { book:'${billInfoModel.bookName}',category:'${billInfoModel.cateName}'} }"

        // 直接保存，由服务端去重
        App.launchIO {
            try {
                CategoryRuleAPI.put(model)
                logger.info { "已自动记住分类: ${billInfoModel.cateName}" }
                // 成功提示
                ToastUtils.info(
                    context.getString(
                        net.ankio.auto.R.string.remember_category_success,
                        billInfoModel.cateName
                    )
                )
            } catch (e: Exception) {
                logger.debug { "记住分类失败: ${e.message}" }
            }
        }
    }

    /**
     * 清洗用于分类规则的名称，剔除订单号/流水号等变化标识
     * 保留短数字（金额、数量等），清理长串标识符
     */
    private fun sanitizeForRule(inputRaw: String): String {
        if (inputRaw.isEmpty()) return inputRaw
        return inputRaw
            // 清理长数字串（5位以上视为订单号）
            .replace(Regex("\\d{5,}"), "")
            // 清理字母数字混合的长串（通常是订单号）
            .replace(Regex("\\b[A-Za-z0-9]{8,}\\b"), "")
            // 清理多余空白
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * 转义 JS 字符串中的特殊字符
     */
    private fun escapeForJs(input: String): String {
        return input.replace("\\", "\\\\").replace("\"", "\\\"")
    }

}
