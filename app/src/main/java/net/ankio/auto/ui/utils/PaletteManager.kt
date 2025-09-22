package net.ankio.auto.ui.utils

import android.content.Context
import android.util.SparseIntArray
import androidx.annotation.ColorInt
import androidx.annotation.IntRange
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import kotlin.math.abs

/**
 * 调色板管理器：提供统一的颜色管理方案
 *
 * 设计原则：
 * 1. 简洁直接：统一管理所有应用颜色资源
 * 2. 强调色由背景色动态计算，避免维护多套资源，保证一致对比度
 * 3. 轻量缓存：使用 SparseIntArray 缓存已解析过的颜色，避免重复解析与读取
 * 4. 动态分配：基于 label 内容自动分配颜色，无需硬编码映射
 *
 * 使用示例：
 * ```kotlin
 * // 基于 label 动态获取颜色
 * val colors = PaletteManager.getColorsByLabel(context, "账本管理")
 * view.setBackgroundColor(colors.background)
 * icon.setColorFilter(colors.emphasis)
 *
 * // 获取调色板颜色
 * val paletteColors = PaletteManager.getColors(context, 15)
 * ```
 */
object PaletteManager {

    /** 可用色系总数（与资源定义保持一致） */
    const val TOTAL_FAMILIES: Int = 50

    /**
     * 双色结果（同一色系下的 强调/背景）
     */
    data class Duo(
        @ColorInt val emphasis: Int,
        @ColorInt val background: Int
    )

    // 简单缓存：key=index(1..50)，value=颜色 int
    private val cacheBg = SparseIntArray()
    private val cacheEmphasis = SparseIntArray()

    // 基于 label 的颜色缓存：key=label.hashCode()，value=Duo
    private val cacheLabelColors = mutableMapOf<String, Duo>()

    /**
     * 基于 label 获取颜色
     * 相同的 label 总是返回相同的颜色
     *
     * @param context 上下文
     * @param label 标签字符串
     * @return 双色方案
     */
    fun getColorsByLabel(context: Context, label: String): Duo {
        return cacheLabelColors.getOrPut(label) {
            val index = calculateColorIndex(label)
            getColors(context, index)
        }
    }

    /**
     * 基于 label 计算调色板索引
     * 使用稳定的哈希算法确保相同输入总是得到相同输出
     *
     * @param label 标签字符串
     * @return 调色板索引 (1-50)
     */
    fun calculateColorIndex(label: String): Int {
        if (label.isEmpty()) return 1

        // 使用 Java 字符串哈希算法，确保跨平台一致性
        var hash = 0
        for (char in label) {
            hash = 31 * hash + char.code
        }

        // 将哈希值映射到 1-50 范围
        return (abs(hash) % TOTAL_FAMILIES) + 1
    }

    /**
     * 获取指定色系的双色（强调/背景）
     * @param context 上下文
     * @param index 色系索引（1..50），超出范围会被正规化到 1..50
     */
    fun getColors(
        context: Context,
        @IntRange(from = 1, to = TOTAL_FAMILIES.toLong()) index: Int
    ): Duo {
        val normalized = normalizeIndex(index)
        var bg = cacheBg.get(normalized)
        if (bg == 0) {
            val resId = getBgColorResId(context, normalized)
            bg = ContextCompat.getColor(context, resId)
            cacheBg.put(normalized, bg)
        }

        var emphasis = cacheEmphasis.get(normalized)
        if (emphasis == 0) {
            emphasis = calculateContrastingColor(bg)
            cacheEmphasis.put(normalized, emphasis)
        }
        return Duo(emphasis = emphasis, background = bg)
    }

    /**
     * 返回指定色系的背景颜色资源 id
     * 命名规则：palette_XX_bg
     */
    fun getBgColorResId(
        context: Context,
        @IntRange(from = 1, to = TOTAL_FAMILIES.toLong()) index: Int
    ): Int {
        val normalized = normalizeIndex(index)
        val name = buildBgName(normalized)
        val resId = context.resources.getIdentifier(name, "color", context.packageName)
        // 理论上应当存在；若异常缺失，回退到 palette_01（兜底不崩溃）
        if (resId == 0) {
            val fallback = context.resources.getIdentifier(
                buildBgName(1),
                "color",
                context.packageName
            )
            return if (fallback != 0) fallback else android.R.color.transparent
        }
        return resId
    }

    /**
     * 清除缓存（主题切换时调用）
     */
    fun clearCache() {
        cacheBg.clear()
        cacheEmphasis.clear()
        cacheLabelColors.clear()
    }

    private fun normalizeIndex(index: Int): Int {
        if (index in 1..TOTAL_FAMILIES) return index
        if (TOTAL_FAMILIES == 0) return 1
        val mod = index % TOTAL_FAMILIES
        return when {
            mod > 0 -> mod
            mod == 0 -> TOTAL_FAMILIES
            else -> (mod + TOTAL_FAMILIES)
        }
    }

    private fun buildBgName(index: Int): String {
        val idx = String.format("%02d", index)
        return "palette_${idx}_bg"
    }

    // 颜色计算参数 - 基于可访问性标准
    const val BRIGHTNESS_THRESHOLD = 0.4f
    const val DARK_FACTOR = 0.6f
    const val LIGHT_FACTOR = 0.4f

    /**
     * 计算与背景色形成良好对比的图标颜色
     * @param backgroundColor 背景颜色
     * @return 对比色
     */
    private fun calculateContrastingColor(@ColorInt backgroundColor: Int): Int {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(backgroundColor, hsl)

        // 根据亮度阈值调整明度
        hsl[2] = if (hsl[2] > BRIGHTNESS_THRESHOLD) {
            // 背景较亮，图标变暗
            (hsl[2] * DARK_FACTOR).coerceAtLeast(0f)
        } else {
            // 背景较暗，图标提亮
            (hsl[2] + (1f - hsl[2]) * LIGHT_FACTOR).coerceAtMost(1f)
        }

        return ColorUtils.HSLToColor(hsl)
    }
}


