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
import android.graphics.*
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import androidx.core.graphics.ColorUtils
import net.ankio.auto.ui.theme.DynamicColors
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * 平衡对比度的流动呼吸背景：
 * - 用 3 段径向渐变（轻度亮 → 容器色 → 轻度暗），
 * - 统一使用 PrimaryContainer 作为基础色，只改变亮度，确保色相一致，
 * - 亮部混合白色 20%，暗部混合黑色 10%，
 * - 圆心沿圆周运动，周期 5s，
 * - 无需外部属性，开箱即用。
 */
class BreathingGradientView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val cycleDuration = 5000L    // 5s 一圈
    private val orbitRadiusFactor = 0.25f    // 轨迹半径 = 25% 宽/高
    private val lightBlendFactor = 0.20f    // 亮部混合白色比例 20%
    private val darkBlendFactor = 0.10f    // 暗部混合黑色比例 10%

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val matrix = Matrix()
    private lateinit var shader: RadialGradient
    private var progress = 0f                // 动画进度 0..1

    // Store a reference to the animator to be able to cancel it later
    private var animator: ValueAnimator? = null

    init {
        setWillNotDraw(false)
        setupAnimator()
    }

    private fun setupAnimator() {
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = cycleDuration
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
        }
    }

    // Start animation when attached to window
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator?.start()
    }

    // Stop animation when detached from window
    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // 统一使用 PrimaryContainer 作为基础色，只改变亮度
        val baseColor = DynamicColors.PrimaryContainer
        // 轻度亮/暗（统一基础色，确保色相一致）
        val light = ColorUtils.blendARGB(baseColor, Color.WHITE, lightBlendFactor)
        val dark = ColorUtils.blendARGB(baseColor, Color.BLACK, darkBlendFactor)

        // 三段径向渐变：light → baseColor → dark（简化，消除色相跳跃）
        shader = RadialGradient(
            w / 2f, h / 2f, max(w, h) * 0.75f,
            intArrayOf(light, baseColor, dark),
            floatArrayOf(0f, 0.5f, 1f),
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
