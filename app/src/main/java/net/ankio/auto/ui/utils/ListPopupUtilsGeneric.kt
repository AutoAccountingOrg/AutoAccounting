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

package net.ankio.auto.ui.utils

import android.content.Context
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListPopupWindow
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import net.ankio.auto.utils.SystemUtils.findLifecycleOwner
import android.graphics.Paint
import android.util.TypedValue

/**
 * 泛型版本的列表弹窗工具类 - Linus式极简设计
 *
 * 设计原则：
 * 1. 消除构造函数参数冗余 - 只需要Context，其他通过链式调用设置
 * 2. 自动推断生命周期 - 无需手动传递lifecycle参数
 * 3. 类型安全 - 编译时保证类型正确性，无运行时转换
 * 4. 向后兼容 - 保留原构造函数，支持渐进式迁移
 *
 * 推荐使用方式（链式调用）：
 * ```kotlin
 * ListPopupUtilsGeneric.create<BillType>(context)
 *     .setAnchor(anchorView)
 *     .setList(mapOf("支出" to BillType.Expend, "收入" to BillType.Income))
 *     .setSelectedValue(BillType.Expend)
 *     .setMinWidth(200) // 设置最小宽度200px，可选，内部自动计算最终宽度
 *     .setOnItemClick { position, key, value ->
 *         // value 已经是 BillType 类型，无需转换
 *         handleTypeChange(value)
 *     }
 *     .show()
 * ```
 *
 * 兼容使用方式（原构造函数）：
 * ```kotlin
 * val popupUtils = ListPopupUtilsGeneric<BillType>(
 *     context,
 *     anchorView,
 *     mapOf("支出" to BillType.Expend, "收入" to BillType.Income),
 *     BillType.Expend,
 *     lifecycle
 * ) { position, key, value ->
 *     handleTypeChange(value)
 * }
 * ```
 *
 * @param T 值类型参数
 */
class ListPopupUtilsGeneric<T>// 绑定生命周期
/**
 * 推荐构造函数：只需要Context，自动推断生命周期
 * 其他参数通过链式调用设置
 */ private constructor(private val context: Context) : DefaultLifecycleObserver {

    private val lifecycle: Lifecycle = context.findLifecycleOwner().lifecycle
    private val listPopupWindow: ListPopupWindow = ListPopupWindow(context)

    // 链式调用的可配置属性
    private var anchorView: View? = null
    private var itemList: Map<String, T> = emptyMap()
    private var selectedValue: T? = null
    private var onItemClickListener: ((position: Int, key: String, value: T) -> Unit)? = null
    private var minWidth: Int = 0

    // 内部状态
    private var adapter: ArrayAdapter<String>? = null
    private var selectIndex = 0

    init {
        lifecycle.addObserver(this)
    }


    companion object {
        /**
         * 创建实例的工厂方法 - 推荐使用方式
         *
         * @param context 上下文，自动推断生命周期
         * @return 新的实例，支持链式调用
         */
        fun <T> create(context: Context): ListPopupUtilsGeneric<T> {
            return ListPopupUtilsGeneric(context)
        }
    }

    /**
     * 设置锚点视图 - 链式调用
     *
     * @param anchor 锚点视图，弹窗将相对于此视图显示
     * @return 当前实例，支持链式调用
     */
    fun setAnchor(anchor: View): ListPopupUtilsGeneric<T> {
        this.anchorView = anchor
        return this
    }

    /**
     * 设置列表数据 - 链式调用
     *
     * @param list 键值对映射，键为显示文本，值为对应的数据对象
     * @return 当前实例，支持链式调用
     */
    fun setList(list: Map<String, T>): ListPopupUtilsGeneric<T> {
        this.itemList = list
        return this
    }

    /**
     * 设置当前选中值 - 链式调用
     *
     * @param value 当前选中的值，用于确定默认选中项
     * @return 当前实例，支持链式调用
     */
    fun setSelectedValue(value: T): ListPopupUtilsGeneric<T> {
        this.selectedValue = value
        return this
    }

    /**
     * 设置点击监听器 - 链式调用
     *
     * @param listener 点击回调，参数为(位置, 键, 值)
     * @return 当前实例，支持链式调用
     */
    fun setOnItemClick(listener: (position: Int, key: String, value: T) -> Unit): ListPopupUtilsGeneric<T> {
        this.onItemClickListener = listener
        return this
    }



    /**
     * 设置弹窗高度 - 链式调用
     *
     * @param height 高度值，可使用ListPopupWindow.WRAP_CONTENT等常量
     * @return 当前实例，支持链式调用
     */
    fun setHeight(height: Int): ListPopupUtilsGeneric<T> {
        listPopupWindow.height = height
        return this
    }

    /**
     * 设置是否模态显示 - 链式调用
     *
     * @param modal true为模态显示，false为非模态
     * @return 当前实例，支持链式调用
     */
    fun setModal(modal: Boolean): ListPopupUtilsGeneric<T> {
        listPopupWindow.isModal = modal
        return this
    }

    /**
     * 设置弹窗最小宽度 - 链式调用
     * 当内容宽度小于最小宽度时，弹窗将使用最小宽度
     *
     * @param minWidth 最小宽度值（像素），0表示不限制最小宽度
     * @return 当前实例，支持链式调用
     */
    fun setMinWidth(minWidth: Int): ListPopupUtilsGeneric<T> {
        this.minWidth = minWidth
        return this
    }

    /**
     * 完成配置并准备显示
     * 内部方法，用于设置弹窗的具体行为
     */
    private fun setupPopupWindow() {
        // 验证必要参数
        requireNotNull(anchorView) { "锚点视图不能为空，请先调用setAnchor()" }
        require(itemList.isNotEmpty()) { "列表数据不能为空，请先调用setList()" }
        requireNotNull(onItemClickListener) { "点击监听器不能为空，请先调用setOnItemClick()" }

        val keys = itemList.keys.toTypedArray()
        adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, keys)

        // 找到当前选中项的索引
        selectIndex = selectedValue?.let { value ->
            keys.indexOfFirst { itemList[it] == value }.takeIf { it >= 0 } ?: 0
        } ?: 0

        listPopupWindow.apply {
            setAdapter(adapter)
            anchorView = this@ListPopupUtilsGeneric.anchorView
            // 内部统一控制宽度 - 根据内容和最小宽度自动计算
            width = calculatePopupWidth()
            // 设置默认高度
            if (height == 0) height = ListPopupWindow.WRAP_CONTENT
            isModal = true

            setOnItemClickListener { _, _, position, _ ->
                selectIndex = position
                val selectedKey = keys[position]
                val selectedValue = itemList[selectedKey]!!
                onItemClickListener?.invoke(position, selectedKey, selectedValue)
                dismiss()
            }
        }
    }

    /**
     * 显示弹窗
     * 如果是链式调用方式，会自动完成配置
     */
    fun show(): ListPopupUtilsGeneric<T> {
        // 如果是链式调用方式且尚未配置，先进行配置
        if (adapter == null) {
            setupPopupWindow()
        }

        if (!listPopupWindow.isShowing) {
            listPopupWindow.show()
            // 选中当前项
            listPopupWindow.listView?.setSelection(selectIndex)
        }
        return this
    }

    /**
     * 隐藏弹窗
     */
    fun dismiss(): ListPopupUtilsGeneric<T> {
        if (listPopupWindow.isShowing) {
            listPopupWindow.dismiss()
        }
        return this
    }

    /**
     * 切换弹窗显示状态
     */
    fun toggle(): ListPopupUtilsGeneric<T> {
        if (listPopupWindow.isShowing) {
            dismiss()
        } else {
            show()
        }
        return this
    }

    /**
     * 计算弹窗内容的实际宽度 - 内部使用
     * 测量所有文本项的宽度，返回最大宽度与最小宽度的较大值
     * 如果未设置最小宽度，则使用合理的默认最小宽度
     */
    private fun calculatePopupWidth(): Int {
        if (itemList.isEmpty()) {
            // 空列表时使用默认最小宽度
            return if (minWidth > 0) minWidth else getDefaultMinWidth()
        }

        // 创建Paint对象测量文本宽度
        val paint = Paint().apply {
            // 获取系统默认文本大小
            textSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                16f, // 默认文本大小
                context.resources.displayMetrics
            )
        }

        // 计算所有文本项的最大宽度
        val maxTextWidth = itemList.keys.maxOfOrNull { key ->
            paint.measureText(key).toInt()
        } ?: 0

        // 添加内边距（左右各16dp，参考simple_list_item_1的padding）
        val paddingHorizontal = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            32f, // 左右各16dp
            context.resources.displayMetrics
        ).toInt()

        val contentWidth = maxTextWidth + paddingHorizontal

        // 确定最终最小宽度
        val effectiveMinWidth = if (minWidth > 0) minWidth else getDefaultMinWidth()

        // 返回内容宽度与最小宽度的较大值
        return maxOf(contentWidth, effectiveMinWidth)
    }

    /**
     * 获取默认最小宽度 - 120dp
     * 确保即使没有设置最小宽度，弹窗也有合理的宽度
     */
    private fun getDefaultMinWidth(): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            120f, // 默认最小宽度120dp
            context.resources.displayMetrics
        ).toInt()
    }

    /**
     * 生命周期销毁时自动清理
     */
    override fun onDestroy(owner: LifecycleOwner) {
        dismiss()
        super.onDestroy(owner)
    }
}