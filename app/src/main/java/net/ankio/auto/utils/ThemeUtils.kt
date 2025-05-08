/*
 * Copyright (C) 2025 ankio(ankio@ankio.net)
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
import androidx.annotation.StyleRes
import androidx.appcompat.view.ContextThemeWrapper
import com.google.android.material.color.DynamicColors
import net.ankio.auto.R
import net.ankio.auto.autoApp


import rikka.core.util.ResourceUtils

object ThemeUtils {

    @StyleRes
    fun getNightThemeStyleRes(context: Context): Int {
        return if (PrefManager.blackDarkTheme && ResourceUtils.isNightMode(context.resources.configuration))
            R.style.ThemeOverlay_Dark else R.style.ThemeOverlay
    }


    val isDark =
        PrefManager.blackDarkTheme && ResourceUtils.isNightMode(autoApp.resources.configuration)
    val isSystemAccent
        get() = DynamicColors.isDynamicColorAvailable() && PrefManager.followSystemAccent

    private val colorThemeMap = mapOf(
        "MATERIAL_DEFAULT" to R.style.ThemeOverlay_MaterialDefault,
        "MATERIAL_SAKURA" to R.style.ThemeOverlay_MaterialSakura,
        "MATERIAL_RED" to R.style.ThemeOverlay_MaterialRed,
        "MATERIAL_PINK" to R.style.ThemeOverlay_MaterialPink,
        "MATERIAL_PURPLE" to R.style.ThemeOverlay_MaterialPurple,
        "MATERIAL_DEEP_PURPLE" to R.style.ThemeOverlay_MaterialDeepPurple,
        "MATERIAL_INDIGO" to R.style.ThemeOverlay_MaterialIndigo,
        "MATERIAL_BLUE" to R.style.ThemeOverlay_MaterialBlue,
        "MATERIAL_LIGHT_BLUE" to R.style.ThemeOverlay_MaterialLightBlue,
        "MATERIAL_CYAN" to R.style.ThemeOverlay_MaterialCyan,
        "MATERIAL_TEAL" to R.style.ThemeOverlay_MaterialTeal,
        "MATERIAL_GREEN" to R.style.ThemeOverlay_MaterialGreen,
        "MATERIAL_LIGHT_GREEN" to R.style.ThemeOverlay_MaterialLightGreen,
        "MATERIAL_LIME" to R.style.ThemeOverlay_MaterialLime,
        "MATERIAL_YELLOW" to R.style.ThemeOverlay_MaterialYellow,
        "MATERIAL_AMBER" to R.style.ThemeOverlay_MaterialAmber,
        "MATERIAL_ORANGE" to R.style.ThemeOverlay_MaterialOrange,
        "MATERIAL_DEEP_ORANGE" to R.style.ThemeOverlay_MaterialDeepOrange,
        "MATERIAL_BROWN" to R.style.ThemeOverlay_MaterialBrown,
        "MATERIAL_BLUE_GREY" to R.style.ThemeOverlay_MaterialBlueGrey
    )

    val colorTheme get() = if (isSystemAccent) "SYSTEM" else PrefManager.themeColor
    val colorThemeStyleRes: Int
        @StyleRes get() = colorThemeMap[colorTheme] ?: R.style.ThemeOverlay_MaterialDefault

    fun themedCtx(context: Context) = ContextThemeWrapper(context, R.style.AppTheme)
}