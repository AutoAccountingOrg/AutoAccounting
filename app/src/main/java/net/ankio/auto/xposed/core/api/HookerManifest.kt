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

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.security.NetworkSecurityPolicy
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.delay
import net.ankio.auto.xposed.core.logger.Logger
import net.ankio.auto.xposed.core.utils.AppRuntime
import net.ankio.auto.xposed.core.utils.DataUtils.get
import net.ankio.auto.xposed.core.utils.DataUtils.set
import net.ankio.auto.xposed.core.utils.MessageUtils.toast
import net.ankio.auto.xposed.core.utils.ThreadUtils
import net.ankio.dex.Dex
import net.ankio.dex.model.Clazz
import net.ankio.dex.result.ClazzResult
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.ezbook.server.constant.DataType
import org.ezbook.server.constant.DefaultData
import org.ezbook.server.constant.Setting
import net.ankio.auto.http.api.SettingAPI

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
    open var minVersion: Int = 0

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

    /**
     * 写日志
     */
    fun log(string: String) {
        Logger.log("$packageName$processName", string)
    }

    /**
     * 写调试日志
     */
    fun logD(string: String) {
        Logger.logD("$packageName$processName", string)
    }

    /**
     * 写错误日志
     */
    fun logE(e: Throwable) {
        Logger.logE("$packageName$processName", e)
    }

    /**
     * 附加资源路径
     */
    fun attachResource(context: Context) {
        XposedHelpers.callMethod(
            context.resources.assets,
            "addAssetPath",
            AppRuntime.modulePath,
        )
    }

    /**
     * 权限检查
     */
    fun permissionCheck() {
        if (permissions.isEmpty()) {
            return
        }
        val context = AppRuntime.application ?: return
        permissions.forEach {
            if (it === Manifest.permission.SYSTEM_ALERT_WINDOW) {
                if (Settings.canDrawOverlays(context)) {
                    return@forEach
                }
            }
            if (context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED) {
                return@forEach
            }
            logE(Throwable("$appName 权限被拒绝: $it，此Hook可能无法正常工作"))
        }
    }

    /**
     * 检查应用程序的版本是否满足最低要求。
     *
     * 该方法首先获取当前应用程序的版本号和版本名称，并记录这些信息。
     * 然后，它会检查当前版本号是否低于设定的最低版本号。
     * 如果版本过低，方法会记录错误日志并显示一个提示消息，告知用户需要升级应用程序。
     *
     * @return 如果当前版本满足最低要求，则返回 `true`；否则返回 `false`。
     */
    fun versionCheck(): Boolean {
        val code = AppRuntime.versionCode
        val name = AppRuntime.versionName
        log("应用版本号: $code, 版本名: $name")

        // 检查App版本是否过低，过低无法使用
        if (minVersion != 0 && code < minVersion) {
            logE(Throwable("自动适配失败，${AppRuntime.manifest.appName}(${code}) 版本过低"))
            toast("${AppRuntime.manifest.appName}版本过低，无法适配，请升级到最新版本后再试。")
            return false
        }
        return true
    }
    /**
     * 在适配之前的操作
     */
    open  fun beforeAdaption():MutableList<Clazz>{
       return rules
    }

    /**
     * 自动适配方法，用于根据预定义的规则进行应用的自动适配。
     *
     * 该方法首先检查是否存在适配规则。如果没有适配规则，则直接返回成功。
     * 如果有适配规则，则检查当前应用的版本号是否与上次适配的版本号一致。
     * 如果一致，则尝试从缓存中加载适配信息，并验证适配信息的完整性。
     * 如果适配信息完整，则返回成功；否则，清除缓存并返回失败。
     *
     * 如果当前版本号与上次适配的版本号不一致，则开始自动适配过程。
     * 该过程包括解析应用的DEX文件，根据规则查找类信息，并将适配结果保存到缓存��。
     * 如果适配成功，则更新适配版本号并返回成功；否则，返回失败。
     *
     * @param rules 需要适配的规则列表
     * @return 返回一个布尔值，表示是否成功启动适配过程。对于异步适配，总是返回 `true`。
     */
    fun autoAdaption(rules: MutableList<Clazz>): Boolean {
        if (rules.isEmpty()) {
            return true
        }

        // 计算当前rules的哈希值
        val currentRulesHash = rules.joinToString(",") {
            "${it.name}:${it.methods.joinToString("|") { m -> m.toString() }}"
        }.hashCode().toString()

        val code = AppRuntime.versionCode
        val savedVersion = get("adaptation_version", "").toIntOrNull() ?: 0
        val savedRulesHash = get("adaptation_rules_hash", "")

        logD("适配版本: $savedVersion, 规则哈希: $savedRulesHash")

        // 版本号相同且规则哈希值相同时，尝试加载缓存的适配结果
        if (savedVersion == code && savedRulesHash == currentRulesHash) {
            try {
                clazz = Gson().fromJson(
                    get("clazz", ""),
                    object : TypeToken<HashMap<String, ClazzResult>>() {}.type
                )
                if (clazz.size == rules.size) {
                    log("从缓存加载适配信息: $clazz")
                    return true
                }
                throw Exception("适配失败: 缓存大小不匹配")
            } catch (e: Exception) {
                // 加载失败时清除所有缓存
                set("adaptation_version", "0")
                set("adaptation_rules_hash", "")
                set("clazz", "")
                logE(e)
            }
        }

        // 需要重新适配 - 异步执行
        startAdaptationAsync(rules, currentRulesHash, code)
        return false // 立即返回，不阻塞调用方
    }

    /**
     * 异步执行适配过程
     */
    private fun startAdaptationAsync(
        rules: MutableList<Clazz>,
        currentRulesHash: String,
        code: Int
    ) {
        ThreadUtils.launch {
            try {
                // 在主线程显示开始提示
                ThreadUtils.runOnUiThread {
                    toast("自动记账开始适配中...")
                }

                val appInfo = AppRuntime.application!!.applicationInfo
                val path = appInfo.sourceDir
                logD("应用包路径: $path")


                try {
                    // CPU密集型操作，在后台线程执行，带进度回调
                    val hashMap = Dex.findClazz(
                        path,
                        AppRuntime.application!!.classLoader,
                        rules
                    ) { found, total, ruleName ->
                        // 实时进度回调，在主线程显示
                        ThreadUtils.runOnUiThread {
                            toast("适配进度 $found/$total: 找到 $ruleName")
                        }
                        logD("适配进度: $found/$total, 找到规则: $ruleName")
                    }


                    if (hashMap.size == rules.size) {
                        // 保存新的适配结果
                        set("adaptation_version", code.toString())
                        set("adaptation_rules_hash", currentRulesHash)
                        clazz = hashMap
                        set("clazz", Gson().toJson(clazz))
                        log("适配成功: $hashMap")

                        // 在主线程显示成功提示并重启
                        ThreadUtils.runOnUiThread {
                            toast("适配成功，即将重启应用...")
                        }

                        // 延迟2秒后重启，让用户看到提示
                        delay(2000L)
                        ThreadUtils.runOnUiThread {
                            AppRuntime.restart()
                        }
                    } else {
                        log("适配失败: $hashMap")
                        rules.forEach { rule ->
                            if (!hashMap.containsKey(rule.name)) {
                                log("未能适配规则: ${rule.name}")
                            }
                        }
                        set("adaptation_version", "0")

                        // 在主线程显示失败提示
                        ThreadUtils.runOnUiThread {
                            toast("适配失败，请检查应用版本是否支持")
                        }
                    }
                } finally {

                }
            } catch (e: Exception) {
                logE(e)
                // 在主线程显示错误提示
                ThreadUtils.runOnUiThread {
                    toast("适配过程发生错误: ${e.message}")
                }
            }
        }
    }

    fun networkError() {
        val policy = NetworkSecurityPolicy.getInstance()
        if (policy != null && !policy.isCleartextTrafficPermitted) {
            // 允许明文流量
            XposedHelpers.callMethod(policy, "setCleartextTrafficPermitted", true)
        }
    }

    /**
     * 分析数据
     */
    fun analysisData(type: DataType, data: String, appPackage: String = packageName) {
        ThreadUtils.launch {

            val filter = SettingAPI.get(Setting.DATA_FILTER, DefaultData.DATA_FILTER).split(",")

            if (filter.all { !data.contains(it) }) {
                log("all filter not contains: $data, $filter")
                return@launch
            }

            var retryCount = 0
            var result: String? = null

            while (result == null && retryCount < 10) {
                result =
                    request("js/analysis?type=${type.name}&app=$appPackage&fromAppData=false", data)

                if (result == null) {
                    retryCount++
                    val delaySeconds = (1L shl (retryCount - 1)) * 10  // 10, 20, 40, 80, 160...
                    logD("分析尝试第 $retryCount 次失败，将在 $delaySeconds 秒后重试...")
                    delay(delaySeconds * 1000L)
                }
            }

            if (result != null) { //会自动拉起
                logD("分析结果: $result")
            } else {
                logD("分析失败，已尝试20次")
            }
        }
    }


    suspend fun request(path: String, json: String = ""): String? {
        return try {
            val uri = "http://127.0.0.1:52045/$path"

            val client = OkHttpClient()
            val body: RequestBody = json
                .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val request = Request.Builder().url(uri).post(body)
                .addHeader("Content-Type", "application/json").build()
            val response = client.newCall(request).execute()
            response.body?.string()

        } catch (e: Exception) {
            logE(e)
            null
        }
    }

    fun clazz(name: String): Class<*> {

        return AppRuntime.classLoader.loadClass(clazz[name]!!.clazzName)!!
    }

    fun method(clazzName: String, methodName: String): String {
        val clazzResult = clazz[clazzName] ?: return ""
        val method = clazzResult.methodResults[methodName] ?: return ""
        return method.methodName
    }

}
