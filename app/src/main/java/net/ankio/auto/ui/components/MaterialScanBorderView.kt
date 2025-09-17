package net.ankio.auto.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.ColorUtils
import net.ankio.auto.ui.theme.DynamicColors

class MaterialScanBorderView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val lightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 10.dp
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    // 四角轻微发光（强调圆角）
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val borderRect = RectF()
    private val borderPath = Path()
    private val pathMeasure = PathMeasure()
    private var pathLength = 0f
    private var cornerRadius = 0f

    // 流光参数
    private var currProgress = 0f
    private val flowLengthRatio = 0.15f // 流光长度
    private val tailLengthRatio = 0.7f // 拖尾在流光中占比

    // 次级流光参数（相位错开，细节更丰富）
    private val secondaryOffset = 0.45f
    private val secondaryFlowLengthRatio = 0.08f

    // 主色和拖尾渐变
    private val primaryColor = DynamicColors.Primary
    private val flowColor = ColorUtils.setAlphaComponent(primaryColor, 230)
    private val tailColor = ColorUtils.setAlphaComponent(primaryColor, 0)
    private val flowColorSecondary = ColorUtils.setAlphaComponent(primaryColor, 180)

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
        cornerRadius = 22.dp
        borderPath.addRoundRect(borderRect, cornerRadius, cornerRadius, Path.Direction.CW)
        pathMeasure.setPath(borderPath, false)
        pathLength = pathMeasure.length
    }

    override fun onDraw(canvas: Canvas) {
        // 保留无静态描边，避免“刻意勾边”的不适感

        // === 计算流光起止点 ===
        val len = pathLength
        val flowLen = len * flowLengthRatio
        val start = currProgress * len
        var end = (start + flowLen)
        if (end > len) end -= len

        // === 拖尾渐变：拉长的水滴形（主流光）===
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

        // 渐变模拟水滴：尾部透明（主流光）
        val shader = LinearGradient(
            tailPos[0], tailPos[1], headPos[0], headPos[1],
            intArrayOf(tailColor, flowColor, tailColor),
            floatArrayOf(0f, tailLengthRatio, 1f),
            Shader.TileMode.CLAMP
        )
        lightPaint.shader = shader
        lightPaint.maskFilter = BlurMaskFilter(6.dp, BlurMaskFilter.Blur.NORMAL)
        canvas.drawPath(lightPath, lightPaint)

        // === 次级流光：相位错开，细节更丰富 ===
        val progress2 = (currProgress + secondaryOffset).let { if (it >= 1f) it - 1f else it }
        val start2 = progress2 * len
        var end2 = start2 + len * secondaryFlowLengthRatio
        if (end2 > len) end2 -= len

        val lightPath2 = Path()
        if (end2 > start2) {
            pathMeasure.getSegment(start2, end2, lightPath2, true)
        } else {
            pathMeasure.getSegment(start2, len, lightPath2, true)
            pathMeasure.getSegment(0f, end2, lightPath2, false)
        }

        val headPos2 = FloatArray(2)
        val tailPos2 = FloatArray(2)
        pathMeasure.getPosTan(end2, headPos2, null)
        pathMeasure.getPosTan(start2, tailPos2, null)

        val shader2 = LinearGradient(
            tailPos2[0], tailPos2[1], headPos2[0], headPos2[1],
            intArrayOf(tailColor, flowColorSecondary, tailColor),
            floatArrayOf(0f, tailLengthRatio, 1f),
            Shader.TileMode.CLAMP
        )
        lightPaint.shader = shader2
        lightPaint.maskFilter = BlurMaskFilter(4.dp, BlurMaskFilter.Blur.NORMAL)
        canvas.drawPath(lightPath2, lightPaint)

        // === 四角轻微发光：强调边界与圆角 ===
        val glowAlpha = 90
        glowPaint.color = ColorUtils.setAlphaComponent(primaryColor, glowAlpha)
        glowPaint.maskFilter = BlurMaskFilter(14.dp, BlurMaskFilter.Blur.NORMAL)
        val r = 3.5f.dp
        // 左上
        canvas.drawCircle(
            borderRect.left + cornerRadius,
            borderRect.top + cornerRadius,
            r,
            glowPaint
        )
        // 右上
        canvas.drawCircle(
            borderRect.right - cornerRadius,
            borderRect.top + cornerRadius,
            r,
            glowPaint
        )
        // 左下
        canvas.drawCircle(
            borderRect.left + cornerRadius,
            borderRect.bottom - cornerRadius,
            r,
            glowPaint
        )
        // 右下
        canvas.drawCircle(
            borderRect.right - cornerRadius,
            borderRect.bottom - cornerRadius,
            r,
            glowPaint
        )
    }
}

// dp 扩展
val Number.dp: Float get() = this.toFloat() * Resources.getSystem().displayMetrics.density
