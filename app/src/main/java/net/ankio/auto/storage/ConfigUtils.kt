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

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.ankio.auto.App
import org.ezbook.server.db.model.SettingModel
import java.io.File
import java.lang.reflect.Type

class ConfigUtils private constructor(context: Context) {

    // Gson 实例
    private val gson = Gson()

    // 存储设置的 Map
    private var settings: MutableMap<String, Any> = mutableMapOf()

    // 设置文件路径
    private val settingsFile: File = File(context.filesDir, "settings.json")

    companion object {
        @Volatile
        private var instance: ConfigUtils? = null

        // 获取单例
        fun getInstance(context: Context): ConfigUtils {
            return instance ?: synchronized(this) {
                instance ?: ConfigUtils(context).also { instance = it }
            }
        }

        // 静态函数调用
        fun init(context: Context) {
            getInstance(context).init()
        }

        fun save(context: Context) {
            getInstance(context).save()
            val settings = getInstance(context).settings.toMutableMap()
            // 同步远程数据库
            App.launch {
                settings.forEach {
                    SettingModel.set(it.key, it.value.toString())
                }
            }
        }

        fun getString(key: String, defaultValue: String = ""): String {
            return getInstance(App.app).getString(key, defaultValue)
        }

        fun putString(key: String, value: String) {
            getInstance(App.app).putString(key, value)
        }

        fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
            return getInstance(App.app).getBoolean(key, defaultValue)
        }

        fun putBoolean(key: String, value: Boolean) {
            getInstance(App.app).putBoolean(key, value)
        }

        fun getInt(key: String, defaultValue: Int = 0): Int {
            return getInstance(App.app).getInt(key, defaultValue)
        }

        fun putInt(key: String, value: Int) {
            getInstance(App.app).putInt(key, value)
        }

        fun getLong(key: String, defaultValue: Long = 0L): Long {
            return getInstance(App.app).getLong(key, defaultValue)
        }

        fun putLong(key: String, value: Long) {
            getInstance(App.app).putLong(key, value)
        }

        fun getFloat(key: String, defaultValue: Float = 0f): Float {
            return getInstance(App.app).getFloat(key, defaultValue)
        }

        fun putFloat(key: String, value: Float) {
            getInstance(App.app).putFloat(key, value)
        }

        fun getDouble(key: String, defaultValue: Double = 0.0): Double {
            return getInstance(App.app).getDouble(key, defaultValue)
        }

        fun putDouble(key: String, value: Double) {
            getInstance(App.app).putDouble(key, value)
        }

        fun remove(key: String) {
            getInstance(App.app).remove(key)
        }

        fun clear() {
            getInstance(App.app).clear()
        }

        fun copyTo(context: Context, file: File) {
            getInstance(App.app).save()
            val settingsFile = File(context.filesDir, "settings.json")
            settingsFile.copyTo(file, true)
        }

        fun copyFrom(context: Context, file: File) {
            file.copyTo(File(context.filesDir, "settings.json"), true)
            getInstance(App.app).init()
        }

        fun reload(context: Context) {
            getInstance(context).init()
        }
    }

    // 初始化：从文件加载设置数据到内存
    fun init() {
        if (settingsFile.exists()) {
            val json = settingsFile.readText()
            try {
                val type: Type = object : TypeToken<MutableMap<String, Any>>() {}.type
                settings = gson.fromJson(json, type) ?: mutableMapOf()
            } catch (e: Exception) {
                settings = mutableMapOf() // 文件格式错误时初始化为空的 Map
            }
        } else {
            settings = mutableMapOf() // 文件不存在时初始化为空的 Map
        }
    }

    // 显式保存函数，将内存中的数据写入文件
    fun save() {
        settingsFile.writeText(gson.toJson(settings))
    }

    // 获取 String 值，提供默认值
    fun getString(key: String, defaultValue: String = ""): String {
        return settings[key] as? String ?: defaultValue
    }

    // 设置 String 值
    fun putString(key: String, value: String) {
        settings[key] = value
    }

    // 获取 Boolean 值，提供默认值
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return settings[key] as? Boolean ?: defaultValue
    }

    // 设置 Boolean 值
    fun putBoolean(key: String, value: Boolean) {
        settings[key] = value
    }

    // 获取 Int 值，提供默认值
    fun getInt(key: String, defaultValue: Int = 0): Int {
        return (settings[key] as? Number)?.toInt() ?: defaultValue
    }

    // 设置 Int 值
    fun putInt(key: String, value: Int) {
        settings[key] = value
    }

    // 获取 Long 值，提供默认值
    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return (settings[key] as? Number)?.toLong() ?: defaultValue
    }

    // 设置 Long 值
    fun putLong(key: String, value: Long) {
        settings[key] = value
    }

    // 获取 Float 值，提供默认值
    fun getFloat(key: String, defaultValue: Float = 0f): Float {
        return (settings[key] as? Number)?.toFloat() ?: defaultValue
    }

    // 设置 Float 值
    fun putFloat(key: String, value: Float) {
        settings[key] = value
    }

    // 获取 Double 值，提供默认值
    fun getDouble(key: String, defaultValue: Double = 0.0): Double {
        return (settings[key] as? Number)?.toDouble() ?: defaultValue
    }

    // 设置 Double 值
    fun putDouble(key: String, value: Double) {
        settings[key] = value
    }

    // 移除某个 key
    fun remove(key: String) {
        settings.remove(key)
    }

    // 清除所有设置
    fun clear() {
        settings.clear()
    }
}
