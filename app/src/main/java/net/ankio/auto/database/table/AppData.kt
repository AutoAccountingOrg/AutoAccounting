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
package net.ankio.auto.database.table

import android.util.Base64
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.constant.DataType
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.Logger

@Entity
class AppData {
    @PrimaryKey(autoGenerate = true)
    var id = 0

    /**
     * 对于App数据，就是Hook得到的数据一般是Json：{} 具体情况具体分析
     * 对于短信数据获取到的是短信内容 {msg:xxx,body:''}
     * 对于通知数据获取到的是如下json:{title:xxx,content:xxx},偷懒省略引号
     * 对于无障碍抓取的数据，也是json
     */
    var data: String = ""

    /**
     * 指的是数据类型
     * 其中=0是app数据，=1是短信数据，=2是通知数据，=3是无障碍抓取的数据
     */
    var type: Int = DataType.App.ordinal

    /**
     * 对于短信，这里是发件人号码
     * 对于通知、Hook、无障碍数据这里是包名
     */
    var source: String = "" // 源自APP

    /**
     * 时间
     */
    var time: Long = 0 // 时间

    /**
     * 是否匹配规则
     */
    var match: Boolean = false

    /**
     * 匹配到的规则名称
     * */
    var rule: String = ""

    /**
     * 关联github issue
     */
    var issue: Int = 0

    fun toJSON(): String {
        return Gson().toJson(this)
    }

    fun hash(): String {
        // 对data计算md5
        return AppUtils.md5(data)
    }

    fun toText(): String {
        return Base64.encodeToString(toJSON().toByteArray(), Base64.NO_WRAP)
    }

    companion object {
        fun fromJSON(json: String): AppData {
            return runCatching {
                Gson().fromJson(json, AppData::class.java)
            }.onFailure {
                Logger.e("数据异常", it)
                Logger.i(json)
            }.getOrDefault(AppData())
        }

        suspend fun fromTxt(txt: String): ArrayList<AppData> =
            withContext(Dispatchers.IO) {
                val list = arrayListOf<AppData>()
                for (line in txt.lines()) {
                    if (line.isNotEmpty()) {
                        runCatching {
                            val base64 = String(Base64.decode(line, Base64.NO_WRAP))
                            list.add(fromJSON(base64))
                        }.onFailure {
                            Logger.e("数据异常", it)
                            Logger.i(line)
                        }
                    }
                }

                list
            }
    }
}
