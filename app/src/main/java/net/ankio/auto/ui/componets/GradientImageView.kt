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

package net.ankio.auto.ui.componets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.ComposeShader
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import android.graphics.Path
import android.graphics.RectF
import net.ankio.auto.R

class GradientImageView : AppCompatImageView {
    constructor(context: Context?) : super(context!!)

    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs) {
        context.theme?.obtainStyledAttributes(
            attrs,
            R.styleable.GradientImageView,
            0, 0
        )?.apply {
            try {
                cornerRadiusTopLeft = getDimension(R.styleable.GradientImageView_cornerRadiusTopLeft, 0f)
                cornerRadiusTopRight = getDimension(R.styleable.GradientImageView_cornerRadiusTopRight, 0f)
                cornerRadiusBottomRight = getDimension(R.styleable.GradientImageView_cornerRadiusBottomRight, 0f)
                cornerRadiusBottomLeft = getDimension(R.styleable.GradientImageView_cornerRadiusBottomLeft, 0f)
            } finally {
                recycle()
            }
        }
    }
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context!!,
        attrs,
        defStyleAttr,
    )

    private val paint = Paint()

    private var gradient: LinearGradient? = null

    private val matrix = Matrix()

    private var shader: BitmapShader? = null
    private var composeShader: ComposeShader? = null

    private var cornerRadiusTopLeft: Float = 0f
    private var cornerRadiusTopRight: Float = 0f
    private var cornerRadiusBottomRight: Float = 0f
    private var cornerRadiusBottomLeft: Float = 0f

    override fun onDraw(canvas: Canvas) {
        val bitmap = drawableToBitmap(drawable)
        if (shader == null) {
            shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }

        val scaleX = width.toFloat() / bitmap.width
        val scaleY = height.toFloat() / bitmap.height
        matrix.setScale(scaleX, scaleY)
        shader!!.setLocalMatrix(matrix)

        if (gradient == null) {
            gradient =
                LinearGradient(
                    0f,
                    height * 0f,
                    0f,
                    height.toFloat(),
                    0xFFFFFFFF.toInt(),
                    0x00FFFFFF,
                    Shader.TileMode.CLAMP,
                )
        }
        if (composeShader == null) {
            composeShader = ComposeShader(shader!!, gradient!!, PorterDuff.Mode.DST_IN)
        }

        paint.shader = composeShader

        // Use Path and Canvas to draw rounded corners
        val path = Path()
        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        val radii = floatArrayOf(
            cornerRadiusTopLeft, cornerRadiusTopLeft,
            cornerRadiusTopRight, cornerRadiusTopRight,
            cornerRadiusBottomRight, cornerRadiusBottomRight,
            cornerRadiusBottomLeft, cornerRadiusBottomLeft
        )
        path.addRoundRect(rect, radii, Path.Direction.CW)
        canvas.clipPath(path)

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        shader = null
        composeShader = null
        invalidate()
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }

        val bitmap =
            Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888,
            )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }
}
