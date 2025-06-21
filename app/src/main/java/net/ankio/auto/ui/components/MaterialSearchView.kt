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

package net.ankio.auto.ui.components

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.widget.SearchView
import net.ankio.auto.App

class MaterialSearchView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SearchView(context, attrs, defStyleAttr) {

    init {
        setupSearchView()
    }

    private fun setupSearchView() {
        // 设置背景

        // 获取搜索输入框并设置样式
        val searchEditText = findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        searchEditText?.apply {
            setHintTextColor(App.getThemeAttrColor(com.google.android.material.R.attr.colorOnSurfaceVariant))
            setTextColor(App.getThemeAttrColor(com.google.android.material.R.attr.colorOnSurface))
            setBackgroundColor(Color.TRANSPARENT)
        }

        // 修改搜索图标样式
        val searchIcon = findViewById<ImageView>(androidx.appcompat.R.id.search_mag_icon)
        searchIcon?.setColorFilter(App.getThemeAttrColor(com.google.android.material.R.attr.colorOnSurface))

        // 修改清除按钮样式
        val closeButton = findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
        closeButton?.setColorFilter(App.getThemeAttrColor(com.google.android.material.R.attr.colorOnSurface))

        // 添加展开和收起动画
        setOnSearchClickListener { applyExpandAnimation() }
        setOnCloseListener {
            applyCollapseAnimation()
            false
        }
    }

    /**
     * 展开动画
     */
    private fun applyExpandAnimation() {
        animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(200)
            .start()
    }

    /**
     * 收起动画
     */
    private fun applyCollapseAnimation() {
        animate()
            .alpha(0f)
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(200)
            .start()
    }
}
