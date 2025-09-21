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

        setupBasicInfo(binding, data)
        setupIconAndColor(holder, binding, data, position)
        setupProgressBar(holder, binding, data, position)
        setupExpansionState(binding, data)
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
            expandCategory(position, newParent)
        } else {
            collapseCategory(position, newParent)
        }
    }

    /**
     * 展开分类：插入子项到列表中
     */
    private fun expandCategory(position: Int, newParent: CategoryStatsItem) {
        val childItems = createChildItems(newParent.children)
        val currentItems = getItems().toMutableList()

        // 替换父项为新对象（已切换展开状态）
        currentItems[position] = newParent
        // 在父项后插入子项
        currentItems.addAll(position + 1, childItems)
        updateItems(currentItems)
    }

    /**
     * 收起分类：从列表中移除子项
     */
    private fun collapseCategory(position: Int, newParent: CategoryStatsItem) {
        val currentItems = getItems().toMutableList()
        val removeCount = countChildItems(currentItems, position)

        if (removeCount > 0) {
            removeChildItems(currentItems, position, removeCount)
        }

        // 更新父项状态
        currentItems[position] = newParent
        updateItems(currentItems)
    }

    /**
     * 创建子项列表
     */
    private fun createChildItems(children: List<BillAPI.CategoryItemDto>): List<CategoryStatsItem> {
        return children.map { child ->
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
    }

    /**
     * 统计指定位置后的子项数量
     */
    private fun countChildItems(items: List<CategoryStatsItem>, startPosition: Int): Int {
        var count = 0
        var nextIndex = startPosition + 1

        while (nextIndex < items.size && items[nextIndex].isChild) {
            count++
            nextIndex++
        }

        return count
    }

    /**
     * 从列表中移除指定数量的子项
     */
    private fun removeChildItems(
        items: MutableList<CategoryStatsItem>,
        position: Int,
        removeCount: Int
    ) {
        repeat(removeCount) {
            items.removeAt(position + 1)
        }
    }

    /**
     * 将名称与位置映射为 1..50 的色系索引，稳定且分散，尽量避免重合。
     *
     * 使用稳定的哈希算法确保相同输入总是产生相同输出，
     * 结合位置信息进一步减少碰撞概率。
     *
     * @param name 分类名称，用于生成哈希的基础值
     * @param position 分类在列表中的位置，用于增加哈希的随机性
     * @return 1到50之间的色系索引
     */
    private fun computePaletteIndex(name: String?, position: Int): Int {
        // 确保名称不为null且去除前后空格
        val safeName = name?.trim().orEmpty()

        // 使用大质数作为种子，减少哈希碰撞
        var hash = 1125899906842597L

        // 对每个字符进行哈希计算，使用另一个大质数作为乘数
        safeName.forEach { ch ->
            hash = (hash * 1315423911L) xor ch.code.toLong()
        }

        // 结合位置信息，进一步增加哈希的分散度
        hash = hash xor (position.toLong() * 1469598103934665603L)

        // 取绝对值后对总色系数量取模，确保结果在1-50范围内
        val idx = (kotlin.math.abs(hash) % PaletteManager.TOTAL_FAMILIES) + 1
        return idx.toInt()
    }

    /**
     * 设置分类数据
     *
     * 将API返回的分类数据转换为适配器使用的内部数据结构。
     * 每个父分类都会被转换为CategoryStatsItem，子分类信息保存在children字段中。
     *
     * @param categories 从API获取的分类列表数据
     */
    fun setCategoryData(categories: List<BillAPI.CategoryItemDto>) {
        val flatItems = categories.map { parent ->
            CategoryStatsItem(
                name = parent.name,
                percent = parent.percent,
                count = parent.count,
                icon = parent.icon,
                isChild = false, // 父分类不是子项
                hasChildren = parent.children.isNotEmpty(), // 判断是否有子分类
                children = parent.children, // 保存子分类数据
                isExpanded = false // 初始状态为未展开
            )
        }
        updateItems(flatItems)
    }

    /**
     * 设置基本信息：分类名称和百分比
     */
    private fun setupBasicInfo(binding: AdapterCategoryStatsBinding, data: CategoryStatsItem) {
        // 设置分类名称
        binding.categoryName.text = data.name

        // 设置百分比，限制在0-100范围内
        val percent = (data.percent * 100).coerceIn(0.0, 100.0)
        binding.categoryPercent.text = String.format(Locale.getDefault(), "%.1f%%", percent)
    }

    /**
     * 设置图标和颜色主题
     */
    private fun setupIconAndColor(
        holder: BaseViewHolder<AdapterCategoryStatsBinding, CategoryStatsItem>,
        binding: AdapterCategoryStatsBinding,
        data: CategoryStatsItem,
        position: Int
    ) {
        // 设置分类图标（先加载图标，再着色）
        binding.categoryIconMore.getIconView().load(data.icon, R.drawable.default_cate)

        // 按名称与位置稳定映射到 1..50 的色系，尽量减少重合
        val paletteIndex = computePaletteIndex(data.name, position)
        val duo = PaletteManager.getColors(holder.context, paletteIndex)
        val emphasisColor = duo.emphasis

        // 图标组件着色：背景=强调色，前景=OnPrimary
        binding.categoryIconMore.setColor(emphasisColor, DynamicColors.OnPrimary)
    }

    /**
     * 设置进度条样式和进度
     */
    private fun setupProgressBar(
        holder: BaseViewHolder<AdapterCategoryStatsBinding, CategoryStatsItem>,
        binding: AdapterCategoryStatsBinding,
        data: CategoryStatsItem,
        position: Int
    ) {
        // 获取当前项目的强调色
        val paletteIndex = computePaletteIndex(data.name, position)
        val duo = PaletteManager.getColors(holder.context, paletteIndex)
        val emphasisColor = duo.emphasis

        // 进度条着色：进度=强调色，背景=Surface
        binding.progressBar.progressTintList = ColorStateList.valueOf(emphasisColor)
        binding.progressBar.progressBackgroundTintList =
            ColorStateList.valueOf(DynamicColors.SurfaceContainerHigh)

        // 去掉子类缩进：统一不显示缩进占位
        binding.indentView.isVisible = false

        // 去掉子类缩进：统一无额外起始外边距，避免复用导致错位
        removeProgressBarMarginStart(binding)

        // 设置进度条百分比
        val progress = (data.percent * 100).coerceIn(0.0, 100.0).toInt()
        binding.progressBar.progress = progress
    }

    /**
     * 移除进度条的起始外边距
     */
    private fun removeProgressBarMarginStart(binding: AdapterCategoryStatsBinding) {
        val params = binding.progressBar.layoutParams as android.view.ViewGroup.MarginLayoutParams
        params.marginStart = 0
        binding.progressBar.layoutParams = params
    }

    /**
     * 设置展开状态图标
     */
    private fun setupExpansionState(binding: AdapterCategoryStatsBinding, data: CategoryStatsItem) {
        // 设置展开图标：由组件的 more 展示
        if (data.hasChildren && !data.isChild) {
            binding.categoryIconMore.showMore()
        } else {
            binding.categoryIconMore.hideMore()
        }
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
