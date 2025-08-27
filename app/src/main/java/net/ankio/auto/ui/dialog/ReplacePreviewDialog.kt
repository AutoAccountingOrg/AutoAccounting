/*
 * Copyright (C) 2024 ankio(ankio@ankio.net)
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

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import net.ankio.auto.R
import net.ankio.auto.databinding.DialogReplacePreviewBinding
import net.ankio.auto.ui.adapter.ReplacePreviewAdapter
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.model.ReplaceItem
import net.ankio.auto.ui.utils.Desensitizer
import net.ankio.auto.ui.utils.DesensitizeResult

/**
 * 替换预览对话框
 * 提供替换项的预览、选择和确认功能
 *
 * 设计原则（遵循Linus好品味）：
 * 1. 简洁构造：无参数构造函数
 * 2. 链式配置：通过链式调用设置数据和回调
 * 3. 消除特殊情况：统一的数据处理逻辑
 * 4. 清晰职责：只负责UI交互，不处理业务逻辑
 */
class ReplacePreviewDialog internal constructor(
    context: android.content.Context
) : BaseSheetDialog<DialogReplacePreviewBinding>(context) {

    private lateinit var adapter: ReplacePreviewAdapter
    private var replaceItems = mutableListOf<ReplaceItem>()
    private var onConfirm: ((List<ReplaceItem>) -> Unit)? = null

    /**
     * 设置脱敏结果数据
     * @param result 脱敏结果，包含原始变更记录
     * @return 当前对话框实例，支持链式调用
     */
    fun setDesensitizeResult(result: DesensitizeResult) = apply {
        // 将脱敏结果转换为替换项列表
        replaceItems.clear()
        replaceItems.addAll(
            result.changes.map { (from, to) ->
                ReplaceItem(from, to)
            }
        )

        // 如果适配器已初始化，更新数据
        if (::adapter.isInitialized) {
            adapter.submitItems(replaceItems)
            updateItemCount()
        }
    }

    /**
     * 设置确认回调
     * @param callback 确认回调，参数为选中的替换项列表
     * @return 当前对话框实例，支持链式调用
     */
    fun setOnConfirm(callback: (List<ReplaceItem>) -> Unit) = apply {
        this.onConfirm = callback
    }

    override fun onViewCreated(view: View?) {
        super.onViewCreated(view)
        setupRecyclerView()
        setupButtons()

        // 设置初始数据
        adapter.submitItems(replaceItems)
        updateItemCount()
    }

    /**
     * 设置RecyclerView
     */
    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        (binding.recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

        adapter = ReplacePreviewAdapter()
            .setOnItemDeleteListener { item, position ->
                // 删除替换项
                replaceItems.removeAt(position)
                adapter.removeItem(position)
                updateItemCount()

                // 如果没有项目了，关闭对话框
                if (replaceItems.isEmpty()) {
                    dismiss()
                }
            }

        binding.recyclerView.adapter = adapter
    }

    /**
     * 设置操作按钮
     */
    private fun setupButtons() {
        // 取消按钮
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        // 确认替换按钮
        binding.btnConfirm.setOnClickListener {
            // 直接处理所有剩余项目
            if (replaceItems.isNotEmpty()) {
                onConfirm?.invoke(replaceItems)
            }
            dismiss()
        }
    }

    /**
     * 更新项目数量显示
     */
    private fun updateItemCount() {
        val totalCount = replaceItems.size

        // 更新项目数量文本
        binding.tvItemCount.text = context.getString(
            R.string.item_count_format,
            totalCount
        )

        // 更新确认按钮状态
        binding.btnConfirm.isEnabled = totalCount > 0
    }
}
