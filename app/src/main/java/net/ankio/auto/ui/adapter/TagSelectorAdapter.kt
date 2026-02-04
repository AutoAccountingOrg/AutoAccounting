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
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors
import com.google.android.material.textview.MaterialTextView
import androidx.core.graphics.ColorUtils
import net.ankio.auto.R
import net.ankio.auto.databinding.AdapterTagListBinding
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import org.ezbook.server.db.model.TagModel
import net.ankio.auto.ui.utils.PaletteManager

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
    private val selectionLimit: Int = 0,
    private val onSelectionChanged: ((List<TagModel>) -> Unit)? = null,
    private val onSelectionLimitReached: ((Int) -> Unit)? = null,

    ) : BaseAdapter<AdapterTagListBinding, TagModel>() {

    /**
     * 已选中的标签集合（仅选择模式使用）
     */
    private val selectedTags: MutableList<TagModel> = mutableListOf()

    /**
     * 内部选择状态更新标记，用于避免触发递归回调
     */
    private var isInternalSelectionUpdate = false

    /**
     * 设置选中的标签集合（仅选择模式使用）
     * @param selectedTags 选中的标签集合
     */
    fun setSelectedTags(selectedTags: Set<TagModel>) {
        this.selectedTags.clear()
        this.selectedTags.addAll(
            if (selectionLimit > 0) selectedTags.take(selectionLimit) else selectedTags
        )
        notifyDataSetChanged()
    }

    companion object {
        // 分组标记常量
        private const val GROUP_MARKER = "-1"
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
     * 创建标签 Chip
     */
    private fun setChip(chip: Chip, tag: TagModel) {
        chip.text = tag.name
        val isSelected = selectedTags.contains(tag)
        // 根据选中态应用样式，避免重复逻辑
        val textColor = applyChipStyle(chip, tag, isSelected)

        if (isEditMode) {
            // 编辑模式：显示删除按钮，长按编辑，点击不响应
            chip.isCheckable = false
            chip.isClickable = true
            chip.isCloseIconVisible = true
            // 删除图标使用与文字一致的色调
            chip.closeIconTint = ColorStateList.valueOf(textColor)
        } else {
            // 选择模式：可选择，不显示删除按钮
            chip.isCheckable = true
            chip.isClickable = true
            chip.isCloseIconVisible = false
            // 选中态仅通过颜色反馈，不使用图标
            chip.isCheckedIconVisible = false
        }

        // 绑定期间禁止触发回调，避免在布局计算中调用 notify
        isInternalSelectionUpdate = true
        try {
            chip.isSelected = isSelected
            chip.isChecked = isSelected
        } finally {
            isInternalSelectionUpdate = false
        }
    }

    /**
     * 应用标签样式，集中处理颜色与描边逻辑
     */
    private fun applyChipStyle(chip: Chip, tag: TagModel, isSelected: Boolean): Int {
        // 使用更清晰的对比，保证可读性
        val surfaceColor = MaterialColors.getColor(
            chip,
            com.google.android.material.R.attr.colorSurfaceContainerLow
        )
        val surfaceStrongColor = MaterialColors.getColor(
            chip,
            com.google.android.material.R.attr.colorSurfaceContainerHighest
        )
        val defaultTextColor = MaterialColors.getColor(
            chip,
            com.google.android.material.R.attr.colorOnSurface
        )
        // 选中态与未选中态颜色统一交由调色板工具计算
        val (textColor, backgroundColor, strokeColor) = PaletteManager.getSelectorTagColors(
            chip.context,
            tag.name,
            defaultTextColor,
            surfaceColor,
            surfaceStrongColor,
            isSelected
        )

        chip.chipBackgroundColor = ColorStateList.valueOf(backgroundColor)
        chip.setTextColor(textColor)
        // 细描边让标签更像 label，而不是按钮
        val strokeWidth = (if (isSelected) 1.5f else 1.0f) * chip.resources.displayMetrics.density
        chip.chipStrokeColor = ColorStateList.valueOf(strokeColor)
        chip.chipStrokeWidth = strokeWidth
        return textColor
    }

    /**
     * 透明度处理，保持颜色主体但降低存在感
     */
    private fun applyAlpha(color: Int, alpha: Float): Int {
        val clampedAlpha = (alpha.coerceIn(0f, 1f) * 255).toInt()
        return Color.argb(clampedAlpha, Color.red(color), Color.green(color), Color.blue(color))
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
        // 删除按钮仅在编辑模式下有效
        holder.binding.chip.setOnCloseIconClickListener {
            if (!isEditMode) return@setOnCloseIconClickListener
            val tag = holder.item as TagModel
            onTagClick(tag, "delete")
        }

        // 仅在编辑模式下处理点击编辑，选择模式交由 Checkable 处理
        holder.binding.chip.setOnClickListener {
            if (!isEditMode) return@setOnClickListener
            val tag = holder.item as TagModel
            onTagClick(tag, "edit")
        }

        // 选择模式下监听勾选变化，负责维护选中集合与限制
        holder.binding.chip.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isEditMode || isInternalSelectionUpdate) return@setOnCheckedChangeListener
            val tag = holder.item as TagModel
            if (isChecked) {
                if (selectionLimit > 0 && selectedTags.size >= selectionLimit) {
                    isInternalSelectionUpdate = true
                    buttonView.isChecked = false
                    isInternalSelectionUpdate = false
                    // 回退选择后刷新样式，确保颜色与状态一致
                    applyChipStyle(buttonView as Chip, tag, false)
                    onSelectionLimitReached?.invoke(selectionLimit)
                    return@setOnCheckedChangeListener
                }
                if (!selectedTags.contains(tag)) {
                    selectedTags.add(tag)
                }
            } else {
                selectedTags.remove(tag)
            }
            onSelectionChanged?.invoke(selectedTags.toList())
            // 直接更新样式，避免在布局计算中触发 notify
            buttonView.isSelected = isChecked
            applyChipStyle(buttonView as Chip, tag, isChecked)
        }
    }

}
