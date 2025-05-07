package net.ankio.auto.ui.components

import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import net.ankio.auto.R

class ExpandableCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : MaterialCardView(context, attrs) {

    private val header by lazy { findViewById<View>(R.id.header) }
    private val detail by lazy { findViewById<View>(R.id.detail) }
    private val icon by lazy { findViewById<ImageView>(R.id.icon) }
    private val titleView by lazy { findViewById<TextView>(R.id.title) }
    private val descView by lazy { findViewById<TextView>(R.id.description) }

    // 主题色
    private val colorSurfaceVariant =
        MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceVariant)
    private val colorOutline =
        MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutline)
    private val colorPrimaryContainer =
        MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimaryContainer)
    private val colorPrimary =
        MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary)

    /** 折叠/展开状态 */
    var isExpanded: Boolean = false
        set(value) {
            field = value
            detail.visibility = if (value) View.VISIBLE else View.GONE
            isChecked = value
            // 切换背景和描边
            setCardBackgroundColor(if (value) colorPrimaryContainer else colorSurfaceVariant)
            strokeColor = if (value) colorPrimary else colorOutline
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
            ta.recycle()
            isExpanded = def
        }

        // 点击 header 时：先收起同级，再做平滑过渡，再切换自己
        rootView.setOnClickListener {
            (parent as? ExpandableCardGroup)?.collapseAll()
            // 平滑过渡
            TransitionManager.beginDelayedTransition(
                parent as ViewGroup,
                AutoTransition().apply { duration = 200 }
            )
            isExpanded = !isExpanded
        }
    }
}
