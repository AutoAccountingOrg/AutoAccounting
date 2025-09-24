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

package net.ankio.auto.ui.components

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import net.ankio.auto.R
import net.ankio.auto.databinding.IconViewLayoutBinding

class IconView : ConstraintLayout {
    private val binding = IconViewLayoutBinding.inflate(LayoutInflater.from(context), this, true)
    private var color = Color.BLACK
    private var tintEnabled = true
    private var baseSpacing = 0f     // 基础间距（dp）

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        attrs?.let(::initAttributes)
    }

    fun imageView(): ImageView = binding.iconViewImage

    private fun initAttributes(attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.IconView).use { a ->
            tintEnabled = a.getBoolean(R.styleable.IconView_iconTintEnabled, true)
            color = a.getColor(R.styleable.IconView_textColor, Color.BLACK)

            a.getDrawable(R.styleable.IconView_iconSrc)?.let {
                binding.iconViewImage.setImageDrawable(it)
            }
            a.getString(R.styleable.IconView_text)?.let {
                binding.iconViewText.text = it
            }

            val textSizePx = a.getDimension(
                R.styleable.IconView_textSize,
                14f * resources.displayMetrics.scaledDensity
            )
            binding.iconViewText.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx)

            // 设置间距，默认为文字大小的0.5倍
            baseSpacing = a.getDimension(
                R.styleable.IconView_iconSpacing,
                textSizePx * 0.5f
            ) / resources.displayMetrics.density

            binding.iconViewText.maxLines = a.getInt(R.styleable.IconView_maxLines, 1)

            updateSizesAndSpacing()
            updateColors()
        }
    }

    fun setIcon(icon: Drawable?, tintEnabled: Boolean = true) {
        binding.iconViewImage.setImageDrawable(icon)
        this.tintEnabled = tintEnabled
        updateIconColor()
    }

    fun setTint(tint: Boolean) {
        tintEnabled = tint
        updateIconColor()
    }

    fun setText(text: CharSequence?) {
        binding.iconViewText.text = text
        binding.iconViewText.setTextColor(color)
    }

    fun getText(): String = binding.iconViewText.text.toString()

    fun setColor(col: Int, tintEnabled: Boolean = true) {
        color = col
        this.tintEnabled = tintEnabled
        updateColors()
    }

    fun setTextSize(size: Float) {
        require(size > 0) { "文字大小必须大于0" }
        binding.iconViewText.setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
        updateSizesAndSpacing()
    }

    /**
     * 设置图标和文字的间距
     */
    fun setIconSpacing(spacingDp: Float) {
        require(spacingDp >= 0) { "间距不能为负数" }
        baseSpacing = spacingDp
        updateSizesAndSpacing()
    }

    fun setMaxLines(maxLines: Int) {
        require(maxLines > 0) { "最大行数必须大于0" }
        binding.iconViewText.maxLines = maxLines
    }

    private fun updateColors() {
        binding.iconViewText.setTextColor(color)
        updateIconColor()
    }

    private fun updateIconColor() {
        if (tintEnabled) {
            binding.iconViewImage.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        } else {
            binding.iconViewImage.clearColorFilter()
        }
    }

    /**
     * 根据文字大小自动调整图标尺寸和间距
     * "好品味"：图标始终跟随文字，保持视觉一致性
     */
    private fun updateSizesAndSpacing() {
        // 图标高度始终等于文字高度
        val textSizePx = binding.iconViewText.textSize
        val iconSizePx = textSizePx.toInt()

        binding.iconViewImage.layoutParams.apply {
            width = iconSizePx
            height = iconSizePx
        }
        binding.iconViewImage.requestLayout()

        // 更新间距
        val scaledSpacingPx = (baseSpacing * resources.displayMetrics.density).toInt()
        binding.iconViewText.updateLayoutParams<ConstraintLayout.LayoutParams> {
            marginStart = scaledSpacingPx
        }
    }
}
