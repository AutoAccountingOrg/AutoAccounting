/*
 * Copyright (C) 2023 ankio(ankio@ankio.net)
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

package net.ankio.auto.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.utils.server.model.SettingModel

object SpUtils {
    val sp = AppUtils.getApplication().getSharedPreferences("setting", 0)

    fun putBooleanRemote(
        key: String,
        value: Boolean,
    ) {
        put(key, value)
    }

    private fun put(
        key: String,
        value: Any,
    ) {
        SettingModel.set(
            SettingModel().apply {
                app = AppUtils.getApplication().packageName
                this.key = key
                this.value = value.toString()
            },
        )
    }

    private suspend fun get(key: String): String {
        return withContext(Dispatchers.IO) {
            SettingModel.get(AppUtils.getApplication().packageName, key)
        }
    }

    suspend fun getBooleanRemote(key: String): Boolean = get(key).toBoolean()

    fun putBoolean(
        key: String,
        value: Boolean,
    ) {
        sp.edit().putBoolean(key, value).apply()
    }

    fun getBoolean(
        key: String,
        default: Boolean,
    ): Boolean = sp.getBoolean(key, default)

    fun putStringRemote(
        key: String,
        value: String,
    ) {
        put(key, value)
    }

    fun putString(
        key: String,
        value: String,
    ) {
        sp.edit().putString(key, value).apply()
    }

    fun getString(
        key: String,
        default: String? = "",
    ): String = sp.getString(key, default) ?: ""

    suspend fun getStringRemote(
        key: String,
        default: String? = "",
    ): String = get(key) ?: default ?: ""

    fun putIntRemote(
        key: String,
        value: Int,
    ) {
        put(key, value)
    }

    suspend fun getIntRemote(
        key: String,
        default: Int = 0,
    ): Int = get(key).toIntOrNull() ?: default

    fun getInt(
        key: String,
        default: Int = 0,
    ): Int {
        return sp.getInt(key, default)
    }

    fun putInt(
        key: String,
        value: Int,
    ) {
        sp.edit().putInt(key, value).apply()
    }
}
