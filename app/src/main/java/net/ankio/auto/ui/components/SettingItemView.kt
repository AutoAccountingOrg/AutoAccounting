package net.ankio.auto.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.core.content.res.use
import net.ankio.auto.R
import net.ankio.auto.databinding.ViewSettingItemBinding
import net.ankio.auto.ui.theme.DynamicColors

class SettingItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var binding: ViewSettingItemBinding =
        ViewSettingItemBinding.inflate(LayoutInflater.from(context), this, true)

    init {


        // 获取自定义属性
        context.obtainStyledAttributes(attrs, R.styleable.SettingItemView).use {
                // 设置图标
                val iconRes = it.getResourceId(R.styleable.SettingItemView_settingIcon, 0)
                if (iconRes != 0) {
                    binding.settingIcon.setImageResource(iconRes)
                }

                // 设置标题
                binding.settingTitle.text = it.getString(R.styleable.SettingItemView_settingTitle)

                // 设置描述
                binding.settingDesc.text = it.getString(R.styleable.SettingItemView_settingDesc)

                binding.root.setCardBackgroundColor(DynamicColors.SurfaceColor1)
        }
    }

    /**
     * 重写 setOnClickListener，将点击监听器设置到 CardView 上
     * 这样 CardView 的涟漪效果可以正常工作
     */
    override fun setOnClickListener(l: OnClickListener?) {
        binding.root.setOnClickListener(l)
    }

    fun setTitle(title: String) {
        binding.settingTitle.text = title
    }

    fun setDesc(desc: String) {
        binding.settingDesc.text = desc
    }

    fun setIcon(@DrawableRes iconRes: Int) {
        binding.settingIcon.setImageResource(iconRes)
    }

}
