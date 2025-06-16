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

package net.ankio.auto.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.ColorUtils

class MaterialScanBorderView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(80, 255, 255, 255) // 常规边框淡色（可选）
        strokeWidth = 4.dp
        style = Paint.Style.STROKE
    }

    private val lightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 6.dp
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val borderRect = RectF()
    private val borderPath = Path()
    private val pathMeasure = PathMeasure()

    // 流光参数
    private var currProgress = 0f
    private val flowLengthRatio = 0.14f // 流光长度占总长的比例（0.0-1.0）

    private var animator: ValueAnimator? = null

    // 自定义颜色渐变
    private val lightColors = intArrayOf(
        Color.argb(220, 102, 204, 255), // 主色
        Color.argb(64, 102, 204, 255)   // 渐隐
    )

    init {
        startAnim()
    }

    private fun startAnim() {
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2500L
            repeatCount = ValueAnimator.INFINITE
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
        val radius = 18.dp
        borderPath.addRoundRect(borderRect, radius, radius, Path.Direction.CW)
        pathMeasure.setPath(borderPath, true)
    }

    override fun onDraw(canvas: Canvas) {
        // 1. 可选：常规描边
        // canvas.drawPath(borderPath, borderPaint)

        // 2. 绘制流光
        val len = pathMeasure.length
        val flowLen = len * flowLengthRatio
        val start = currProgress * len
        val end = (start + flowLen) % len

        val lightPath = Path()
        if (end > start) {
            pathMeasure.getSegment(start, end, lightPath, true)
        } else {
            // 跨越起点
            pathMeasure.getSegment(start, len, lightPath, true)
            pathMeasure.getSegment(0f, end, lightPath, true)
        }

        // 流光渐变
        val pos = FloatArray(2)
        pathMeasure.getPosTan((start + flowLen / 2) % len, pos, null)
        lightPaint.shader = LinearGradient(
            pos[0], pos[1], pos[0] + 1, pos[1] + 1, // 只为动画刷新
            lightColors, floatArrayOf(0f, 1f), Shader.TileMode.CLAMP
        )
        canvas.drawPath(lightPath, lightPaint)
    }
}

// dp 扩展
val Number.dp: Float get() = this.toFloat() * Resources.getSystem().displayMetrics.density