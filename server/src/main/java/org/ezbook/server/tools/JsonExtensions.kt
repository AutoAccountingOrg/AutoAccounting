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

package org.ezbook.server.tools

import com.google.gson.JsonElement
import com.google.gson.JsonObject

/**
 * JSON 安全访问扩展函数
 * 解决 Gson JsonNull 导致的异常问题
 */

/**
 * 安全获取 Long 值
 * @param key JSON 键名
 * @param defaultValue 默认值
 * @return Long 值，如果字段不存在或为 null 则返回默认值
 */
fun JsonObject.safeGetLong(key: String, defaultValue: Long = 0L): Long {
    val element = get(key)
    return if (element == null || element.isJsonNull) {
        defaultValue
    } else {
        runCatching { element.asLong }.getOrDefault(defaultValue)
    }
}

/**
 * 安全获取 Double 值
 * @param key JSON 键名
 * @param defaultValue 默认值
 * @return Double 值，如果字段不存在或为 null 则返回默认值
 */
fun JsonObject.safeGetDouble(key: String, defaultValue: Double = 0.0): Double {
    val element = get(key)
    return if (element == null || element.isJsonNull) {
        defaultValue
    } else {
        runCatching { element.asDouble }.getOrDefault(defaultValue)
    }
}

/**
 * 安全获取 String 值
 * @param key JSON 键名
 * @param defaultValue 默认值
 * @return String 值，如果字段不存在或为 null 则返回默认值
 */
fun JsonObject.safeGetString(key: String, defaultValue: String = ""): String {
    val element = get(key)
    return if (element == null || element.isJsonNull) {
        defaultValue
    } else {
        runCatching { element.asString }.getOrDefault(defaultValue)
    }
}

fun JsonObject?.safeGetStringNonBlank(key: String, default: String): String {
    val value = this?.safeGetString(key, default)
    return if (value.isNullOrBlank()) default else value
}
/**
 * 安全获取 Boolean 值
 * @param key JSON 键名
 * @param defaultValue 默认值
 * @return Boolean 值，如果字段不存在或为 null 则返回默认值
 */
fun JsonObject.safeGetBoolean(key: String, defaultValue: Boolean = false): Boolean {
    val element = get(key)
    return if (element == null || element.isJsonNull) {
        defaultValue
    } else {
        runCatching { element.asBoolean }.getOrDefault(defaultValue)
    }
}

/**
 * 安全获取 Int 值
 * @param key JSON 键名
 * @param defaultValue 默认值
 * @return Int 值，如果字段不存在或为 null 则返回默认值
 */
fun JsonObject.safeGetInt(key: String, defaultValue: Int = 0): Int {
    val element = get(key)
    return if (element == null || element.isJsonNull) {
        defaultValue
    } else {
        runCatching { element.asInt }.getOrDefault(defaultValue)
    }
}
