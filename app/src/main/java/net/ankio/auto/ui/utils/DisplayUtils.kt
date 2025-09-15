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

import android.content.Context
import android.content.res.Configuration
import android.graphics.Point
import android.os.Build
import android.util.TypedValue
import android.view.Display
import android.view.WindowManager
import net.ankio.auto.autoApp


/**
 * 显示工具类
 * 用于处理屏幕显示相关的实用方法，包括：
 * - 屏幕尺寸适配
 * - 屏幕大小判断
 * - 显示密度调整
 * - 窗口模式判断
 */
object DisplayUtils {

    // 使用 @Volatile 确保多线程安全
    @Volatile
    private var navHeight: Int = -1

    @Volatile
    private var statusHeight: Int = -1

    /**
     * 获取导航栏高度
     * @param context 上下文对象
     * @return 导航栏高度（像素）
     */
    fun getNavigationBarHeight(context: Context): Int {
        if (navHeight < 0) {
            synchronized(this) {
                if (navHeight < 0) {  // 双重检查锁定
                    val res = context.resources
                    val navId = res.getIdentifier("navigation_bar_height", "dimen", "android")
                    navHeight = if (navId > 0) res.getDimensionPixelSize(navId) else 0
                }
            }
        }
        return navHeight
    }

    /**
     * 获取状态栏高度
     * @param context 上下文对象
     * @return 状态栏高度（像素）
     */
    fun getStatusBarHeight(context: Context): Int {
        if (statusHeight < 0) {
            synchronized(this) {
                if (statusHeight < 0) {  // 双重检查锁定
                    val res = context.resources
                    val statusId = res.getIdentifier("status_bar_height", "dimen", "android")
                    statusHeight = if (statusId > 0) res.getDimensionPixelSize(statusId) else 0
                }
            }
        }
        return statusHeight
    }

    /**
     * 判断当前窗口是否为横屏模式
     * 注意：这里判断的是窗口方向，而不是设备方向
     * @param context 上下文对象
     * @return true 如果窗口为横屏模式
     */
    fun isWindowLandscape(context: Context): Boolean {
        return context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    /**
     * 将dp值转换为像素值
     * @param dpValue dp值
     * @return 对应的像素值
     */
    fun dp2px(dpValue: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dpValue,
            autoApp.resources.displayMetrics
        ).toInt()
    }

    /**
     * 将dp值转换为像素值（接受Int参数的重载方法）
     * @param dpValue dp值
     * @return 对应的像素值
     */
    fun dp2px(dpValue: Int): Int = dp2px(dpValue.toFloat())

    /**
     * 将像素值转换为dp值
     * @param pxValue 像素值
     * @return 对应的dp值
     */
    private fun px2dp(pxValue: Float): Int {
        val scale = autoApp.resources.displayMetrics.density
        return (pxValue / scale + 0.5f).toInt()
    }

    /**
     * 将像素值转换为dp值（接受Int参数的重载方法）
     * @param pxValue 像素值
     * @return 对应的dp值
     */
    fun px2dp(pxValue: Int): Int = px2dp(pxValue.toFloat())

    /**
     * 获取真实屏幕尺寸（包括状态栏和导航栏）
     *
     * 此方法会返回设备屏幕的完整尺寸，包括系统装饰（状态栏、导航栏等）。
     * 对于不同的Android版本，使用不同的API来确保兼容性。
     *
     * @param context 上下文对象
     * @return Point对象，包含屏幕的宽度(x)和高度(y)，单位为像素
     */
    fun getRealScreenSize(context: Context): Point {
        val screenSize = Point()
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager

        @Suppress("DEPRECATION")
        windowManager?.let { wm ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11 (API 30) 及以上版本使用新的 API
                val bounds = wm.currentWindowMetrics.bounds
                screenSize.x = bounds.width()
                screenSize.y = bounds.height()
            } else {
                // 低版本使用已弃用但仍可用的 API
                val display = wm.defaultDisplay
                display.getRealSize(screenSize)
            }
        }

        return screenSize
    }
}