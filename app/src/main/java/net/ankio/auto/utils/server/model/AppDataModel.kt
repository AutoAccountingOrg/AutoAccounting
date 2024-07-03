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
package net.ankio.auto.utils.server.model

import kotlinx.coroutines.launch
import net.ankio.auto.utils.AppUtils

class AppDataModel:BaseModel() {


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
    var type: Int = 0

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
    var match: Int = 0

    /**
     * 匹配到的规则名称
     * */
    var rule: String = ""

    /**
     * 关联github issue
     */
    var issue: Int = 0

    companion object {
        fun put(appData: AppDataModel) {
            AppUtils.getScope().launch {
                AppUtils.getService().sendMsg("data/put", appData)
            }
        }



        fun remove(id: Int) {
            AppUtils.getScope().launch {
                AppUtils.getService().sendMsg("data/remove", mapOf("id" to id))
            }
        }
    }
}
