package net.ankio.auto.xposed.core.utils

import android.content.Context
import android.content.SharedPreferences
import com.crossbowffs.remotepreferences.RemotePreferences
import net.ankio.auto.storage.SpUtils
import net.ankio.auto.xposed.core.App.Companion.TAG


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
        val editor = sharedPreferences.edit() // 获取编辑器
        editor.putString(key, value)
        editor.apply() // 提交修改
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

    private fun getPref(): SharedPreferences? {
        if (AppRuntime.application == null) {
            AppRuntime.manifest.log("getPref: application is null")
            return null
        }
        return runCatching {
            RemotePreferences(
                AppRuntime.application,
                SpUtils.PERF_AUTHORITY,
                SpUtils.PREF_NAME,
                true
            )
        }.onFailure {
            AppRuntime.manifest.log("getPref: ${it.message}")
            AppRuntime.manifest.logE(it)
        }.getOrNull()
    }

    /**
     * 读取配置
     * @param key String
     * @return String
     */
    fun configString(key: String, def: String = ""): String {
        return getPref()?.getString(key, def) ?: def
    }

    /**
     * 读取配置
     * @param key String
     * @return String
     */
    fun configBoolean(key: String, def: Boolean = false): Boolean {
        return getPref()?.getBoolean(key, def) ?: def
    }
}