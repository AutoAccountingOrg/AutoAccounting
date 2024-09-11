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

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.annotation.AttrRes
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.elevation.SurfaceColors
import com.quickersilver.themeengine.ThemeEngine
import com.quickersilver.themeengine.ThemeMode
import com.zackratos.ultimatebarx.ultimatebarx.addStatusBarTopPadding
import com.zackratos.ultimatebarx.ultimatebarx.navigationBar
import com.zackratos.ultimatebarx.ultimatebarx.statusBar
import net.ankio.auto.App
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
    }

    var mStatusBarColor: Int? = null
    var mStatusBarColor2: Int? = null
    var last = mStatusBarColor

    /**
     * 在子activity手动调用该方法
     */
    open fun onViewCreated() {
        // 主题初始化
        val themeMode = ThemeEngine.getInstance(this@BaseActivity).themeMode

        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        statusBar {
            fitWindow = false
            background.transparent()
            light =
                !(themeMode == ThemeMode.DARK || (themeMode == ThemeMode.AUTO && currentNightMode == Configuration.UI_MODE_NIGHT_YES))
        }
        // 根据主题设置statusBar
        navigationBar { transparent() }
        toolbarLayout?.addStatusBarTopPadding()
        mStatusBarColor = getThemeAttrColor(android.R.attr.colorBackground)
        mStatusBarColor2 = SurfaceColors.SURFACE_4.getColor(this@BaseActivity)
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