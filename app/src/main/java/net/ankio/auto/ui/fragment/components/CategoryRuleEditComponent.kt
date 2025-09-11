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

package net.ankio.auto.ui.fragment.components

import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.ankio.auto.R
import net.ankio.auto.databinding.ComponentCategoryEditBinding
import net.ankio.auto.databinding.DialogRegexInputBinding
import net.ankio.auto.databinding.DialogRegexMoneyBinding
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseComponent
import net.ankio.auto.ui.components.FlowElement
import net.ankio.auto.ui.components.FlowLayoutManager
import net.ankio.auto.ui.dialog.BookSelectorDialog
import net.ankio.auto.ui.dialog.BottomSheetDialogBuilder
import net.ankio.auto.ui.dialog.CategorySelectorDialog
import net.ankio.auto.ui.dialog.DateTimePickerDialog
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.utils.ListPopupUtilsGeneric
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.utils.BillTool
import org.ezbook.server.db.model.CategoryRuleModel
import java.util.Calendar

/**
 * 分类规则编辑组件 - 支持编辑和只读模式的通用组件
 *
 * 设计原则（遵循Linus好品味）：
 * 1. 单一职责：只处理分类规则的UI逻辑
 * 2. 简洁实现：消除特殊情况，统一处理流程
 * 3. 类型安全：使用类型安全的数据结构替代HashMap
 * 4. 生命周期管理：自动处理资源清理
 * 5. 简化构造：只需要ViewBinding，自动推断生命周期
 *
 * 功能特性：
 * - 支持编辑模式：可添加、修改、删除条件
 * - 支持只读模式：仅展示已有条件
 * - 动态条件构建：金额范围、时间范围、商家名称等
 * - 账本和分类选择：集成账本管理功能
 * - 数据验证：确保规则完整性
 */
class CategoryRuleEditComponent(
    binding: ComponentCategoryEditBinding
) : BaseComponent<ComponentCategoryEditBinding>(binding) {

    /** 当前编辑的分类规则模型 */
    private var categoryRuleModel: CategoryRuleModel = CategoryRuleModel()

    /** 流式布局管理器（组件销毁时置空，避免持有View引用） */
    private var flexboxLayout: FlowLayoutManager? = null

    /** 是否为只读模式 */
    private var readOnly: Boolean = false

    /** 选中的账本远程ID，-1表示默认账本 */
    private var remoteBookId: String = "-1"

    /** 选中的账本名称 */
    private var bookName: String = ""

    /** 选中的分类名称 */
    private var category: String = ""

    /**
     * 设置规则模型并重新渲染UI
     *
     * @param model 分类规则模型，为null时创建新模型
     * @param readOnly 是否为只读模式
     */
    fun setRuleModel(model: CategoryRuleModel? = null, readOnly: Boolean = false) {
        this.readOnly = readOnly
        if (readOnly) {
            flexboxLayout?.textAppearance =
                com.google.android.material.R.style.TextAppearance_Material3_TitleSmall
        }
        categoryRuleModel = model ?: CategoryRuleModel()
        setupRuleUI()
    }


    /**
     * 获取分类规则模型
     *
     * 功能说明：
     * 1. 从FlowElement中提取有效的条件数据
     * 2. 拼接生成JavaScript条件表达式
     * 3. 构建完整的分类规则模型
     * 4. 验证规则的完整性
     *
     * @return 构建好的分类规则模型，验证失败时返回空模型
     */
    fun getRule(): CategoryRuleModel? {
        val elements = binding.flexboxLayout.getElements()
        val conditionParts = mutableListOf<String>()
        val elementDataList = mutableListOf<MutableMap<String, Any>>()

        var isFirstCondition = true

        // 一次遍历处理所有逻辑
        for (element in elements) {
            val elementData = element.data

            // 只处理包含JS条件的元素
            if (!elementData.containsKey("js")) continue

            // 跳过无效数据（content为空）
            if (elementData.containsKey("content") &&
                elementData["content"].toString().isEmpty()
            ) continue

            // 处理连接符逻辑
            if (isFirstCondition) {
                // 首个条件不需要连接符
                elementData.remove("jsPre")
                isFirstCondition = false
            } else {
                // 后续条件默认使用"与"连接
                if (!elementData.containsKey("jsPre")) {
                    elementData["jsPre"] = " && "
                }
                conditionParts.add(elementData["jsPre"] as String)
            }

            // 添加JS条件
            conditionParts.add(elementData["js"] as String)
            elementDataList.add(elementData)
        }

        // 添加分类结果数据
        val categoryData = mutableMapOf<String, Any>(
            "book" to bookName,
            "category" to category,
            "id" to remoteBookId
        )
        elementDataList.add(categoryData)

        // 生成完整的JavaScript规则
        val condition = conditionParts.joinToString("")
        val js = "if($condition){ return { book:'$bookName',category:'$category'} }"

        // 更新规则模型
        categoryRuleModel.js = js
        categoryRuleModel.element = Gson().toJson(elementDataList)
        categoryRuleModel.creator = "user"
        // 验证规则完整性
        return validateRule()
    }

    /**
     * 验证分类规则的完整性
     *
     * @return 验证失败时返回null，成功时返回规则模型
     */
    private fun validateRule(): CategoryRuleModel? {
        val js = categoryRuleModel.js

        when {
            js.contains("if()") -> {
                ToastUtils.info(R.string.useless_condition)
                return null
            }

            js.contains("book:''") -> {
                ToastUtils.info(context.getString(R.string.useless_book))
                return null
            }

            js.contains("category:''") -> {
                ToastUtils.info(context.getString(R.string.useless_category))
                return null
            }
        }

        return categoryRuleModel
    }

    private fun setupRuleUI() {
        if (flexboxLayout == null) return
        // 清理现有UI
        flexboxLayout?.removeAllElements()

        val listType = object : TypeToken<MutableList<HashMap<String, Any>>>() {}.type
        val list: MutableList<HashMap<String, Any>> =
            Gson().fromJson(categoryRuleModel.element, listType) ?: mutableListOf(
                hashMapOf("book" to "默认账本", "category" to "其他")
            )
        val lastElement = list.removeAt(list.lastIndex)
        flexboxLayout?.appendTextView(context.getString(R.string.if_condition_true))
        for (hashMap in list) {
            if (hashMap.containsKey("jsPre") && readOnly) {
                flexboxLayout?.appendButton(
                    if ((hashMap["jsPre"] as String).contains("&&")) {
                        context.getString(
                            R.string.and,
                        )
                    } else {
                        context.getString(R.string.or)
                    },
                )
            }
            flexboxLayout?.appendWaveTextview(
                hashMap["text"] as String,
                connector = !readOnly,
                data = hashMap
            ) { elem, textview ->
                if (readOnly) return@appendWaveTextview

                when (hashMap["type"]) {
                    "timeRange" -> inputTimeRange(elem)
                    "moneyRange" -> inputMoneyRange(elem)
                    "shopName" -> inputShopName(elem)
                    "shopItem" -> inputShopItem(elem)
                    "type" -> inputBillType(elem, textview)
                }

            }
        }

        if (!readOnly) {
            flexboxLayout?.appendAddButton(callback = { elem, textview ->
                if (readOnly) return@appendAddButton
                //添加按钮，用于添加
                showSelectType(textview, elem)
            })
        }

        flexboxLayout?.appendTextView(context.getString(R.string.condition_result_book))
        bookName = lastElement["book"] as String
        flexboxLayout?.appendWaveTextview(
            text = bookName,
            data = lastElement
        ) { elem, textview ->
            if (readOnly) return@appendWaveTextview
            onClickBook(elem)
        }
        flexboxLayout?.appendTextView(context.getString(R.string.condition_result_category))
        category = lastElement["category"] as String
        flexboxLayout?.appendWaveTextview(
            text = category,
            data = lastElement
        ) { elem, textview ->
            if (readOnly) return@appendWaveTextview
            onClickCategory(elem)
        }
    }


    /**
     * 显示条件类型选择菜单
     */
    private fun showSelectType(view: View, element: FlowElement) {
        val conditionTypes = arrayOf(
            context.getString(R.string.type_money),
            context.getString(R.string.type_time),
            context.getString(R.string.type_shop),
            context.getString(R.string.type_item),
            context.getString(R.string.type_type)
        )

        val menuItems = conditionTypes.mapIndexed { index, name -> name to index }.toMap()

        ListPopupUtilsGeneric.create<Int>(context)
            .setAnchor(view)
            .setList(menuItems)
            .setSelectedValue(0)
            .setOnItemClick { _, _, value ->
                handleConditionTypeSelection(element, value, view)
            }
            .toggle()
    }

    /**
     * 处理条件类型选择
     */
    private fun handleConditionTypeSelection(element: FlowElement, typeIndex: Int, view: View) {
        when (typeIndex) {
            0 -> inputMoneyRange(element)      // 金额范围
            1 -> inputTimeRange(element)       // 时间范围
            2 -> inputShopName(element)        // 商家名称
            3 -> inputShopItem(element)        // 商品名称
            4 -> inputBillType(element, view)        // 账单类型
            else -> Logger.w("无效的条件类型索引: $typeIndex")
        }
    }

    override fun onComponentCreate() {
        super.onComponentCreate()
        flexboxLayout = binding.flexboxLayout
    }

    override fun onComponentDestroy() {
        super.onComponentDestroy()
        flexboxLayout?.removeAllElements()
        flexboxLayout = null
    }

    /**
     * 输入账单类型条件（收入/支出）
     *
     * 功能说明：
     * 1. 显示账单类型选择弹窗（支出/收入）
     * 2. 根据用户选择更新FlowElement的数据和显示
     * 3. 处理已存在类型条件的替换逻辑
     *
     * @param element 要设置类型条件的流动布局元素
     */
    private fun inputBillType(element: FlowElement, view: View) {
        // 定义账单类型的显示文本
        val billTypeTexts = arrayOf(
            context.getString(R.string.type_for_pay),    // 支出
            context.getString(R.string.type_for_income)  // 收入
        )

        // 定义对应的JavaScript条件表达式
        val billTypeJS = arrayOf("type === 'Expend'", "type === 'Income'")

        // 直接创建菜单数据，避免不必要的类型转换
        val menuItems = hashMapOf<String, Int>()
        billTypeTexts.forEachIndexed { index, text ->
            menuItems[text] = index
        }

        // 创建并显示选择弹窗
        ListPopupUtilsGeneric.create<Int>(context)
            .setAnchor(view)
            .setList(menuItems)
            .setSelectedValue(0)
            .setOnItemClick { _, key, typeIndex ->
            val conditionText = context.getString(R.string.type_pay, key)

            // 创建新的元素数据
            val newData = mutableMapOf<String, Any>(
                "js" to billTypeJS[typeIndex],
                "type" to "type",
                "text" to conditionText
            )

            // 判断是替换现有类型条件还是添加新条件
            if (element.data["type"] == "type") {
                // 替换现有的类型条件
                element.replaceAsWaveTextview(
                    conditionText,
                    newData,
                    element.connector
                ) { elem, view ->
                    if (readOnly) return@replaceAsWaveTextview
                    inputBillType(elem, view)
                }
            } else {
                // 添加新的类型条件
                flexboxLayout?.appendWaveTextview(
                    conditionText,
                    element,
                    true,
                    newData
                ) { elem, view ->
                    if (readOnly) return@appendWaveTextview
                    inputBillType(elem, view)
                }
            }

            Logger.d("设置账单类型条件: $conditionText")
            }
            .toggle()
    }

    /**
     * 输入商家名称条件
     */
    private fun inputShopName(element: FlowElement) {
        showTextInputDialog(
            element,
            R.string.shop_input,
            "shopName",
            context.getString(R.string.shop_name)
        )
    }

    /**
     * 输入商品名称条件
     */
    private fun inputShopItem(element: FlowElement) {
        showTextInputDialog(
            element,
            R.string.shop_item_input,
            "shopItem",
            context.getString(R.string.shop_item_name)
        )
    }

    /**
     * 显示文本输入对话框
     */
    private fun showTextInputDialog(
        element: FlowElement,
        titleRes: Int,
        conditionType: String,
        displayName: String
    ) {
        val inputBinding = DialogRegexInputBinding.inflate(LayoutInflater.from(context))

        // 初始化已保存的数据
        val elementData = element.data
        val savedMatchTypeIndex = extractMatchTypeIndex(elementData)
        val savedContent = elementData.getOrDefault("content", "").toString()

        // 设置匹配类型选项
        setupMatchTypeSpinner(inputBinding, savedMatchTypeIndex)

        // 设置初始内容
        inputBinding.content.setText(savedContent)

        // 显示对话框
        BaseSheetDialog.create<BottomSheetDialogBuilder>(context)
            .setTitleInt(titleRes)
            .addCustomView(inputBinding.root)
            .setPositiveButton(R.string.sure_msg) { _, _ ->
                handleTextInputConfirm(
                    element,
                    inputBinding,
                    conditionType,
                    displayName
                )
            }
            .setNegativeButton(R.string.cancel_msg, null)
            .show(true)
    }

    /**
     * 提取匹配类型索引
     */
    private fun extractMatchTypeIndex(data: Map<String, Any>): Int {
        return when (val selectValue = data.getOrDefault("select", 0)) {
            is Int -> selectValue
            is Double -> selectValue.toInt()
            else -> 0
        }
    }

    /**
     * 设置匹配类型选择器
     */
    private fun setupMatchTypeSpinner(inputBinding: DialogRegexInputBinding, selectedIndex: Int) {
        val options = arrayOf(
            context.getString(R.string.input_contains),
            context.getString(R.string.input_regex)
        )

        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        inputBinding.spinner.adapter = adapter
        inputBinding.spinner.setSelection(selectedIndex.coerceIn(0, 1))
    }

    /**
     * 处理文本输入确认
     */
    private fun handleTextInputConfirm(
        element: FlowElement,
        inputBinding: DialogRegexInputBinding,
        conditionType: String,
        displayName: String
    ) {
        val selectedTypeIndex = inputBinding.spinner.selectedItemPosition
        val content = inputBinding.content.text.toString().trim()

        if (content.isEmpty()) {
            ToastUtils.info("请输入有效内容")
            return
        }

        val jsTemplates = arrayOf(
            "%s.indexOf(\"%s\")!==-1",  // 包含模式
            "%s.match(/%s/)"           // 正则模式
        )
        val messageRes = arrayOf(
            R.string.shop_name_contains,
            R.string.shop_name_regex
        )

        // 生成JavaScript代码和显示文本
        val jsCode = String.format(jsTemplates[selectedTypeIndex], conditionType, content)
        val displayText = context.getString(messageRes[selectedTypeIndex], displayName, content)

        // 更新元素数据
        val newData = mutableMapOf<String, Any>(
            "type" to conditionType,
            "js" to jsCode,
            "text" to displayText,
            "select" to selectedTypeIndex,
            "content" to content
        )

        if (element.data["type"] == conditionType) {
            // 替换现有的类型条件
            element.replaceAsWaveTextview(displayText, newData, element.connector) { elem, view ->
                if (readOnly) return@replaceAsWaveTextview
                if (conditionType == "shopItem") {
                    inputShopItem(elem)
                } else {
                    inputShopName(elem)
                }
            }
        } else {
            // 添加新的类型条件
            flexboxLayout?.appendWaveTextview(displayText, element, true, newData) { elem, view ->
                if (readOnly) return@appendWaveTextview
                if (conditionType == "shopItem") {
                    inputShopItem(elem)
                } else {
                    inputShopName(elem)
                }
            }
        }

        Logger.d("设置文本条件: $displayText")
    }

    /**
     * 输入时间范围条件
     */
    private fun inputTimeRange(element: FlowElement) {
        val currentTime = Calendar.getInstance()
        val hour = currentTime.get(Calendar.HOUR_OF_DAY)
        val minute = currentTime.get(Calendar.MINUTE)

        val elementData = element.data
        var minTime = elementData.getOrDefault("minTime", "$hour:$minute").toString()
        var maxTime = elementData.getOrDefault("maxTime", "$hour:$minute").toString()

        showTimer(minTime, context.getString(R.string.select_time_lower)) { it1 ->
            minTime = it1
            showTimer(maxTime, context.getString(R.string.select_time_higher)) {
                maxTime = it
                val js = "common.isTimeInRange('$minTime','$maxTime',currentTime)"
                val input = context.getString(R.string.time_range, minTime, maxTime)

                val newData = mutableMapOf<String, Any>(
                    "js" to js,
                    "minTime" to minTime,
                    "maxTime" to maxTime,
                    "text" to input,
                    "type" to "timeRange"
                )

                if (element.data["type"] == "timeRange") {
                    // 替换现有的类型条件
                    element.replaceAsWaveTextview(input, newData, element.connector) { elem, view ->
                        if (readOnly) return@replaceAsWaveTextview
                        inputTimeRange(elem)
                    }
                } else {
                    // 添加新的类型条件
                    flexboxLayout?.appendWaveTextview(input, element, true, newData) { elem, view ->
                        if (readOnly) return@appendWaveTextview
                        inputTimeRange(elem)
                    }
                }
            }
        }
    }

    /**
     * 显示时间选择器
     */
    private fun showTimer(
        time: String,
        title: String,
        callback: (String) -> Unit
    ) {
        val result = time.split(":")
        val dialog = BaseSheetDialog.create<DateTimePickerDialog>(context)
        dialog.setDateTime(0, 0, 0, result[0].toInt(), result[1].toInt())
            .setTitle(title)
            .setTimeOnly(true)
            .setOnDateTimeSelected { year, month, day, hour, minute ->
                callback("$hour:$minute")
            }.show(true)
    }

    /**
     * 输入金额范围条件
     */
    private fun inputMoneyRange(element: FlowElement) {
        val moneyRangeBinding = DialogRegexMoneyBinding.inflate(LayoutInflater.from(context))
        val elementData = element.data

        moneyRangeBinding.lower.setText(elementData.getOrDefault("minAmount", "").toString())
        moneyRangeBinding.higher.setText(elementData.getOrDefault("maxAmount", "").toString())

        BaseSheetDialog.create<BottomSheetDialogBuilder>(context)
            .setTitleInt(R.string.money_range)
            .addCustomView(moneyRangeBinding.root)
            .setPositiveButton(R.string.sure_msg) { _, _ ->
                handleMoneyRangeConfirm(element, moneyRangeBinding)
            }
            .setNegativeButton(R.string.cancel_msg, null)
            .show(true)
    }

    /**
     * 处理金额范围确认
     */
    private fun handleMoneyRangeConfirm(
        element: FlowElement,
        moneyRangeBinding: DialogRegexMoneyBinding
    ) {
        val maxAmount = runCatching {
            moneyRangeBinding.higher.text.toString().toFloat()
        }.getOrDefault(0f)

        val minAmount = runCatching {
            moneyRangeBinding.lower.text.toString().toFloat()
        }.getOrDefault(0f)

        var js = ""
        var input = ""

        when {
            maxAmount == 0f && minAmount > 0 -> {
                js = "money > $minAmount"
                input = context.getString(R.string.money_max_info, minAmount.toString())
            }

            minAmount == 0f && maxAmount > 0 -> {
                js = "money < $maxAmount"
                input = context.getString(R.string.money_min_info, maxAmount.toString())
            }

            minAmount > 0 && maxAmount > 0 && maxAmount > minAmount -> {
                js = "money < $maxAmount && money > $minAmount"
                input = context.getString(
                    R.string.money_range_info,
                    minAmount.toString(),
                    maxAmount.toString()
                )
            }

            minAmount > 0 && maxAmount > 0 && maxAmount == minAmount -> {
                js = "money == $minAmount"
                input = context.getString(R.string.money_equal_info, minAmount.toString())
            }
        }

        if (js.isEmpty()) {
            ToastUtils.info(R.string.money_error)
            return
        }

        val newData = mutableMapOf<String, Any>(
            "js" to js,
            "minAmount" to minAmount,
            "maxAmount" to maxAmount,
            "text" to input,
            "type" to "moneyRange"
        )

        if (element.data["type"] == "moneyRange") {
            // 替换现有的类型条件
            element.replaceAsWaveTextview(input, newData, element.connector) { elem, view ->
                if (readOnly) return@replaceAsWaveTextview
                inputMoneyRange(elem)
            }
        } else {
            // 添加新的类型条件
            flexboxLayout?.appendWaveTextview(input, element, true, newData) { elem, view ->
                if (readOnly) return@appendWaveTextview
                inputMoneyRange(elem)
            }
        }
    }

    /**
     * 处理账本选择点击事件
     */
    private fun onClickBook(element: FlowElement) {
        BaseSheetDialog.create<BookSelectorDialog>(context)
            .setShowSelect(false)
            .setCallback { bookItem, _ ->
                bookName = bookItem.name
                remoteBookId = bookItem.remoteId

                element.remove().setAsWaveTextview(bookName, element.connector) { elem, view ->
                    if (readOnly) return@setAsWaveTextview
                    onClickBook(elem)
                }

                Logger.d("选择账本: ${bookItem.name} (${bookItem.remoteId})")
            }
            .show(cancel = true)
    }

    /**
     * 处理分类选择点击事件
     */
    private fun onClickCategory(element: FlowElement) {
        BaseSheetDialog.create<BookSelectorDialog>(context)
            .setShowSelect(true)
            .setCallback { bookModel, type ->
                BaseSheetDialog.create<CategorySelectorDialog>(context)
                    .setBook(bookModel.remoteId)
                    .setType(type)
                    .setCallback { parent, child ->
                        val categoryName = if (parent == null) {
                            "其他"
                        } else {
                            BillTool.getCateName(parent.name!!, child?.name)
                        }

                        category = categoryName

                        element.remove()
                            .setAsWaveTextview(category, element.connector) { elem, view ->
                                if (readOnly) return@setAsWaveTextview
                                onClickCategory(elem)
                            }

                        Logger.d("选择分类: $categoryName")
                    }
                    .show(cancel = true)
            }
            .show(cancel = true)
    }
}