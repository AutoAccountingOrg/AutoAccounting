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
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import com.google.android.flexbox.FlexboxLayout

class FlowLayoutManager(context: Context, attrs: AttributeSet) : FlexboxLayout(context, attrs) {
    var textAppearance = com.google.android.material.R.style.TextAppearance_Material3_HeadlineLarge
    private var elements: ArrayList<FlowElement> = ArrayList() // 更清晰的命名

    // 常量定义，避免魔法字符串
    companion object {
        private const val ADD_BUTTON_TEXT = "+"
    }

    /**
     * 创建并添加 FlowElement 的通用工厂方法
     * 消除重复代码，体现"好品味"
     */
    private fun createAndAddElement(
        elem: FlowElement? = null,
        initData: MutableMap<String, Any>? = null,
        setup: (FlowElement) -> Unit
    ): FlowElement {
        val flowElement = FlowElement(context, this, elem, initData)
        setup(flowElement)
        elements.add(flowElement)
        return flowElement
    }

    fun appendTextView(
        text: String,
        elem: FlowElement? = null,
    ): FlowElement = createAndAddElement(elem) { element ->
        element.setAsTextView(text)
    }

    fun appendAddButton(
        callback: (FlowElement, TextView) -> Unit,
        elem: FlowElement? = null,
    ): FlowElement = createAndAddElement(elem) { element ->
        element.setAsButton(ADD_BUTTON_TEXT) { it, view ->
            callback(it, view)
        }
        element.data["text"] = ADD_BUTTON_TEXT
    }

    fun appendWaveTextview(
        text: String,
        elem: FlowElement? = null,
        connector: Boolean = false,
        data: MutableMap<String, Any>? = null,
        callback: (FlowElement, TextView) -> Unit,
    ): FlowElement = createAndAddElement(elem, data) { element ->
        element.setAsWaveTextview(text, connector) { it, view ->
            callback(it, view)
        }
    }

    /**
     * 移除所有元素
     * 修复命名：removed -> remove
     */
    fun removeAllElements() {
        elements.forEach { it.remove() }
        elements.clear()
    }

    /**
     * 移除指定元素
     * 修复命名：removed -> remove
     */
    fun removeElement(flowElement: FlowElement) {
        flowElement.remove()
        elements.remove(flowElement)
    }

    /**
     * 查找添加按钮
     * 优化：使用 find 替代 forEach，更高效且符合 Kotlin 习惯
     */
    fun findAddButton(): View? {
        return elements.find { element ->
            element.elementType == FlowElementType.Button &&
                    element.data["text"] == ADD_BUTTON_TEXT
        }?.getFirstView()
    }

    /**
     * 查找添加按钮
     * 优化：使用 find 替代 forEach，更高效且符合 Kotlin 习惯
     */
    fun findAddButtonElement(): FlowElement? {
        return elements.find { element ->
            element.elementType == FlowElementType.Button &&
                    element.data["text"] == ADD_BUTTON_TEXT
        }
    }

    /**
     * 查找第一个波浪文本
     * 优化：使用 find 替代 forEach
     */
    fun findFirstWave(): View? {
        return elements.find { element ->
            element.elementType is FlowElementType.Wave && element.firstWaveTextView
        }?.getFirstView()
    }

    /**
     * 获取所有元素
     * 重命名：getViewMap -> getElements，更清晰
     */
    fun getElements(): List<FlowElement> = elements.toList() // 返回不可变列表，更安全

    /**
     * 添加通用按钮
     * 使用工厂方法消除重复
     */
    fun appendButton(
        text: String,
        elem: FlowElement? = null,
        callback: (FlowElement, TextView) -> Unit = { _, _ -> } // 默认空回调
    ): FlowElement = createAndAddElement(elem) { element ->
        element.setAsButton(text, callback)
    }

}