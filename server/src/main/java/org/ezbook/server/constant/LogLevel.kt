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

package org.ezbook.server.constant

import android.util.Log

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR, FATAL;

    fun toAndroidLevel(): Int {
        return when (this) {
            DEBUG -> Log.DEBUG
            INFO -> Log.INFO
            WARN -> Log.WARN
            ERROR -> Log.ERROR
            FATAL -> Log.ASSERT
        }
    }

    companion object {
        fun fromAndroidLevel(value: Int): LogLevel {
            return when (value) {
                Log.VERBOSE, Log.DEBUG -> DEBUG
                Log.INFO -> INFO
                Log.WARN -> WARN
                Log.ERROR -> ERROR
                Log.ASSERT -> FATAL
                else -> DEBUG
            }
        }
    }
}