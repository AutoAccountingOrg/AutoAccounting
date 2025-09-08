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

package net.ankio.auto.ui.components

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.google.android.flexbox.FlexboxLayout
import net.ankio.auto.App
import net.ankio.auto.R
import net.ankio.auto.storage.Logger
import net.ankio.auto.utils.SystemUtils
import net.ankio.auto.ui.utils.toThemeColor

/**
 * 流动布局元素类型定义
 * 使用密封类确保类型安全，避免魔法数字的使用
 */
sealed class FlowElementType {
    /** 按钮类型：可点击的操作按钮，如"与"、"或"连接符 */
    object Button : FlowElementType()

    /** 静态文本类型：不可交互的展示文本 */
    object Text : FlowElementType()

    /** 波浪文本类型：可交互的规则文本，支持字符级别的点击操作 */
    data class Wave(
        val isConnector: Boolean = false,  // 是否为连接符（与/或按钮）
        val isFirst: Boolean = false       // 是否为首个波浪文本（影响删除行为）
    ) : FlowElementType()
}


/**
 * 流动布局元素类
 *
 * 负责管理流动布局中的各种UI元素，包括：
 * - 可交互的波浪文本（支持字符级点击）
 * - 静态文本显示
 * - 操作按钮（连接符等）
 *
 * 核心特性：
 * 1. 动态插入：支持在指定位置插入元素
 * 2. 类型安全：使用密封类管理元素类型
 * 3. 状态管理：维护插入位置和元素数据
 * 4. 交互回调：支持自定义点击事件处理
 *
 * @param context Android上下文
 * @param flowLayoutManager 流动布局管理器，负责实际的视图操作
 * @param prev 前一个元素，用于确定插入位置（null表示末尾添加）
 * @param initData 初始化数据（可选）
 */
class FlowElement(
    private val context: Context,
    private val flowLayoutManager: FlowLayoutManager,
    prev: FlowElement? = null,
    initData: MutableMap<String, Any>? = null,
) {
    /** 当前元素包含的所有视图 */
    private var elements = ArrayList<View>()

    /** 元素的动态数据存储，支持各种类型的键值对 */
    var data = mutableMapOf<String, Any>()

    /** 元素类型，决定渲染方式和交互行为 */
    var elementType: FlowElementType = FlowElementType.Button

    /** 下一个视图的插入位置索引 */
    private var index = 0

    /** 是否为连接符元素（"与"/"或"切换按钮） */
    var connector = false

    /** 波浪文本的点击回调函数 */
    var waveCallback: ((FlowElement, TextView) -> Unit)? = null

    /** 是否为首个波浪文本视图（影响删除时的行为） */
    var firstWaveTextView = false

    /** 连接符视图的引用（延迟初始化） */
    private lateinit var connectorView: TextView

    init {
        // 计算插入位置：基于前一个元素或布局末尾
        index = calculateInsertPosition(prev)

        // 清理可能存在的旧视图
        this.remove()

        // 初始化数据存储
        data = initData ?: mutableMapOf("text" to "")
    }

    /**
     * 计算插入位置的纯函数
     *
     * 位置计算策略：
     * - prev 为 null：在布局末尾添加
     * - prev 不为 null：在 prev 元素的最后一个视图之后插入
     *
     * @param prev 前一个元素，用于确定插入位置
     * @return 插入位置的索引
     */
    private fun calculateInsertPosition(prev: FlowElement?): Int {
        return when (prev) {
            null -> flowLayoutManager.childCount // 末尾添加
            else -> prev.getViewStart()      // prev 元素之后插入
        }
    }

    /**
     * 移除当前元素的所有视图
     *
     * 移除策略：
     * 1. 保存当前插入位置，避免状态混乱
     * 2. 逐个移除所有关联的视图
     * 3. 清理内部状态，保持对象可重用
     *
     * @return 返回自身，支持链式调用
     */
    fun remove(): FlowElement {
        // 保存当前插入位置，确保移除后状态一致
        val currentInsertPos = getViewStart()

        // 移除所有元素视图
        elements.forEach { view ->
            flowLayoutManager.removeView(view)
        }

        // 移除连接符视图（如果存在）
        if (::connectorView.isInitialized) {
            flowLayoutManager.removeView(connectorView)
        }

        // 清理内部状态
        elements.clear()
        index = currentInsertPos

        return this
    }

    /**
     * 获取下一个插入位置
     *
     * 位置计算逻辑：
     * - 如果没有元素：返回当前索引
     * - 如果有元素：返回最后一个元素的位置 + 1
     *
     * 这个方法体现了"好品味"：消除了特殊情况的复杂判断
     *
     * @return 下一个元素应该插入的位置索引
     */
    private fun getInsertPosition(): Int {
        return if (elements.isEmpty()) {
            index
        } else {
            flowLayoutManager.indexOfChild(elements.last()) + 1
        }
    }

    /**
     * 获取元素在布局中的起始位置
     *
     * @return 第一个视图的位置索引，如果没有视图则返回当前索引
     */
    fun getViewStart(): Int {
        return if (elements.isEmpty()) index else flowLayoutManager.indexOfChild(elements.first())
    }

    /**
     * 获取元素在布局中的结束位置
     *
     * @return 最后一个视图的位置索引，如果没有视图则返回当前索引
     */
    fun getViewEnd(): Int {
        return if (elements.isEmpty()) index else flowLayoutManager.indexOfChild(elements.last())
    }

    /**
     * 设置为波浪文本视图
     *
     * 波浪文本是一种特殊的交互式文本显示方式：
     * 1. 将文本按字符拆分为独立的可点击元素
     * 2. 支持字符级别的点击回调
     * 3. 可以包含连接符（"与"/"或"按钮）
     * 4. 支持长按删除操作
     *
     * @param text 要显示的文本内容
     * @param connector 是否需要创建连接符按钮
     * @param callback 字符点击时的回调函数
     * @return 处理完成后的插入位置索引
     */
    fun setAsWaveTextview(
        text: String,
        connector: Boolean = false,
        callback: ((FlowElement, TextView) -> Unit)?,
    ): Int {
        // 设置元素类型和回调
        elementType = FlowElementType.Wave(isConnector = connector, isFirst = firstWaveTextView)
        waveCallback = callback


        // 处理首个波浪文本的特殊逻辑
        setupFirstWavePosition()

        // 在当前波浪文本之前创建连接符（用于显示“且/或”在两条件之间）
        if (shouldCreateConnector(connector)) {
            createConnectorButton()
        }

        // 创建字符级别的波浪文本视图
        createWaveTextViews(text, callback)
        return index
    }

    /**
     * 使用新的数据和文本替换当前波浪文本视图
     * - 保留 jsPre（连接符）
     * - 原子性更新 data 并重建视图
     */
    fun replaceAsWaveTextview(
        text: String,
        newData: MutableMap<String, Any>,
        connector: Boolean = false,
        callback: ((FlowElement, TextView) -> Unit)?,
    ): Int {
        val jsPre = data["jsPre"]
        data.clear()
        data.putAll(newData)
        if (jsPre != null) data["jsPre"] = jsPre
        return remove().setAsWaveTextview(text, connector, callback)
    }

    /**
     * 判断是否需要创建连接器按钮
     *
     * 连接器创建条件：
     * 1. 布局中已存在波浪文本元素（findFirstWave() != null）
     * 2. 当前元素需要连接符（connector = true）
     *
     * @param connector 是否请求创建连接符
     * @return true 如果需要创建连接器按钮
     */
    private fun shouldCreateConnector(connector: Boolean): Boolean {
        return flowLayoutManager.findFirstWave() != null && connector
    }

    /**
     * 创建连接器按钮（"与"/"或"切换按钮）
     *
     * 连接器用于在多个波浪文本之间建立逻辑关系：
     * - "与"（&&）：所有条件都必须满足
     * - "或"（||）：任意条件满足即可
     *
     * 用户可以通过点击按钮在两种逻辑关系之间切换
     */
    private fun createConnectorButton() {
        this.connector = true
        val content = getConnectorText()
        // 直接创建一个按钮视图，但不改变当前元素类型
        val buttonColor =
            com.google.android.material.R.attr.colorOnSecondaryContainer.toThemeColor()
        val textView = createBaseTextView(
            text = content,
            textColor = buttonColor,
            isClickable = true,
            backgroundResource = R.drawable.rounded_border3,
            gravity = Gravity.CENTER,
            padding = intArrayOf(10, 10, 10, 10),
            width = net.ankio.auto.utils.SystemUtils.dp2px(50f),
            textSizeScale = 0.7f
        )
        textView.setOnClickListener {
            toggleConnectorType(textView)
        }
        elements.add(textView)
        flowLayoutManager.addView(textView, index++)
    }

    /**
     * 获取连接器显示文本
     *
     * 根据 data["jsPre"] 中存储的JavaScript逻辑操作符决定显示文本：
     * - "||" 对应 "或"
     * - "&&" 对应 "与"（默认值）
     *
     * @return 本地化的连接符文本
     */
    private fun getConnectorText(): String {
        return if (data.containsKey("jsPre")) {
            // 检查已存储的JavaScript操作符
            if ((data["jsPre"] as String).contains("||")) {
                context.getString(R.string.or)
            } else {
                context.getString(R.string.and)
            }
        } else {
            // 默认使用"与"逻辑
            data["jsPre"] = " && "
            context.getString(R.string.and)
        }
    }

    /**
     * 切换连接器类型（与/或）
     *
     * 切换逻辑：
     * - 当前显示"或" → 切换为"与"（&&）
     * - 当前显示"与" → 切换为"或"（||）
     *
     * 同时更新data中的JavaScript表达式和UI显示
     *
     * @param view 连接器按钮的TextView
     */
    private fun toggleConnectorType(view: TextView) {
        view.text = if (view.text == context.getString(R.string.or)) {
            // 从"或"切换为"与"
            data["jsPre"] = " && "
            context.getString(R.string.and)
        } else {
            // 从"与"切换为"或"
            data["jsPre"] = " || "
            context.getString(R.string.or)
        }
    }

    /**
     * 设置首个波浪文本的位置标记
     *
     * 首个波浪文本具有特殊意义：
     * - 影响长按删除的行为（会触发"添加"按钮）
     * - 用于确定是否需要显示连接符
     *
     * 逻辑简化：如果布局中没有其他波浪文本，当前就是第一个
     */
    private fun setupFirstWavePosition() {
        if (flowLayoutManager.findFirstWave() == null) {
            firstWaveTextView = true
            // 保持当前插入位置不变，体现"好品味"的设计
        }
    }

    /**
     * 创建波浪文本视图集合
     *
     * 性能优化策略：
     * 1. 避免使用 split("") 产生的空字符串开销
     * 2. 直接遍历字符串，减少中间对象创建
     * 3. 在文本前后添加空格，改善视觉效果
     *
     * @param text 原始文本内容
     * @param callback 字符点击回调函数
     */
    private fun createWaveTextViews(
        text: String,
        callback: ((FlowElement, TextView) -> Unit)?
    ) {
        // 在文本前后添加空格，改善视觉间距
        val processedText = " $text "

        // 直接遍历字符，体现 Linus "简单就是美" 的哲学
        for (char in processedText) {
            val waveTextView = createWaveView(char.toString())
            configureWaveTextView(waveTextView, callback)
            elements.add(waveTextView)
            flowLayoutManager.addView(waveTextView, index++)
        }
    }

    /**
     * 配置波浪文本视图的交互事件
     *
     * 交互设计：
     * - 单击：触发字符级别的自定义回调
     * - 长按：根据元素类型执行删除或特殊操作
     *
     * @param waveTextView 要配置的波浪文本视图
     * @param callback 单击时的回调函数
     */
    private fun configureWaveTextView(
        waveTextView: TextView,
        callback: ((FlowElement, TextView) -> Unit)?
    ) {
        // 配置单击事件：执行自定义回调
        waveTextView.setOnClickListener {
            callback?.invoke(this, waveTextView)
        }

        // 配置长按事件：执行删除逻辑
        waveTextView.setOnLongClickListener {
            handleWaveTextLongClick()
            true // 消费事件，避免冒泡
        }
    }

    /**
     * 处理波浪文本的长按删除事件
     *
     * 删除行为策略：
     * 1. 首个波浪文本：删除后触发"添加"按钮，用于创建新的规则
     * 2. 连接符元素：直接删除，不触发其他操作
     * 3. 普通波浪文本：（当前未处理，可能需要扩展）
     *
     * 这种设计避免了复杂的分支判断，体现了"好品味"
     */
    private fun handleWaveTextLongClick() {
        when {
            firstWaveTextView -> {
                // 删除首个波浪文本，并触发添加按钮重新开始
                flowLayoutManager.removeElement(this)
                flowLayoutManager.findAddButton()?.callOnClick()
            }

            connector -> {
                // 删除连接符，简单直接
                flowLayoutManager.removeElement(this)
            }
            // 注意：普通波浪文本的长按当前没有处理
            // 这可能是有意的设计选择，避免误删除
        }
    }

    /**
     * 创建基础 TextView 的通用工厂方法
     *
     * 这是一个体现 Linus "好品味" 的设计：
     * 1. 消除重复代码：三种文本视图都使用这个统一方法
     * 2. 参数化配置：通过可选参数支持不同需求
     * 3. 默认值合理：大部分情况下使用默认配置即可
     * 4. 职责单一：只负责创建和配置 TextView
     *
     * @param text 显示文本
     * @param textColor 文字颜色（可选）
     * @param isClickable 是否可点击
     * @param backgroundResource 背景资源ID（可选）
     * @param gravity 文本对齐方式（可选）
     * @param padding 内边距数组 [left, top, right, bottom]（可选）
     * @param width 固定宽度（可选）
     * @param textSizeScale 字体大小缩放比例（可选，默认1.0为原始大小）
     * @return 配置完成的 TextView 实例
     */
    private fun createBaseTextView(
        text: String,
        textColor: Int? = null,
        isClickable: Boolean = false,
        backgroundResource: Int? = null,
        gravity: Int? = null,
        padding: IntArray? = null, // [left, top, right, bottom]
        width: Int? = null,
        textSizeScale: Float = 1.0f
    ): TextView {
        val textView = TextView(context)

        // 基础配置
        textView.text = text
        textView.setTextAppearance(flowLayoutManager.textAppearance)

        // 可选配置：字体大小缩放
        if (textSizeScale != 1.0f) {
            val currentSize = textView.textSize
            textView.textSize =
                currentSize * textSizeScale / context.resources.displayMetrics.scaledDensity
        }

        // 可选配置：文字颜色
        textColor?.let { textView.setTextColor(it) }

        // 可选配置：点击属性
        if (isClickable) {
            textView.isClickable = true
            textView.isFocusable = true
            textView.background = ResourcesCompat.getDrawable(
                context.resources,
                R.drawable.ripple_effect,
                context.theme
            )
        }

        // 可选配置：背景资源
        backgroundResource?.let { textView.setBackgroundResource(it) }

        // 可选配置：对齐方式
        gravity?.let { textView.gravity = it }

        // 可选配置：内边距（带参数验证）
        padding?.let {
            require(it.size == 4) { "内边距数组必须包含4个元素: [left, top, right, bottom]" }
            textView.setPadding(it[0], it[1], it[2], it[3])
        } ?: textView.setPadding(0, 0, 0, 0)

        // 可选配置：固定宽度
        width?.let { textView.width = it }

        // 设置 FlexboxLayout 布局参数
        val layoutParams = FlexboxLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        textView.layoutParams = layoutParams

        return textView
    }

    /**
     * 创建波浪文本视图的专用工厂方法
     *
     * 波浪文本特点：
     * - 使用主题色突出显示
     * - 支持点击交互
     * - 适用于字符级别的操作
     *
     * @param text 显示的字符文本
     * @return 配置好的波浪文本视图
     */
    private fun createWaveView(text: String): TextView {
        val primaryColor = com.google.android.material.R.attr.colorPrimary.toThemeColor()
        return createBaseTextView(
            text = text,
            textColor = primaryColor,
            isClickable = true
        )
    }

    /**
     * 设置为静态文本视图
     *
     * 用于显示不可交互的文本内容：
     * - 按字符拆分显示
     * - 无点击交互
     * - 适用于信息展示
     *
     * @param text 要显示的文本内容
     */
    fun setAsTextView(text: String) {
        elementType = FlowElementType.Text

        // 直接遍历字符，避免 split("") 的性能开销
        for (char in text) {
            val textView = createBaseTextView(char.toString())
            elements.add(textView)
            flowLayoutManager.addView(textView, index++)
        }
    }

    /**
     * 设置为按钮视图
     *
     * 按钮特点：
     * - 具有明显的视觉边界（圆角边框）
     * - 居中对齐文本
     * - 固定尺寸，避免布局不一致
     * - 支持点击回调
     *
     * @param text 按钮显示文本
     * @param callback 点击时的回调函数
     */
    fun setAsButton(
        text: String,
        callback: (FlowElement, TextView) -> Unit,
    ) {
        elementType = FlowElementType.Button

        // 获取适合按钮的文字颜色
        val buttonColor =
            com.google.android.material.R.attr.colorOnSecondaryContainer.toThemeColor()

        // 创建具有统一样式的按钮
        val textView = createBaseTextView(
            text = text,
            textColor = buttonColor,
            isClickable = true,
            backgroundResource = R.drawable.rounded_border3,
            gravity = Gravity.CENTER,
            padding = intArrayOf(10, 10, 10, 10),
            width = SystemUtils.dp2px(50f), // 固定宽度，确保一致性
            textSizeScale = 0.7f // 按钮字体比普通文本小10%，提升视觉层次
        )

        // 配置点击事件
        textView.setOnClickListener {
            callback(this, textView)
        }

        elements.add(textView)
        flowLayoutManager.addView(textView, index++)
    }

    /**
     * 获取第一个视图元素
     *
     * 用于：
     * - 调试和检查
     * - 特殊的布局操作
     * - 获取元素的起始视图
     *
     * @return 第一个视图，如果没有元素则返回 null
     */
    fun getFirstView(): View? {
        return elements.firstOrNull()
    }
}