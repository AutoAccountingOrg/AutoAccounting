/*
 * Copyright (C) 2025 ankio(ankio@ankio.net)
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

package net.ankio.auto.xposed.core.api

import net.ankio.auto.xposed.core.logger.XposedLogger
import net.ankio.dex.model.Clazz
import net.ankio.dex.result.ClazzResult

/**
 * HookerManifest
 * 所有的Hooker都需要继承这个接口
 */
abstract class HookerManifest {
    /**
     * 包名
     */
    abstract val packageName: String
    open var processName: String = ""

    /**
     * 应用名，显示在日志里面的名称
     */
    abstract val appName: String

    /**
     * hook入口
     */
    abstract fun hookLoadPackage()

    /**
     * 是否为系统应用
     */
    open val systemApp: Boolean = false

    /**
     * 需要hook的功能，一个功能一个hooker，方便进行错误捕获
     * @return MutableList<PartHooker>
     */
    abstract var partHookers: MutableList<PartHooker>

    /**
     * 需要的适配的clazz列表
     * @return MutableList<Clazz>
     */
    abstract var rules: MutableList<Clazz>

    /**
     * 最低支持的版本
     */
    open var minVersion: Long = 0

    /**
     * 应用需要附加（直接授权）的权限
     */
    open var permissions: MutableList<String> = mutableListOf()

    /**
     * application名
     */
    open var applicationName: String = "android.app.Application"

    /**
     * 保存已经成功获取到的class
     */
    open var clazz = HashMap<String, ClazzResult>()

    fun d(msg: String, tr: Throwable? = null) = XposedLogger.d(msg, tr)

    fun i(msg: String, tr: Throwable? = null) = XposedLogger.i(msg, tr)

    fun w(msg: String, tr: Throwable? = null) = XposedLogger.w(msg, tr)

    fun e(msg: String, tr: Throwable? = null) = XposedLogger.e(msg, tr)

    fun e(tr: Throwable) = XposedLogger.e(tr)
}
