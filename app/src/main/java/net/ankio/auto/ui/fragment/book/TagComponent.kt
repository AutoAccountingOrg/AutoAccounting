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

package net.ankio.auto.ui.fragment.book

import android.content.res.ColorStateList
import android.content.res.Configuration
import android.content.res.Resources.Theme
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import com.google.android.material.chip.Chip
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.databinding.ComponentTagBinding
import net.ankio.auto.ui.api.BaseComponent
import net.ankio.auto.http.api.TagAPI
import org.ezbook.server.db.model.TagModel
import androidx.core.graphics.toColorInt
import net.ankio.auto.utils.ThemeUtils
import androidx.core.content.ContextCompat
import net.ankio.auto.utils.toThemeColor

/**
 * 标签组件
 * 用于显示和管理标签列表
 *
 * @param binding 组件绑定
 * @param lifecycle 生命周期
 */
class TagComponent(binding: ComponentTagBinding, private val lifecycle: Lifecycle) :
    BaseComponent<ComponentTagBinding>(binding, lifecycle) {

    private var onTagSelected: ((TagModel, String) -> Unit)? = null
    private var onSelectionChanged: ((Set<TagModel>) -> Unit)? = null
    private val dataItems = mutableListOf<TagModel>()
    private var isEditMode: Boolean = false

    /**
     * 设置标签选择回调
     * @param callback 回调函数，参数为选中的TagModel和操作类型
     */
    fun setOnTagSelectedListener(callback: (TagModel, String) -> Unit) {
        this.onTagSelected = callback
    }

    /**
     * 设置选择状态变化回调
     * @param callback 回调函数，参数为选中的标签集合
     */
    fun setOnSelectionChangedListener(callback: (Set<TagModel>) -> Unit) {
        this.onSelectionChanged = callback
    }

    /**
     * 设置显示模式（编辑模式和选择模式互斥）
     * @param editMode true=编辑模式（显示删除按钮，长按编辑）, false=选择模式（点击选择）
     */
    fun setEditMode(editMode: Boolean) {
        this.isEditMode = editMode
        lifecycle.coroutineScope.launch {
            refreshChips()
        }
    }

    override fun init() {
        super.init()
        loadData()
    }

    /**
     * 加载标签数据
     */
    private fun loadData() {
        // 使用组件的生命周期作用域
        lifecycle.coroutineScope.launch {
            try {
                dataItems.clear()
                val newData = TagAPI.all()
                dataItems.addAll(newData)
                refreshChips()
            } catch (e: Exception) {
                // 加载失败，隐藏内容显示空状态
                showEmptyState()
            }
        }
    }

    /**
     * 刷新Chip显示 - 支持分组显示
     */
    private suspend fun refreshChips() {
        val container = binding.tagContainer
        container.removeAllViews()

        if (dataItems.isEmpty()) {
            // 显示空状态
            showEmptyState()
        } else {
            // 按分组组织标签
            val groupedTags = dataItems.groupBy { it.group.ifEmpty { "其他" } }

            // 定义分组显示顺序（与TagUtils中的顺序保持一致）
            val groupOrder = TagAPI.getGroups()

            groupOrder.forEach { groupName ->
                val tagsInGroup = groupedTags[groupName]
                if (!tagsInGroup.isNullOrEmpty()) {
                    // 添加分组标题
                    val groupTitle = createGroupTitle(groupName)
                    container.addView(groupTitle)

                    // 添加该分组的ChipGroup
                    val chipGroup = createChipGroup()
                    tagsInGroup.forEach { tag ->
                        val chip = createChip(tag)
                        chipGroup.addView(chip)
                    }
                    container.addView(chipGroup)

                    // 添加分组间距
                    if (groupName != groupOrder.last { groupedTags.containsKey(it) }) {
                        val spacer = createGroupSpacer()
                        container.addView(spacer)
                    }
                }
            }

            showContentState()
        }
    }

    /**
     * 显示空状态
     */
    private fun showEmptyState() {
        binding.emptyView.visibility = android.view.View.VISIBLE
        binding.contentView.visibility = android.view.View.GONE
    }

    /**
     * 显示内容状态
     */
    private fun showContentState() {
        binding.emptyView.visibility = android.view.View.GONE
        binding.contentView.visibility = android.view.View.VISIBLE
    }

    /**
     * 创建分组标题
     * @param groupName 分组名称
     * @return 分组标题TextView
     */
    private fun createGroupTitle(groupName: String): MaterialTextView {
        val titleView = MaterialTextView(context)
        titleView.text = groupName
        titleView.textSize = 16f
        titleView.typeface = Typeface.DEFAULT_BOLD

        // 使用主题的primary颜色
        val primaryColor = com.google.android.material.R.attr.colorPrimary.toThemeColor()
        titleView.setTextColor(primaryColor)

        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(0, 24, 0, 12) // 上边距24dp，下边距12dp
        titleView.layoutParams = layoutParams

        return titleView
    }

    /**
     * 创建ChipGroup
     * @return ChipGroup实例
     */
    private fun createChipGroup(): com.google.android.material.chip.ChipGroup {
        val chipGroup = com.google.android.material.chip.ChipGroup(context)

        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        chipGroup.layoutParams = layoutParams

        // 设置ChipGroup属性
        chipGroup.isSingleLine = false
        chipGroup.isSingleSelection = false
        chipGroup.chipSpacingHorizontal =
            context.resources.getDimensionPixelSize(R.dimen.one_padding)
        chipGroup.chipSpacingVertical = context.resources.getDimensionPixelSize(R.dimen.one_padding)

        return chipGroup
    }

    /**
     * 创建分组间距
     * @return 间距View
     */
    private fun createGroupSpacer(): View {
        val spacer = View(context)
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            context.resources.getDimensionPixelSize(R.dimen.padding) // 16dp间距
        )
        spacer.layoutParams = layoutParams
        return spacer
    }

    /**
     * 创建Chip
     */
    private fun createChip(tag: TagModel): Chip {
        val chip = Chip(context)
        chip.text = tag.name
        chip.tag = tag.id

        // 设置颜色 - 优化背景和前景色
        val baseColor = try {
            tag.color.toColorInt()
        } catch (e: Exception) {
            "#E57373".toColorInt()
        }

        // 根据主题模式计算背景色和前景色
        val isDarkMode = isDarkTheme()
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

            // 设置适配主题的删除图标
            //  chip.closeIcon = ContextCompat.getDrawable(context, R.drawable.ic_close_themed)

            // 设置删除图标的颜色，与文字颜色保持一致以确保可见性
            chip.closeIconTint = ColorStateList.valueOf(textColor)

            // 长按编辑
            chip.setOnClickListener {
                onTagSelected?.invoke(tag, "edit")
            }

            // 删除按钮点击
            chip.setOnCloseIconClickListener {
                onTagSelected?.invoke(tag, "delete")
            }
        } else {
            // 选择模式：可选择，不显示删除按钮
            chip.isCheckable = true
            chip.isClickable = true
            chip.isCloseIconVisible = false

            // 点击选择
            chip.setOnCheckedChangeListener { _, isChecked ->
                onTagSelected?.invoke(tag, "select")
                notifySelectionChanged()
            }
        }

        return chip
    }

    /**
     * 检测是否为夜间模式
     */
    private fun isDarkTheme(): Boolean {
        return ThemeUtils.isDark
    }

    /**
     * 获取适应主题的背景色
     * @param baseColor 原始颜色
     * @param isDarkMode 是否为夜间模式
     * @return 适应主题的背景色
     */
    private fun getAdaptiveBackgroundColor(baseColor: Int, isDarkMode: Boolean): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(baseColor, hsv)
        val backgroundHsv = hsv.clone()

        if (isDarkMode) {
            // 夜间模式：降低饱和度，使用较低的明度
            backgroundHsv[1] = Math.max(0.2f, hsv[1] * 0.4f) // 饱和度降低到40%，保持一定色彩
            backgroundHsv[2] = Math.max(0.15f, Math.min(0.35f, hsv[2] * 0.6f)) // 明度范围0.15-0.35，较暗
        } else {
            // 浅色模式：大幅降低饱和度，使用较高的明度
            backgroundHsv[1] = Math.max(0.1f, hsv[1] * 0.3f) // 饱和度降低到30%
            backgroundHsv[2] = Math.max(0.85f, Math.min(0.95f, hsv[2] + 0.4f)) // 明度范围0.85-0.95，较亮
        }

        return Color.HSVToColor(backgroundHsv)
    }


    /**
     * 获取适应主题的文字色
     * @param baseColor 原始颜色
     * @param isDarkMode 是否为夜间模式
     * @return 适应主题的文字色
     */
    private fun getAdaptiveTextColor(baseColor: Int, isDarkMode: Boolean): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(baseColor, hsv)
        val textHsv = hsv.clone()

        if (isDarkMode) {
            // 夜间模式：使用较亮的文字色
            textHsv[1] = Math.min(1.0f, hsv[1] + 0.2f) // 稍微增加饱和度
            textHsv[2] = Math.max(0.6f, Math.min(0.9f, hsv[2] + 0.5f)) // 明度范围0.6-0.9，较亮
        } else {
            // 浅色模式：使用较深的文字色
            textHsv[1] = Math.min(1.0f, hsv[1] + 0.3f) // 增加饱和度
            textHsv[2] = Math.max(0.2f, Math.min(0.5f, hsv[2] - 0.3f)) // 明度范围0.2-0.5，较深
        }

        return Color.HSVToColor(textHsv)
    }

    /**
     * 通知选择状态变化
     */
    private fun notifySelectionChanged() {
        if (!isEditMode) {
            val selectedTags = getSelectedTags()
            onSelectionChanged?.invoke(selectedTags)
        }
    }

    /**
     * 获取容器中所有的ChipGroup
     * @return ChipGroup列表
     */
    private fun getAllChipGroups(): List<com.google.android.material.chip.ChipGroup> {
        val chipGroups = mutableListOf<com.google.android.material.chip.ChipGroup>()
        val container = binding.tagContainer

        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            if (child is com.google.android.material.chip.ChipGroup) {
                chipGroups.add(child)
            }
        }

        return chipGroups
    }

    /**
     * 刷新数据
     */
    fun refreshData() {
        loadData()
    }

    /**
     * 获取当前数据列表
     */
    fun getDataItems(): List<TagModel> = dataItems.toList()

    /**
     * 获取当前模式
     * @return true=编辑模式, false=选择模式
     */
    fun isEditMode(): Boolean = isEditMode

    /**
     * 设置选中的标签（仅选择模式有效）
     * @param selectedTags 选中的标签集合
     */
    fun setSelectedTags(selectedTags: Set<TagModel>) {
        if (!isEditMode) {
            val selectedIds = selectedTags.map { it.id }.toSet()
            val chipGroups = getAllChipGroups()

            chipGroups.forEach { chipGroup ->
                for (i in 0 until chipGroup.childCount) {
                    val chip = chipGroup.getChildAt(i) as? Chip ?: continue
                    val tagId = chip.tag as? Long ?: continue
                    chip.isChecked = selectedIds.contains(tagId)
                }
            }
        }
    }

    /**
     * 获取选中的标签（仅选择模式有效）
     * @return 选中的标签集合
     */
    fun getSelectedTags(): Set<TagModel> {
        if (isEditMode) return emptySet()

        val selectedTags = mutableSetOf<TagModel>()
        val chipGroups = getAllChipGroups()

        chipGroups.forEach { chipGroup ->
            for (i in 0 until chipGroup.childCount) {
                val chip = chipGroup.getChildAt(i) as? Chip ?: continue
                if (chip.isChecked) {
                    val tagId = chip.tag as? Long ?: continue
                    val tag = dataItems.find { it.id == tagId }
                    if (tag != null) {
                        selectedTags.add(tag)
                    }
                }
            }
        }

        return selectedTags
    }

    /**
     * 清空选择（仅选择模式有效）
     */
    fun clearSelection() {
        if (!isEditMode) {
            val chipGroups = getAllChipGroups()

            chipGroups.forEach { chipGroup ->
                for (i in 0 until chipGroup.childCount) {
                    val chip = chipGroup.getChildAt(i) as? Chip ?: continue
                    chip.isChecked = false
                }
            }

            onSelectionChanged?.invoke(emptySet())
        }
    }
}