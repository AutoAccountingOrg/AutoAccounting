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
import android.graphics.Color
import android.os.Build
import android.os.Process
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.MaterialColors
import com.tencent.bugly.crashreport.CrashReport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.ankio.auto.http.LicenseNetwork
import net.ankio.auto.storage.ConfigUtils
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.activity.HomeActivity
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.utils.ExceptionHandler
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.utils.PrefManager.darkTheme
import net.ankio.auto.utils.ThemeUtils
import org.ezbook.server.constant.DefaultData
import org.ezbook.server.constant.Setting
import rikka.material.app.LocaleDelegate
import java.math.BigInteger
import java.security.MessageDigest
import java.util.Locale
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

lateinit var autoApp: App
class App : Application() {


    companion object {


        val licenseNetwork = LicenseNetwork()

        var statusBarHeight: Int = 0

        /* App实例 */
        lateinit var app: Application

        /**
         * 是否是调试模式
         */
        var debug: Boolean = false


        /* 全局协程 */
        private val job = Job()
        private val scope = CoroutineScope(Dispatchers.IO + job)

        fun pageStopOrDestroy() {
            ConfigUtils.save(app)
        }

        /**
         * 获取全局协程
         */
        fun launch(
            context: CoroutineContext = EmptyCoroutineContext,
            block: suspend CoroutineScope.() -> Unit
        ) {
            scope.launch(context = context, block = block)
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
                    packageManager.getApplicationInfo(
                        packageName,
                        PackageManager.ApplicationInfoFlags.of(0)
                    )
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
                    packageManager.getPackageInfo(
                        packageName,
                        PackageManager.PackageInfoFlags.of(0)
                    )
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
                ThemeUtils.themedCtx(autoApp),
                attrResId,
                Color.WHITE,
            )
        }

        /**
         * 获取主题Context
         */
        fun getThemeContext(context: Context): Context {
            return ThemeUtils.themedCtx(context)
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
            val intent = Intent(app, HomeActivity::class.java)
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
            return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                app.resources.displayMetrics,
            ).toInt()
        }


        /**
         * 打开记账软件应用
         */
        fun startBookApp() {
            val packageName = ConfigUtils.getString(Setting.BOOK_APP_ID, DefaultData.BOOK_APP)
            val launchIntent = app.packageManager.getLaunchIntentForPackage(packageName)
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
            Logger.d(sp.toString())
        }
    }

    override fun onCreate() {
        super.onCreate()

        autoApp = this
        app = this
        if (!BuildConfig.DEBUG) {
            initBugly()
        }

        initTheme()

        initLanguage()

        // 初始化 Toast 框架
        ToastUtils.init(this)


    }

    private fun initTheme() {
        AppCompatDelegate.setDefaultNightMode(darkTheme)
        // setTheme(ThemeUtils.colorThemeStyleRes)
    }

    private fun initLanguage() {
        applyLocale(PrefManager.language)
    }

    fun getLocale(tag: String): Locale {
        return if (tag == "SYSTEM") LocaleDelegate.systemLocale
        else Locale.forLanguageTag(tag)
    }

    private fun applyLocale(languageTag: String) {
        LocaleDelegate.defaultLocale = getLocale(languageTag)
        val config = resources.configuration
        config.setLocale(LocaleDelegate.defaultLocale)
        createConfigurationContext(config)
    }


    /** 初始化 Bugly */
    private fun initBugly() {


        ExceptionHandler.init(this)

        val strategy = CrashReport.UserStrategy(this).apply {
            // 版本号、包名——便于在 Bugly 后台快速定位
            appVersion = BuildConfig.VERSION_NAME
            appPackageName = BuildConfig.APPLICATION_ID
            // 设备型号：用品牌 + 型号能帮助你在后台过滤同类设备
            deviceModel = "${Build.BRAND} ${Build.MODEL}"
        }

        // 第 2 个参数替换为你的 Bugly App Id
        CrashReport.initCrashReport(
            this,
            "af9e0f4181",
            BuildConfig.DEBUG,   // true 会在日志里输出调试信息
            strategy
        )
    }


}
