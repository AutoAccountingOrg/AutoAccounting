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

    /** 对话框打开时的分类名，用于检测用户是否手动修改了分类 */
    private var initialCateName: String = ""

    override fun onComponentCreate() {
        super.onComponentCreate()
        setupCheckboxes()
        setupClickListeners()
    }

    /**
     * 设置账单信息
     */
    fun setBillInfo(billInfoModel: BillInfoModel) {
        this.billInfoModel = billInfoModel
        if (this.initialCateName.isEmpty()) {
            this.initialCateName = billInfoModel.cateName
        }
        refresh()
    }

    /**
     * 刷新显示
     */
    fun refresh() {
        // 操作按钮组件无需根据数据刷新UI
    }

    /**
     * 初始化复选框状态并监听变更，双向同步 PrefManager
     */
    private fun setupCheckboxes() {
        // 初始状态与 PrefManager 同步
        binding.checkboxRememberCategory.isChecked = PrefManager.rememberCategory
        binding.checkboxRememberAsset.isChecked = PrefManager.autoAssetMapping

        // 用户切换时写回设置
        binding.checkboxRememberCategory.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.rememberCategory = isChecked
        }
        binding.checkboxRememberAsset.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.autoAssetMapping = isChecked
        }
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
            if (binding.checkboxRememberCategory.isChecked) {
                // 仅当分类被用户明确修改，且当前账单不需要再自动分类时，才执行自动记忆
                if (initialCateName != billInfoModel.cateName && !billInfoModel.needReCategory()) {
                    rememberCategoryAuto()
                }
            }

            if (binding.checkboxRememberAsset.isChecked) {
                rememberAssetMap()
            }

            onConfirmClickListener?.invoke()
        }
    }

    /**
     * 记住资产映射
     *
     * 用 rawAccountNameFrom/To（映射前的原始名）作为查询 key，
     * 与当前 accountNameFrom/To 比较：不同则说明发生了映射或用户修改，
     * 尝试更新映射表；mapName 已一致时自动跳过，不会重复写入。
     */
    private fun rememberAssetMap() {
        if (!::billInfoModel.isInitialized) return

        // 构建 (原始名, 当前资产名) 对，raw == current 说明无需映射，直接过滤
        val pairs: List<Pair<String, String>> = when (billInfoModel.type) {
            BillType.Expend, BillType.Income, BillType.ExpendReimbursement,
            BillType.IncomeRefund, BillType.IncomeReimbursement -> listOf(
                billInfoModel.rawAccountNameFrom to billInfoModel.accountNameFrom
            )

            BillType.Transfer -> listOf(
                billInfoModel.rawAccountNameFrom to billInfoModel.accountNameFrom,
                billInfoModel.rawAccountNameTo to billInfoModel.accountNameTo
            )

            BillType.ExpendLending, BillType.ExpendRepayment -> listOf(
                billInfoModel.rawAccountNameFrom to billInfoModel.accountNameFrom
            )

            BillType.IncomeLending, BillType.IncomeRepayment -> listOf(
                billInfoModel.rawAccountNameTo to billInfoModel.accountNameTo
            )
        }
            .filter { (raw, curr) -> raw.isNotBlank() && curr.isNotBlank() && raw != curr }

        if (pairs.isEmpty()) return

        App.launchIO {
            pairs.forEach { (rawName, newName) ->
                val model: AssetsMapModel = AssetsMapAPI.getByName(rawName) ?: return@forEach
                if (model.mapName != newName) {
                    model.mapName = newName
                    runCatchingExceptCancel {
                        AssetsMapAPI.put(model)
                    }.onFailure { e ->
                        Logger.d("更新资产映射失败: ${e.message}")
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
                Logger.i("已自动记住分类: ${billInfoModel.cateName}")
                // 成功提示
                ToastUtils.info(
                    context.getString(
                        net.ankio.auto.R.string.remember_category_success,
                        billInfoModel.cateName
                    )
                )
            } catch (e: Exception) {
                Logger.d("记住分类失败: ${e.message}")
            }
        }
    }

    /**
     * 清洗用于分类规则的名称，剔除所有数字、金额和变化标识，
     * 只保留稳定的文本特征用于分类匹配。
     */
    private fun sanitizeForRule(inputRaw: String): String {
        if (inputRaw.isEmpty()) return inputRaw
        return inputRaw
            // 清理金额格式（含货币符号和单位）：¥100.00元、$5.5、100元 等
            .replace(Regex("[¥$€£￥]?\\d+\\.?\\d*\\s?[元円]?"), "")
            // 清理所有剩余数字
            .replace(Regex("\\d+"), "")
            // 清理纯字母长串（订单号残余）
            .replace(Regex("[A-Za-z]{6,}"), "")
            // 清理多余空白和标点残留
            .replace(Regex("[\\s\\-_/]+"), " ")
            .trim()
    }

    /**
     * 转义 JS 字符串中的特殊字符
     */
    private fun escapeForJs(input: String): String {
        return input.replace("\\", "\\\\").replace("\"", "\\\"")
    }

}
