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

package net.ankio.auto.ui.fragment.components

import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.SimpleItemAnimator
import net.ankio.auto.databinding.ComponentCategoryBinding
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.ui.adapter.CategorySelectorAdapter
import net.ankio.auto.ui.api.BaseComponent
import net.ankio.auto.http.api.CategoryAPI
import org.ezbook.server.constant.BillType
import org.ezbook.server.db.model.CategoryModel

/**
 * 分类选择组件
 *
 * 设计原则（遵循Linus好品味）：
 * 1. 简化构造：只需要ViewBinding，自动推断生命周期
 * 2. 统一协程管理：使用BaseComponent的launch方法
 * 3. 清晰的职责分离：专注于分类选择逻辑
 * 
 * @param binding 组件绑定对象
 */
class CategoryComponent(
    binding: ComponentCategoryBinding
) : BaseComponent<ComponentCategoryBinding>(binding) {

    // 分类选择回调函数
    private var onCategorySelected: ((CategoryModel?, CategoryModel?) -> Unit)? = null

    // 长按回调函数
    private var onCategoryLongClick: ((CategoryModel, Int, View) -> Unit)? = null

    // 父类别
    private var categoryModel1: CategoryModel? = null

    // 子类别
    private var categoryModel2: CategoryModel? = null

    // 显示状态
    private var expand = false

    // 默认一行的项目数
    private var line = 5

    // 类别列表
    private var items = mutableListOf<CategoryModel>()
    private var totalItems = 0

    // 最后点击的位置
    private var lastPosition = -1

    // 账本ID
    private var book: String = ""

    // 账单类型
    private var type: BillType = BillType.Expend

    // 编辑模式
    private var isEditMode: Boolean = false

    // RecyclerView适配器
    private lateinit var adapter: CategorySelectorAdapter

    /**
     * 设置分类选择回调
     * @param callback 回调函数，参数为父分类和子分类
     */
    fun setOnCategorySelectedListener(callback: (CategoryModel?, CategoryModel?) -> Unit) {
        this.onCategorySelected = callback
    }

    /**
     * 设置长按回调
     * @param callback 回调函数，参数为被长按的分类和位置
     */
    fun setOnCategoryLongClickListener(callback: (CategoryModel, Int, View) -> Unit) {
        this.onCategoryLongClick = callback
    }

    /**
     * 设置账本信息
     * @param bookId 账本ID
     * @param billType 账单类型
     * @param editMode 是否编辑模式
     */
    fun setBookInfo(bookId: String, billType: BillType, editMode: Boolean = false) {
        this.book = bookId
        this.type = billType
        this.isEditMode = editMode
        if (!::adapter.isInitialized) {
            setupRecyclerView()
            loadData()
        }
    }

    /**
     * SpanSizeLookup类，用于计算网格布局中每个项目的跨度大小
     */
    internal inner class SpecialSpanSizeLookup : SpanSizeLookup() {
        override fun getSpanSize(i: Int): Int {
            val categoryModel: CategoryModel = items[i]
            // 面板占满一行，添加按钮和普通分类占一个格子
            return if (categoryModel.isPanel()) 5 else 1
        }
    }

    override fun onComponentCreate() {
        super.onComponentCreate()

    }

    /**
     * 设置RecyclerView
     */
    private fun setupRecyclerView() {
        val recyclerView = binding.statusPage.contentView!!
        // 设置布局管理器
        val layoutManager = GridLayoutManager(context, line)
        layoutManager.spanSizeLookup = SpecialSpanSizeLookup()
        recyclerView.layoutManager = layoutManager
        (recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

        // 初始化适配器 - 使用链式调用配置
        adapter = CategorySelectorAdapter()
            .editMode(isEditMode)
            .onItemClick { item, pos, hasChild, view ->
                // 处理添加按钮点击
                categoryModel1 = item
                onCategorySelected?.invoke(categoryModel1, null)
                if (item.isAddBtn()) {
                    return@onItemClick
                }
                val panelPosition = getPanelIndex(pos) // 在当前位置，面板应该插入到哪里
                val lastPanelPosition = getPanelIndex(lastPosition) // 在上一个位置，面板在那里

                if (pos == lastPosition) { // 两次点击同一个，收起面板
                    if (hasChild) {
                        // 点击两次，说明需要删除
                        items.removeAt(panelPosition)
                        adapter.updateItems(items)
                        lastPosition = -1 // 归位
                        expand = false
                        categoryModel2 = null
                        return@onItemClick
                    }
                    lastPosition = -1
                }

                // 构造数据
                val category = getPanelData(view, item)

                // 同一行，不需要做删除，直接更新数据即可
                if (panelPosition == lastPanelPosition) {
                    if (hasChild) {
                        if (expand) {
                            items[panelPosition] = category
                            adapter.updateItems(items)
                        } else {
                            items.add(panelPosition, category)
                            adapter.updateItems(items)
                            expand = true
                        }
                    } else {
                        // 没有就移除
                        if (lastPosition != -1 && expand) {
                            items.removeAt(lastPanelPosition)
                            adapter.updateItems(items)
                            lastPosition = -1 // 归位
                            expand = false
                            return@onItemClick
                        }
                    }
                } else {
                    // 不同行的需要先删除
                    if (lastPosition != -1 && expand) {
                        items.removeAt(lastPanelPosition)
                        adapter.updateItems(items)
                        expand = false
                    }
                    if (hasChild) {
                        items.add(panelPosition, category)
                        adapter.updateItems(items)
                        expand = true
                    }
                }
                lastPosition = pos
            }
            .onItemChildClick { item, _ ->
                categoryModel2 = item
                onCategorySelected?.invoke(categoryModel1, categoryModel2)
            }
            .onItemLongClick { item, pos, view ->
                onCategoryLongClick?.invoke(item, pos, view)
            }

        recyclerView.adapter = adapter
    }

    /**
     * 计算插入面板的索引
     * @param position 被点击的项目的位置
     * @return 插入面板的索引
     */
    private fun getPanelIndex(position: Int): Int {
        var line = (position + 1) / 5
        if ((position + 1) % 5 != 0) {
            line++
        }
        var location = line * 5
        if (location > totalItems) {
            location = totalItems
        }
        return location
    }

    /**
     * 计算面板的偏移量并创建一个假的类别
     * @param view 被点击的视图
     * @param item 被点击的项目
     * @return 假的类别
     */
    private fun getPanelData(view: View, item: CategoryModel): CategoryModel {
        val categoryModel = CategoryModel()
        categoryModel.remoteId = "-9999"
        categoryModel.remoteParentId = item.id.toString()
        categoryModel.remoteBookId = book

        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val params = view.layoutParams as MarginLayoutParams

        var leftDistanceWithMargin =
            location[0] + view.paddingLeft + params.leftMargin - view.width / 2

        if (PrefManager.uiRoundStyle && !isEditMode) {
            leftDistanceWithMargin -= view.width / 2
        }

        categoryModel.id = leftDistanceWithMargin.toLong()
        return categoryModel
    }

    /**
     * 加载分类数据
     */
    private fun loadData() {
        binding.statusPage.showLoading()

        launch {
            try {
                items.clear()
                val newData = CategoryAPI.list(book, type, "-1")

                // 如果数据为空，创建默认分类
                val collection = newData.ifEmpty {
                    val defaultCategoryModel = CategoryModel()
                    defaultCategoryModel.name = "其他"
                    listOf(defaultCategoryModel)
                }

                totalItems = collection.size
                items.addAll(collection)

                // 在编辑模式下，为一级分类添加"添加"按钮
                if (isEditMode) {
                    val addButton = adapter.createAddButtonItem(book, type, "-1")
                    items.add(addButton)
                    totalItems += 1
                }

                adapter.updateItems(items)
                binding.statusPage.showContent()
            } catch (e: Exception) {
                binding.statusPage.showError()
            }
        }
    }

    /**
     * 刷新数据
     */
    fun refreshData() {
        if (book.isNotEmpty()) {
            loadData()
        }
    }

    /**
     * 获取当前选中的分类
     * @return Pair<父分类, 子分类>
     */
    fun getSelectedCategories(): Pair<CategoryModel?, CategoryModel?> {
        return Pair(categoryModel1, categoryModel2)
    }

    /**
     * 获取当前数据列表
     */
    fun getDataItems(): List<CategoryModel> = items.toList()

    /**
     * 清除选择
     */
    fun clearSelection() {
        categoryModel1 = null
        categoryModel2 = null
        if (expand && lastPosition != -1) {
            val panelPosition = getPanelIndex(lastPosition)
            if (panelPosition < items.size) {
                items.removeAt(panelPosition)
                adapter.updateItems(items)
            }
            expand = false
            lastPosition = -1
        }
    }

} 