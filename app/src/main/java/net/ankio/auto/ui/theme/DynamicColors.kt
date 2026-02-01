/*
 * Copyright (C) 2025 ankio
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

package net.ankio.auto.ui.theme

import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.google.android.material.color.MaterialColors
import com.google.android.material.elevation.SurfaceColors
import com.google.android.material.R as MaterialR
import androidx.appcompat.R as AppCompatR
import net.ankio.auto.autoApp
import net.ankio.auto.utils.ThemeUtils

/**
 * 统一的主题色访问入口：Color.Primary/OnPrimary 等。
 *
 * 原则：
 * - 简洁直接：属性名称与 MD3 token 一致
 * - 无副作用：仅解析主题色，不做其它逻辑
 * - 不破坏现有：保留原有 `Int.toThemeColor()` 的使用方式
 */
object DynamicColors {
    // 通用解析（应用主题）
    @JvmStatic
    @ColorInt
    fun resolve(@AttrRes attr: Int, @ColorInt defaultColor: Int = AndroidColor.WHITE): Int {
        val themed = ThemeUtils.themedCtx(autoApp)
        return MaterialColors.getColor(themed, attr, defaultColor)
    }

    // 通用解析（指定上下文）
    @JvmStatic
    @ColorInt
    fun resolve(
        context: Context,
        @AttrRes attr: Int,
        @ColorInt defaultColor: Int = AndroidColor.WHITE
    ): Int {
        return MaterialColors.getColor(context, attr, defaultColor)
    }


    // 常用主题色（属性式访问）
    val Primary get() = resolve(AppCompatR.attr.colorPrimary)
    val OnPrimary get() = resolve(MaterialR.attr.colorOnPrimary)
    val PrimaryContainer get() = resolve(MaterialR.attr.colorPrimaryContainer)
    val OnPrimaryContainer get() = resolve(MaterialR.attr.colorOnPrimaryContainer)

    val Secondary get() = resolve(MaterialR.attr.colorSecondary)
    val OnSecondary get() = resolve(MaterialR.attr.colorOnSecondary)
    val SecondaryContainer get() = resolve(MaterialR.attr.colorSecondaryContainer)
    val OnSecondaryContainer get() = resolve(MaterialR.attr.colorOnSecondaryContainer)

    val Tertiary get() = resolve(MaterialR.attr.colorTertiary)
    val OnTertiary get() = resolve(MaterialR.attr.colorOnTertiary)
    val TertiaryContainer get() = resolve(MaterialR.attr.colorTertiaryContainer)
    val OnTertiaryContainer get() = resolve(MaterialR.attr.colorOnTertiaryContainer)

    val Error get() = resolve(AppCompatR.attr.colorError)
    val OnError get() = resolve(MaterialR.attr.colorOnError)
    val ErrorContainer get() = resolve(MaterialR.attr.colorErrorContainer)
    val OnErrorContainer get() = resolve(MaterialR.attr.colorOnErrorContainer)

    val Surface get() = resolve(MaterialR.attr.colorSurface)
    val OnSurface get() = resolve(MaterialR.attr.colorOnSurface)
    val SurfaceVariant get() = resolve(MaterialR.attr.colorSurfaceVariant)
    val OnSurfaceVariant get() = resolve(MaterialR.attr.colorOnSurfaceVariant)
    val SurfaceInverse get() = resolve(MaterialR.attr.colorSurfaceInverse)
    val OnSurfaceInverse get() = resolve(MaterialR.attr.colorOnSurfaceInverse)

    val SurfaceDim get() = resolve(MaterialR.attr.colorSurfaceDim)
    val SurfaceBright get() = resolve(MaterialR.attr.colorSurfaceBright)
    val SurfaceContainerLowest get() = resolve(MaterialR.attr.colorSurfaceContainerLowest)
    val SurfaceContainerLow get() = resolve(MaterialR.attr.colorSurfaceContainerLow)
    val SurfaceContainer get() = resolve(MaterialR.attr.colorSurfaceContainer)
    val SurfaceContainerHigh get() = resolve(MaterialR.attr.colorSurfaceContainerHigh)
    val SurfaceContainerHighest get() = resolve(MaterialR.attr.colorSurfaceContainerHighest)

    val Background get() = resolve(MaterialR.attr.backgroundColor)
    val OnBackground get() = resolve(MaterialR.attr.colorOnBackground)

    val Outline get() = resolve(MaterialR.attr.colorOutline)
    val OutlineVariant get() = resolve(MaterialR.attr.colorOutlineVariant)

    val SurfaceColor1 get() = SurfaceColors.SURFACE_1.getColor(ThemeUtils.themedCtx(autoApp))
    val SurfaceColor2 get() = SurfaceColors.SURFACE_2.getColor(ThemeUtils.themedCtx(autoApp))
    val SurfaceColor3 get() = SurfaceColors.SURFACE_3.getColor(ThemeUtils.themedCtx(autoApp))
    val SurfaceColor4 get() = SurfaceColors.SURFACE_4.getColor(ThemeUtils.themedCtx(autoApp))
    val SurfaceColor5 get() = SurfaceColors.SURFACE_5.getColor(ThemeUtils.themedCtx(autoApp))

    val PrimaryInverse get() = resolve(MaterialR.attr.colorPrimaryInverse)
}

fun Int.toHex(): String {
    return String.format("#%06X", 0xFFFFFF and this)
}

fun Int.toARGBHex(): String {
    val r = (this shr 16) and 0xFF
    val g = (this shr 8) and 0xFF
    val b = this and 0xFF
    val a = (this shr 24) and 0xFF // Alpha 在 Android 中是在最前面的

    // 将 Alpha 拼在 RGB 后面
    return String.format("#%02X%02X%02X%02X", r, g, b, a)
}
