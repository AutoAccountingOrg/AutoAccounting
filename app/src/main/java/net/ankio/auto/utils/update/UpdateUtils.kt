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

package net.ankio.auto.utils.update

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.Logger
import net.ankio.auto.utils.SpUtils
import net.ankio.auto.utils.request.RequestsUtils

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

    }


    private val appBaseUrl = getUrl()
    private var appUrl: String = if (SpUtils.getInt("setting_update_type", 0) == 0) {
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
    private suspend fun request(
        url: String,
        local: Int,
    ):UpdateInfo? = withContext(Dispatchers.IO) {
       runCatching {
          val result =  requestUtils.get(url, cacheTime = 60)
           val json =  Gson().fromJson(result.byteArray.toString(Charsets.UTF_8), UpdateInfo::class.java)
           //本地版本小于云端版本就是需要更新
           if (local < json.code) {
               Logger.i(
                   " 升级信息：$url\n" +
                           " 版本:${json.version}\n" +
                           " 版本号:${json.code}\n" +
                           " 更新时间:${json.date}\n" +
                           " 更新日志:${json.log}"
               )

               if(json.file!=""){
                     json.file = "$appUrl${json.file}"
               }else{
                     json.file = ruleUrl
               }
               json
           } else {
               Logger.i("无需更新")
               null
           }
       }.onFailure {
           Logger.i("检测更新出错：$it")
       }.getOrNull()
    }

    /**
     * 检查App更新
     */
    suspend fun checkAppUpdate():UpdateInfo? {
        SpUtils.getBoolean("checkApp", true).apply {
            if (!this) {
                return null
            }
        }
       return  request("$appUrl/index.json", AppUtils.getVersionCode())
    }

    /**
     * 检查规则更新
     */
    suspend fun checkRuleUpdate():UpdateInfo?  {
        SpUtils.getBoolean("checkRule", true).apply {
            if (!this) {
                return null
            }
        }
        return request("$ruleUrl/index.json", SpUtils.getInt("ruleVersion", 0))
    }
}