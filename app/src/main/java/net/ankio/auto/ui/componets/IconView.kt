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
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import net.ankio.auto.R
import net.ankio.auto.databinding.IconViewLayoutBinding

class IconView : ConstraintLayout {
    private val binding: IconViewLayoutBinding
    private var color: Int = Color.BLACK

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        // 使用 ViewBinding 加载布局
        binding = IconViewLayoutBinding.inflate(LayoutInflater.from(context), this, true)

        attrs?.let { initAttributes(it) }
    }

    fun getSpFromPx(px: Float, context: Context): Float {
        val scaledDensity = context.resources.displayMetrics.scaledDensity
        return px / scaledDensity
    }

    private fun initAttributes(attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.IconView).apply {
            try {
                val iconTintEnabled = getBoolean(R.styleable.IconView_iconTintEnabled, true)
                getDrawable(R.styleable.IconView_iconSrc)?.let { icon ->
                    setIcon(icon, iconTintEnabled)
                }
                getString(R.styleable.IconView_text)?.let { text ->
                    setText(text)
                }
                val textSizePx = getDimension(R.styleable.IconView_textSize, 14f.spToPx())
                binding.iconViewText.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx)
                val iconSizePx = getDimensionPixelSize(R.styleable.IconView_iconSize, 24.dpToPx())
                val iconSizeDp = (iconSizePx / resources.displayMetrics.density).toInt()
                setIconSize(iconSizeDp)
                setColor(getColor(R.styleable.IconView_textColor, Color.BLACK), iconTintEnabled)

                val maxLines = getInt(R.styleable.IconView_maxLines, 1)  // 默认值为 1
                setMaxLines(maxLines)

            } finally {
                recycle()
            }
        }
    }

    fun setIcon(icon: Drawable?, tintEnabled: Boolean = true) {
        binding.iconViewImage.setImageDrawable(icon)
        setImageColorFilter(color, tintEnabled)
    }

    fun setText(text: CharSequence?) {

        binding.iconViewText.apply {
            setTextColor(color)
            setText(text)
        }
    }

    fun getText(): String = binding.iconViewText.text.toString()

    private fun setImageColorFilter(color: Int, tintEnabled: Boolean = true) {
        when {
            !tintEnabled -> binding.iconViewImage.clearColorFilter()
            else -> binding.iconViewImage.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        }
    }

    fun setColor(col: Int, tintEnabled: Boolean = true) {
        color = col
        binding.apply {
            iconViewText.setTextColor(col)
            setImageColorFilter(col, tintEnabled)
        }
    }

    /**
     * 设置文字大小
     * @param size 文字大小，单位 sp
     */
    fun setTextSize(size: Float) {
        // 使用 TypedValue.COMPLEX_UNIT_SP 明确指定单位为 SP
        binding.iconViewText.setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
    }

    /**
     * 设置图标大小
     * @param sizeDp 图标大小，单位 dp
     */
    fun setIconSize(sizeDp: Int) {
        // 将 dp 转换为 px
        val sizePx = (sizeDp * resources.displayMetrics.density).toInt()
        binding.iconViewImage.layoutParams = binding.iconViewImage.layoutParams.apply {
            width = sizePx
            height = sizePx
        }
        binding.iconViewImage.requestLayout()
    }

    // 可选：添加工具方法
    private fun Float.spToPx(): Float {
        return this * resources.displayMetrics.scaledDensity
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    /**
     * 设置文本的最大行数
     * @param maxLines 最大行数
     */
    fun setMaxLines(maxLines: Int) {
        binding.iconViewText.maxLines = maxLines
    }
}
