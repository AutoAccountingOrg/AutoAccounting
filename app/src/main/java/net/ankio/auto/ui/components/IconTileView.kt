package net.ankio.auto.ui.components

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.widget.*
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.res.use
import androidx.core.graphics.ColorUtils
import androidx.core.widget.ImageViewCompat
import net.ankio.auto.ui.theme.DynamicColors
import net.ankio.auto.ui.utils.PaletteManager
import net.ankio.auto.R

/**
 * 图标瓦片视图 - 显示圆形背景图标和文字标签的组合组件
 *
 * 特性：
 * - 自动根据背景色调整图标颜色
 * - 支持外部传入强调色
 * - 支持XML属性配置
 * - Material Design 风格
 *
 * 使用示例：
 * ```kotlin
 * // 方式1：仅设置背景色，自动计算图标颜色
 * iconTile.setCircleColor(backgroundColor)
 *
 * // 方式2：同时设置背景色和图标色
 * iconTile.setColors(backgroundColor, iconColor)
 *
 * // 方式3：使用PaletteManager的双色方案（推荐）
 * val colors = PaletteManager.getColorsByLabel(context, "标签")
 * iconTile.setColors(colors)
 * ```
 */
class IconTileView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    // 设计常量 - 基于Material Design规范
    private companion object {
        const val CIRCLE_SIZE_DP = 56
        const val ICON_SIZE_DP = 24
        const val TEXT_SIZE_SP = 11f
        const val TEXT_TOP_MARGIN_DP = 4
        const val TEXT_SIDE_PADDING_DP = 16
        const val CONTAINER_VERTICAL_PADDING_DP = 8

        // 颜色计算参数 - 基于可访问性标准
        const val BRIGHTNESS_THRESHOLD = 0.4f
        const val DARK_FACTOR = 0.6f
        const val LIGHT_FACTOR = 0.4f
    }

    private val circleSize = dp(CIRCLE_SIZE_DP)
    private val iconSize = dp(ICON_SIZE_DP)

    // 视图组件
    private lateinit var iconView: ImageView
    private lateinit var labelView: TextView
    private val circleBg = GradientDrawable().apply { shape = GradientDrawable.OVAL }

    @ColorInt
    private var circleColor = DynamicColors.SurfaceVariant

    init {
        setupLayout()
        createViews()
        parseAttributes(attrs)
    }

    /**
     * 设置布局基础属性
     */
    private fun setupLayout() {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        isClickable = true
        foreground = resolveDrawable(android.R.attr.selectableItemBackgroundBorderless)
        setPadding(0, dp(CONTAINER_VERTICAL_PADDING_DP), 0, dp(CONTAINER_VERTICAL_PADDING_DP))
    }

    /**
     * 创建子视图
     */
    private fun createViews() {
        // 图标容器：圆形背景 + 图标
        iconView = ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER)
        }

        val iconContainer = FrameLayout(context).apply {
            layoutParams = LayoutParams(circleSize, circleSize)
            background = circleBg
            addView(iconView)
        }
        addView(iconContainer)

        // 文字标签
        labelView = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE_SP)
            setTextColor(DynamicColors.OnSurface)
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            textAlignment = TEXT_ALIGNMENT_CENTER

            val textWidth = circleSize + dp(TEXT_SIDE_PADDING_DP)
            layoutParams = LayoutParams(textWidth, LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(TEXT_TOP_MARGIN_DP)
                gravity = Gravity.CENTER_HORIZONTAL
            }
        }
        addView(labelView)
    }

    /**
     * 解析XML属性
     */
    private fun parseAttributes(attrs: AttributeSet?) {
        if (attrs == null) return
        
        context.theme.obtainStyledAttributes(attrs, R.styleable.IconTileView, 0, 0).use { ta ->
            setIcon(ta.getResourceId(R.styleable.IconTileView_tileIcon, 0))
            setLabel(ta.getString(R.styleable.IconTileView_tileLabel) ?: "")
        }
    }

    // ========== 公共API ==========

    /**
     * 设置图标资源
     * @param resId 图标资源ID，0表示清空图标
     */
    fun setIcon(@DrawableRes resId: Int) {
        if (resId != 0) {
            iconView.setImageResource(resId)
        } else {
            iconView.setImageDrawable(null)
        }
    }

    /**
     * 设置标签文字
     * @param text 标签文本
     */
    fun setLabel(text: CharSequence) {
        labelView.text = text
    }
    /**
     * 设置背景色和图标色
     * @param backgroundColor 背景颜色
     * @param iconColor 图标颜色
     */
    fun setColors(@ColorInt backgroundColor: Int, @ColorInt iconColor: Int) {
        circleColor = backgroundColor
        circleBg.setColor(backgroundColor)
        ImageViewCompat.setImageTintList(iconView, ColorStateList.valueOf(iconColor))
    }

    /**
     * 使用PaletteManager的双色方案
     * @param colors 包含背景色和强调色的双色方案
     */
    fun setColors(colors: PaletteManager.Duo) {
        setColors(colors.background, colors.emphasis)
    }

    // ========== 颜色处理 ==========

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

    // ========== 工具方法 ==========

    /**
     * dp转px
     */
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    /**
     * 解析主题颜色属性
     */
    private fun resolveColor(attr: Int): Int = DynamicColors.resolve(context, attr)

    /**
     * 解析主题Drawable属性
     */
    private fun resolveDrawable(attr: Int) =
        context.obtainStyledAttributes(intArrayOf(attr)).use { it.getDrawable(0) }
}

