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
import kotlin.math.PI

/**
 * 简单的两色呼吸渐变背景：
 * - 基于 PrimaryContainer 的柔和渐变（亮变体 → 基础色）
 * - 圆心沿圆周运动 + 半径动态变化（呼吸效果）
 * - 周期 5s，无需外部属性，开箱即用
 */
class BreathingGradientView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val cycleDuration = 5000L    // 5s 一圈
    private val orbitRadiusFactor = 0.25f    // 轨迹半径 = 25% 宽/高
    private val radiusMinFactor = 0.6f    // 最小半径因子
    private val radiusMaxFactor = 1.0f    // 最大半径因子
    private val brightnessBoost = 0.15f    // 亮度提升15%，用于生成亮变体
    private val saturationReduce = 0.5f    // 饱和度降低到50%，更柔和

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private lateinit var shader: RadialGradient
    private var progress = 0f                // 动画进度 0..1
    private var baseRadius = 0f              // 基础半径
    private var colorA = 0                   // 颜色A（亮）
    private var colorB = 0                   // 颜色B（基础色）

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
        // 使用 PrimaryContainer 作为基础色（更柔和，适合背景）
        val baseColor = DynamicColors.PrimaryContainer

        // 转换为HSL进行调整
        val hslBase = FloatArray(3)
        ColorUtils.colorToHSL(baseColor, hslBase)

        // 颜色B：降低饱和度，更柔和
        val hslB = hslBase.clone()
        hslB[1] *= saturationReduce  // 降低饱和度
        colorB = ColorUtils.HSLToColor(hslB)

        // 颜色A：在颜色B基础上提升亮度，形成亮变体
        val hslA = hslB.clone()
        hslA[2] = (hslA[2] + brightnessBoost).coerceAtMost(1.0f)  // 提升亮度
        colorA = ColorUtils.HSLToColor(hslA)
        
        // 计算基础半径
        baseRadius = max(w, h) * 0.75f

        // 简单的两色径向渐变：A（亮） → B（基础）
        shader = RadialGradient(
            w / 2f, h / 2f, baseRadius,
            intArrayOf(colorA, colorB),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = shader
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!::shader.isInitialized) return

        // 计算呼吸半径：使用sin函数实现平滑的呼吸效果
        val breathPhase = progress * (2 * PI).toFloat()
        val radiusScale = radiusMinFactor + (radiusMaxFactor - radiusMinFactor) *
                ((sin(breathPhase) + 1f) / 2f)
        val currentRadius = baseRadius * radiusScale

        // 圆心圆周运动
        val orbitPhase = progress * (2 * PI).toFloat()
        val centerX = width / 2f + width * orbitRadiusFactor * cos(orbitPhase)
        val centerY = height / 2f + height * orbitRadiusFactor * sin(orbitPhase)

        // 重新创建shader以更新半径和圆心位置
        shader = RadialGradient(
            centerX, centerY, currentRadius,
            intArrayOf(colorA, colorB),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = shader

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }
}
