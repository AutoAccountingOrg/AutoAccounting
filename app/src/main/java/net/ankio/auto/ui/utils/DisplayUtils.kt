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

import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import android.graphics.Point
import android.os.Build
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Display
import android.view.WindowManager
import net.ankio.auto.App
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseActivity


/**
 * 显示工具类
 * 用于处理屏幕显示相关的实用方法，包括：
 * - 屏幕尺寸适配
 * - 屏幕大小判断
 * - 显示密度调整
 * - 窗口模式判断
 */
object DisplayUtils {
    // 屏幕尺寸阈值常量
    private const val MINI_SCREEN_WIDTH = 360
    private const val MINI_SCREEN_HEIGHT = 640
    private const val SMALL_SCREEN_WIDTH = 480
    private const val SMALL_SCREEN_HEIGHT = 800
    private const val MEDIUM_SCREEN_WIDTH = 720
    private const val MEDIUM_SCREEN_HEIGHT = 1280

    private var noCompatDensity: Float = 0f
    private var noCompatScaledDensity: Float = 0f

    fun setCustomDensity(activity: BaseActivity) {
        try {
            val appDisplayMetrics: DisplayMetrics = App.app.resources.displayMetrics
            val screenWidth = appDisplayMetrics.widthPixels / appDisplayMetrics.density
            
            // 只在屏幕宽度小于360dp或大于1080dp时进行缩放
            if (screenWidth in 360f..1080f) return
            
            // 首次初始化
            if (noCompatDensity == 0f) {
                noCompatDensity = appDisplayMetrics.density
                noCompatScaledDensity = appDisplayMetrics.scaledDensity
                
                // 监听字体大小变化
                App.app.registerComponentCallbacks(object : ComponentCallbacks {
                    override fun onConfigurationChanged(newConfig: Configuration) {
                        if (newConfig.fontScale > 0 && noCompatDensity != 0f) {
                            // 保持字体大小跟随系统设置
                            noCompatScaledDensity = App.app.resources.displayMetrics.scaledDensity
                            setCustomDensity(activity)
                        }
                    }

                    override fun onLowMemory() {}
                })
            }

            // 计算目标密度
            val targetDensity = when {
                screenWidth < 360f -> appDisplayMetrics.widthPixels / 360f  // 小屏幕适配
                screenWidth > 1080f -> appDisplayMetrics.widthPixels / 1080f  // 大屏幕适配
                else -> appDisplayMetrics.density  // 正常屏幕保持原始密度
            }
            
            // 使用字体缩放因子调整文字大小
            val fontScale = App.app.resources.configuration.fontScale
            val baseScaleDensity = noCompatScaledDensity * fontScale
            // 大屏幕时稍微增加字体大小，但保持原有比例
            val targetScaleDensity = if (screenWidth > 1080f) {
                baseScaleDensity * 1.3f
            } else {
                baseScaleDensity
            }
            
            // 计算DPI
            val targetDensityDpi = (160 * targetDensity).toInt()

            // 应用到应用级别
            appDisplayMetrics.apply {
                density = targetDensity
                scaledDensity = targetScaleDensity
                densityDpi = targetDensityDpi
            }

            // 应用到当前Activity
            activity.resources.displayMetrics.apply {
                density = targetDensity
                scaledDensity = targetScaleDensity
                densityDpi = targetDensityDpi
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    /**
     * 是否是超小屏幕
     */
    fun isMiniScreen(context: Context): Boolean {
        val point = getScreenSize(context)
        return point.x < MINI_SCREEN_WIDTH || point.y < MINI_SCREEN_HEIGHT
    }
    /**
     * 是否是小屏幕
     */
    fun isSmallScreen(context: Context): Boolean {
        val point = getScreenSize(context)
        return point.x < SMALL_SCREEN_WIDTH || point.y < SMALL_SCREEN_HEIGHT
    }
    /**
     * 是否是中等屏幕
     */
    fun isMediumScreen(context: Context): Boolean {
        val point = getScreenSize(context)
        return point.x < MEDIUM_SCREEN_WIDTH || point.y < MEDIUM_SCREEN_HEIGHT
    }


    /**
     * 获取窗口尺寸，不关注设备状态，只关注窗口状态
     */
     fun getScreenSize(context: Context): Point {
        val point = Point()
        val displayMetrics = context.resources.displayMetrics
        val density = displayMetrics.density

        // 将实际像素按密度转换为dp值
        val widthDp = (displayMetrics.widthPixels / density).toInt()
        val heightDp = (displayMetrics.heightPixels / density).toInt()


        point.x = widthDp
        point.y = heightDp

        Logger.d("getScreenSize: x=${point.x}, y=${point.y}")
        return point
    }

    /**
     * 平行窗口模式（华为、小米）
     */
    fun inMagicWindow(context: Context): Boolean {
        val config: String = context.resources.configuration.toString()
        return config.contains("hwMultiwindow-magic") || config.contains("miui-magic-windows") || config.contains("hw-magic-windows")
    }
    /**
     * 窗口是横屏，不关注设备状态，只关注窗口态
     */
    fun isWindowLandscape(context: Context): Boolean {
        val orientation: Int = context.resources.configuration.orientation
        return orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    /**
     * dp转px
     */
    fun dp2px(context: Context, dpValue: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dpValue,
            context.resources.displayMetrics
        ).toInt()
    }

    /**
     * px转dp
     */
    fun px2dp(context: Context, pxValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (pxValue / scale + 0.5f).toInt()
    }
    /**
     * 获取屏幕真实尺寸（包含刘海区域）
     */
    fun getRealScreenSize(context: Context): Point {
        val point = Point()
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 使用 WindowMetrics 获取屏幕尺寸
            val metrics = windowManager.currentWindowMetrics
            val bounds = metrics.bounds
            point.x = bounds.width()
            point.y = bounds.height()
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealSize(point)
        }

        return point
    }


    fun isTabletOrFoldable(context: Context): Boolean {
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        return screenWidth > screenHeight || screenWidth / displayMetrics.density > 600
    }
}