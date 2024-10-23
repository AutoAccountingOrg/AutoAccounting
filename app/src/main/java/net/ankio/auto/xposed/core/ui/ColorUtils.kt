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

package net.ankio.auto.xposed.core.ui

import android.content.Context
import android.content.res.Configuration

object ColorUtils {
    private fun isDarkMode(context: Context): Boolean {
        return Configuration.UI_MODE_NIGHT_YES == (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
    }

    fun getMainColor(context: Context): Int {
        return if (isDarkMode(context)) -0x2c2c2d else -0xcacacb
    }

    fun getSubColor(context: Context): Int {
        return if (isDarkMode(context)) -0x9a9a9b else -0x666667
    }

    fun getBackgroundColor(context: Context): Int {
        return if (isDarkMode(context)) -0xd1d1d2 else -0x1
    }

}