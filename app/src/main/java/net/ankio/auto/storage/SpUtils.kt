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

package net.ankio.auto.storage

import android.content.SharedPreferences
import net.ankio.auto.App

/**
 * SharedPreferences工具类
 */
object SpUtils {

    //所有设置相关的键
    const val DEBUG = "debug_mode" //调试模式

    const val BILL_AUTO_GROUP = "bill_auto_group" //账单自动分组(去重）



    val sp: SharedPreferences = App.app.getSharedPreferences("setting", 0)

    /**
     * 保存数据到SharedPreferences
     * @param key 键
     * @param value 值
     */
    fun putBoolean(
        key: String,
        value: Boolean,
    ) {
        sp.edit().putBoolean(key, value).apply()
    }
    /**
     * 从SharedPreferences获取数据
     * @param key 键
     * @param default 默认值
     * @return 返回获取到的值
     */
    fun getBoolean(
        key: String,
        default: Boolean,
    ): Boolean = sp.getBoolean(key, default)

    /**
     * 保存数据到SharedPreferences
     * @param key 键
     * @param value 值
     */
    fun putString(
        key: String,
        value: String,
    ) {
        sp.edit().putString(key, value).apply()
    }

    /**
     * 从SharedPreferences获取数据
     * @param key 键
     * @param default 默认值
     * @return 返回获取到的值
     */
    fun getString(
        key: String,
        default: String? = "",
    ): String = sp.getString(key, default) ?: ""

    /**
     * 从SharedPreferences获取数据
     * @param key 键
     * @param default 默认值
     * @return 返回获取到的值
     */
    fun getInt(
        key: String,
        default: Int = 0,
    ): Int {
        return sp.getInt(key, default)
    }

    /**
     * 保存数据到SharedPreferences
     * @param key 键
     * @param value 值
     */
    fun putInt(
        key: String,
        value: Int,
    ) {
        sp.edit().putInt(key, value).apply()
    }

    fun getLong(s: String, i: Long): Long {
        return sp.getLong(s, i)
    }
    fun putLong(s: String, i: Long) {
        sp.edit().putLong(s, i).apply()
    }
}
