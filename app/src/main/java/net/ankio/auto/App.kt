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
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import com.tencent.bugly.crashreport.CrashReport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import net.ankio.auto.constant.WorkMode
import net.ankio.auto.http.LicenseNetwork
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.utils.CoroutineUtils
import net.ankio.auto.utils.ExceptionHandler
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.utils.PrefManager.darkTheme
import net.ankio.auto.utils.SystemUtils
import net.ankio.auto.xposed.core.logger.LoggerConfig
import rikka.material.app.LocaleDelegate
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/** 全局App实例 */
lateinit var autoApp: App

/**
 * 应用程序主类 - 只负责核心初始化
 */
open class App : Application() {

    companion object {
        /** 许可证网络管理器 */
        lateinit var licenseNetwork: LicenseNetwork


        /**
         * 在主线程启动协程
         */
        fun launch(
            context: CoroutineContext = EmptyCoroutineContext, block: suspend CoroutineScope.() -> Unit
        ) {
            CoroutineUtils.launchOnMain(context, block)
        }

        fun launchIO(
            block: suspend CoroutineScope.() -> Unit
        ) {
            CoroutineUtils.launchOnMain(
                Dispatchers.IO, block
            )
        }

    }

    override fun onCreate() {
        super.onCreate()

        // 设置全局实例
        autoApp = this


            LoggerConfig.init(BuildConfig.APPLICATION_ID, PrefManager.debugMode)
        }

        // 初始化核心组件（优先初始化PrefManager以获取用户设置）
        initSystemComponents()
        initBuglyIfRelease()
        initUI()
        initNetwork()
    }

    /**
     * 初始化系统组件
     */
    private fun initSystemComponents() {
        SystemUtils.init(this)
    }


    /**
     * 非调试模式下初始化Bugly
     */
    private fun initBuglyIfRelease() {
        ExceptionHandler.init(this)
        if (!PrefManager.debugMode) {
            initBugly()
        }
    }

    /**
     * 初始化UI相关
     */
    private fun initUI() {
        initTheme()
        initLanguage()
        ToastUtils.init(this)
    }

    /**
     * 初始化网络
     */
    private fun initNetwork() {
        licenseNetwork = LicenseNetwork()
    }

    /**
     * 初始化主题
     */
    private fun initTheme() {
        AppCompatDelegate.setDefaultNightMode(darkTheme)
    }

    /**
     * 初始化语言
     */
    private fun initLanguage() {
        applyLocale(PrefManager.language)
    }

    /**
     * 获取语言环境
     */
    fun getLocale(tag: String): Locale {
        return if (tag == "SYSTEM") LocaleDelegate.systemLocale
        else Locale.forLanguageTag(tag)
    }

    /**
     * 应用语言设置
     */
    private fun applyLocale(languageTag: String) {
        LocaleDelegate.defaultLocale = getLocale(languageTag)
        val config = resources.configuration
        config.setLocale(LocaleDelegate.defaultLocale)
        createConfigurationContext(config)
    }

    /**
     * 初始化Bugly崩溃收集
     */
    private fun initBugly() {


        val strategy = CrashReport.UserStrategy(this).apply {
            appVersion = BuildConfig.VERSION_NAME
            appPackageName = BuildConfig.APPLICATION_ID
            deviceModel = "${Build.BRAND} ${Build.MODEL}"
        }

        CrashReport.initCrashReport(
            this, "af9e0f4181", PrefManager.debugMode, strategy
        )
    }
}
