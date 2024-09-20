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
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import androidx.annotation.AttrRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.elevation.SurfaceColors
import com.quickersilver.themeengine.ThemeEngine
import com.quickersilver.themeengine.ThemeMode
import net.ankio.auto.App
import net.ankio.auto.storage.Logger
import net.ankio.auto.utils.LanguageUtils


/**
 * 基础的BaseActivity
 */
open class BaseActivity : AppCompatActivity() {
    open var toolbarLayout: AppBarLayout? = null
    open var toolbar: MaterialToolbar? = null

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
        // 主题初始化
        ThemeEngine.applyToActivity(this@BaseActivity)

        // 等待view创建完成
        window.decorView.post {
            onViewCreated()
        }
    }

    var mStatusBarColor: Int? = null
    var mStatusBarColor2: Int? = null
    var last = mStatusBarColor


    /**
     * 在子activity手动调用该方法
     */
    fun onViewCreated() {
        // 主题初始化
        val themeMode = ThemeEngine.getInstance(this@BaseActivity).themeMode

        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val  light =
            !(themeMode == ThemeMode.DARK || (themeMode == ThemeMode.AUTO && currentNightMode == Configuration.UI_MODE_NIGHT_YES))
        mStatusBarColor = getThemeAttrColor(android.R.attr.colorBackground)
        mStatusBarColor2 = SurfaceColors.SURFACE_4.getColor(this@BaseActivity)
        enableImmersiveMode(light)

    }

    private fun enableImmersiveMode(light: Boolean) {
        //状态栏
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        //导航栏
        //当背景透明时去掉灰色蒙层
        window.isNavigationBarContrastEnforced = false
        //导航栏背景颜色透明
        window.navigationBarColor =  Color.TRANSPARENT
        WindowCompat.getInsetsController(window,window.decorView).isAppearanceLightStatusBars = light
        WindowCompat.getInsetsController(window,window.decorView).isAppearanceLightNavigationBars = light

        // 获取根布局
        val rootView = findViewById<View>(android.R.id.content)  as ViewGroup

        rootView.setOnApplyWindowInsetsListener { v, insets ->
            val statusBarHeight = getStatusBarHeight(insets)
            val navigationBarHeight = getNavigationBarHeight(insets)
            // 找到第一个子view
            val mainGroup = rootView.getChildAt(0) as ViewGroup
            val firstView = mainGroup.getChildAt(0)
            val lastView = mainGroup.getChildAt(rootView.childCount - 1)

            if (firstView is ViewGroup){
                val firstViewGroup = firstView as ViewGroup
                val firstViewGroupChild = firstViewGroup.getChildAt(0)
                val params = firstViewGroupChild.layoutParams as ViewGroup.MarginLayoutParams
                params.topMargin = statusBarHeight
             //   params.height = statusBarHeight
               // firstViewGroupChild.layoutParams = params
            }else{
                // 设置padding
                firstView.setPadding(0, statusBarHeight, 0, 0)
            }

            lastView.setPadding(0, 0, 0, navigationBarHeight)
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
     * 获取主题色
     */
    fun getThemeAttrColor(
        @AttrRes attrResId: Int,
    ): Int {
        return App.getThemeAttrColor(attrResId)
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