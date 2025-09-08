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

package org.ezbook.server.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.ezbook.server.constant.DataType

@Entity
class AppDataModel {
    @PrimaryKey(autoGenerate = true)
    var id = 0L

    /**
     * 对于App数据，就是Hook得到的数据一般是Json：{} 具体情况具体分析
     * 对于短信数据获取到的是短信内容 {msg:xxx,body:''}
     * 对于通知数据获取到的是如下json:{title:xxx,content:xxx},偷懒省略引号
     * 对于无障碍抓取的数据，也是json
     */
    var data: String = ""

    /**
     * 指的是数据类型
     */
    var type: DataType = DataType.DATA

    /**
     * 包名
     */
    var app: String = "" // 源自APP

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

    /**
     * 使用的规则版本
     */
    var version: String = ""


    companion object {
        /**
         * 标识 AI 生成规则的后缀。
         * 与客户端保持一致，凡是规则字符串包含该片段，均视为 AI 生成。
         */
        private const val AI_GENERATED_SUFFIX = "生成"
    }

    /**
     * 判断当前条目的规则是否为 AI 生成。
     *
     * @return 当 `rule` 字符串包含 AI 标识时返回 true，否则返回 false。
     */
    fun isAiGeneratedRule(): Boolean {
        return rule.contains(AI_GENERATED_SUFFIX)
    }

    /**
     * 判断是否为“有效匹配”。
     * 要求：已匹配（match=true）且规则非空，且规则不是 AI 生成。
     *
     * @return 满足上述条件返回 true，否则返回 false。
     */
    fun hasValidMatch(): Boolean {
        return isMatched() && !isAiGeneratedRule()
    }

    /**
     * 是否满足“已匹配且规则非空”。
     */
    fun isMatched(): Boolean {
        return match && rule.isNotEmpty()
    }

    override fun toString(): String {
        return "AppDataModel(id=$id, data='', type=$type, app='$app', time=$time, match=$match, rule='$rule', issue=$issue)"
    }

}