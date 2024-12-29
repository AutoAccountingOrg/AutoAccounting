package net.ankio.auto.ui.componets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import net.ankio.auto.R
import net.ankio.auto.databinding.ViewSettingItemBinding

class SettingItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {


    private var binding: ViewSettingItemBinding =
        ViewSettingItemBinding.inflate(LayoutInflater.from(context), this, true)

    init {

        // 获取自定义属性
        context.obtainStyledAttributes(attrs, R.styleable.SettingItemView).apply {

            try { // 设置图标
                val iconRes = getResourceId(R.styleable.SettingItemView_settingIcon, 0)
                if (iconRes != 0) {
                    binding.settingIcon.setImageResource(iconRes)
                }

                // 设置标题
                binding.settingTitle.text = getString(R.styleable.SettingItemView_settingTitle)

                // 设置描述
                binding.settingDesc.text = getString(R.styleable.SettingItemView_settingDesc)

            } finally {
                recycle()
            }
        }
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