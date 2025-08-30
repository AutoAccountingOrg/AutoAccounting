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

package net.ankio.auto.ui.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.textview.MaterialTextView
import net.ankio.auto.R
import net.ankio.auto.databinding.AdapterTagListBinding
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import net.ankio.auto.utils.ThemeUtils
import net.ankio.auto.ui.utils.toThemeColor
import org.ezbook.server.db.model.TagModel

/**
 * 标签选择器适配器
 * 支持分组展示，可以处理标签项和分组标题项
 *
 * @param onTagClick 标签点击回调
 * @param isEditMode 是否编辑模式
 */
class TagSelectorAdapter(
    private val onTagClick: (TagModel, String) -> Unit,
    private var isEditMode: Boolean = false,

    ) : BaseAdapter<AdapterTagListBinding, TagModel>() {

    private val selectedTags: MutableList<TagModel> = mutableListOf()
    fun setSelectedTags(selectedTags: Set<TagModel>) {
        this.selectedTags.clear()
        this.selectedTags.addAll(selectedTags)
        notifyDataSetChanged()
    }

    companion object {
        // 默认标签颜色
        private const val DEFAULT_TAG_COLOR = "#E57373"

        // 分组标记常量
        private const val GROUP_MARKER = "-1"

        // 夜间模式颜色调整参数
        private const val DARK_MODE_SATURATION_FACTOR = 0.4f
        private const val DARK_MODE_BRIGHTNESS_FACTOR = 0.6f
        private const val DARK_MODE_MIN_SATURATION = 0.2f
        private const val DARK_MODE_MIN_BRIGHTNESS = 0.15f
        private const val DARK_MODE_MAX_BRIGHTNESS = 0.35f
        private const val DARK_MODE_TEXT_SATURATION_OFFSET = 0.2f
        private const val DARK_MODE_TEXT_BRIGHTNESS_OFFSET = 0.5f
        private const val DARK_MODE_TEXT_MIN_BRIGHTNESS = 0.6f
        private const val DARK_MODE_TEXT_MAX_BRIGHTNESS = 0.9f

        // 浅色模式颜色调整参数
        private const val LIGHT_MODE_SATURATION_FACTOR = 0.3f
        private const val LIGHT_MODE_BRIGHTNESS_OFFSET = 0.4f
        private const val LIGHT_MODE_MIN_SATURATION = 0.1f
        private const val LIGHT_MODE_MIN_BRIGHTNESS = 0.85f
        private const val LIGHT_MODE_MAX_BRIGHTNESS = 0.95f
        private const val LIGHT_MODE_TEXT_SATURATION_OFFSET = 0.3f
        private const val LIGHT_MODE_TEXT_BRIGHTNESS_OFFSET = -0.3f
        private const val LIGHT_MODE_TEXT_MIN_BRIGHTNESS = 0.2f
        private const val LIGHT_MODE_TEXT_MAX_BRIGHTNESS = 0.5f
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder<AdapterTagListBinding, TagModel>,
        data: TagModel,
        position: Int
    ) {
        if (data.group == GROUP_MARKER) {
            renderGroupTitle(holder, data.name)
        } else {
            renderTag(holder, data)
        }
    }

    /**
     * 渲染分组标题
     */
    private fun renderGroupTitle(
        holder: BaseViewHolder<AdapterTagListBinding, TagModel>,
        groupName: String
    ) {
        val binding = holder.binding

        // 隐藏标签面板，显示分组标题
        binding.chip.visibility = View.GONE
        binding.groupTitle.visibility = View.VISIBLE

        // 设置分组标题文本和样式
        binding.groupTitle.text = groupName

        // 设置根容器宽度为 match_parent
        val layoutParams = binding.root.layoutParams
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        binding.root.layoutParams = layoutParams
    }

    /**
     * 渲染标签面板
     */
    private fun renderTag(holder: BaseViewHolder<AdapterTagListBinding, TagModel>, tag: TagModel) {
        val binding = holder.binding

        // 隐藏分组标题，显示标签面板
        binding.groupTitle.visibility = View.GONE
        binding.chip.visibility = View.VISIBLE

        // 设置根容器宽度为 wrap_content
        val layoutParams = binding.root.layoutParams
        layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
        binding.root.layoutParams = layoutParams

        setChip(binding.chip, tag)
    }

    /**
     * 创建标签Chip
     */
    private fun setChip(chip: Chip, tag: TagModel) {
        chip.text = tag.name
        // 设置颜色 - 优化背景和前景色
        val baseColor = try {
            tag.color.toColorInt()
        } catch (e: IllegalArgumentException) {
            DEFAULT_TAG_COLOR.toColorInt()
        }

        // 根据主题模式计算背景色和前景色
        val isDarkMode = ThemeUtils.isDark
        val backgroundColor = getAdaptiveBackgroundColor(baseColor, isDarkMode)
        val textColor = getAdaptiveTextColor(baseColor, isDarkMode)

        chip.chipBackgroundColor = ColorStateList.valueOf(backgroundColor)
        chip.setTextColor(textColor)

        // 去掉边框，让chip看起来更简洁
        chip.chipStrokeWidth = 0f

        if (isEditMode) {
            // 编辑模式：显示删除按钮，长按编辑，点击不响应
            chip.isCheckable = false
            chip.isClickable = true
            chip.isCloseIconVisible = true

            // 设置删除图标的颜色，与文字颜色保持一致以确保可见性
            chip.closeIconTint = ColorStateList.valueOf(textColor)


        } else {
            // 选择模式：可选择，不显示删除按钮
            chip.isCheckable = true
            chip.isClickable = true
            chip.isCloseIconVisible = false

        }

        chip.isSelected = selectedTags.contains(tag)
    }

    /**
     * 获取适应主题的背景色
     * @param baseColor 原始颜色
     * @param isDarkMode 是否为夜间模式
     * @return 适应主题的背景色
     */
    private fun getAdaptiveBackgroundColor(baseColor: Int, isDarkMode: Boolean): Int {
        return adjustColor(baseColor, isDarkMode, isForText = false)
    }

    /**
     * 获取适应主题的文字色
     * @param baseColor 原始颜色
     * @param isDarkMode 是否为夜间模式
     * @return 适应主题的文字色
     */
    private fun getAdaptiveTextColor(baseColor: Int, isDarkMode: Boolean): Int {
        return adjustColor(baseColor, isDarkMode, isForText = true)
    }

    /**
     * 根据主题模式调整颜色
     * @param baseColor 原始颜色
     * @param isDarkMode 是否为夜间模式
     * @param isForText 是否为文字颜色（否则为背景色）
     * @return 调整后的颜色
     */
    private fun adjustColor(baseColor: Int, isDarkMode: Boolean, isForText: Boolean): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(baseColor, hsv)
        val adjustedHsv = hsv.clone()

        if (isDarkMode) {
            if (isForText) {
                // 夜间模式文字色：使用较亮的颜色
                adjustedHsv[1] = (hsv[1] + DARK_MODE_TEXT_SATURATION_OFFSET).coerceAtMost(1.0f)
                adjustedHsv[2] = (hsv[2] + DARK_MODE_TEXT_BRIGHTNESS_OFFSET)
                    .coerceIn(DARK_MODE_TEXT_MIN_BRIGHTNESS, DARK_MODE_TEXT_MAX_BRIGHTNESS)
            } else {
                // 夜间模式背景色：降低饱和度，使用较低的明度
                adjustedHsv[1] = (hsv[1] * DARK_MODE_SATURATION_FACTOR)
                    .coerceAtLeast(DARK_MODE_MIN_SATURATION)
                adjustedHsv[2] = (hsv[2] * DARK_MODE_BRIGHTNESS_FACTOR)
                    .coerceIn(DARK_MODE_MIN_BRIGHTNESS, DARK_MODE_MAX_BRIGHTNESS)
            }
        } else {
            if (isForText) {
                // 浅色模式文字色：使用较深的颜色
                adjustedHsv[1] = (hsv[1] + LIGHT_MODE_TEXT_SATURATION_OFFSET).coerceAtMost(1.0f)
                adjustedHsv[2] = (hsv[2] + LIGHT_MODE_TEXT_BRIGHTNESS_OFFSET)
                    .coerceIn(LIGHT_MODE_TEXT_MIN_BRIGHTNESS, LIGHT_MODE_TEXT_MAX_BRIGHTNESS)
            } else {
                // 浅色模式背景色：大幅降低饱和度，使用较高的明度
                adjustedHsv[1] = (hsv[1] * LIGHT_MODE_SATURATION_FACTOR)
                    .coerceAtLeast(LIGHT_MODE_MIN_SATURATION)
                adjustedHsv[2] = (hsv[2] + LIGHT_MODE_BRIGHTNESS_OFFSET)
                    .coerceIn(LIGHT_MODE_MIN_BRIGHTNESS, LIGHT_MODE_MAX_BRIGHTNESS)
            }
        }

        return Color.HSVToColor(adjustedHsv)
    }


    /**
     * 获取选中的标签（仅选择模式有效）
     * @return 选中的标签集合
     */
    fun getSelectedTags(): MutableList<TagModel> {
        if (isEditMode) return mutableListOf()
        return selectedTags
    }

    /**
     * 清空选择（仅选择模式有效）
     */
    fun clearSelection() {
        if (isEditMode) return
        selectedTags.clear()
        notifyDataSetChanged()
    }

    override fun areItemsSame(oldItem: TagModel, newItem: TagModel): Boolean {
        return oldItem.group == newItem.group && oldItem.name == newItem.name
    }

    override fun areContentsSame(oldItem: TagModel, newItem: TagModel): Boolean {
        return oldItem.group == newItem.group && oldItem.name == newItem.name
    }

    override fun onInitViewHolder(holder: BaseViewHolder<AdapterTagListBinding, TagModel>) {
        holder.binding.chip.setOnCloseIconClickListener {
            val tag = holder.item as TagModel
            onTagClick(tag, "delete")
        }

        holder.binding.chip.setOnClickListener {
            val tag = holder.item as TagModel
            onTagClick(tag, "edit")
        }

        holder.binding.chip.setOnCheckedChangeListener { _, isChecked ->
            val tag = holder.item as TagModel
            if (isChecked && !selectedTags.contains(tag)) {
                selectedTags.add(tag)
            } else if (!isChecked) {
                selectedTags.remove(tag)
            }
        }
    }

}
