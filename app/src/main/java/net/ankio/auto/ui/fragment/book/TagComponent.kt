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
import android.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import net.ankio.auto.databinding.ComponentTagBinding
import net.ankio.auto.ui.api.BaseComponent
import net.ankio.auto.http.api.TagAPI
import org.ezbook.server.db.model.TagModel
import androidx.core.graphics.toColorInt

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
        refreshChips()
    }

    override fun init() {
        super.init()
        loadData()
    }

    /**
     * 加载标签数据
     */
    private fun loadData() {
        binding.statusPage.showLoading()

        // 使用组件的生命周期作用域
        lifecycle.coroutineScope.launch {
            try {
                dataItems.clear()
                val newData = TagAPI.all()
                dataItems.addAll(newData)
                refreshChips()
            } catch (e: Exception) {
                // 加载失败，显示错误状态
                binding.statusPage.showError()
            }
        }
    }

    /**
     * 刷新Chip显示
     */
    private fun refreshChips() {
        val chipGroup = binding.chipGroup
        chipGroup.removeAllViews()

        if (dataItems.isEmpty()) {
            // 显示空状态
            binding.statusPage.showEmpty()
        } else {
            // 显示内容
            dataItems.forEach { tag ->
                val chip = createChip(tag)
                chipGroup.addView(chip)
            }
            binding.statusPage.showContent()
        }
    }

    /**
     * 创建Chip
     */
    private fun createChip(tag: TagModel): Chip {
        val chip = Chip(context)
        chip.text = tag.name
        chip.tag = tag.id

        // 设置颜色
        val color = try {
            tag.color.toColorInt()
        } catch (e: Exception) {
            "#2196F3".toColorInt()
        }
        chip.chipBackgroundColor = ColorStateList.valueOf(color)

        if (isEditMode) {
            // 编辑模式：显示删除按钮，长按编辑，点击不响应
            chip.isCheckable = false
            chip.isClickable = false
            chip.isCloseIconVisible = true

            // 长按编辑
            chip.setOnLongClickListener {
                onTagSelected?.invoke(tag, "edit")
                true
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
     * 通知选择状态变化
     */
    private fun notifySelectionChanged() {
        if (!isEditMode) {
            val selectedTags = getSelectedTags()
            onSelectionChanged?.invoke(selectedTags)
        }
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
            val chipGroup = binding.chipGroup

            for (i in 0 until chipGroup.childCount) {
                val chip = chipGroup.getChildAt(i) as? Chip ?: continue
                val tagId = chip.tag as? Long ?: continue
                chip.isChecked = selectedIds.contains(tagId)
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
        val chipGroup = binding.chipGroup

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

        return selectedTags
    }

    /**
     * 清空选择（仅选择模式有效）
     */
    fun clearSelection() {
        if (!isEditMode) {
            val chipGroup = binding.chipGroup

            for (i in 0 until chipGroup.childCount) {
                val chip = chipGroup.getChildAt(i) as? Chip ?: continue
                chip.isChecked = false
            }

            onSelectionChanged?.invoke(emptySet())
        }
    }
}