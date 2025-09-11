package net.ankio.auto.xposed.core.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.withTimeoutOrNull
import net.ankio.auto.http.api.SettingAPI
import net.ankio.auto.xposed.core.App.Companion.TAG


object DataUtils {


    /**
     * 保存数据
     * @param key String
     * @param value String
     */
    fun set(key: String, value: String) {
        // 在 system_server 或应用未就绪时直接返回，避免无意义操作
        if (AppRuntime.manifest.packageName == "android" || AppRuntime.application == null) return

        val sharedPreferences: SharedPreferences =
            AppRuntime.application!!.getSharedPreferences(TAG, Context.MODE_PRIVATE)
        sharedPreferences.edit {
            putString(key, value)
        }
    }

    /**
     * 读取数据
     * @param key String
     * @return String
     */
    fun get(key: String, def: String = ""): String {
        // 在 system_server 或应用未就绪时返回默认值，确保稳定性
        if (AppRuntime.manifest.packageName == "android" || AppRuntime.application == null) return def

        val sharedPreferences: SharedPreferences =
            AppRuntime.application!!.getSharedPreferences(TAG, Context.MODE_PRIVATE)
        return sharedPreferences.getString(key, def) ?: def
    }

    private const val TIMEOUT_MS = 3000L

    /**
     * 读取配置
     * @param key String
     * @return String
     */
    suspend fun configString(key: String, def: String = ""): String {
        val result = withTimeoutOrNull(TIMEOUT_MS) {
            SettingAPI.get(key, def)
        } ?: ""
        return result.ifEmpty { def }
    }

    /**
     * 读取配置
     * @param key String
     * @return String
     */
    suspend fun configBoolean(key: String, def: Boolean = false): Boolean {
        val result = withTimeoutOrNull(TIMEOUT_MS) {
            SettingAPI.get(key, def.toString())
        } ?: ""
        return if (result.isEmpty()) def else result.toBoolean()
    }
}