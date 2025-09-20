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

package net.ankio.auto.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import net.ankio.auto.R
import net.ankio.auto.utils.BillTool
import org.ezbook.server.constant.BillType
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/**
 * 极简折线图视图（无外部库）。
 * - 输入为两个序列（收入、支出）；
 * - 自适应缩放到视图高度；
 * - 智能绘制坐标轴，支持数量缩写显示。
 */
class LineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /** 标签序列（横坐标显示） */
    private var labelSeries: List<String> = emptyList()

    /** 收入序列（按时间升序） */
    private var incomeSeries: List<Double> = emptyList()

    /** 支出序列（按时间升序） */
    private var expenseSeries: List<Double> = emptyList()

    private val incomePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density * 3
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val expensePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density * 3
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.gray40)
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density * 0.5f
        pathEffect = DashPathEffect(floatArrayOf(5f, 5f), 0f)
    }

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.textSecondary)
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density * 1f
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.textSecondary)
        textSize = resources.displayMetrics.scaledDensity * 10
        textAlign = Paint.Align.CENTER
    }

    private val labelSpacing = (8 * resources.displayMetrics.density).toInt()

    /**
     * 设置数据序列
     */
    fun setData(labels: List<String>, income: List<Double>, expense: List<Double>) {
        labelSeries = labels
        incomeSeries = income
        expenseSeries = expense
        updateColors()
        invalidate()
    }

    /**
     * 根据用户偏好更新颜色
     */
    private fun updateColors() {
        incomePaint.color = ContextCompat.getColor(context, BillTool.getColor(BillType.Income))
        expensePaint.color = ContextCompat.getColor(context, BillTool.getColor(BillType.Expend))
    }

    /**
     * 格式化数值为缩写形式（10k, 1.5M等）
     */
    private fun formatNumber(value: Double): String {
        return when {
            value >= 1_000_000_000 -> String.format("%.1fG", value / 1_000_000_000)
            value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000)
            value >= 1_000 -> String.format("%.1fk", value / 1_000)
            value >= 1 -> String.format("%.0f", value)
            else -> String.format("%.2f", value)
        }.replace(".0", "")
    }

    /**
     * 计算智能刻度值
     */
    private fun calculateTicks(maxValue: Double, tickCount: Int = 5): List<Double> {
        if (maxValue <= 0) return listOf(0.0)

        val rawStep = maxValue / (tickCount - 1)
        val magnitude = 10.0.pow(floor(log10(rawStep)))
        val normalizedStep = rawStep / magnitude

        val niceStep = when {
            normalizedStep <= 1.0 -> magnitude
            normalizedStep <= 2.0 -> 2 * magnitude
            normalizedStep <= 5.0 -> 5 * magnitude
            else -> 10 * magnitude
        }

        val ticks = mutableListOf<Double>()
        var tick = 0.0
        while (tick <= maxValue && ticks.size < tickCount) {
            ticks.add(tick)
            tick += niceStep
        }

        return ticks
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 计算绘制区域，为坐标轴标签留出最小空间
        val labelHeight = labelPaint.textSize.toInt() + labelSpacing
        val chartLeft = labelSpacing * 4  // 为纵轴标签留最小空间
        val chartTop = labelSpacing
        val chartRight = width - labelSpacing
        val chartBottom = height - labelHeight  // 为横轴标签留空间

        val w = chartRight - chartLeft
        val h = chartBottom - chartTop
        if (w <= 0 || h <= 0) return

        val maxPoints = maxOf(incomeSeries.size, expenseSeries.size, labelSeries.size)
        if (maxPoints < 2) return

        val maxVal = maxOf(
            (incomeSeries.maxOrNull() ?: 0.0),
            (expenseSeries.maxOrNull() ?: 0.0)
        ).coerceAtLeast(1e-6)

        // 绘制坐标轴
        drawAxes(canvas, chartLeft, chartTop, chartRight, chartBottom, maxVal)

        // 绘制网格线
        drawGrid(canvas, chartLeft, chartTop, w, h, maxVal)

        fun buildPath(series: List<Double>): Path {
            val path = Path()
            val size = series.size
            for (i in 0 until size) {
                val x = chartLeft + (w * (i.toFloat() / (size - 1).coerceAtLeast(1))).toInt()
                val yRatio = (series[i] / maxVal).toFloat()
                val y = chartTop + (h * (1f - yRatio))
                if (i == 0) path.moveTo(x.toFloat(), y) else path.lineTo(x.toFloat(), y)
            }
            return path
        }

        // 绘制折线
        if (incomeSeries.isNotEmpty()) {
            canvas.drawPath(buildPath(incomeSeries), incomePaint)
        }
        if (expenseSeries.isNotEmpty()) {
            canvas.drawPath(buildPath(expenseSeries), expensePaint)
        }
    }

    /**
     * 绘制坐标轴
     */
    private fun drawAxes(
        canvas: Canvas,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        maxVal: Double
    ) {
        // 绘制X轴
        canvas.drawLine(
            left.toFloat(),
            bottom.toFloat(),
            right.toFloat(),
            bottom.toFloat(),
            axisPaint
        )

        // 绘制Y轴
        canvas.drawLine(left.toFloat(), top.toFloat(), left.toFloat(), bottom.toFloat(), axisPaint)

        // 绘制Y轴标签（纵坐标）
        val yTicks = calculateTicks(maxVal, 5)
        val h = bottom - top
        for (tick in yTicks) {
            val yRatio = (tick / maxVal).toFloat()
            val y = top + (h * (1f - yRatio))

            // 绘制刻度线
            canvas.drawLine((left - labelSpacing).toFloat(), y, left.toFloat(), y, axisPaint)

            // 绘制标签
            labelPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(
                formatNumber(tick),
                (left - labelSpacing * 2).toFloat(),
                y + labelPaint.textSize / 3,
                labelPaint
            )
        }

        // 绘制X轴标签（横坐标）- 分离刻度线和标签显示
        if (labelSeries.isNotEmpty()) {
            val w = right - left
            val labelCount = labelSeries.size

            // 1. 绘制所有刻度线
            for (i in labelSeries.indices) {
                val x = left + (w * i.toFloat() / (labelCount - 1).coerceAtLeast(1))
                canvas.drawLine(
                    x,
                    bottom.toFloat(),
                    x,
                    (bottom + labelSpacing).toFloat(),
                    axisPaint
                )
            }

            // 2. 显示标签，较小字体减少重叠
            val maxLabels = minOf(labelCount, 10) // 最多10个标签，10sp字体下通常不会重叠
            val labelStep = if (labelCount <= maxLabels) 1 else labelCount / maxLabels

            for (i in labelSeries.indices step labelStep) {
                val x = left + (w * i.toFloat() / (labelCount - 1).coerceAtLeast(1))

                // 绘制标签
                labelPaint.textAlign = Paint.Align.CENTER
                canvas.drawText(
                    labelSeries[i],
                    x,
                    (bottom + labelPaint.textSize + labelSpacing).toFloat(),
                    labelPaint
                )
            }
        }
    }

    /**
     * 绘制网格线
     */
    private fun drawGrid(canvas: Canvas, left: Int, top: Int, w: Int, h: Int, maxVal: Double) {
        // 绘制水平网格线（基于Y轴刻度）
        val yTicks = calculateTicks(maxVal, 5)
        for (tick in yTicks.drop(1)) { // 跳过0刻度线
            val yRatio = (tick / maxVal).toFloat()
            val y = top + (h * (1f - yRatio))
            canvas.drawLine(left.toFloat(), y, (left + w).toFloat(), y, gridPaint)
        }

        // 绘制垂直网格线，基于数据点位置（跳过首尾避免与边框重叠）
        if (labelSeries.isNotEmpty()) {
            val labelCount = labelSeries.size

            for (i in 1 until labelCount - 1) { // 跳过首尾
                val x = left + (w * i.toFloat() / (labelCount - 1))
                canvas.drawLine(x, top.toFloat(), x, (top + h).toFloat(), gridPaint)
            }
        }
    }
}


