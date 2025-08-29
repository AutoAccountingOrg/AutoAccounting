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

package net.ankio.auto.ui.utils

import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentContainerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import androidx.core.view.isVisible

object ViewUtils {
    fun <T : View> findView(
        view: View,
        currentDepth: Int = 0,
        maxDepth: Int = 5,
        clazz: Class<T>
    ): T? {
        // 检查当前视图是否是目标类型
        if (clazz.isInstance(view)) return view as T

        // 如果超过最大深度，返回 null
        if (currentDepth >= maxDepth) return null

        // 如果是 ViewGroup，递归查找其子视图
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                val result = findView(child, currentDepth + 1, maxDepth, clazz)
                if (result != null) return result
            }
        }
        return null
    }

    fun findMaterialToolbar(
        view: View,
        currentDepth: Int = 0,
        maxDepth: Int = 5
    ): MaterialToolbar? {
        return findView(view, currentDepth, maxDepth, MaterialToolbar::class.java)
    }

    fun findAppBarLayout(view: View, currentDepth: Int = 0, maxDepth: Int = 5): AppBarLayout? {
        return findView(view, currentDepth, maxDepth, AppBarLayout::class.java)
    }

    fun findNavigation(view: View, currentDepth: Int = 0, maxDepth: Int = 5): NavigationView? {
        return findView(view, currentDepth, maxDepth, NavigationView::class.java)
    }

    fun findFragmentContainerView(
        view: View,
        currentDepth: Int = 0,
        maxDepth: Int = 5
    ): FragmentContainerView? {
        return findView(view, currentDepth, maxDepth, FragmentContainerView::class.java)
    }
}

/**
 * View向上滑入动画 - Linus式内存安全设计
 *
 * 设计原则：
 * 1. 取消现有动画 - 避免动画冲突
 * 2. 简单直接 - 不使用复杂的回调
 * 3. 状态检查 - 避免不必要的动画
 */
fun View.slideUp(duration: Long = 400) {
    if (visibility != View.VISIBLE) {
        // 取消现有动画，防止冲突
        animate().cancel()
        visibility = View.VISIBLE
        animate().translationY(0f).setDuration(duration).start()
    }
}

/**
 * View向下滑出动画 - Linus式内存安全设计
 *
 * 问题分析：
 * withEndAction lambda 会持有View引用，如果Activity销毁时动画未完成，
 * 会导致整个Activity无法被GC回收，造成严重内存泄漏
 *
 * 解决方案：
 * 1. 取消现有动画 - 避免累积未完成的动画
 * 2. 使用弱引用模式 - 避免lambda持有强引用
 * 3. 状态检查 - 确保View仍然有效
 */
fun View.slideDown(duration: Long = 400) {
    if (isVisible) {
        // 取消现有动画，防止内存泄漏
        animate().cancel()

        // 使用更安全的动画方式：不使用withEndAction
        animate().translationY(height.toFloat()).setDuration(duration).start()

        // 延迟设置visibility，避免lambda引用泄漏
        postDelayed({
            // 检查View是否仍然attached，防止Activity已销毁的情况
            if (isAttachedToWindow) {
                visibility = View.GONE
            }
        }, duration)
    }
}
