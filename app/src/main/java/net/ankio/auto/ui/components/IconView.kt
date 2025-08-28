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

    /**
     * 获取内部ImageView引用
     * "最佳实践"：暴露ImageView让调用方自行处理业务逻辑
     * 保持组件独立性，不与业务耦合
     */
    fun imageView(): ImageView = binding.iconViewImage

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

    /**
     * 设置图标着色状态
     * @param tint 是否启用着色
     */
    fun setTint(tint: Boolean) {
        setImageColorFilter(color, tint)
    }

    /**
     * 设置文字内容
     * @param text 文字内容，可为null
     */
    fun setText(text: CharSequence?) {
        binding.iconViewText.text = text
        binding.iconViewText.setTextColor(color)
    }

    /**
     * 获取文字内容
     * @return 当前显示的文字
     */
    fun getText(): String = binding.iconViewText.text.toString()

    /**
     * 设置图标颜色过滤器
     * "好品味"：消除不必要的when语句，直接用if
     */
    private fun setImageColorFilter(color: Int, tintEnabled: Boolean = true) {
        if (tintEnabled) {
            binding.iconViewImage.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        } else {
            binding.iconViewImage.clearColorFilter()
        }
    }

    /**
     * 设置组件颜色（包括文字和图标）
     * @param col 颜色值
     * @param tintEnabled 是否对图标启用着色
     */
    fun setColor(col: Int, tintEnabled: Boolean = true) {
        color = col
        binding.iconViewText.setTextColor(col)
        setImageColorFilter(col, tintEnabled)
    }

    /**
     * 设置文字大小
     * @param size 文字大小，单位 sp，必须大于0
     */
    fun setTextSize(size: Float) {
        require(size > 0) { "文字大小必须大于0" }
        binding.iconViewText.setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
    }

    /**
     * 设置图标大小
     * @param sizeDp 图标大小，单位 dp
     */
    fun setIconSize(sizeDp: Int) {
        require(sizeDp > 0) { "图标大小必须大于0" }
        val sizePx = sizeDp.dpToPx()
        binding.iconViewImage.layoutParams = binding.iconViewImage.layoutParams.apply {
            width = sizePx
            height = sizePx
        }
        binding.iconViewImage.requestLayout()
    }

    /**
     * 统一的单位转换工具方法
     * "好品味"：集中单位转换逻辑，消除重复
     */
    private fun Float.spToPx(): Float = this * resources.displayMetrics.scaledDensity

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    /**
     * 设置文本的最大行数
     * @param maxLines 最大行数，必须大于0
     */
    fun setMaxLines(maxLines: Int) {
        require(maxLines > 0) { "最大行数必须大于0" }
        binding.iconViewText.maxLines = maxLines
    }
}
