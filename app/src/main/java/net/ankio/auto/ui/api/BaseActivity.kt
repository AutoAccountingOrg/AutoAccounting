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
import android.os.Bundle
import net.ankio.auto.R
import net.ankio.auto.utils.ThemeUtils
import rikka.material.app.MaterialActivity


/**
 * 基础的BaseActivity
 */
open class BaseActivity : MaterialActivity() {
    override fun computeUserThemeKey() =
        ThemeUtils.colorTheme + ThemeUtils.getNightThemeStyleRes(this)

    override fun onApplyTranslucentSystemBars() {
        super.onApplyTranslucentSystemBars()
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
    }

    override fun onApplyUserThemeResource(theme: Resources.Theme, isDecorView: Boolean) {
        if (!ThemeUtils.isSystemAccent)
            theme.applyStyle(ThemeUtils.colorThemeStyleRes, true)
        theme.applyStyle(ThemeUtils.getNightThemeStyleRes(this), true) //blackDarkMode
        theme.applyStyle(
            rikka.material.preference.R.style.ThemeOverlay_Rikka_Material3_Preference,
            true
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState)
    }


    /**
     * Activity 扩展：一行代码启动目标 Activity
     *
     * @param finishCurrent  是否在跳转后关闭当前 Activity，默认 false
     * @param builder        可选的 Intent 配置 λ，可以 putExtra / setFlags 等
     *
     * 用法：
     *   goTo<DetailActivity>()                       // 纯跳转
     *   goTo<LoginActivity>(finishCurrent = true)    // 跳转并关闭当前页
     *   goTo<EditActivity> {                         // 携带数据
     *       putExtra("id", 42)
     *       putExtra("name", "Ankio")
     *   }
     */
    inline fun <reified T : BaseActivity> BaseActivity.start(
        finishCurrent: Boolean = false,
        noinline builder: Intent.() -> Unit = {}
    ) {
        val intent = Intent(this, T::class.java).apply(builder)
        startActivity(intent)
        if (finishCurrent) finish()
    }

}