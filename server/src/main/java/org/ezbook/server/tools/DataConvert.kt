/*
 * Copyright (C) 2026 ankio(ankio@ankio.net)
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

package org.ezbook.server.tools

import com.google.gson.GsonBuilder

object DataConvert {

    /**
     * 将 JSON 扁平化展开，使用【】表示嵌套
     *
     * 转换规则：
     * - 对象嵌套: key:【subKey:value,...】
     * - 数组: key:【item1,item2,...】
     * - 基本类型: key:value
     *
     * 示例：
     * {"user":{"name":"张三","age":20},"tags":["A","B"]}
     * → user:【name:张三,age:20】,tags:【A,B】
     *
     * [{"a":1},{"b":2}]
     * → 【a:1,b:2】
     */
    fun convert(data: String): String {
        val trimmed = data.trim()
        return runCatching {
            val gson = GsonBuilder().disableHtmlEscaping().create()
            // 不假设类型，让 Gson 自动判断是对象还是数组
            val json = gson.fromJson(trimmed, com.google.gson.JsonElement::class.java)
            return convertJsonElement(json)
        }.getOrElse { trimmed }
    }

    /**
     * 递归转换 JsonElement
     * 使用数据结构驱动，避免字符串替换的陷阱
     */
    /**
     * 递归转换 JsonElement
     * 使用数据结构驱动，避免字符串替换的陷阱
     *
     * 增强特性：
     * - 自动检测字符串中的 JSON 并递归解析
     * - 处理"双重JSON"问题（JSON 字符串化后再嵌套）
     */
    private fun convertJsonElement(element: com.google.gson.JsonElement): String {
        return when {
            // 基本类型：字符串、数字、布尔
            element.isJsonPrimitive -> {
                val primitive = element.asJsonPrimitive
                when {
                    primitive.isString -> {
                        val str = primitive.asString
                        // 尝试解析字符串中的 JSON
                        tryParseJsonString(str) ?: str
                    }

                    else -> primitive.toString()
                }
            }

            // 对象：转换为 key:value,key:value
            element.isJsonObject -> {
                element.asJsonObject.entrySet().joinToString(",") { (key, value) ->
                    val valueStr = when {
                        value.isJsonPrimitive -> convertJsonElement(value)
                        else -> "【${convertJsonElement(value)}】"
                    }
                    "$key:$valueStr"
                }
            }

            // 数组：转换为 【item1,item2】
            element.isJsonArray -> {
                element.asJsonArray.joinToString(",") { item ->
                    convertJsonElement(item)
                }
            }

            // null
            element.isJsonNull -> "null"

            else -> element.toString()
        }.replace("\\r\\n|\\r|\\n".toRegex(), "")
    }

    /**
     * 尝试将字符串解析为 JSON
     * 如果成功，返回转换后的格式；否则返回 null
     */
    private fun tryParseJsonString(str: String): String? {
        // 快速判断：不像 JSON 就直接返回
        val trimmed = str.trim()
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return null
        }

        return try {
            val gson = GsonBuilder().disableHtmlEscaping().create()
            val parsed = gson.fromJson(trimmed, com.google.gson.JsonElement::class.java)
            // 递归转换解析出的 JSON
            "【${convertJsonElement(parsed)}】"
        } catch (e: Exception) {
            // 不是有效的 JSON，返回 null
            null
        }
    }

    /**
     * 移除 Markdown 代码块标记；若存在代码块，仅返回代码块内容
     * 目的：避免 AI 返回 ```json ... ``` 导致 JSON 解析失败
     */
    fun String.removeCodeBlock(): String {
        val trimmed = trim()
        // 优先提取所有代码块内容，避免夹杂解释性文本
        val fenceRegex = Regex("```[\\w-]*\\s*([\\s\\S]*?)\\s*```")
        val blocks = fenceRegex.findAll(trimmed).toList()
        if (blocks.isEmpty()) {
            return trimmed
        }
        return blocks.joinToString("\n") { it.groupValues[1].trim() }.trim()
    }
}

/**
 * 移除 Markdown 代码块标记的便捷方法
 * 说明：复用 DataConvert 的实现，保持处理逻辑一致
 */
fun String.removeMarkdown(): String {
    return with(DataConvert) { removeCodeBlock() }
}