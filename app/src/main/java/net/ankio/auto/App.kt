/*
 * Copyright (C) 2023 ankio(ankio@ankio.net)
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

package net.ankio.auto

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import androidx.annotation.AttrRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.color.MaterialColors
import com.hjq.toast.Toaster
import com.quickersilver.themeengine.ThemeEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.ankio.auto.app.model.AppInfo
import net.ankio.auto.broadcast.LocalBroadcastHelper
import net.ankio.auto.utils.ExceptionHandler
import net.ankio.auto.storage.SpUtils
import net.ankio.auto.ui.utils.ToastUtils

class App : Application() {
    override fun onTerminate() {
        super.onTerminate()
        /**
         * 取消全局协程
         */
        job.cancel()
    }

    companion object{
        /* App实例 */
        lateinit var app: Application
        /**
         * 是否是调试模式
         */
        var debug:Boolean = false
        /* 全局协程 */
        private val job = Job()
        private val scope = CoroutineScope(Dispatchers.IO + job)

        /**
         * 获取全局协程
         */
         fun launch(block: suspend CoroutineScope.() -> Unit) {
            scope.launch(block = block)
        }
        /**
         * 获取App应用信息
         * @param packageName 应用包名
         * @return Array<Any?>?
         */
        fun getAppInfoFromPackageName(packageName: String): Array<Any?>? {
            return try {
                val packageManager: PackageManager = app.packageManager

                val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
                } else {
                    packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
                }

                val appName = packageManager.getApplicationLabel(appInfo).toString()

                val appIcon = try {
                    packageManager.getApplicationIcon(appInfo)
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                    null
                }

                val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA)
                }

                val appVersion = packageInfo.versionName

                arrayOf(appName, appIcon, appVersion)
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
                null
            }
        }

        /**
         * 在主线程运行
         * @param action () -> Unit
         */
        fun runOnUiThread(action: () -> Unit) {
            app.mainExecutor.execute(action)
        }

        /**
         * 获取主题色
         */
        fun getThemeAttrColor(
            @AttrRes attrResId: Int,
        ): Int {
            return MaterialColors.getColor(
                ContextThemeWrapper(
                    app,
                    ThemeEngine.getInstance(app).getTheme(),
                ),
                attrResId,
                Color.WHITE,
            )
        }

        /**
         * 获取主题Context
         */
        fun getThemeContext(context: Context): Context {
            return ContextThemeWrapper(context, ThemeEngine.getInstance(context).getTheme())
        }

        /**
         * 是否安装了某个应用
         */
        fun isAppInstalled(appName: String?): Boolean {
            return try {
                appName?.let {
                    app.packageManager.getPackageInfo(it, PackageManager.GET_ACTIVITIES)
                    true
                } ?: false
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    /**
     * 初始化
     */
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        app = this
        // 初始化调试模式
        debug = BuildConfig.DEBUG || SpUtils.getBoolean("debug", false)

        // 设置全局异常
        ExceptionHandler.init(this)
        // 初始化 Toast 框架
        ToastUtils.init(this)
    }




}
