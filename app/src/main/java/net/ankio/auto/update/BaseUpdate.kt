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
import net.ankio.auto.App
import net.ankio.auto.R
import net.ankio.auto.request.RequestsUtils
import net.ankio.auto.storage.SpUtils
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.utils.Logger
import org.markdownj.MarkdownProcessor
import java.text.SimpleDateFormat
import java.util.Calendar

abstract class BaseUpdate(context: Context) {
    protected val url = "https://dl.ez-book.org/自动记账"
    abstract val repo: String
    var download = ""
    fun github(): String {
        return "https://api.github.com/repos/AutoAccountingOrg/$repo/releases/latest"
    }


    abstract val dir: String

    fun pan(): String {
        return "$url/$dir"
    }

    var version = ""
    var log = ""
    var date = ""
    protected val request = RequestsUtils(context)
    abstract fun ruleVersion(): String
    abstract fun onCheckedUpdate()

    suspend fun check(showToast: Boolean = false): Boolean {
        // 判断是否在1小时之前检查过
        if (!App.debug && System.currentTimeMillis() - SpUtils.getLong(
                "rule_update_time",
                0
            ) < 3600000
        ) {
            return false
        }
        SpUtils.putLong("rule_update_time", System.currentTimeMillis()) // 记录检查时间

        val list = if (SpUtils.getString("update_channel", "github") == "github") {
            checkVersionFromGithub(ruleVersion())
        } else {
            checkVersionFromPan(ruleVersion())
        }

        version = list[0]
        log = list[1]
        date = list[2]
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
                val json = Gson().fromJson(String(it.byteArray), JsonObject::class.java)
                version = json.get("version").asString
                log = json.get("log").asString
                date = json.get("date").asString
                date = date(date)
                if (version != localVersion) {
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
    private suspend fun checkVersionFromGithub(localVersion: String): Array<String> {
        try {
            request.get(github()).let {
                val json = Gson().fromJson(String(it.byteArray), JsonObject::class.java)
                version = json.get("tag_name").asString
                log = json.get("body").asString
                date = json.get("published_at").asString

                //转换日期
                date = date(date)


                val processor = MarkdownProcessor()

                // 转换 Markdown 为 HTML
                log = processor.markdown(log)

                if (version != localVersion) {
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


    fun date(date:String): String {
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
}