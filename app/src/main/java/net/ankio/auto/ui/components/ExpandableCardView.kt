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

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.google.android.material.card.MaterialCardView
import net.ankio.auto.ui.theme.DynamicColors
import com.google.android.material.materialswitch.MaterialSwitch
import net.ankio.auto.R

class ExpandableCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : MaterialCardView(context, attrs) {
    private var onCardClick: (() -> Unit)? = null
    fun setOnCardClickListener(listener: () -> Unit) {
        onCardClick = listener
    }

    private val header by lazy { findViewById<View>(R.id.header) }
    private val detail by lazy { findViewById<View>(R.id.detail) }
    val icon by lazy { findViewById<ImageView>(R.id.icon) }
    val titleView by lazy { findViewById<TextView>(R.id.title) }
    val descView by lazy { findViewById<TextView>(R.id.description) }
    val switch by lazy { findViewById<MaterialSwitch>(R.id.cardSwitch) }


    /** 折叠/展开状态 */
    var isExpanded: Boolean = false
        set(value) {
            field = value
            detail.visibility = if (value) View.VISIBLE else View.GONE
            isChecked = value
            // 切换背景和描边
            setCardBackgroundColor(if (value) DynamicColors.PrimaryContainer else DynamicColors.SurfaceContainer)
            strokeColor = if (value) DynamicColors.Primary else DynamicColors.Outline
            strokeWidth = 0
        }

    init {
        // inflate 布局
        inflate(context, R.layout.view_expandable_card, this)

        // 解析自定义属性
        attrs?.let {
            val ta = context.obtainStyledAttributes(it, R.styleable.ExpandableCardView)
            ta.getResourceId(R.styleable.ExpandableCardView_ecv_icon, 0)
                .takeIf { it != 0 }?.let(icon::setImageResource)
            ta.getString(R.styleable.ExpandableCardView_ecv_titleText)
                ?.let(titleView::setText)
            ta.getString(R.styleable.ExpandableCardView_ecv_descriptionText)
                ?.let(descView::setText)
            val def = ta.getBoolean(R.styleable.ExpandableCardView_ecv_expanded, false)
            val sw = ta.getBoolean(R.styleable.ExpandableCardView_ecv_switch, false)
            ta.recycle()
            isExpanded = def
            useSwitch(sw)
        }


        this.setOnClickListener {
            if (isExpanded) {
                onCardClick?.invoke()
                return@setOnClickListener
            }
            // 1) collapse 其它
            (parent as? ExpandableCardGroup)?.collapseAll()
            // 2) 流畅动画
            TransitionManager.beginDelayedTransition(
                parent as ViewGroup,
                AutoTransition().apply { duration = 200 }
            )
            // 3) 切换展开/折叠
            isExpanded = !isExpanded
        }
    }

    private lateinit var callback: (old: Int, new: Int) -> Unit
    fun setOnVisibilityChanged(fn: (old: Int, new: Int) -> Unit) {
        callback = fn
    }

    override fun setVisibility(visibility: Int) {
        val old = getVisibility()
        super.setVisibility(visibility)
        if (::callback.isInitialized) {
            callback(old, visibility)
        }
    }

    fun setTitle(title: String) {
        this.titleView.text = title
    }

    fun setDescription(desc: String) {
        this.descView.text = desc
    }

    fun setOnSwitchChanged(fn: CompoundButton.OnCheckedChangeListener) {
        this.switch.setOnCheckedChangeListener(fn)
    }

    fun useSwitch(boolean: Boolean) {
        this.switch.visibility = if (boolean) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }


    fun setSwitch(boolean: Boolean) {
        this.switch.isChecked = boolean
    }


    fun switch(): Boolean {
        return this.switch.isChecked
    }

    fun enableTint(boolean: Boolean) {
        if (boolean) {
            icon.imageTintList = ColorStateList.valueOf(DynamicColors.Primary)
        } else {
            icon.imageTintList = null
        }
    }



}