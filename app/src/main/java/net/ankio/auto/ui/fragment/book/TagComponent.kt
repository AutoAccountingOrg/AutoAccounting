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

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.flexbox.FlexboxLayoutManager
import kotlinx.coroutines.launch
import net.ankio.auto.databinding.ComponentTagBinding
import net.ankio.auto.http.api.TagAPI
import net.ankio.auto.ui.adapter.TagSelectorAdapter
import net.ankio.auto.ui.api.BaseComponent
import org.ezbook.server.db.model.TagModel

/**
 * 标签组件
 * 使用StatusPage+Adapter架构重构，支持分组显示
 *
 * @param binding 组件绑定
 * @param lifecycle 生命周期
 */
class TagComponent(binding: ComponentTagBinding, private val lifecycle: Lifecycle) :
    BaseComponent<ComponentTagBinding>(binding, lifecycle) {

    private var onTagSelected: ((TagModel, String) -> Unit)? = null
    private var onSelectionChanged: ((Set<TagModel>) -> Unit)? = null
    private var isEditMode: Boolean = false
    private lateinit var adapter: TagSelectorAdapter

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
        if (::adapter.isInitialized) {
            // 重新创建适配器以应用新的模式
            setupRecyclerView()
            lifecycle.coroutineScope.launch {
                refreshData()
            }
        }
    }

    override fun init() {
        super.init()
        setupRecyclerView()
        loadData()
    }


    /**
     * 设置RecyclerView
     */
    private fun setupRecyclerView() {
        binding.statusPage.swipeRefreshLayout?.setOnRefreshListener {
            refreshData()
            binding.statusPage.swipeRefreshLayout!!.isRefreshing = false
        }
        val recyclerView = binding.statusPage.contentView!!

        // 设置布局管理器，使用GridLayoutManager来处理分组和标签的不同布局
        val layoutManager = FlexboxLayoutManager(context)
        recyclerView.layoutManager = layoutManager
        (recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

        // 初始化适配器
        adapter = TagSelectorAdapter(
            onTagClick = { tag, action ->
                handleTagClick(tag, action)
            },
            isEditMode = isEditMode
        )

        recyclerView.adapter = adapter
    }

    /**
     * 加载标签数据
     */
    private fun loadData() {
        binding.statusPage.showLoading()
        
        lifecycle.coroutineScope.launch {
            try {
                // 拉取并分组（空组名→"其他"）
                val grouped = TagAPI.all().groupBy { it.group.ifEmpty { "其他" } }

                if (grouped.isEmpty()) {
                    adapter.updateItems(emptyList())
                    binding.statusPage.showEmpty()
                    return@launch
                }

// 构建 adapter 列表：每个分组先插入一条“分组头”，再跟分组内元素
                val adapterItems: List<TagModel> = grouped.flatMap { (key, valueList) ->
                    buildList {
                        add(TagModel().apply {
                            group = "-1"    // 你的“分组头”标记
                            name = key
                        })
                        addAll(valueList)
                    }
                }

                adapter.updateItems(adapterItems)
                binding.statusPage.showContent()

            } catch (e: Exception) {
                binding.statusPage.showError()
            }
        }
    }


    /**
     * 处理标签点击事件
     */
    private fun handleTagClick(tag: TagModel, action: String) {
        // 根据当前模式与动作类型分发一次性回调，避免重复触发导致二次导航
        when (action) {
            // 选择模式下，通知选中集合变化；编辑模式忽略此动作
            "select" -> {
                if (!isEditMode) {
                    onSelectionChanged?.invoke(adapter.getSelectedTags().toSet())
                }
            }

            // 编辑模式下，分发编辑/删除回调；选择模式下忽略
            "edit", "delete" -> {
                if (isEditMode) {
                    onTagSelected?.invoke(tag, action)
                }
            }
        }
    }









    /**
     * 刷新数据
     */
    fun refreshData() {
        loadData()
    }

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
            if (::adapter.isInitialized) {
                adapter.setSelectedTags(selectedTags)
            }
        }
    }

    /**
     * 获取选中的标签（仅选择模式有效）
     * @return 选中的标签集合
     */
    fun getSelectedTags(): Set<TagModel> {
        return adapter.getSelectedTags().toSet()
    }

    /**
     * 清空选择（仅选择模式有效）
     */
    fun clearSelection() {
        if (!isEditMode) {
            if (::adapter.isInitialized) {
                adapter.clearSelection()
            }
            onSelectionChanged?.invoke(emptySet())
        }
    }
}