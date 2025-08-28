/*
 * Copyright (C) 2024 ankio(ankio@ankio.net)
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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.ComposeShader
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import net.ankio.auto.R

/**
 * 渐变图片视图组件
 * 支持圆角和渐变效果的自定义ImageView
 */
class GradientImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    // 绘制相关组件
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val matrix = Matrix()
    private val clipPath = Path()
    private val rectF = RectF()

    // 缓存的着色器和位图
    private var cachedShader: ComposeShader? = null
    private var cachedBitmap: Bitmap? = null
    private var lastDrawableRef: Drawable? = null

    // 圆角半径数组 [TL, TL, TR, TR, BR, BR, BL, BL]
    private val cornerRadii = FloatArray(8)

    init {
        // 解析自定义属性
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.GradientImageView,
            defStyleAttr, 0
        ).apply {
            try {
                // 优先使用统一圆角，如果没有则使用独立圆角
                val uniformRadius = getDimension(R.styleable.GradientImageView_cornerRadius, 0f)
                if (uniformRadius > 0f) {
                    setCornerRadius(uniformRadius)
                } else {
                    setCornerRadius(
                        getDimension(R.styleable.GradientImageView_cornerRadiusTopLeft, 0f),
                        getDimension(R.styleable.GradientImageView_cornerRadiusTopRight, 0f),
                        getDimension(R.styleable.GradientImageView_cornerRadiusBottomRight, 0f),
                        getDimension(R.styleable.GradientImageView_cornerRadiusBottomLeft, 0f)
                    )
                }
            } finally {
                recycle()
            }
        }
    }

    /**
     * 设置统一圆角半径
     */
    private fun setCornerRadius(radius: Float) {
        repeat(4) { i ->
            cornerRadii[i * 2] = radius
            cornerRadii[i * 2 + 1] = radius
        }
    }

    /**
     * 设置独立圆角半径
     */
    private fun setCornerRadius(
        topLeft: Float,
        topRight: Float,
        bottomRight: Float,
        bottomLeft: Float
    ) {
        cornerRadii[0] = topLeft; cornerRadii[1] = topLeft
        cornerRadii[2] = topRight; cornerRadii[3] = topRight
        cornerRadii[4] = bottomRight; cornerRadii[5] = bottomRight
        cornerRadii[6] = bottomLeft; cornerRadii[7] = bottomLeft
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != oldw || h != oldh) {
            invalidateCache()
        }
    }

    override fun onDraw(canvas: Canvas) {
        val drawable = drawable ?: return

        // 智能缓存：仅在需要时重建
        if (shouldRebuildCache(drawable)) {
            rebuildCache(drawable)
        }

        cachedShader?.let { shader ->
            paint.shader = shader

            // 设置剪切路径
            rectF.set(0f, 0f, width.toFloat(), height.toFloat())
            clipPath.reset()
            clipPath.addRoundRect(rectF, cornerRadii, Path.Direction.CW)

            canvas.save()
            canvas.clipPath(clipPath)
            canvas.drawRect(rectF, paint)
            canvas.restore()
        }
    }

    /**
     * 判断是否需要重建缓存
     */
    private fun shouldRebuildCache(drawable: Drawable): Boolean {
        return cachedShader == null ||
                cachedBitmap == null ||
                lastDrawableRef != drawable ||
                width <= 0 || height <= 0
    }

    /**
     * 重建着色器缓存
     */
    private fun rebuildCache(drawable: Drawable) {
        if (width <= 0 || height <= 0) return

        // 获取位图
        val bitmap = getBitmapFromDrawable(drawable)
        if (bitmap.isRecycled) return

        // 创建位图着色器
        val bitmapShader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

        // 计算缩放矩阵
        val scaleX = width.toFloat() / bitmap.width
        val scaleY = height.toFloat() / bitmap.height
        matrix.setScale(scaleX, scaleY)
        bitmapShader.setLocalMatrix(matrix)

        // 创建渐变着色器
        val gradientShader = LinearGradient(
            0f, 0f,
            0f, height.toFloat(),
            0xFFFFFFFF.toInt(),
            0x00FFFFFF,
            Shader.TileMode.CLAMP
        )

        // 组合着色器
        cachedShader = ComposeShader(bitmapShader, gradientShader, PorterDuff.Mode.DST_IN)
        cachedBitmap = bitmap
        lastDrawableRef = drawable
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        invalidateCache()
    }

    /**
     * 高效的 Drawable 到 Bitmap 转换
     */
    private fun getBitmapFromDrawable(drawable: Drawable): Bitmap {
        // 直接使用 BitmapDrawable 的 bitmap，避免不必要的转换
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }

        // 处理特殊情况：无有效尺寸的 drawable
        val drawableWidth = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1
        val drawableHeight = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1

        val bitmap = Bitmap.createBitmap(drawableWidth, drawableHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }

    /**
     * 清理缓存
     */
    private fun invalidateCache() {
        cachedShader = null
        lastDrawableRef = null
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        invalidateCache()
        // 清理位图资源
        cachedBitmap?.let {
            if (!it.isRecycled && it != (drawable as? BitmapDrawable)?.bitmap) {
                it.recycle()
            }
        }
        cachedBitmap = null
    }
}
