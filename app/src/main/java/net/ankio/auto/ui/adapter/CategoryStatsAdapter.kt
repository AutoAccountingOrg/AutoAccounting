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
import androidx.core.view.isVisible
import net.ankio.auto.R
import net.ankio.auto.databinding.AdapterCategoryStatsBinding
import net.ankio.auto.http.api.BillAPI
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import net.ankio.auto.ui.utils.PaletteManager
import net.ankio.auto.ui.theme.DynamicColors
import net.ankio.auto.ui.utils.load
import java.util.Locale
import kotlin.math.abs

/**
 * 分类统计适配器
 * 支持可展开的树形结构显示
 */
class CategoryStatsAdapter : BaseAdapter<AdapterCategoryStatsBinding, CategoryStatsItem>() {

    override fun onInitViewHolder(holder: BaseViewHolder<AdapterCategoryStatsBinding, CategoryStatsItem>) {
        // 设置点击监听器
        holder.binding.root.setOnClickListener {
            val item = holder.item
            if (item != null && item.hasChildren && !item.isChild) {
                toggleExpand(item)
            }
        }
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder<AdapterCategoryStatsBinding, CategoryStatsItem>,
        data: CategoryStatsItem,
        position: Int
    ) {
        val binding = holder.binding
        val density = holder.context.resources.displayMetrics.density

        // 设置分类名称
        binding.categoryName.text = data.name

        // 设置百分比
        val percent = (data.percent * 100).coerceIn(0.0, 100.0)
        binding.categoryPercent.text = String.format(Locale.getDefault(), "%.1f%%", percent)

        // 设置分类图标（先加载图标，再着色）
        //binding.categoryIconMore.setIcon(R.drawable.default_cate)
        binding.categoryIconMore.getIconView().load(data.icon, R.drawable.default_cate)

        // 按名称与位置稳定映射到 1..50 的色系，尽量减少重合
        val paletteIndex = computePaletteIndex(data.name, position)


        val duo = PaletteManager.getColors(holder.context, paletteIndex)
        val emphasisColor = duo.emphasis

        // 图标组件着色：背景=强调色，前景=OnPrimary
        binding.categoryIconMore.setColor(emphasisColor, DynamicColors.OnPrimary)
        // 文本颜色保持不变（不修改）
        // 进度条着色：进度=强调色，背景=Surface
        binding.progressBar.progressTintList = ColorStateList.valueOf(emphasisColor)
        binding.progressBar.progressBackgroundTintList =
            ColorStateList.valueOf(DynamicColors.Surface)

        // 去掉子类缩进：统一不显示缩进占位
        binding.indentView.isVisible = false

        // 去掉子类缩进：统一无额外起始外边距，避免复用导致错位
        run {
            val params =
                binding.progressBar.layoutParams as android.view.ViewGroup.MarginLayoutParams
            params.marginStart = 0
            binding.progressBar.layoutParams = params
        }

        // 设置展开图标：由组件的 more 展示
        if (data.hasChildren && !data.isChild) {
            binding.categoryIconMore.showMore()
        } else {
            binding.categoryIconMore.hideMore()
        }

        // 设置进度条百分比
        val progress = (data.percent * 100).coerceIn(0.0, 100.0).toInt()
        binding.progressBar.progress = progress
    }

    /**
     * 切换展开/收起状态
     */
    private fun toggleExpand(item: CategoryStatsItem) {
        val position = indexOf(item)
        if (position == -1) return

        // 不要原地修改数据项，使用 copy() 生成新对象替换，确保 DiffUtil 能检测到变化
        val newParent = item.copy(isExpanded = !item.isExpanded)

        if (newParent.isExpanded) {
            // 展开：插入子项
            val childItems = newParent.children.map { child ->
                CategoryStatsItem(
                    name = child.name,
                    percent = child.percent,
                    count = child.count,
                    icon = child.icon,
                    isChild = true,
                    hasChildren = false,
                    children = emptyList(),
                    isExpanded = false
                )
            }

            // 重构数据列表：在指定位置插入子项
            val currentItems = getItems().toMutableList()
            // 替换父项为新对象（已切换展开状态）
            currentItems[position] = newParent
            currentItems.addAll(position + 1, childItems)
            updateItems(currentItems)
        } else {
            // 收起：移除子项
            val currentItems = getItems().toMutableList()
            var removeCount = 0
            var nextIndex = position + 1

            while (nextIndex < currentItems.size && currentItems[nextIndex].isChild) {
                removeCount++
                nextIndex++
            }

            if (removeCount > 0) {
                repeat(removeCount) {
                    currentItems.removeAt(position + 1)
                }
                // 替换父项为新对象（已切换收起状态）
                currentItems[position] = newParent
                updateItems(currentItems)
            }
            // 若没有子项可移除，也需要刷新父项图标状态
            if (removeCount == 0) {
                currentItems[position] = newParent
                updateItems(currentItems)
            }
        }
    }

    /**
     * 将名称与位置映射为 1..50 的色系索引，稳定且分散，尽量避免重合。
     */
    private fun computePaletteIndex(name: String?, position: Int): Int {
        val safeName = name?.trim().orEmpty()
        var hash = 1125899906842597L // 大质数 seed，减少碰撞
        safeName.forEach { ch ->
            hash = (hash * 1315423911L) xor ch.code.toLong()
        }
        hash = hash xor (position.toLong() * 1469598103934665603L)
        val idx = (kotlin.math.abs(hash) % PaletteManager.TOTAL_FAMILIES) + 1
        return idx.toInt()
    }

    /**
     * 设置分类数据
     */
    fun setCategoryData(categories: List<BillAPI.CategoryItemDto>) {
        val flatItems = categories.map { parent ->
            CategoryStatsItem(
                name = parent.name,
                percent = parent.percent,
                count = parent.count,
                icon = parent.icon,
                isChild = false,
                hasChildren = parent.children.isNotEmpty(),
                children = parent.children,
                isExpanded = false
            )
        }
        updateItems(flatItems)
    }

    override fun areItemsSame(oldItem: CategoryStatsItem, newItem: CategoryStatsItem): Boolean {
        return oldItem.name == newItem.name && oldItem.isChild == newItem.isChild
    }

    override fun areContentsSame(oldItem: CategoryStatsItem, newItem: CategoryStatsItem): Boolean {
        return oldItem == newItem
    }
}

/**
 * 分类统计数据项
 */
data class CategoryStatsItem(
    val name: String,
    val percent: Double,
    val count: Int = 0,
    val icon: String? = null,
    val isChild: Boolean = false,
    val hasChildren: Boolean = false,
    val children: List<BillAPI.CategoryItemDto> = emptyList(),
    var isExpanded: Boolean = false
)
