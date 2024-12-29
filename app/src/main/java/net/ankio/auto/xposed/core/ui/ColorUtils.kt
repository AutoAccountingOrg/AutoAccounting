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
import android.graphics.Color

open class ColorUtils {
    open fun isDarkMode(context: Context): Boolean {
        return Configuration.UI_MODE_NIGHT_YES == (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
    }


    open val mainColorLight = "#353535"
    open val subColorLight = "#999999"
    open val backgroundColorLight = "#ffffff"

    open val mainColorDark = "#d3d3d3"
    open val subColorDark = "#656565"
    open val backgroundColorDark = "#2e2e2e"

    open fun getMainColor(context: Context): Int {
        return Color.parseColor(if (isDarkMode(context)) mainColorDark else mainColorLight)
    }

    open fun getSubColor(context: Context): Int {
        return Color.parseColor(if (isDarkMode(context)) subColorDark else subColorLight)
    }

    open fun getBackgroundColor(context: Context): Int {
        return Color.parseColor(if (isDarkMode(context)) backgroundColorDark else backgroundColorLight)
    }

}