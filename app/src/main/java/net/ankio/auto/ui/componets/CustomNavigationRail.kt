/*
 * Copyright (C) 2024 ankio(ankio@ankio.net)
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

package net.ankio.auto.ui.componets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import net.ankio.auto.App
import net.ankio.auto.R
import net.ankio.auto.ui.models.RailMenuItem


class CustomNavigationRail @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    // ScrollView 包裹 LinearLayout
    private val scrollView: ScrollView

    init {
        orientation = VERTICAL

        // 创建一个 ScrollView
        scrollView = ScrollView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }

        // 创建一个内部的 LinearLayout，作为 ScrollView 的子视图
        val internalLayout = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        // 将内部的 LinearLayout 添加到 ScrollView 中
        scrollView.addView(internalLayout)

        // 将 ScrollView 添加到 CustomNavigationRail
        addView(scrollView)
    }

    private val menuItems = mutableListOf<RailMenuItem>()
    private var onItemSelectedListener: ((RailMenuItem) -> Unit)? = null

    fun triggerFirstItem(): Boolean {
        if (scrollView.getChildAt(0) is LinearLayout && (scrollView.getChildAt(0) as LinearLayout).childCount > 0) {
            (scrollView.getChildAt(0) as LinearLayout).getChildAt(0).performClick()
            return true
        }
        return false
    }

    fun addMenuItem(menuItem: RailMenuItem) {
        menuItems.add(menuItem)
        val view = LayoutInflater.from(context).inflate(R.layout.custom_navigation_rail_item, this, false)
        val iconView: AppCompatImageView = view.findViewById(R.id.iconView)
        val textView: TextView = view.findViewById(R.id.textView)

        iconView.setImageDrawable(menuItem.icon)
        textView.text = menuItem.text

        val textColor = App.getThemeAttrColor(com.google.android.material.R.attr.colorOnBackground)
        textView.setTextColor(textColor)

        val selectColor = App.getThemeAttrColor(com.google.android.material.R.attr.colorOnSecondaryContainer)

        view.setOnClickListener {
            onItemSelectedListener?.invoke(menuItem)
            // 移除其他项目的背景色
            for (i in 0 until (scrollView.getChildAt(0) as LinearLayout).childCount) {
                (scrollView.getChildAt(0) as LinearLayout).getChildAt(i).background = null
                (scrollView.getChildAt(0) as LinearLayout).getChildAt(i)
                    .findViewById<TextView>(R.id.textView).setTextColor(textColor)
            }
            // 设置选中项目的背景色
            view.setBackgroundResource(R.drawable.navigation_rail_item_background)
            textView.setTextColor(selectColor)
        }

        // 添加菜单项到内部的 LinearLayout 中
        (scrollView.getChildAt(0) as LinearLayout).addView(view)
    }

    fun setOnItemSelectedListener(listener: (RailMenuItem) -> Unit) {
        onItemSelectedListener = listener
    }
}