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

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.quickersilver.themeengine.ThemeEngine
import com.quickersilver.themeengine.ThemeMode
import net.ankio.auto.App
import net.ankio.auto.ui.utils.DisplayUtils
import net.ankio.auto.ui.utils.ViewUtils
import net.ankio.auto.utils.LanguageUtils


/**
 * 基础的BaseActivity
 */
open class BaseActivity : AppCompatActivity() {
    /**
     * 重构context
     */
    override fun attachBaseContext(newBase: Context?) {
        val context =
            newBase?.let {
                LanguageUtils.initAppLanguage(it)
            }
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DisplayUtils.setCustomDensity(this)
        // 主题初始化
        ThemeEngine.applyToActivity(this@BaseActivity)
        // 等待view创建完成
        window.decorView.post {
            onViewCreated()
        }
    }


    /**
     * 在子activity手动调用该方法
     */
    private fun onViewCreated() {
        // 主题初始化
        val themeMode = ThemeEngine.getInstance(this@BaseActivity).themeMode

        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val light =
            !(themeMode == ThemeMode.DARK || (themeMode == ThemeMode.AUTO && currentNightMode == Configuration.UI_MODE_NIGHT_YES))
        enableImmersiveMode(light)

    }

    /**
     * 沉浸式模式
     */
    private fun enableImmersiveMode(light: Boolean) {
        //状态栏
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        //导航栏
        //当背景透明时去掉灰色蒙层
        window.isNavigationBarContrastEnforced = false
        //导航栏背景颜色透明
        window.navigationBarColor = Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars =
            light
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars =
            light

        // 获取根布局
        val rootView = findViewById<View>(android.R.id.content) as ViewGroup

        rootView.setOnApplyWindowInsetsListener { v, insets ->
            val rootLayout = rootView.getChildAt(0) as ViewGroup
            val statusBarHeight = getStatusBarHeight(insets)
            val navigationBarHeight = getNavigationBarHeight(insets)
            App.statusBarHeight = statusBarHeight
            App.navigationBarHeight = navigationBarHeight
            // 找到第一个子view
            val fragmentContainerView = ViewUtils.findFragmentContainerView(rootLayout)
                ?: return@setOnApplyWindowInsetsListener insets
            var firstView = fragmentContainerView.getChildAt(0)
            val appBarLayout = ViewUtils.findAppBarLayout(fragmentContainerView)
            if (appBarLayout != null) {
                firstView = appBarLayout
            }
            if (firstView is ViewGroup) {
                val firstViewGroupChild = firstView.getChildAt(0)
                val params = firstViewGroupChild.layoutParams as ViewGroup.MarginLayoutParams
                params.topMargin = statusBarHeight
                firstViewGroupChild.layoutParams = params
            } else {
                // 设置padding
                firstView.setPadding(0, statusBarHeight, 0, 0)
            }
            val lastView = fragmentContainerView.getChildAt(fragmentContainerView.childCount - 1)
            val navigation = rootLayout.getChildAt(rootLayout.childCount - 1)
            // Logger.d("navigation:$navigation,visibility:${navigation?.visibility}")

            lastView.postDelayed({
                if (navigation == null || navigation.visibility == View.GONE) {
                    lastView.setPadding(0, 0, 0, navigationBarHeight)
                } else {
                    lastView.setPadding(0, 0, 0, 0)
                }
            }, 300)

            // 返回未消费的 insets
            v.onApplyWindowInsets(insets)
        }
    }

    @SuppressLint("InternalInsetResource")
    private fun getStatusBarHeight(insets: WindowInsets): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            insets.getInsets(WindowInsets.Type.statusBars()).top
        } else {
            val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
            if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
        }
    }

    @SuppressLint("InternalInsetResource")
    private fun getNavigationBarHeight(insets: WindowInsets): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            insets.getInsets(WindowInsets.Type.navigationBars()).bottom
        } else {
            val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
            if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
        }
    }


    /**
     * 切换activity
     */
    inline fun <reified T : BaseActivity> Context.start() {
        val intent = Intent(this, T::class.java)
        startActivity(intent)
    }

    /**
     * 切换activity
     */
    inline fun <reified T : BaseActivity> Context.startNew() {
        val intent = Intent(this, T::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    /**
     * 重新创建activity
     */
    fun recreateActivity() {
        runOnUiThread {
            recreate()
        }
    }


    override fun onStop() {
        super.onStop()
        App.pageStopOrDestroy()
    }
}