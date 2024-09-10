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
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.os.Process
import androidx.annotation.AttrRes
import androidx.appcompat.view.ContextThemeWrapper
import com.google.android.material.color.MaterialColors
import com.quickersilver.themeengine.ThemeEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.ankio.auto.storage.Logger
import net.ankio.auto.storage.SpUtils
import net.ankio.auto.ui.activity.MainActivity
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.utils.ExceptionHandler
import org.ezbook.server.constant.Setting
import java.math.BigInteger
import java.security.MessageDigest

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
        /**
         * 复制到剪切板
         */
        fun copyToClipboard(text: String?) {
            val clipboard = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("text", text)
            clipboard.setPrimaryClip(clip)
        }

        /**
         * 重启应用
         */
        fun restart() {
            val intent = Intent(app, MainActivity::class.java)
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
            app.startActivity(intent)
            Process.killProcess(Process.myPid())
        }

        /**
         * 获取md5
         */
        fun md5(input: String): String {
            val md5Digest = MessageDigest.getInstance("MD5")
            val messageDigest = md5Digest.digest(input.toByteArray())
            val number = BigInteger(1, messageDigest)
            var md5Hash = number.toString(16)
            while (md5Hash.length < 32) {
                md5Hash = "0$md5Hash"
            }
            return md5Hash
        }

        fun dp2px(dp: Float): Int {
            val scale = Resources.getSystem().displayMetrics.density
            return (dp * scale + 0.5f).toInt()
        }

        /**
         * 打开记账软件应用
         */
        fun startBookApp() {
            val packageName = SpUtils.getString(Setting.BOOK_APP_ID, "")
            val launchIntent = App.app.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                app.startActivity(launchIntent)
            }
        }

        /**
         * 打印 Intent 的详细信息
         */
        fun printIntent(intent: Intent) {
            // 输出 Intent 的基本信息
            val sp = StringBuilder()
            sp.append("Action: ${intent.action}")
            sp.append("Data: ${intent.data}")
            sp.append("Categories: ${intent.categories}")
            sp.append("Type: ${intent.type}")
            sp.append("Flags: ${intent.flags}")

            // 输出所有 extras（键值对）
            intent.extras?.let { extras ->
                sp.append("Extras:")
                for (key in extras.keySet()) {
                    sp.append("$key: ${extras.get(key)}")
                }
            } ?: run {
                sp.append("No extras")
            }
            Logger.i(sp.toString())
        }
    }

    /**
     * 初始化
     */
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        app = this
        // 初始化调试模式
        debug = BuildConfig.DEBUG || SpUtils.getBoolean(Setting.DEBUG_MODE, false)

        if (!BuildConfig.DEBUG){
            // 设置全局异常
            ExceptionHandler.init(this)
        }


        // 初始化 Toast 框架
        ToastUtils.init(this)
    }



}
