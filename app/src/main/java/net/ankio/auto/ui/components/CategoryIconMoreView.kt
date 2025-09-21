package net.ankio.auto.ui.components

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.widget.ImageViewCompat
import androidx.core.graphics.drawable.DrawableCompat
import net.ankio.auto.R
import net.ankio.auto.databinding.ComponentCategoryIconMoreBinding
import net.ankio.auto.ui.theme.DynamicColors
import net.ankio.auto.ui.utils.setCategoryIcon
import org.ezbook.server.db.model.CategoryModel
import androidx.core.view.isVisible

/**
 * 分类图标 + 右下角 more 小图标
 * - 通过自定义属性或主题控制主/副背景色
 * - 封装为可复用组件，避免 include 方式的样式分散
 */
class CategoryIconMoreView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: ComponentCategoryIconMoreBinding =
        ComponentCategoryIconMoreBinding.inflate(LayoutInflater.from(context), this, true)

    @ColorInt
    private var primaryColor: Int = DynamicColors.Primary
    @ColorInt
    private var onPrimaryColor: Int = DynamicColors.OnPrimary
    @ColorInt
    private var secondaryColor: Int = DynamicColors.Secondary
    @ColorInt
    private var onSecondaryColor: Int = DynamicColors.OnSecondary

    init {
        parseAttributes(attrs)
        applyColors()
    }

    private fun parseAttributes(attrs: AttributeSet?) {
        if (attrs == null) return
        val ta = context.obtainStyledAttributes(attrs, R.styleable.CategoryIconMoreView)
        try {
            val iconRes =
                ta.getResourceId(R.styleable.CategoryIconMoreView_cim_icon, R.drawable.default_cate)
            val moreIconRes =
                ta.getResourceId(R.styleable.CategoryIconMoreView_cim_moreIcon, R.drawable.ic_more2)

            setIcon(iconRes)
            // 直接通过 ViewBinding 设置 more 图标，避免冗余方法
            binding.ivMore.setImageResource(moreIconRes)

            if (ta.hasValue(R.styleable.CategoryIconMoreView_cim_primaryColor)) {
                primaryColor =
                    ta.getColor(R.styleable.CategoryIconMoreView_cim_primaryColor, primaryColor)
            }
            if (ta.hasValue(R.styleable.CategoryIconMoreView_cim_secondaryColor)) {
                secondaryColor =
                    ta.getColor(R.styleable.CategoryIconMoreView_cim_secondaryColor, secondaryColor)
            }
        } finally {
            ta.recycle()
        }
    }

    private fun applyColors() {
        // 背景：主图标与 more 使用同一背景色
        tintBackground(binding.itemImageIcon, primaryColor)
        tintBackground(binding.ivMore, primaryColor)

        // 前景：主图标与 more 都应用前景色（可由外部传入）
        ImageViewCompat.setImageTintList(
            binding.itemImageIcon,
            ColorStateList.valueOf(onPrimaryColor)
        )
        ImageViewCompat.setImageTintList(binding.ivMore, ColorStateList.valueOf(onSecondaryColor))
    }

    private fun tintBackground(target: View, @ColorInt color: Int) {
        val bg = target.background?.mutate() ?: return
        DrawableCompat.setTint(bg, color)
        target.background = bg
    }

    fun setIcon(@DrawableRes resId: Int) {
        binding.itemImageIcon.setImageResource(resId)
    }

    fun hideMore() {
        binding.ivMore.visibility = View.GONE
    }

    fun showMore() {
        binding.ivMore.visibility = View.VISIBLE
    }

    /**
     * 设置分类主图标（基于分类模型）
     */
    fun setCategoryIcon(categoryModel: CategoryModel) {
        binding.itemImageIcon.setCategoryIcon(categoryModel)
    }

    /**
     * 指示右下角 more 是否可见
     */
    fun isMoreVisible(): Boolean {
        return binding.ivMore.isVisible
    }

    // 不再提供 setActive，颜色应由外部传入统一控制

    /**
     * 暴露内部主图标视图，便于外部过渡/动画保持兼容
     */
    fun getIconView(): ImageView = binding.itemImageIcon

    /**
     * 外部设置：背景色与前景色
     * - 背景色：同时应用于主图标背景与 more 背景
     * - 前景色：同时应用于主图标与 more 的前景（矢量）
     */
    fun setColor(@ColorInt background: Int, @ColorInt foreground: Int) {
        primaryColor = background
        secondaryColor = background
        onPrimaryColor = foreground
        onSecondaryColor = foreground
        applyColors()
    }
}


