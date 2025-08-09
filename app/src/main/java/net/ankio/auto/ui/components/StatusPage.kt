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
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.CircularProgressIndicator
import net.ankio.auto.R
import net.ankio.auto.databinding.StatusPageBinding


class StatusPage : ConstraintLayout {

    private var loadingView: CircularProgressIndicator? = null
    private var emptyIcon: ImageView? = null
    private var emptyText: TextView? = null
    private var errorIcon: ImageView? = null
    private var errorText: TextView? = null

    /** 内容容器，用于承载自定义内容视图 */
    private var contentContainer: FrameLayout? = null

    /** 默认的RecyclerView，保持向后兼容 */
    var contentView: RecyclerView? = null
        private set

    /** 当前的自定义内容视图 */
    private var customContentView: View? = null

    /** 标记是否已经初始化完成 */
    private var isInitialized = false

    /** 临时存储在初始化完成前添加的子视图 */
    private val pendingChildViews = mutableListOf<Pair<View, ViewGroup.LayoutParams?>>()


    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    fun init(
        context: Context,
        attrs: AttributeSet?
    ) {
        val binding = StatusPageBinding.inflate(LayoutInflater.from(context), this, true)
        loadingView = binding.loadingView
        emptyIcon = binding.emptyIcon
        emptyText = binding.emptyText
        errorIcon = binding.errorIcon
        errorText = binding.errorText
        contentContainer = binding.contentContainer
        contentView = binding.contentView
        val root = binding.rootView
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.StatusPage,
            0,
            0,
        ).apply {
            try {
                val height = getString(R.styleable.StatusPage_innerHeight)
                val layoutParams = root.layoutParams
                //   layoutParams.height = if (height.equals("wrap_content")) ViewGroup.LayoutParams.WRAP_CONTENT else ViewGroup.LayoutParams.MATCH_PARENT
                root.layoutParams = layoutParams
            } finally {
                recycle()
            }

        }

        // 标记初始化完成
        isInitialized = true

        // 处理在初始化前添加的子视图
        processPendingChildViews()
    }

    /**
     * 处理在初始化完成前添加的子视图
     */
    private fun processPendingChildViews() {
        if (pendingChildViews.isNotEmpty()) {
            pendingChildViews.forEach { (view, layoutParams) ->
                addChildToContentContainer(view, layoutParams)
            }
            pendingChildViews.clear()
        }
    }

    /**
     * 重写addView方法，拦截子视图添加并重定向到内容容器
     */
    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        if (!isInitialized) {
            // 如果还未初始化完成，先存储子视图
            child?.let {
                pendingChildViews.add(Pair(it, params))
            }
        } else {
            // 已初始化，直接添加到内容容器
            addChildToContentContainer(child, params)
        }
    }

    /**
     * 将子视图添加到内容容器
     */
    private fun addChildToContentContainer(child: View?, params: ViewGroup.LayoutParams?) {
        child?.let { view ->
            contentContainer?.let { container ->
                // 隐藏默认的RecyclerView
                contentView?.visibility = View.GONE

                // 转换布局参数为FrameLayout.LayoutParams
                val frameParams = when (params) {
                    is FrameLayout.LayoutParams -> params
                    else -> FrameLayout.LayoutParams(
                        params?.width ?: FrameLayout.LayoutParams.MATCH_PARENT,
                        params?.height ?: FrameLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                container.addView(view, frameParams)
                customContentView = view
            }
        }
    }

    fun showLoading() {
        setVisibility(loading = true)
    }

    fun showEmpty() {
        setVisibility(empty = true)
    }

    fun showError() {
        setVisibility(error = true)
    }

    fun showContent() {
        setVisibility(content = true)
    }

    /**
     * 设置自定义内容视图（代码方式）
     *
     * @param view 要显示的自定义视图
     * @param layoutParams 可选的布局参数，如果为null则使用MATCH_PARENT, WRAP_CONTENT
     */
    fun setCustomContentView(view: View, layoutParams: FrameLayout.LayoutParams? = null) {
        contentContainer?.let { container ->
            // 清除所有现有的自定义内容（包括XML嵌套的）
            clearCustomContent()

            // 隐藏默认的RecyclerView
            contentView?.visibility = View.GONE

            // 添加新的自定义视图
            val params = layoutParams ?: FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            container.addView(view, params)
            customContentView = view
        }
    }

    /**
     * 清除所有自定义内容，只保留默认的RecyclerView
     */
    private fun clearCustomContent() {
        contentContainer?.let { container ->
            // 移除除RecyclerView外的所有子视图
            val childrenToRemove = mutableListOf<View>()
            for (i in 0 until container.childCount) {
                val child = container.getChildAt(i)
                if (child != contentView) {
                    childrenToRemove.add(child)
                }
            }
            childrenToRemove.forEach { child ->
                container.removeView(child)
            }
            customContentView = null
        }
    }

    /**
     * 移除自定义内容视图，恢复使用默认的RecyclerView
     */
    fun removeCustomContentView() {
        clearCustomContent()
        // 恢复显示默认的RecyclerView
        contentView?.visibility = View.VISIBLE
    }

    /**
     * 获取当前的自定义内容视图
     *
     * @return 当前的自定义视图，如果没有则返回null
     */
    fun getCustomContentView(): View? = customContentView

    /**
     * 检查是否正在使用自定义内容视图
     *
     * @return true如果当前使用自定义视图，false如果使用默认RecyclerView
     */
    fun isUsingCustomContent(): Boolean = customContentView != null

    private fun setVisibility(
        loading: Boolean = false,
        empty: Boolean = false,
        error: Boolean = false,
        content: Boolean = false
    ) {
        loadingView?.visibility = if (loading) View.VISIBLE else View.GONE

        emptyIcon?.visibility = if (empty) View.VISIBLE else View.GONE
        emptyText?.visibility = if (empty) View.VISIBLE else View.GONE

        errorIcon?.visibility = if (error) View.VISIBLE else View.GONE
        errorText?.visibility = if (error) View.VISIBLE else View.GONE

        contentContainer?.visibility = if (content) View.VISIBLE else View.GONE
    }
}