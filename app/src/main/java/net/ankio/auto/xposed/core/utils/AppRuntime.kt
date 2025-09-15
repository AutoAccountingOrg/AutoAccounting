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

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Process
import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.BuildConfig
import net.ankio.auto.xposed.core.api.HookerManifest
import net.ankio.auto.xposed.core.logger.Logger

object AppRuntime {
    /**
     * 进程级运行时环境单例。
     *
     * 职责：
     * - 暴露应用 `Application`、`ClassLoader`、模块路径等运行时元数据
     * - 保存当前目标应用的 `HookerManifest`
     * - 提供便捷能力：进程重启、动态加载 so、查询版本信息
     *
     * 线程安全：
     * - `init` 应在同一进程生命周期内调用一次；其余读取为无锁读，满足发布/订阅即可
     * - `debug` 值会在 IO 线程异步刷新，初始化后短时间内读取到默认值属预期行为
     */
    /**
     * 是否处于调试模式。
     *
     * 来源：默认取 `BuildConfig.DEBUG`，在 `init` 之后通过配置中心异步覆盖。
     * 注意：初始化后短时间内存在读到默认值的窗口期。
     */
    var debug = BuildConfig.DEBUG

    /**
     * 应用 `Application` 实例。
     *
     * 在非应用进程（如系统服务或早期阶段）可能为 null。
     */
    var application: Application? = null

    /**
     * 当前使用的类加载器。
     *
     * 优先使用应用的 `classLoader`，在应用未就绪时退回为传入的备用 `loader`。
     */
    lateinit var classLoader: ClassLoader

    /**
     * 模块安装路径（用于定位资源/文件）。
     * 约定：建议以 `/` 结尾以便进行路径拼接。
     */
    var modulePath: String = ""

    /**
     * 模块 so 目录路径。
     * 约定：必须以 `/` 结尾；`load(name)` 会拼接为 `moduleSoPath + "lib$name.so"`。
     */
    var moduleSoPath: String = ""

    /**
     * 当前目标应用（或模块）名，来源于 `HookerManifest.appName`。
     */
    var name: String = ""

    /**
     * 当前目标应用的 Hook 描述信息。
     * 在 `init` 完成后可用。
     */
    lateinit var manifest: HookerManifest

    /**
     * 初始化运行时环境。
     *
     * 侧效应：
     * - 记录 `application`、`classLoader`、`manifest`、`name`
     * - 在 IO 线程异步刷新 `debug` 值（读取配置项 `debugMode`）
     *
     * @param app 应用实例；在非应用进程可能为 null
     * @param loader 备用类加载器；当 `app` 为 null 时使用
     * @param m 目标应用的 `HookerManifest`
     */
    fun init(app: Application?, loader: ClassLoader, m: HookerManifest) {
        application = app
        classLoader = app?.classLoader ?: loader
        manifest = m
        name = m.appName
        CoroutineUtils.withIO {
            debug = DataUtils.configBoolean("debugMode", BuildConfig.DEBUG)
            Logger.d("Hook: ${m.appName}(${m.packageName}) 运行在${if (debug) "调试" else "生产"}模式")
        }
    }

    /**
     * 附加资源路径
     */
    fun attachResource(context: Context) {
        XposedHelpers.callMethod(
            context.resources.assets,
            "addAssetPath",
            modulePath,
        )
    }

    /**
     * 重启当前应用进程。
     *
     * 当 `application` 为 null 时不执行任何操作。
     * 实现方式：启动首页 Activity（`CLEAR_TOP | NEW_TASK`），随后杀死当前进程。
     */
    fun restart() {
        val app = application ?: return
        val intent = app.packageManager.getLaunchIntentForPackage(app.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_NEW_TASK)
        if (intent != null) app.startActivity(intent)
        Process.killProcess(Process.myPid())
    }

    /**
     * 加载指定名称的 so 库。
     *
     * 将构造绝对路径：`moduleSoPath + "lib$name.so"` 并调用 `System.load`。
     * 成功与失败均会记录日志。
     *
     * @param name 不含前缀 `lib` 与后缀 `.so` 的库名
     */
    @SuppressLint("UnsafeDynamicallyLoadedCode")
    fun load(name: String) {
        try {
            val file = moduleSoPath + "lib$name.so"
            System.load(file)
            Logger.d("Load $name success")
        } catch (e: Throwable) {
            Logger.e("Load $name failed : $e", e)
        }
    }

}