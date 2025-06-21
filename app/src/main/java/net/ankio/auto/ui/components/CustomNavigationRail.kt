package net.ankio.auto.ui.components

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.setPadding
import net.ankio.auto.App
import net.ankio.auto.R
import net.ankio.auto.ui.models.RailMenuItem
import androidx.core.view.isEmpty

class CustomNavigationRail @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val scrollView = ScrollView(context)
    private val internalLayout = LinearLayout(context)
    private val menuItems = mutableListOf<RailMenuItem>()
    private var onItemSelectedListener: ((RailMenuItem) -> Unit)? = null
    private var selectedView: View? = null

    init {
        orientation = VERTICAL
        scrollView.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        internalLayout.orientation = VERTICAL
        internalLayout.gravity = Gravity.CENTER_HORIZONTAL
        internalLayout.setPadding(dp(8))
        scrollView.addView(internalLayout)
        addView(scrollView)
    }

    fun clear() {
        menuItems.clear()                 // 清空数据
        internalLayout.removeAllViews()    // 清空所有 item 视图
        selectedView = null                // 清空选中项
    }


    fun addMenuItem(item: RailMenuItem) {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.custom_navigation_rail_item, internalLayout, false)
        val iconView = view.findViewById<AppCompatImageView>(R.id.iconView)
        val textView = view.findViewById<TextView>(R.id.textView)

        iconView.setImageDrawable(item.icon)
        textView.text = item.text

        val defaultColor = App.getThemeAttrColor(com.google.android.material.R.attr.colorOnSurface)
        val selectedColor =
            App.getThemeAttrColor(com.google.android.material.R.attr.colorOnSecondaryContainer)

        // iconView.setColorFilter(defaultColor)
        textView.setTextColor(defaultColor)

        view.setOnClickListener {
            // 清除之前选中项的状态
            selectedView?.apply {
                setBackgroundColor(Color.TRANSPARENT)

                findViewById<TextView>(R.id.textView)?.apply {
                    setTextColor(defaultColor)
                    setTypeface(null, Typeface.NORMAL) // 取消加粗
                }
                // 可选：图标颜色恢复
                // findViewById<ImageView>(R.id.iconView)?.setColorFilter(defaultColor)
            }

            // 设置当前选中项样式
            view.setBackgroundResource(R.drawable.rail_selected_bg)

            view.findViewById<TextView>(R.id.textView)?.apply {
                setTextColor(selectedColor)
                setTypeface(null, Typeface.BOLD) // 加粗
            }

            // 可选：图标颜色高亮
            // view.findViewById<ImageView>(R.id.iconView)?.setColorFilter(selectedColor)

            selectedView = view
            onItemSelectedListener?.invoke(item)
        }

        menuItems.add(item)
        internalLayout.addView(view)
    }

    fun setOnItemSelectedListener(listener: (RailMenuItem) -> Unit) {
        onItemSelectedListener = listener
    }


    fun performFirstItem(): Boolean {
        if (menuItems.isEmpty() || internalLayout.isEmpty()) return false
        internalLayout.getChildAt(0)?.performClick()
        return true
    }

    private fun dp(px: Int): Int = (px * resources.displayMetrics.density).toInt()
}
