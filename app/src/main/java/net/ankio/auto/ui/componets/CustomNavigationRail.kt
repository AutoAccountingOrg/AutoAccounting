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
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import net.ankio.auto.App
import net.ankio.auto.R
import net.ankio.auto.ui.utils.RailMenuItem


class CustomNavigationRail @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        orientation = VERTICAL
    }

    private val menuItems = mutableListOf<RailMenuItem>()
    private var onItemSelectedListener: ((RailMenuItem) -> Unit)? = null
    fun triggerFirstItem() {
        if (childCount > 0) getChildAt(0).performClick()
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

        val selectColor  = App.getThemeAttrColor(com.google.android.material.R.attr.colorOnSecondaryContainer)

        view.setOnClickListener {
            onItemSelectedListener?.invoke(menuItem)
            // 移除其他项目的背景色
            for (i in 0 until childCount) {
                getChildAt(i).background = null
                getChildAt(i).findViewById<TextView>(R.id.textView).setTextColor(textColor)
            }
            // 设置选中项目的背景色
            view.setBackgroundResource(R.drawable.navigation_rail_item_background)
            textView.findViewById<TextView>(R.id.textView).setTextColor(selectColor)
        }

        addView(view)
    }

    fun setOnItemSelectedListener(listener: (RailMenuItem) -> Unit) {
        onItemSelectedListener = listener
    }
}