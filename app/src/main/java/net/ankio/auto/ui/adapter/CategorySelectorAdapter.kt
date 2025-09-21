/*
 * Copyright (C) 2023 ankio(ankio@ankio.net)
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

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.databinding.AdapterCategoryListBinding
import net.ankio.auto.http.api.CategoryAPI
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import net.ankio.auto.ui.utils.setCategoryIcon
import org.ezbook.server.constant.BillType
import org.ezbook.server.db.model.CategoryModel
import net.ankio.auto.ui.theme.DynamicColors

/**
 * 分类选择器适配器 - 支持链式调用配置
 *
 * 使用示例:
 * ```kotlin
 * val adapter = CategorySelectorAdapter()
 *     .onItemClick { item, pos, hasChild, view ->
 *         // 处理项目点击
 *         selectCategory(item)
 *     }
 *     .onItemChildClick { item, pos ->
 *         // 处理子项点击
 *         navigateToSubCategory(item)
 *     }
 *     .onItemLongClick { item, pos, view ->
 *         // 处理长按事件
 *         showContextMenu(item, view)
 *     }
 *     .editMode(true)
 *
 * recyclerView.adapter = adapter
 * ```
 *
 * 相比原来的构造函数参数地狱，现在更加清晰和灵活。
 */
class CategorySelectorAdapter : BaseAdapter<AdapterCategoryListBinding, CategoryModel>() {

    // 回调函数配置 - 使用链式调用设置
    private var itemClickHandler: ((CategoryModel, Int, Boolean, View) -> Unit)? = null
    private var childClickHandler: ((CategoryModel, Int) -> Unit)? = null
    private var longClickHandler: ((CategoryModel, Int, View) -> Unit)? = null
    private var isEditMode: Boolean = false

    /**
     * 链式调用方法 - 配置点击事件
     */
    fun onItemClick(handler: (item: CategoryModel, pos: Int, hasChild: Boolean, view: View) -> Unit) =
        apply {
            itemClickHandler = handler
        }

    /**
     * 链式调用方法 - 配置子项点击事件
     */
    fun onItemChildClick(handler: (item: CategoryModel, pos: Int) -> Unit) = apply {
        childClickHandler = handler
    }

    /**
     * 链式调用方法 - 配置长按事件
     */
    fun onItemLongClick(handler: (item: CategoryModel, pos: Int, view: View) -> Unit) = apply {
        longClickHandler = handler
    }

    /**
     * 链式调用方法 - 配置编辑模式
     */
    fun editMode(enabled: Boolean) = apply {
        isEditMode = enabled
    }

    override fun onInitViewHolder(holder: BaseViewHolder<AdapterCategoryListBinding, CategoryModel>) {
    }

    /**
     * 二级分类缓存
     */
    private val childMap = hashMapOf<Long, MutableList<CategoryModel>>()

    /**
     * 当前面板对应的Item
     */
    private var panelItem: CategoryModel? = null


    /**
     * 加载二级分类的数据
     */
    private suspend fun loadChildData(
        parentCategory: CategoryModel
    ): MutableList<CategoryModel> = withContext(Dispatchers.IO) {
        if (childMap.containsKey(parentCategory.id)) return@withContext childMap[parentCategory.id]!!
        val list = CategoryAPI.list(
            parentCategory.remoteBookId,
            parentCategory.type,
            parentCategory.remoteId
        ).toMutableList()
        childMap[parentCategory.id] = list
        return@withContext list
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder<AdapterCategoryListBinding, CategoryModel>,
        data: CategoryModel,
        position: Int
    ) {
        val binding = holder.binding
        setActive(binding, false)
        if (data.isPanel()) {
            renderPanel(holder, data, holder.context)
        } else {
            renderCategory(holder, data, position)
        }

    }


    /**
     * 渲染面板
     */
    private fun renderPanel(
        holder: BaseViewHolder<AdapterCategoryListBinding, CategoryModel>,
        parentCCategory: CategoryModel,
        context: Context
    ) {
        val binding = holder.binding
        binding.icon.visibility = View.GONE
        binding.container.visibility = View.VISIBLE
        val adapter: CategorySelectorAdapter
        if (binding.recyclerView.adapter == null) {
            binding.recyclerView.layoutManager = GridLayoutManager(context, 5)
            adapter = CategorySelectorAdapter()
                .editMode(isEditMode)
                .onItemClick { item, pos, hasChild, view ->
                    childClickHandler?.invoke(item, pos)
                }
                .onItemChildClick { _, _ ->
                    // 子项的子项点击处理（暂时为空）
                }
                .onItemLongClick { item, pos, view ->
                    longClickHandler?.invoke(item, pos, view)
                }
            binding.recyclerView.adapter = adapter
        } else {
            adapter = binding.recyclerView.adapter as CategorySelectorAdapter
        }

        // 面板没有子类，所以无法渲染~

        launchInAdapter {
            val list = loadChildData(panelItem!!)
            // 在编辑模式下，为二级分类添加"添加"按钮
            if (isEditMode) {
                val addButton = createAddButtonItem(
                    panelItem!!.remoteBookId,
                    panelItem!!.type,
                    panelItem!!.remoteId
                )
                if (list.find { addButton.remoteId === it.remoteId } == null)
                    list.add(addButton)
            }

            adapter.updateItems(list)
            val leftDistanceView2: Int = parentCCategory.id.toInt()
            val layoutParams = binding.imageView.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.leftMargin = leftDistanceView2 // 设置左边距
        }


    }


    /**
     * 创建添加按钮的CategoryModel - 使用链式调用风格
     * @param bookId 账本ID
     * @param type 分类类型
     * @param parentId 父分类ID，"-1"表示一级分类
     * @return 添加按钮的CategoryModel
     */
    fun createAddButtonItem(
        bookId: String,
        type: BillType,
        parentId: String = "-1"
    ): CategoryModel = CategoryModel().apply {
        remoteId = "-9998"      // 特殊ID标识添加按钮
        remoteBookId = bookId
        remoteParentId = parentId
        this.type = type
        name = "添加"
        icon = "add"            // 特殊图标标识
    }

    private var prevBinding: AdapterCategoryListBinding? = null

    /**
     * 渲染分类图标
     */
    private fun renderCategory(
        holder: BaseViewHolder<AdapterCategoryListBinding, CategoryModel>,
        data: CategoryModel,
        position: Int
    ) {
        val binding = holder.binding
        binding.icon.visibility = View.VISIBLE
        binding.container.visibility = View.GONE

        // 特殊处理添加按钮
        val iconMore = binding.categoryIconMore
        if (data.isAddBtn()) {
            iconMore.setIcon(R.drawable.float_add)
            iconMore.hideMore()
        } else {
            iconMore.setCategoryIcon(data)
        }


        binding.itemText.text = data.name

        binding.root.setOnClickListener {
            if (data.isPanel()) return@setOnClickListener
            val hasChild = iconMore.isMoreVisible()
            prevBinding?.let { setActive(it, false) }
            prevBinding = binding
            setActive(binding, true)
            panelItem = data
            itemClickHandler?.invoke(data, position, hasChild, iconMore.getIconView())
        }

        // 添加长按监听器（仅在编辑模式下且不是添加按钮时）
        binding.root.setOnLongClickListener {
            longClickHandler?.invoke(data, position, binding.root)
            true
        }
        // 本身就是二级菜单，无需继续获取二级菜单
        if (data.isChild() || data.isAddBtn()) {
            renderMoreItem(binding, false)
            return
        }

        launchInAdapter {
            // 检查当前绑定的数据是否还是同一个
            if (holder.item == data) {
                val child = runCatching { loadChildData(data) }.getOrElse { emptyList() }
                val hasChild = child.isNotEmpty() || isEditMode
                renderMoreItem(binding, hasChild)
            }
        }
    }

    private fun renderMoreItem(binding: AdapterCategoryListBinding, hasChild: Boolean) {
        if (hasChild) binding.categoryIconMore.showMore() else binding.categoryIconMore.hideMore()

    }

    private fun setActive(
        binding: AdapterCategoryListBinding,
        isActive: Boolean,
    ) {
        binding.itemText.setTextColor(if (isActive) DynamicColors.Primary else DynamicColors.Secondary)
        // 外部传色：背景与 more 前景
        val bg = if (isActive) DynamicColors.Primary else DynamicColors.SurfaceContainerHigh
        val fg = if (isActive) DynamicColors.OnPrimary else DynamicColors.Secondary
        binding.categoryIconMore.setColor(bg, fg)
    }

    override fun areItemsSame(oldItem: CategoryModel, newItem: CategoryModel): Boolean {
        if (oldItem.isPanel() == newItem.isPanel()) return true
        return oldItem.id == newItem.id
    }

    override fun areContentsSame(oldItem: CategoryModel, newItem: CategoryModel): Boolean {
        //  if (oldItem.isPanel() == newItem.isPanel()) return true
        return oldItem == newItem
    }

}

