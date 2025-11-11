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
import android.content.res.Configuration
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.view.ContextThemeWrapper
import com.google.android.material.color.DynamicColors
import net.ankio.auto.R
import net.ankio.auto.autoApp
import rikka.core.util.ResourceUtils

object ThemeUtils {


    /**
     * 判断当前配置是否应该启用夜间主题。
     *
     * 处理策略：
     * 1. 用户强制浅色：忽略系统配置，直接返回 false；
     * 2. 用户强制夜间：忽略系统配置，直接返回 true；
     * 3. 跟随系统或自动：回落到当前配置的夜间标记；
     *
     * @param config 资源配置对象，用于读取当前的 UI_MODE 标记
     * @return true 表示夜间主题应该生效；false 则使用浅色主题
     */
    private fun isNightModeEnabled(config: Configuration): Boolean {
        return when (PrefManager.darkTheme) {
            AppCompatDelegate.MODE_NIGHT_NO -> false
            AppCompatDelegate.MODE_NIGHT_YES -> true
            else -> ResourceUtils.isNightMode(config)
        }
    }

    /**
     * 根据用户的夜间主题设定，统一调整上下文的 UI_MODE。
     *
     * 目的非常直接：保证在“禁用深色主题”时，无论系统处于何种模式，
     * 颜色解析始终走浅色资源；同理，强制深色也会完整套用夜间色板。
     *
     * @param context 原始上下文，可能携带系统的夜间配置
     * @return 调整后的上下文，颜色解析结果与用户设定保持一致
     */
    private fun wrapContextWithResolvedUiMode(context: Context): Context {
        val currentConfig = context.resources.configuration
        val shouldUseNight = isNightModeEnabled(currentConfig)
        val desiredNightFlag = if (shouldUseNight)
            Configuration.UI_MODE_NIGHT_YES
        else
            Configuration.UI_MODE_NIGHT_NO
        val currentNightFlag = currentConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (currentNightFlag == desiredNightFlag) {
            return context
        }
        val override = Configuration(currentConfig).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or desiredNightFlag
        }
        return context.createConfigurationContext(override)
    }

    @StyleRes
    fun getNightThemeStyleRes(context: Context): Int {
        return if (PrefManager.blackDarkTheme && isNightModeEnabled(context.resources.configuration))
            R.style.ThemeOverlay_Black else R.style.ThemeOverlay
    }


    val isDark: Boolean
        get() = PrefManager.blackDarkTheme && isNightModeEnabled(autoApp.resources.configuration)
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

    fun themedCtx(context: Context): Context {
        val resolvedContext = wrapContextWithResolvedUiMode(context)
        return if (isSystemAccent) {
            // 跟随系统：启用动态色，并应用基础 AppTheme，确保完整的 MD3 token 存在
            val dynamicWrapped = DynamicColors.wrapContextIfAvailable(resolvedContext)
            ContextThemeWrapper(dynamicWrapped, R.style.AppTheme)
        } else {
            // 自定义主题：先套用基础 AppTheme，提供完整的 MD3 token，再叠加自定义配色 Overlay
            val base = ContextThemeWrapper(resolvedContext, R.style.AppTheme)
            ContextThemeWrapper(base, colorThemeStyleRes)
        }

    }

}
