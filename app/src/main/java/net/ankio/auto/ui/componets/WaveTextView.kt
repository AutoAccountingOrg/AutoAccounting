/*
 * Copyright (C) 2023 ankio(ankio@ankio.net)
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

package net.ankio.auto.ui.componets

import android.content.Context
import android.graphics.Path
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import net.ankio.auto.R

/**
 * 给textview添加滚动波浪线
 */
class WaveTextView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : AppCompatTextView(context, attrs, defStyleAttr) {
        private var waveColor: Int
        private var waveAmplitude: Float
        private val waveFrequency: Float
        private val waveSpeed: Float
        private var waveOffset: Float = 0f
        private val path = Path()

    /*
        private val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL_AND_STROKE
        }
     */

        init {
            val typedArray =
                context.obtainStyledAttributes(
                    attrs,
                    R.styleable.WaveTextView,
                    defStyleAttr,
                    0,
                )
            waveColor =
                typedArray.getColor(
                    R.styleable.WaveTextView_waveColor,
                    currentTextColor, // 默认颜色
                )
            waveAmplitude =
                typedArray.getDimension(
                    R.styleable.WaveTextView_waveAmplitude,
                    (this.lineHeight / 9).toFloat(),
                ).coerceAtLeast(1f) // 确保振幅不为零
            waveFrequency =
                typedArray.getFloat(
                    R.styleable.WaveTextView_waveFrequency,
                    0.1f, // 默认频率
                )
            waveSpeed =
                typedArray.getFloat(
                    R.styleable.WaveTextView_waveSpeed,
                    0.7f, // 默认速度
                )
            typedArray.recycle()
        }

        override fun setTextColor(color: Int) {
            super.setTextColor(color)
            waveColor = color
        }

    /*    override fun onDraw(canvas: Canvas) {
          //  background = AppCompatResources.getDrawable(context,R.drawable.ripple_effect)
            val textHeight = this.lineHeight / 3

            // 计算底部 padding
            val paddingBottom = (textHeight + waveAmplitude).toInt()
            // 绘制文本之前增加底部 padding
            setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)

            super.onDraw(canvas)

            val centerY = height - paddingBottom.toFloat()
            val startX = 0f
            val endX = width.toFloat()

            paint.color = waveColor

            path.reset()

            // 移动到左下角
            path.moveTo(0f, centerY)

            for (x in startX.toInt()..endX.toInt()) {
                val radians = (x + waveOffset) * waveFrequency
                val yOffset = waveAmplitude * sin(radians.toDouble())
                val y = centerY + yOffset.toFloat() // +textHeight
                path.lineTo(x.toFloat(), y.toFloat())
            }

            // 从右到左，绘制波浪线的下半部分
            for (x in endX.toInt() downTo startX.toInt()) {
                val radians = (x + waveOffset) * waveFrequency
                val yOffset = waveAmplitude * sin(radians.toDouble())
                val y = centerY + yOffset.toFloat() + textHeight*0.4
                path.lineTo(x.toFloat(), y.toFloat())
            }

            path.close()

            canvas.drawPath(path, paint)

            waveOffset += waveSpeed
            invalidate()
        }*/
    }
