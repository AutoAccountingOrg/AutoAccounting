package net.ankio.auto.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.ColorUtils
import net.ankio.auto.utils.ThemeUtils
import net.ankio.auto.utils.toThemeColor

class MaterialScanBorderView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val lightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 10.dp
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val borderRect = RectF()
    private val borderPath = Path()
    private val pathMeasure = PathMeasure()
    private var pathLength = 0f

    // 流光参数
    private var currProgress = 0f
    private val flowLengthRatio = 0.15f // 流光长度
    private val tailLengthRatio = 0.7f // 拖尾在流光中占比

    // 主色和拖尾渐变
    private val primaryColor = com.google.android.material.R.attr.colorPrimary.toThemeColor()
    private val flowColor = ColorUtils.setAlphaComponent(primaryColor, 230)
    private val tailColor = ColorUtils.setAlphaComponent(primaryColor, 0)

    private var animator: ValueAnimator? = null

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        startAnim()
    }

    private fun startAnim() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2200L
            repeatCount = ValueAnimator.INFINITE
            interpolator = null // 视觉匀速（线性）
            addUpdateListener {
                currProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val padding = 7.dp
        borderRect.set(
            padding, padding,
            w.toFloat() - padding,
            h.toFloat() - padding
        )
        borderPath.reset()
        val radius = 22.dp
        borderPath.addRoundRect(borderRect, radius, radius, Path.Direction.CW)
        pathMeasure.setPath(borderPath, false)
        pathLength = pathMeasure.length
    }

    override fun onDraw(canvas: Canvas) {
        // 可选描边
        // canvas.drawPath(borderPath, borderPaint)

        // === 计算流光起止点 ===
        val len = pathLength
        val flowLen = len * flowLengthRatio
        val start = currProgress * len
        var end = (start + flowLen)
        if (end > len) end -= len

        // === 拖尾渐变：拉长的水滴形 ===
        // 构造流光Path
        val lightPath = Path()
        if (end > start) {
            pathMeasure.getSegment(start, end, lightPath, true)
        } else {
            // 跨越起点
            pathMeasure.getSegment(start, len, lightPath, true)
            pathMeasure.getSegment(0f, end, lightPath, false)
        }

        // 计算头尾位置
        val headPos = FloatArray(2)
        val tailPos = FloatArray(2)
        pathMeasure.getPosTan(end, headPos, null)
        pathMeasure.getPosTan(start, tailPos, null)

        // 渐变模拟水滴：尾部透明
        val shader = LinearGradient(
            tailPos[0], tailPos[1], headPos[0], headPos[1],
            intArrayOf(tailColor, flowColor, tailColor),
            floatArrayOf(0f, tailLengthRatio, 1f),
            Shader.TileMode.CLAMP
        )
        lightPaint.shader = shader
        lightPaint.maskFilter = BlurMaskFilter(6.dp, BlurMaskFilter.Blur.NORMAL)
        canvas.drawPath(lightPath, lightPaint)
    }
}

// dp 扩展
val Number.dp: Float get() = this.toFloat() * Resources.getSystem().displayMetrics.density
