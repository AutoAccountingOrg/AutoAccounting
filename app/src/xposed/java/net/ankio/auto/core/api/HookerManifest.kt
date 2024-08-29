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

package net.ankio.auto.core.api

import android.app.Application
import android.content.Context
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.launch
import net.ankio.auto.constant.DataType
import net.ankio.auto.core.App
import net.ankio.auto.core.logger.Logger
import net.ankio.auto.request.RequestsUtils
import net.ankio.dex.model.Clazz
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * HookerManifest
 * 所有的Hooker都需要继承这个接口
 */
abstract class HookerManifest {
    /**
     * 包名
     */
    abstract val packageName: String

    /**
     * 应用名，显示在日志里面的名称
     */
    abstract val appName: String

    /**
     * hook入口，用于执行全局的hook操作
     * @param application Application
     */
    abstract fun hookLoadPackage(application: Application?, classLoader: ClassLoader)

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
    open var clazz = HashMap<String, String>()

    /**
     * 已经成功获取到的class
     */
    fun getClass(name: String): Class<*>? {
        return clazz[name]?.let {
            try {
                Class.forName(it)
            } catch (e: ClassNotFoundException) {
                null
            }
        }
    }

    /**
     * 写日志
     */
    fun log(string: String) {
        Logger.log(packageName, string)
    }

    /**
     * 写调试日志
     */
    fun logD(string: String) {
        Logger.logD(packageName, string)
    }

    /**
     * 写错误日志
     */
    fun logE(e: Throwable) {
        Logger.logE(packageName, e)
    }

    /**
     * 附加资源路径
     */
    fun attachAssetPath(context: Context) {
        XposedHelpers.callMethod(
            context.resources.assets,
            "addAssetPath",
            App.modulePath,
        )
    }

    /**
     * 分析数据
     */
    fun analysisData(type: DataType, data: String) {
        App.scope.launch {
            request("/js/analysis?type=${type.name}&app=$packageName&fromAppData=false", data)
        }
    }

    suspend fun request(path:String,json:String = ""):String?{
        return runCatching {
            val uri = "http://localhost:52045/$path"
            // 创建一个OkHttpClient对象
            val client = OkHttpClient()
            // set as json post
            val body: RequestBody = json
                .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            // 创建一个Request
            val request = Request.Builder().url(uri).post(body)
                .addHeader("Content-Type", "application/json").build()
            // 发送请求获取响应
            val response = client.newCall(request).execute()
            // 如果请求成功
            response.body?.string()

        }.getOrNull()
    }
}
