package net.ankio.auto.xposed.core.utils

import android.content.Context
import android.content.SharedPreferences
import com.crossbowffs.remotepreferences.RemotePreferences
import kotlinx.coroutines.runBlocking
import net.ankio.auto.storage.SpUtils
import net.ankio.auto.xposed.core.App.Companion.TAG
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.model.SettingModel


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

    /**
     * 读取配置
     * @param key String
     * @return String
     */
    fun configString(key: String, def: String = ""): String {
        return runBlocking {
            SettingModel.get(key, def)
        }
    }

    /**
     * 读取配置
     * @param key String
     * @return String
     */
    fun configBoolean(key: String, def: Boolean = false): Boolean {
        return runBlocking {
            SettingModel.get(key, def.toString()).toBoolean()
        }
    }
}