package net.ankio.auto.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import net.ankio.auto.App


const val NAME = "config"

@SuppressLint("StaticFieldLeak")
val context = App.context

val sp: SharedPreferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
object SpUtils {
    fun putBoolean(key: String,value:Boolean) = sp.edit().putBoolean(key, value).apply()
    fun getBoolean(key: String,result: Boolean = false) = sp.getBoolean(key, result)
    fun putString(key: String,value: String) = sp.edit().putString(key, value).apply()
    fun getString(key: String, result: String? = ""): String = sp.getString(key, result)?:""
    fun putInt(key: String,value:Int) = sp.edit().putInt(key, value).apply()

    fun getInt(key: String, result: Int = 0): Int = sp.getInt(key, result)

    fun putLong(key: String,value:Long) = sp.edit().putLong(key, value).apply()

    fun getLong(key: String, result: Long = 0): Long = sp.getLong(key, result)
}