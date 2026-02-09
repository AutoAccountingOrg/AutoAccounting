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
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import net.ankio.auto.R
import net.ankio.auto.databinding.StatusPageBinding
import androidx.core.view.isVisible

/**
 * 统一的状态页组件：加载/空/错误/内容 四种状态切换。
 * 保持原有对外行为与属性不变，仅优化实现与可读性。
 */
class StatusPage @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    // 视图绑定（一次性绑定，避免可空引用）
    private val binding: StatusPageBinding =
        StatusPageBinding.inflate(LayoutInflater.from(context), this, true)

    // 分组容器：用于整体切换四种状态
    private val groupLoading: View = binding.groupLoading
    private val groupEmpty: View = binding.groupEmpty
    private val groupError: View = binding.groupError
    private val groupContent: View = binding.groupContent

    // 对外可用的内容与刷新控件（保持可空类型以兼容现有调用处）
    var contentView: RecyclerView? = binding.contentView

    // 由外部页面提供 SwipeRefreshLayout（当 StatusPage 外置于外部 Swipe 包裹时）
    var swipeRefreshLayout: SwipeRefreshLayout? = null


    init {
        // 读取自定义属性以控制容器高度
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.StatusPage,
            0,
            0,
        ).apply {
            try {
                val height = getString(R.styleable.StatusPage_innerHeight)
                val layoutParams = binding.root.layoutParams
                when (height) {
                    "wrap_content" -> layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    "match_parent" -> layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                }
                binding.root.layoutParams = layoutParams
            } finally {
                recycle()
            }
        }

        showContent()
    }

    /**
     * 显示加载状态。
     * 当通过 [useSwipeLayout] 设置为使用 Swipe 刷新动画时，将触发顶部刷新动画并显示内容分组；
     * 否则显示整页加载分组覆盖层。
     */
    fun showLoading() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout?.isRefreshing = true
            setVisibility(content = true)
        } else {
            setVisibility(loading = true)
        }
    }

    /** 显示空状态，并确保刷新动画关闭。 */
    fun showEmpty() {
        swipeRefreshLayout?.isRefreshing = false
        setVisibility(empty = true)
    }

    /** 显示错误状态，并确保刷新动画关闭。 */
    fun showError() {
        swipeRefreshLayout?.isRefreshing = false
        setVisibility(error = true)
    }

    /** 显示内容状态，并确保刷新动画关闭。 */
    fun showContent() {
        swipeRefreshLayout?.isRefreshing = false
        setVisibility(content = true)
    }


    /** 是否处于加载状态 */
    fun isLoading(): Boolean = groupLoading.isVisible

    fun isContent(): Boolean = groupContent.isVisible

    /** 切换四个分组容器的可见性。 */
    private fun setVisibility(
        loading: Boolean = false,
        empty: Boolean = false,
        error: Boolean = false,
        content: Boolean = false,
    ) {
        groupLoading.visibility = if (loading) View.VISIBLE else View.GONE
        groupEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        groupError.visibility = if (error) View.VISIBLE else View.GONE
        groupContent.visibility = if (content) View.VISIBLE else View.GONE
    }
}