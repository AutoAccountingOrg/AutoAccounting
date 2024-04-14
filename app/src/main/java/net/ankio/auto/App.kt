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
import com.hjq.toast.Toaster
import net.ankio.auto.database.Db
import net.ankio.auto.utils.AppTimeMonitor
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.ExceptionHandler
import net.ankio.auto.utils.Logger

class App : Application() {
    override fun onTerminate() {
        super.onTerminate()
        AppUtils.getJob().cancel()
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        // 初始化工具类
        AppUtils.setApplication(this)
        AppUtils.setService(this)
        // 监控
        AppTimeMonitor.startMonitoring("App初始化")

        // 数据库初始化
        Db.init(this)
        // 日志初始化
        Logger.init()
        // 设置全局异常
        ExceptionHandler.init(this)
        // 初始化 Toast 框架
        Toaster.init(this)

        AppTimeMonitor.stopMonitoring("App初始化")
    }
}
