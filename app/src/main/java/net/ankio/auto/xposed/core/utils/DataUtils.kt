package net.ankio.auto.xposed.core.utils

import android.content.Context
import android.content.SharedPreferences
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.BuildConfig
import net.ankio.auto.xposed.core.App.Companion.TAG
import net.ankio.auto.xposed.core.logger.Logger

object DataUtils{

    const val PREF_NAME = "AutoConfig"
    /**
     * 保存数据
     * @param key String
     * @param value String
     */
    fun set(key: String, value: String) {

        if (AppRuntime.manifest.packageName == "android"){
            return
        }

        if (AppRuntime.application == null) {
            return
        }
        val sharedPreferences: SharedPreferences =
            AppRuntime.application !!.getSharedPreferences(TAG, Context.MODE_PRIVATE) // 私有数据
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
        if (AppRuntime.manifest.packageName == "android"){
            return def
        }
        if (AppRuntime.application  == null) {
            return def
        }
        val sharedPreferences: SharedPreferences =
            AppRuntime.application !!.getSharedPreferences(TAG, Context.MODE_PRIVATE) // 私有数据
        return sharedPreferences.getString(key, def) ?: def
    }
    private val pref = XSharedPreferences(BuildConfig.APPLICATION_ID, PREF_NAME)
    /**
     * 读取配置
     * @param key String
     * @return String
     */
    fun configString(key: String,def: String = ""): String {
        if (!pref.file.canRead()){
            Logger.logE(TAG, Throwable("无法使用XSharedPreferences读取配置，一些设置可能无法按照预期工作"))
            return def
        }
        return  pref.getString(key, def) ?: def
    }

    /**
     * 读取配置
     * @param key String
     * @return String
     */
    fun configBoolean(key: String,def: Boolean = false): Boolean {
        if (!pref.file.canRead()){
            Logger.logE(TAG, Throwable("无法使用XSharedPreferences读取配置，一些设置可能无法按照预期工作"))
            return def
        }
        return  pref.getBoolean(key,def)
    }
}