/*
 * Copyright (C) 2023 ankio(ankio@ankio.net)
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

import androidx.room.Entity
import androidx.room.PrimaryKey
import net.ankio.auto.constant.DataType

@Entity
class AppData {
    //其他信息处理规则
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
    var type: DataType = DataType.App

    /**
     * 对于短信，这里是发件人号码
     * 对于通知、Hook、无障碍数据这里是包名
     */
    var from: String = "" //源自APP

    /**
     * 时间
     */
    var time: Long = 0 //时间

    /**
     * 是否匹配规则
     */
    var match:Boolean = false

    /**
     * 匹配到的规则名称
     * */
    var rule:String = ""

    /**
     * 关联github issue
     */
    var issue:Int = 0
}
