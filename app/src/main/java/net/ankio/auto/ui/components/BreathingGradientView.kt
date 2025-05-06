package net.ankio.auto.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import androidx.core.graphics.ColorUtils
import com.google.android.material.color.MaterialColors
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * 平衡对比度的流动呼吸背景：
 * - 用 4 段径向渐变（轻度亮 → 容器色 → 容器色 → 轻度暗），
 * - 混合比例 25%，消除过亮/过暗，
 * - 圆心沿圆周运动，周期 5s，
 * - 无需外部属性，开箱即用。
 */
class BreathingGradientView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val cycleDuration = 5000L    // 5s 一圈
    private val orbitRadiusFactor = 0.25f    // 轨迹半径 = 25% 宽/高
    private val blendFactor = 0.25f    // 混合白/黑比例 25%

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val matrix = Matrix()
    private lateinit var shader: RadialGradient
    private var progress = 0f                // 动画进度 0..1

    init {
        setWillNotDraw(false)
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = cycleDuration
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // 取容器色
        val theme1 = MaterialColors.getColor(
            this, com.google.android.material.R.attr.colorPrimaryContainer, 0xFFBB86FC.toInt()
        )
        val theme2 = MaterialColors.getColor(
            this, com.google.android.material.R.attr.colorSecondaryContainer, 0xFF03DAC6.toInt()
        )
        // 轻度亮/暗
        val light = ColorUtils.blendARGB(theme1, Color.WHITE, blendFactor)
        val dark = ColorUtils.blendARGB(theme2, Color.BLACK, blendFactor)

        // 四段径向渐变：light → theme1 → theme2 → dark
        shader = RadialGradient(
            w / 2f, h / 2f, max(w, h) * 0.75f,
            intArrayOf(light, theme1, theme2, dark),
            floatArrayOf(0f, 0.3f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = shader
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 中心圆周运动
        val angle = progress * (2 * Math.PI).toFloat()
        val cxOff = width * orbitRadiusFactor * cos(angle)
        val cyOff = height * orbitRadiusFactor * sin(angle)
        matrix.setTranslate(cxOff, cyOff)
        shader.setLocalMatrix(matrix)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }
}
