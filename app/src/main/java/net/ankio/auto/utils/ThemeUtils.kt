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
import android.graphics.Color
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.appcompat.view.ContextThemeWrapper
import com.google.android.material.color.MaterialColors
import com.quickersilver.themeengine.ThemeEngine

object ThemeUtils {
    /**
     * 获取主题色
     */
    fun getThemeAttrColor(context: Context,  @AttrRes attrResId: Int): Int {
        return MaterialColors.getColor(ContextThemeWrapper(context, ThemeEngine.getInstance(context).getTheme()), attrResId, Color.WHITE)
    }
}