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

package net.ankio.auto.ui.api

import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.ankio.auto.storage.Logger
import net.ankio.auto.utils.ThemeUtils
import rikka.material.app.MaterialActivity
import kotlin.coroutines.cancellation.CancellationException


/**
 * 基础Activity类，继承自MaterialActivity
 *
 * 提供以下功能：
 * 1. 主题管理：支持动态主题切换和夜间模式
 * 2. 状态栏和导航栏透明化处理
 * 3. Activity跳转扩展方法
 *
 * 所有继承此类的Activity都将自动获得这些基础功能
 */
open class BaseActivity : MaterialActivity() {

    /**
     * 计算用户主题键值
     * 结合颜色主题和夜间模式样式资源生成唯一的主题标识
     *
     * @return 主题键值字符串
     */
    override fun computeUserThemeKey() =
        ThemeUtils.colorTheme + ThemeUtils.getNightThemeStyleRes(this)

    /**
     * 应用透明系统栏
     * 设置状态栏和导航栏为透明，实现沉浸式体验
     */
    override fun onApplyTranslucentSystemBars() {
        super.onApplyTranslucentSystemBars()

        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

    }

    /**
     * 应用用户主题资源
     * 根据当前主题设置和夜间模式应用相应的样式
     *
     * @param theme 主题资源对象
     * @param isDecorView 是否为装饰视图
     */
    override fun onApplyUserThemeResource(theme: Resources.Theme, isDecorView: Boolean) {

        // 如果不是系统强调色，应用自定义颜色主题
        if (!ThemeUtils.isSystemAccent) {
            Logger.d("Applying custom color theme: ${ThemeUtils.colorThemeStyleRes}")
            theme.applyStyle(ThemeUtils.colorThemeStyleRes, true)
        }

        // 应用夜间模式样式
        val nightThemeRes = ThemeUtils.getNightThemeStyleRes(this)
        Logger.d("Applying night theme: $nightThemeRes")
        theme.applyStyle(nightThemeRes, true)


    }

    /**
     * Activity扩展：一行代码启动目标Activity
     *
     * 提供便捷的Activity跳转方法，支持：
     * - 纯跳转
     * - 跳转后关闭当前Activity
     * - 携带额外数据跳转
     *
     * @param finishCurrent 是否在跳转后关闭当前Activity，默认false
     * @param builder 可选的Intent配置lambda，可以putExtra/setFlags等
     *
     * 使用示例：
     * ```kotlin
     * // 纯跳转
     * goTo<DetailActivity>()
     *
     * // 跳转并关闭当前页
     * goTo<LoginActivity>(finishCurrent = true)
     *
     * // 携带数据跳转
     * goTo<EditActivity> {
     *     putExtra("id", 42)
     *     putExtra("name", "Ankio")
     * }
     * ```
     */
    inline fun <reified T : BaseActivity> BaseActivity.start(
        finishCurrent: Boolean = false,
        noinline builder: Intent.() -> Unit = {}
    ) {
        val targetActivity = T::class.java.simpleName
        Logger.d("Starting activity: $targetActivity, finishCurrent: $finishCurrent")
        
        val intent = Intent(this, T::class.java).apply(builder)
        startActivity(intent)

        if (finishCurrent) {
            Logger.d("Finishing current activity: ${this.javaClass.simpleName}")
            finish()
        }

    }

    protected fun launch(block: suspend CoroutineScope.() -> Unit) {
        lifecycleScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                Logger.d("Activity取消: ${e.message}")
            }
        }
    }

}