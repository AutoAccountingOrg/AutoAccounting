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

fun View.slideUp(duration: Long = 400) {
    if (visibility != View.VISIBLE) {
        visibility = View.VISIBLE
        animate().translationY(0f).setDuration(duration).start()
    }
}

fun View.slideDown(duration: Long = 400) {
    if (isVisible) {
        animate().translationY(height.toFloat()).setDuration(duration)
            .withEndAction { visibility = View.GONE }
            .start()
    }
}
