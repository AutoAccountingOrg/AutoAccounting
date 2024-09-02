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

package net.ankio.auto.ui.dialog

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.SimpleItemAnimator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.databinding.DialogCategorySelectBinding
import net.ankio.auto.storage.SpUtils
import net.ankio.auto.ui.adapter.CategorySelectorAdapter
import net.ankio.auto.ui.componets.StatusPage
import org.ezbook.server.constant.BillType
import org.ezbook.server.db.model.CategoryModel

/**
 * 这个类表示一个用于选择类别的对话框。
 * 它扩展了BaseSheetDialog类。
 *
 * @property context 对话框要显示的上下文。
 * @property book 书籍ID。
 * @property type 账单类型。
 * @property callback 当选择一个类别时要调用的函数。
 */
class CategorySelectorDialog(
    private val context: Context,
    private var book: String = "",
    private val type: BillType = BillType.Expend,
    private val callback: (CategoryModel?, CategoryModel?) -> Unit,
) : BaseSheetDialog(context) {
    // 父类别
    private var categoryModel1: CategoryModel? = null

    // 子类别
    private var categoryModel2: CategoryModel? = null

    // 显示状态
    private var expand = false

    // 默认一行的项目数
    private var line = 5

    // 对话框的绑定对象
    private lateinit var binding: DialogCategorySelectBinding

    // 类别列表
    private var items = mutableListOf<CategoryModel>()
    private var totalItems = 0

    /**
     * 这个类提供了一种查找给定项目的跨度大小的方法。
     * 它扩展了SpanSizeLookup类。
     */
    internal inner class SpecialSpanSizeLookup : SpanSizeLookup() {
        /**
         * 这个方法返回给定位置的项目的跨度大小。
         *
         * @param i 项目的位置。
         * @return 项目的跨度大小。
         */
        override fun getSpanSize(i: Int): Int {
            val categoryModel: CategoryModel = items[i]
            return if (categoryModel.isPanel()) 5 else 1
        }
    }

    // 最后点击的位置
    private var lastPosition = -1

    // RecyclerView的适配器
    // private lateinit var adapter: CategorySelectorAdapter

    /**
     * 这个方法计算插入面板的索引。
     *
     * @param position 被点击的项目的位置。
     * @return 插入面板的索引。
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
     * 这个方法计算面板的偏移量并创建一个假的类别。
     *
     * @param view 被点击的视图。
     * @param item 被点击的项目。
     * @return 假的类别。
     */
    private fun getPanelData(
        view: View,
        item: CategoryModel,
    ): CategoryModel {
        val categoryModel = CategoryModel()
        categoryModel.remoteId = "-9999"
        categoryModel.remoteParentId = item.id.toString()
        categoryModel.remoteBookId = book
        /* categoryModel.type = type.value*/
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val params = view.layoutParams as MarginLayoutParams

        var leftDistanceWithMargin = location[0] + view.paddingLeft + params.leftMargin - view.width/2

        if (SpUtils.getBoolean("setting_use_round_style",false)){
            leftDistanceWithMargin-=view.width/2
        }

        categoryModel.id = leftDistanceWithMargin.toLong()
        return categoryModel
    }

    private lateinit var statusPage: StatusPage

    private lateinit var adapter: CategorySelectorAdapter

    /**
     * 这个方法为对话框填充视图。
     *
     * @param inflater 可以用来填充对话框中任何视图的LayoutInflater对象。
     * @return 对话框的根视图。
     */
    override fun onCreateView(inflater: LayoutInflater): View {
        // 为对话框填充布局
        binding = DialogCategorySelectBinding.inflate(inflater)

        // 设置卡片视图
        this.cardView = binding.cardView
        cardViewInner = binding.cardViewInner
        statusPage = binding.statusPage
        // 为RecyclerView设置布局管理器
        val layoutManager = GridLayoutManager(context, line)
        layoutManager.spanSizeLookup = SpecialSpanSizeLookup()
        val recyclerView = binding.statusPage.contentView!!
        recyclerView.layoutManager = layoutManager
        (recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        expand = false
        // 为RecyclerView设置适配器
        adapter =
            CategorySelectorAdapter(
                items,
                onItemClick = { item, pos, hasChild, view ->
                    categoryModel1 = item
                    val panelPosition = getPanelIndex(pos) // 在当前位置，面板应该插入到哪里。

                    val lastPanelPosition = getPanelIndex(lastPosition) // 在上一个位置，面板在那里

                    if (pos == lastPosition) { // 两次点击同一个，收起面板
                        if (hasChild) {
                            // 点击两次，说明需要删除
                            items.removeAt(panelPosition)
                            // 计算位置的面板删除
                            adapter.notifyItemRemoved(panelPosition)
                            lastPosition = -1 // 归位
                            expand = false
                            categoryModel2 = null
                            return@CategorySelectorAdapter
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
                                adapter.notifyItemChanged(panelPosition, category)
                            } else {
                                items.add(panelPosition, category)
                                adapter.notifyItemInserted(panelPosition)
                                expand = true
                            }
                        } else {
                            // 没有就移除
                            if (lastPosition != -1 && expand) {
                                items.removeAt(lastPanelPosition)
                                adapter.notifyItemRemoved(lastPanelPosition)
                                lastPosition = -1 // 归位
                                expand = false
                                return@CategorySelectorAdapter
                            }
                        }
                    } else {
                        // 不同行的需要先删除
                        if (lastPosition != -1 && expand) {
                            items.removeAt(lastPanelPosition)
                            adapter.notifyItemRemoved(lastPanelPosition)
                            expand = false
                        }
                        if (hasChild) {
                            items.add(panelPosition, category)
                            adapter.notifyItemInserted(panelPosition)
                            expand = true
                        }
                    }
                    lastPosition = pos
                },
                onItemChildClick = { item, _ ->
                    categoryModel2 = item
                },
            )

        // 为RecyclerView设置适配器
        recyclerView.adapter = adapter
        // 为按钮设置点击监听器
        binding.button.setOnClickListener {
            // 当按钮被点击时，调用回调函数
            if (categoryModel1 != null) {
                callback(categoryModel1, categoryModel2)
            }
            // 关闭对话框
            dismiss()
        }

        // 从数据库加载类别
        lifecycleScope.launch {
            statusPage.showLoading()
            loadData()
        }
        return binding.root
    }

    private suspend fun loadData() = withContext(Dispatchers.IO) {
        val newData = CategoryModel.list(book, type, "-1")
        val defaultCategoryModel = CategoryModel()
        defaultCategoryModel.name = "其他"
        val collection =
            newData.map { it }.takeIf { it.isNotEmpty() } ?: listOf(defaultCategoryModel)
        totalItems = collection.size
        withContext(Dispatchers.Main) {
            items.addAll(collection)
            // 在主线程更新 UI
            adapter.notifyItemInserted(0)
            statusPage.showContent()
        }
    }
}
