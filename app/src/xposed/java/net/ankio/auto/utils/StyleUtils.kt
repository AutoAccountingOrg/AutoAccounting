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

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.util.TypedValue
import android.widget.TextView


object StyleUtils {
    const val TEXT_SIZE_DEFAULT = 15.0f
    const val TEXT_SIZE_BIG = 18.0f
    const val TEXT_SIZE_SMALL = 12.0f
    const val TEXT_COLOR_DEFAULT = Color.BLACK
    const val TEXT_COLOR_SECONDARY = -0x756767
    const val LINE_COLOR_DEFAULT = -0x1a1a1b
    fun apply(textView: TextView) {
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DEFAULT)
        textView.setTextColor(TEXT_COLOR_DEFAULT)
    }

    fun isDarkMode(context: Context): Boolean {
        return Configuration.UI_MODE_NIGHT_YES == context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    }
}