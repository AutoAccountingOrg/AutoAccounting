/*
 * Copyright (C) 2026 ankio(ankio@ankio.net)
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
 *  limitations under the License.
 */

package net.ankio.auto.ui.utils

import android.content.Context
import android.graphics.Color
import androidx.core.graphics.ColorUtils

/**
 * 标签颜色工具类
 *
 * 说明：
 * - 颜色完全由标签文本计算，避免依赖存储颜色
 * - 统一输出前景/背景颜色，供 UI 使用
 */
object TagColorUtils {

    /**
     * 获取强调色（用于选中态或强调态）
     * @param context 上下文
     * @param tagName 标签文本
     * @return 强调色
     */
    fun getAccentColor(context: Context, tagName: String): Int {
        return PaletteManager.getColorsByLabel(context, tagName).emphasis
    }

    /**
     * 获取标签显示颜色（文本/背景）
     * @param context 上下文
     * @param tagName 标签文本
     * @param defaultTextColor 默认文本色
     * @param defaultBackgroundColor 默认背景色
     * @return Pair(文本色, 背景色)
     */
    fun getLabelColors(
        context: Context,
        tagName: String,
        defaultTextColor: Int,
        defaultBackgroundColor: Int
    ): Pair<Int, Int> {
        val palette = PaletteManager.getColorsByLabel(context, tagName)
        // 调色板颜色降噪处理，避免抢走主视觉
        val mixedTextColor = ColorUtils.blendARGB(defaultTextColor, palette.emphasis, 0.45f)
        val mixedBackgroundColor = ColorUtils.blendARGB(
            defaultBackgroundColor,
            palette.background,
            0.2f
        )
        val textColor = applyAlpha(mixedTextColor, 0.8f)
        val backgroundColor = applyAlpha(mixedBackgroundColor, 0.8f)
        return if (tagName.isEmpty()) {
            defaultTextColor to defaultBackgroundColor
        } else {
            textColor to backgroundColor
        }
    }

    /**
     * 透明度处理，保持颜色主体但降低存在感
     */
    private fun applyAlpha(color: Int, alpha: Float): Int {
        val clampedAlpha = (alpha.coerceIn(0f, 1f) * 255).toInt()
        return Color.argb(
            clampedAlpha,
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )
    }
}
