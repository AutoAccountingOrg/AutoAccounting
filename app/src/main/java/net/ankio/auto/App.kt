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
import android.os.StrictMode
import com.hjq.toast.Toaster
import net.ankio.auto.ui.activity.MainActivity
import net.ankio.auto.utils.AppTimeMonitor
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.ExceptionHandler

class App : Application() {
    override fun onTerminate() {
        super.onTerminate()
        AppUtils.getJob().cancel()
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        // 初始化工具类
        AppUtils.setApplication(this)
        // 监控
        AppTimeMonitor.startMonitoring("App初始化")
        // 设置全局异常
        ExceptionHandler.init(this)
        // 初始化 Toast 框架
        Toaster.init(this)

        AppTimeMonitor.stopMonitoring("App初始化")
    }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectCustomSlowCalls() // 配合StrictMode.noteSlowCall使用
                    .detectDiskReads() // 是否在主线程中进行磁盘读取
                    .detectDiskWrites() // 是否在主线程中进行磁盘写入
                    .detectNetwork() // 是否在主线程中进行网络请求
                    .penaltyDialog() // 弹出违规提示对话框
                    .penaltyLog() // 在Logcat 中打印违规异常信息
                    .penaltyFlashScreen() // 会造成屏幕闪烁
                    .penaltyDropBox() // 将违规信息记录到 dropbox 系统日志目录中
                    .build(),
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectActivityLeaks() // Activity是否内存泄漏
                    .detectLeakedSqlLiteObjects() // 数据库是否未关闭
                    .detectLeakedClosableObjects() // 文件是否未关闭
                    .setClassInstanceLimit(MainActivity::class.java, 1) // 某个类在内存中实例上限
                    .detectLeakedRegistrationObjects() // 对象是否被正确关闭
                    .penaltyLog() // 打印日志
                    .penaltyDeath() // 直接Crash掉当前应用程序
                    .build(),
            )
        }
    }
}
