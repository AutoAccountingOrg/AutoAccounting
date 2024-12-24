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
import android.content.SharedPreferences
import net.ankio.auto.App

object SpUtils {
    private fun getPref():SharedPreferences.Editor{
        //getSharedPreferences
        val pref = runCatching {
            App.app.getSharedPreferences("AutoConfig", Context.MODE_WORLD_READABLE)
        }.onFailure {
            it.printStackTrace()
            Logger.e("框架不支持MODE_WORLD_READABLE，部分设置可能不生效。")
        }.getOrNull()?:App.app.getSharedPreferences("AutoConfig", Context.MODE_PRIVATE)

        val editor = pref.edit()

        return editor

    }

    fun getString(key: String, defValue: String = ""): String {
        val pref = App.app.getSharedPreferences("AutoConfig", Context.MODE_PRIVATE)
        return pref.getString(key, defValue) ?: defValue
    }

    fun putString(key: String, value: String) {
        val editor = getPref()
        editor.putString(key, value)
        editor.apply()
    }


    fun getBoolean(key: String, defValue: Boolean = false): Boolean {
        val pref = App.app.getSharedPreferences("AutoConfig", Context.MODE_PRIVATE)
        return pref.getBoolean(key, defValue)
    }

    fun putBoolean(key: String, value: Boolean) {
        val editor = getPref()
        editor.putBoolean(key, value)
        editor.apply()
    }

    fun getInt(key: String, defValue: Int = 0): Int {
        val pref = App.app.getSharedPreferences("AutoConfig", Context.MODE_PRIVATE)
        return pref.getInt(key, defValue)
    }

    fun putInt(key: String, value: Int) {
        val editor = getPref()
        editor.putInt(key, value)
        editor.apply()
    }
}