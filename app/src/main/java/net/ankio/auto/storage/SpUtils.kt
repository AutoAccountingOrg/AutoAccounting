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
        val pref = App.app.getSharedPreferences("AutoConfig", Context.MODE_WORLD_READABLE)
        val editor = pref.edit()

        return editor

    }
    fun putString(key: String, value: String) {
        val editor = getPref()
        editor.putString(key, value)
        editor.apply()
    }

    fun putBoolean(key: String, value: Boolean) {
        val editor = getPref()
        editor.putBoolean(key, value)
        editor.apply()
    }

    fun putInt(key: String, value: Int) {
        val editor = getPref()
        editor.putInt(key, value)
        editor.apply()
    }
}