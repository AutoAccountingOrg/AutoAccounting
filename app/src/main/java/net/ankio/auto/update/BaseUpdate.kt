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

package net.ankio.auto.update

import android.app.Activity
import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import net.ankio.auto.R
import net.ankio.auto.request.RequestsUtils
import net.ankio.auto.storage.ConfigUtils
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.utils.ToastUtils
import org.ezbook.server.constant.Setting
import org.markdownj.MarkdownProcessor
import java.text.SimpleDateFormat
import java.util.Calendar

abstract class BaseUpdate(context: Context) {
    protected val url = "https://dl.ez-book.org/自动记账"
    abstract val repo: String
    var download = ""
    open var github  = ""


    abstract val dir: String

    fun pan(): String {
        return "$url/$dir"
    }

    var version = ""
    var log = ""
    var date = ""
    protected val request = RequestsUtils(context,3600)
    abstract fun ruleVersion(): String
    abstract fun onCheckedUpdate()

    suspend fun check(showToast: Boolean = false): Boolean {
        Logger.d("Checking for updates")

        ConfigUtils.putLong(Setting.RULE_UPDATE_TIME, System.currentTimeMillis()) // 记录检查时间

        val list = if (ConfigUtils.getString(
                Setting.UPDATE_CHANNEL,
                UpdateChannel.Github.name
            ) == UpdateChannel.Github.name
        ) {
            checkVersionFromGithub(ruleVersion())
        } else {
            checkVersionFromPan(ruleVersion())
        }

        version = list[0]
        log = list[1]
        date = list[2]

        Logger.i("Version: $version")
        Logger.i("Log: $log")
        Logger.i("Date: $date")

        return if (version != "") {
            onCheckedUpdate()
            true
        } else {
            if (showToast) {
                ToastUtils.info(R.string.no_need_to_update)
            }
            false
        }

    }

    /**
     * 从网盘检查版本
     */
    private suspend fun checkVersionFromPan(localVersion: String): Array<String> {
        try {
            request.get("${pan()}/index.json").let {
                val json = Gson().fromJson(it.second, JsonObject::class.java)
                version = json.get("version").asString
                log = json.get("log").asString
                date = json.get("date").asString
                date = date(date)
                if (checkVersionLarge(version,localVersion)>0) {
                    Logger.i("New version found")
                    return arrayOf(
                        version,
                        log,
                        date
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Logger.e("checkVersionFromPan", e)
        }
        return arrayOf("", "", "")
    }

    /**
     * 从github检查版本
     */
    open suspend fun checkVersionFromGithub(localVersion: String): Array<String> {

        try {
            request.get(github).let {
                val json = Gson().fromJson(it.second, JsonObject::class.java)
                version = json.get("tag_name").asString
                log = json.get("body").asString
                date = json.get("published_at").asString

                //转换日期
                date = date(date)


                val processor = MarkdownProcessor()

                // 转换 Markdown 为 HTML
                log = processor.markdown(log)

                Logger.i("LocalVersion: $localVersion,Version: $version")

                if (checkVersionLarge(version,localVersion)>0) {
                    Logger.i("New version found")
                    return arrayOf(
                        version,
                        log,
                        date
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Logger.e("checkVersionFromGithub", e)
        }
        return arrayOf("", "", "")
    }

    abstract fun update(activity: Activity)


    fun date(date: String): String {
        //转换日期
        val t = date.replace("T", " ").replace("Z", "")
        //将当前的时间+8小时
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val d = sdf.parse(t)
        val c = Calendar.getInstance()
        c.time = d
        c.add(Calendar.HOUR_OF_DAY, 8)
        return sdf.format(c.time)
    }

    /**
     * 检查两个版本号哪个大
     * @param localVersion 本地版本号
     * @param cloudVersion 云端版本号
     * @return 返回 1 表示 localVersion 大于 cloudVersion，-1 表示 cloudVersion 大于 localVersion，0 表示两者相等
     */
    fun checkVersionLarge(localVersion: String, cloudVersion: String): Int {
        val channel = ConfigUtils.getString(
            Setting.CHECK_UPDATE_TYPE,
            UpdateType.Stable.name
        )
        val localParts = localVersion.replace("-${channel}","").split(".")
        val cloudParts = cloudVersion.replace("-${channel}","").split(".")

        // 找出较长的版本号长度，补齐较短版本号的空位
        val maxLength = maxOf(localParts.size, cloudParts.size)

        for (i in 0 until maxLength) {
            val localPart = localParts.getOrNull(i)?.toIntOrNull() ?: 0  // 如果某个部分不存在，默认视为0
            val cloudPart = cloudParts.getOrNull(i)?.toIntOrNull() ?: 0

            when {
                localPart > cloudPart -> return 1
                localPart < cloudPart -> return -1
            }
        }
        return 0 // 如果所有部分都相等，返回 0
    }


}