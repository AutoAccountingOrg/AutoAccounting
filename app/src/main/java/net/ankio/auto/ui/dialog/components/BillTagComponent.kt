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

package net.ankio.auto.ui.dialog.components

import android.view.View
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.flexbox.FlexboxLayoutManager
import net.ankio.auto.R
import net.ankio.auto.databinding.ComponentBillTagBinding
import net.ankio.auto.http.api.TagAPI
import net.ankio.auto.ui.adapter.TagSelectorAdapter
import net.ankio.auto.ui.api.BaseComponent
import net.ankio.auto.ui.utils.ToastUtils
import org.ezbook.server.db.model.BillInfoModel
import org.ezbook.server.db.model.TagModel

/**
 * 账单标签选择组件
 *
 * 职责：
 * - 展示可选标签并支持多选
 * - 限制最大选择数量，避免特殊情况
 * - 将选中结果回写到账单模型
 */
class BillTagComponent(
    binding: ComponentBillTagBinding
) : BaseComponent<ComponentBillTagBinding>(binding) {

    companion object {
        private const val MAX_TAG_COUNT = 8
        private const val GROUP_MARKER = "-1"
        private const val DEFAULT_GROUP_NAME = "其他"
    }

    private lateinit var billInfoModel: BillInfoModel
    private lateinit var adapter: TagSelectorAdapter
    private var tagItems: List<TagModel> = emptyList()
    private var pendingTagNames: List<String> = emptyList()

    override fun onComponentCreate() {
        super.onComponentCreate()
        setupRecyclerView()
        // 默认显示加载状态，避免空白闪烁
        binding.statusPage.showLoading()
        loadTags()
    }

    /**
     * 设置账单信息
     * @param billInfoModel 账单信息模型
     */
    fun setBillInfo(billInfoModel: BillInfoModel) {
        this.billInfoModel = billInfoModel
        refreshSelection()
    }

    /**
     * 设置RecyclerView
     */
    private fun setupRecyclerView() {
        val recyclerView = binding.statusPage.contentView ?: return
        recyclerView.layoutManager = FlexboxLayoutManager(context)
        (recyclerView.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        adapter = TagSelectorAdapter(
            onTagClick = { _, _ -> },
            isEditMode = false,
            selectionLimit = MAX_TAG_COUNT,
            onSelectionChanged = { selected ->
                updateBillTags(selected)
            },
            onSelectionLimitReached = { limit ->
                ToastUtils.info(context.getString(R.string.bill_tag_limit, limit))
            }
        )
        recyclerView.adapter = adapter
    }

    /**
     * 加载标签数据并构建分组列表
     */
    private fun loadTags() {
        // 刷新加载状态，保持用户感知一致
        binding.statusPage.showLoading()
        launch {
            try {
                val allTags = TagAPI.all()
                val billTags = getCurrentTagNames()
                val mergedTags = mergeUnknownTags(allTags, billTags)
                tagItems = buildGroupedItems(mergedTags)
                adapter.updateItems(tagItems)
                refreshSelection()
                // 加载完成后更新空态/列表
                if (mergedTags.isEmpty()) {
                    binding.statusPage.showEmpty()
                } else {
                    binding.statusPage.showContent()
                }
            } catch (e: Exception) {
                // 加载失败时仅展示空态，避免卡在加载
                adapter.updateItems(emptyList())
                binding.statusPage.showEmpty()
            }
        }
    }

    /**
     * 获取当前账单标签名称列表
     */
    private fun getCurrentTagNames(): List<String> {
        if (::billInfoModel.isInitialized) {
            pendingTagNames = billInfoModel.getTagList()
        }
        return pendingTagNames
    }

    /**
     * 合并账单中存在但标签库不存在的标签，避免数据丢失
     * @param allTags 标签库内的标签
     * @param billTags 账单已关联的标签名称
     */
    private fun mergeUnknownTags(allTags: List<TagModel>, billTags: List<String>): List<TagModel> {
        val knownNames = allTags.map { it.name }.toSet()
        val unknownTags = billTags
            .filter { it.isNotEmpty() && !knownNames.contains(it) }
            .map { name ->
                TagModel().apply {
                    this.name = name
                    this.group = DEFAULT_GROUP_NAME
                }
            }
        return (allTags + unknownTags).distinctBy { it.name }
    }

    /**
     * 构建分组后的列表结构：分组头 + 标签项
     * @param tags 原始标签列表
     */
    private fun buildGroupedItems(tags: List<TagModel>): List<TagModel> {
        val grouped = tags.groupBy { it.group.ifEmpty { DEFAULT_GROUP_NAME } }
        return grouped.flatMap { (groupName, tagList) ->
            buildList {
                add(
                    TagModel().apply {
                        group = GROUP_MARKER
                        name = groupName
                    }
                )
                addAll(tagList)
            }
        }
    }

    /**
     * 刷新选择状态，确保 UI 与账单数据一致
     */
    private fun refreshSelection() {
        if (!::billInfoModel.isInitialized || !::adapter.isInitialized) return
        val selectedNames = billInfoModel.getTagList().toSet()
        val selectableTags = tagItems.filter { it.group != GROUP_MARKER }
        val selectedTags = selectableTags.filter { selectedNames.contains(it.name) }.toSet()
        adapter.setSelectedTags(selectedTags)
    }

    /**
     * 更新账单标签内容
     * @param selectedTags 选中的标签列表
     */
    private fun updateBillTags(selectedTags: List<TagModel>) {
        if (!::billInfoModel.isInitialized) return
        val names = selectedTags.map { it.name }.distinct()
        billInfoModel.setTagList(names)
        pendingTagNames = names
    }
}
