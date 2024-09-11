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
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import net.ankio.auto.R

class IconView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {
    private var imageView: ImageView
    private var textView: TextView
    private var color: Int = 0

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.icon_view_layout, this, true)

        imageView = findViewById(R.id.icon_view_image)
        textView = findViewById(R.id.icon_view_text)

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.IconView,
            0,
            0,
        ).apply {
            try {
                val iconTintEnabled = getBoolean(R.styleable.IconView_iconTintEnabled, true)
                val icon = getDrawable(R.styleable.IconView_iconSrc)
                val text = getString(R.styleable.IconView_text)

                val textSize = getDimension(R.styleable.IconView_textSize, 14f)
                val iconSize = getDimensionPixelSize(R.styleable.IconView_iconSize, 32) * 2

                setTextSize(textSize)
                setIconSize(iconSize)
                setColor(getColor(R.styleable.IconView_textColor, Color.BLACK))

                setIcon(icon, iconTintEnabled)
                setText(text)
            } finally {
                recycle()
            }
        }
    }

    fun setIcon(
        icon: Drawable?,
        tintEnabled: Boolean = false,
    ) {
        imageView.setImageDrawable(icon)
        if (!tintEnabled) {
            imageView.clearColorFilter()
        } else {
            imageView.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        }
    }

    fun setText(text: CharSequence?) {
        textView.setTextColor(color)
        textView.text = text
    }

    fun getText(): String {
        return textView.text.toString()
    }

    fun setColor(col: Int) {
        color = col
    }

    fun setTextSize(size: Float) {
        textView.textSize = size
    }

    fun setIconSize(size: Int) {
        imageView.layoutParams.width = size
        imageView.layoutParams.height = size
        imageView.requestLayout()
    }
}
