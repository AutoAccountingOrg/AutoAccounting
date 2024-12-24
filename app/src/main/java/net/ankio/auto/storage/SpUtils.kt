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
    const val PREF_NAME = "AutoConfig"
    const val PERF_AUTHORITY = "net.ankio.auto.preferences"
    private fun getPref():SharedPreferences{
        return App.app.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    private fun getPrefRead():SharedPreferences{
        return getPref()
    }

    private fun getPrefWrite():SharedPreferences.Editor{
      val pref = getPref()
      return pref.edit()
    }
    fun getString(key: String, defValue: String = ""): String {
        return getPrefRead().getString(key, defValue) ?: defValue
    }

    fun putString(key: String, value: String) {
        val editor = getPrefWrite()
        editor.putString(key, value)
        editor.apply()
    }


    fun getBoolean(key: String, defValue: Boolean = false): Boolean {
        return getPrefRead().getBoolean(key, defValue)
    }

    fun putBoolean(key: String, value: Boolean) {
        val editor = getPrefWrite()
        editor.putBoolean(key, value)
        editor.apply()
    }

}