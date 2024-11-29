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

package net.ankio.auto.xposed.core.utils

import android.app.Application
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Process
import net.ankio.auto.xposed.core.App.Companion.TAG
import net.ankio.auto.xposed.core.api.HookerManifest
import net.ankio.auto.xposed.core.logger.Logger

object AppRuntime {
    /**
     * 表示应用程序是否处于调试模式。
     */
    var debug = false
    /**
     * 表示应用程序的实例。
     */
    var application: Application? = null
    /**
     * 表示应用程序的类加载器。
     */
    lateinit var classLoader: ClassLoader
    /**
     * 表示应用程序的模块路径。
     */
    var modulePath: String = ""
    /**
     * 表示应用程序的模块so路径。
     */
    var moduleSoPath: String = ""
    /**
     * 表示应用程序的模块名称。
     */
    var name:String = ""
    /**
     *
     */
    lateinit var manifest:HookerManifest
    /**
     * 表示应用程序的版本代码。
     *
     * 该属性通过延迟初始化来获取应用程序的版本代码。它尝试从应用程序的包管理器中获取版本代码，
     * 如果获取失败，则返回默认值0。
     *
     * 版本代码是一个整数值，通常用于标识应用程序的不同版本。较高的版本代码表示较新的版本。
     */
    public val versionCode by lazy {
         runCatching {
            application!!.packageManager.getPackageInfo(
                application!!.packageName,
                0
            ).longVersionCode.toInt()
        }.getOrElse {
            0
        }
    }

    /**
     * 表示应用程序的版本名称。
     */
    val versionName by lazy {
        runCatching {
            application!!.packageManager.getPackageInfo(
                application!!.packageName,
                0
            ).versionName
        }.getOrElse {
            ""
        }
    }



    fun restart() {
        if (application == null)return
        val intent = application!!.packageManager.getLaunchIntentForPackage(application!!.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_NEW_TASK)
        application!!.startActivity(intent)
        Process.killProcess(Process.myPid())
    }

    /**
     * 加载so库
     */
    fun load(name:String){
        try {
            val file = moduleSoPath+"lib$name.so"
            System.load(file)
            Logger.logD(TAG,"Load $name success")
        } catch (e: Throwable) {
            Logger.logD(TAG,"Load $name failed : $e")
            Logger.logE(TAG,e)
        }
    }


}