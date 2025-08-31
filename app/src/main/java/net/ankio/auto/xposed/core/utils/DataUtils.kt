package net.ankio.auto.xposed.core.utils

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.runBlocking
import net.ankio.auto.xposed.core.App.Companion.TAG
import net.ankio.auto.http.api.SettingAPI
import org.ezbook.server.db.model.SettingModel
import androidx.core.content.edit


object DataUtils {


    /**
     * 保存数据
     * @param key String
     * @param value String
     */
    fun set(key: String, value: String) {

        if (AppRuntime.manifest.packageName == "android") {
            return
        }

        if (AppRuntime.application == null) {
            return
        }
        val sharedPreferences: SharedPreferences =
            AppRuntime.application!!.getSharedPreferences(TAG, Context.MODE_PRIVATE) // 私有数据
        sharedPreferences.edit { // 获取编辑器
            putString(key, value)
        } // 提交修改
    }

    /**
     * 读取数据
     * @param key String
     * @return String
     */
    fun get(key: String, def: String = ""): String {
        if (AppRuntime.manifest.packageName == "android") {
            return def
        }
        if (AppRuntime.application == null) {
            return def
        }
        val sharedPreferences: SharedPreferences =
            AppRuntime.application!!.getSharedPreferences(TAG, Context.MODE_PRIVATE) // 私有数据
        return sharedPreferences.getString(key, def) ?: def
    }

    /**
     * 读取配置
     * @param key String
     * @return String
     */
    fun configString(key: String, def: String = ""): String {
        return runBlocking {
            val result = SettingAPI.get(key, def)
            result.ifEmpty { def }
        }
    }

    /**
     * 读取配置
     * @param key String
     * @return String
     */
    fun configBoolean(key: String, def: Boolean = false): Boolean {
        return runBlocking {
            val result = SettingAPI.get(key, def.toString())
            if (result.isEmpty()) def else result.toBoolean()
        }
    }
}