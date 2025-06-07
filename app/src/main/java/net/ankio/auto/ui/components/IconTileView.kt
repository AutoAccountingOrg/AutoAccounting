package net.ankio.auto.ui.components

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
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
import com.google.android.material.R as MaterialR
import net.ankio.auto.R

class IconTileView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val circleSize = dp(56)
    private val iconSize = dp(24)

    private val iconView = ImageView(context)
    private val labelView = TextView(context)
    private val circleBg = GradientDrawable().apply { shape = GradientDrawable.OVAL }

    @ColorInt
    private var circleColor = resolveColor(MaterialR.attr.colorSurfaceVariant)

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        isClickable = true
        foreground = resolveDrawable(android.R.attr.selectableItemBackgroundBorderless)
        setPadding(0, dp(8), 0, dp(8))

        // 圆形 + 图标
        addView(FrameLayout(context).apply {
            layoutParams = LayoutParams(circleSize, circleSize)
            background = circleBg
            addView(iconView.apply {
                layoutParams = FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER)
            })
        })

        // 文字
        addView(labelView.apply {
            // 1️⃣ 文字样式
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)                         // 稍微小一点
            setTextColor(resolveColor(MaterialR.attr.colorOnSurface))
            maxLines = 2                                                       // 最多两行
            ellipsize = TextUtils.TruncateAt.END                               // 超出省略号

            // 2️⃣ 宽度 & 顶部间距
            val textWidth = circleSize + dp(16)                                // 圆 + 两边内边距
            layoutParams = LayoutParams(textWidth, LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(4)                                              // 缩小顶部间距
                gravity = Gravity.CENTER_HORIZONTAL                            // 水平居中
            }

            // 3️⃣ 居中对齐
            textAlignment = TEXT_ALIGNMENT_CENTER
        })


        // 解析 XML
        context.theme.obtainStyledAttributes(attrs, R.styleable.IconTileView, 0, 0).use { ta ->
            setIcon(ta.getResourceId(R.styleable.IconTileView_tileIcon, 0))
            setLabel(ta.getString(R.styleable.IconTileView_tileLabel) ?: "")
            setCircleColor(
                ta.getColor(
                    R.styleable.IconTileView_circleColor,
                    circleColor
                )
            )
        }
    }

    /* ---------- 公共 ---------- */
    fun setIcon(@DrawableRes resId: Int) {
        if (resId != 0) iconView.setImageResource(resId)
    }

    fun setLabel(text: CharSequence) {
        labelView.text = text
    }

    fun setCircleColor(@ColorInt c: Int) {
        circleColor = c; circleBg.setColor(c); applyAutoTint()
    }

    /* ---------- 自动染色核心 ---------- */
    /* ---------- 自动染色核心 ---------- */
    private fun applyAutoTint() {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(circleColor, hsl)

        hsl[2] = if (hsl[2] > 0.4f) {          // 圆色较亮 → 变暗
            (hsl[2] * 0.6f).coerceAtLeast(0f)
        } else {                               // 圆色较暗 → 提亮
            (hsl[2] + (1f - hsl[2]) * 0.4f).coerceAtMost(1f)
        }

        val tint = ColorUtils.HSLToColor(hsl)
        ImageViewCompat.setImageTintList(iconView, ColorStateList.valueOf(tint))
    }

    /* ---------- 工具 ---------- */
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun resolveColor(attr: Int) = TypedValue().let {
        context.theme.resolveAttribute(attr, it, true); it.data
    }

    private fun resolveDrawable(attr: Int) =
        context.obtainStyledAttributes(intArrayOf(attr)).use { it.getDrawable(0) }
}

