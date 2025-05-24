package net.ankio.auto.ui.components

import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.switchmaterial.SwitchMaterial
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


}