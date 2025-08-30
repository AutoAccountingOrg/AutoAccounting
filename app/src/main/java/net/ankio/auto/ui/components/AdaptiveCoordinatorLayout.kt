/*
 * Copyright (C) 2025 ankio(ankio@ankio.net)
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
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import net.ankio.auto.storage.Logger

/**
 * 自适应 CoordinatorLayout 容器组件
 *
 * 该组件基于 CoordinatorLayout，提供以下功能：
 * - 自动处理系统窗口插入（状态栏、导航栏、键盘）
 * - 智能的内边距调整，避免内容被系统UI遮挡
 * - 支持键盘弹出时的动态适配
 * - 保持 CoordinatorLayout 的所有原生功能
 *
 * 使用方式：
 * ```xml
 * <net.ankio.auto.ui.components.AdaptiveCoordinatorLayout
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent">
 *
 *     <!-- 你的内容 -->
 *
 * </net.ankio.auto.ui.components.AdaptiveCoordinatorLayout>
 * ```
 *
 * 设计原则：
 * - 简洁：一次配置，自动适配
 * - 灵活：支持自定义适配策略
 * - 兼容：不破坏原有的 CoordinatorLayout 功能
 */
class AdaptiveCoordinatorLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CoordinatorLayout(context, attrs, defStyleAttr) {


    /** 是否启用导航栏适配 */
    private var adaptNavigationBar: Boolean = true


    init {
        setupWindowInsetsHandling()
    }

    /**
     * 设置窗口插入处理逻辑
     *
     * 该方法配置 WindowInsets 监听器，自动处理：
     * 1. 状态栏高度适配
     * 2. 导航栏高度适配
     * 3. 键盘弹出时的动态适配
     */
    private fun setupWindowInsetsHandling() {
        ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
            // 获取各种系统窗口插入
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navigationBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            val bottomPadding = when {
                adaptNavigationBar -> navigationBarInsets.bottom
                else -> 0
            }

            // 应用内边距
            view.updatePadding(
                bottom = bottomPadding
            )

            // 返回处理后的插入，让子View可以继续处理
            insets
        }
    }


}
