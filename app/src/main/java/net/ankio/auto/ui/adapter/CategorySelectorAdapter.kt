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
import net.ankio.auto.App
import net.ankio.auto.R
import net.ankio.auto.databinding.AdapterCategoryListBinding
import net.ankio.auto.http.api.CategoryAPI
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import net.ankio.auto.ui.utils.setCategoryIcon
import org.ezbook.server.constant.BillType
import org.ezbook.server.db.model.CategoryModel

/**
 * 分类选择器适配器
 * @property dataItems 分类数据列表
 * @property onItemClick 点击事件回调
 * @property onItemChildClick 子项点击事件回调
 * @property onItemLongClick 长按事件回调
 * @property isEditMode 是否编辑模式
 */

class CategorySelectorAdapter(
    private val onItemClick: (item: CategoryModel, pos: Int, hasChild: Boolean, view: View) -> Unit,
    private val onItemChildClick: (item: CategoryModel, pos: Int) -> Unit,
    private val onItemLongClick: ((item: CategoryModel, pos: Int, view: View) -> Unit)? = null,
    private var isEditMode: Boolean = false
) : BaseAdapter<AdapterCategoryListBinding, CategoryModel>(

) {
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
            adapter = CategorySelectorAdapter(
                isEditMode = isEditMode,
                onItemClick = { item, pos, hasChild, view ->
                    onItemChildClick(item, pos)
                },
                onItemChildClick = { item, pos ->

                },
                onItemLongClick = { item, pos, view ->
                    onItemLongClick?.invoke(item, pos, view)
                }
            )
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
     * 创建添加按钮的CategoryModel
     * @param bookId 账本ID
     * @param type 分类类型
     * @param parentId 父分类ID，"-1"表示一级分类
     * @return 添加按钮的CategoryModel
     */
    fun createAddButtonItem(
        bookId: String,
        type: BillType,
        parentId: String = "-1"
    ): CategoryModel {
        val addItem = CategoryModel()
        addItem.remoteId = "-9998" // 特殊ID标识添加按钮
        addItem.remoteBookId = bookId
        addItem.remoteParentId = parentId
        addItem.type = type
        addItem.name = "添加"
        addItem.icon = "add" // 特殊图标标识
        return addItem
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
        if (data.isAddBtn()) {
            // 添加按钮的特殊渲染
            binding.itemImageIcon.setImageResource(R.drawable.float_add)
            binding.ivMore.visibility = View.GONE

        } else {
            binding.itemImageIcon.setCategoryIcon(data)
        }
        

        binding.itemText.text = data.name

        binding.root.setOnClickListener {
            if (data.isPanel()) return@setOnClickListener
            val hasChild = binding.ivMore.isVisible
            if (prevBinding != null) {
                setActive(prevBinding!!, false)
            }
            prevBinding = binding
            setActive(binding, true)
            panelItem = data
            onItemClick(data, position, hasChild, binding.itemImageIcon)
        }

        // 添加长按监听器（仅在编辑模式下且不是添加按钮时）
        binding.root.setOnLongClickListener {
            onItemLongClick?.invoke(data, position, binding.root)
            true
        }
        // 本身就是二级菜单，无需继续获取二级菜单
        if (data.isChild() || data.isAddBtn()) {
            renderMoreItem(binding, false)
            return
        }

        launchInAdapter {
            try {
                // 检查当前绑定的数据是否还是同一个
                if (holder.item == data) {
                    val child = loadChildData(data)
                    val hasChild = child.isNotEmpty() || isEditMode
                    renderMoreItem(binding, hasChild)
                }
            } catch (e: Exception) {
                // 如果加载失败，隐藏更多按钮
                renderMoreItem(binding, false)
            }
        }
    }

    private fun renderMoreItem(binding: AdapterCategoryListBinding, hasChild: Boolean) {
        binding.ivMore.visibility = if (hasChild) View.VISIBLE else View.GONE

    }

    private fun setActive(
        binding: AdapterCategoryListBinding,
        isActive: Boolean,
    ) {
        val (textColor, imageBackground, imageColorFilter) =
            if (isActive) {
                Triple(
                    App.getThemeAttrColor(com.google.android.material.R.attr.colorPrimary),
                    R.drawable.rounded_border,
                    App.getThemeAttrColor(com.google.android.material.R.attr.colorOnPrimary),
                )
            } else {
                Triple(
                    App.getThemeAttrColor(com.google.android.material.R.attr.colorSecondary),
                    R.drawable.rounded_border_,
                    App.getThemeAttrColor(com.google.android.material.R.attr.colorSecondary),
                )
            }

        binding.itemText.setTextColor(textColor)
        binding.itemImageIcon.apply {
            setBackgroundResource(imageBackground)
            setColorFilter(imageColorFilter)
        }
        binding.ivMore.apply {
            setBackgroundResource(imageBackground)
            setColorFilter(imageColorFilter)
        }
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

