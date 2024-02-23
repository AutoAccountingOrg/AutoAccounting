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

package net.ankio.auto.utils

/**
 * 更新工具，用于处理App更新、规则更新
 */
class UpdateUtils {

    companion object{
        fun getUrl():String{
            return SpUtils.getString("app_url", "https://cloud.ankio.net/d/阿里云盘/自动记账/")
        }

        fun setUrl(url: String){
             SpUtils.putString("app_url", url)
        }

        fun getUpdateType():Boolean{
            return SpUtils.getBoolean("update_type_commit", false)
        }

        fun setUpdateType(type: Boolean){
            SpUtils.putBoolean("update_type_commit", type)
        }
    }


    private val appBaseUrl = getUrl()
    private var appUrl: String = if (!getUpdateType()) {
        "${appBaseUrl}版本更新/稳定版/"
    } else {
        "${appBaseUrl}版本更新/持续构建版/"
    }

    private var ruleUrl = "${appBaseUrl}规则更新/"

    private val requestUtils = RequestsUtils(AppUtils.getApplication())

    /**
     * {
     *     "version":"持续构建版 v1.0",
     *     "code":299,
     *     "date":"2023年02月23日 12:22:32",
     *     "log":"日志信息",
     * }
     */
    private fun request(
        url: String,
        local: Int,
        onUpdate: (version: String, log: String, date: String,code:Int) -> Unit
    ) {
        requestUtils.get(url,
            cacheTime = 60,
            onSuccess = { bytes, _ ->

                runCatching {
                    val json = requestUtils.json(bytes)
                    val version = json?.get("version")?.asString ?: ""
                    val code = json?.get("code")?.asInt ?: 0
                    val date = json?.get("date")?.asString ?: ""
                    val log = json?.get("log")?.asString ?: ""
                    //本地版本小于云端版本就是需要更新
                    if (local < code) {
                        onUpdate(version, log, date,code)
                        Logger.i(
                            " 升级信息：$url\n" +
                                    " 版本:$version\n" +
                                    " 版本号:$code\n" +
                                    " 更新时间:$date\n" +
                                    " 更新日志:$log"
                        )
                    } else {
                        Logger.i("无需更新")
                    }
                }.onFailure {
                    Logger.e("检测更新出错", it)
                }

            },
            onError = {
                Logger.i("检测更新出错：$it")
            })
    }

    /**
     * 检查App更新
     */
    fun checkAppUpdate(onUpdate: (version: String, log: String, date: String, download: String,code:Int) -> Unit) {
        SpUtils.getBoolean("checkApp", true).apply {
            if (!this) {
                return
            }
        }
        request("$appUrl/index.json", AppUtils.getVersionCode()) { version, log, date,code ->
            onUpdate(
                version,
                log,
                date,
                appUrl + (if (AppUtils.getApplicationId()
                        .endsWith("xposed")
                ) "/xposed.apk" else "/helper.apk")
                ,code
            )
        }
    }

    /**
     * 检查规则更新
     */
    fun checkRuleUpdate(onUpdate: (version: String, log: String, date: String, category: String, rule: String,code:Int) -> Unit) {
        SpUtils.getBoolean("checkRule", true).apply {
            if (!this) {
                return
            }
        }
        request("$ruleUrl/index.json", SpUtils.getInt("ruleVersion",0)) { version, log, date,code ->
            onUpdate(version, log, date, "$ruleUrl/category.js", "$ruleUrl/rule.js",code)
        }
    }
}