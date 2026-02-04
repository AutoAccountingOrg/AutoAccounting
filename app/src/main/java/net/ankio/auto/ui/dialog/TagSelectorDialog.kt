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

package net.ankio.auto.ui.dialog

import android.view.View
import net.ankio.auto.databinding.DialogTagSelectBinding
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.api.bindAs
import net.ankio.auto.ui.dialog.components.BillTagComponent
import org.ezbook.server.db.model.BillInfoModel

/**
 * 标签选择对话框
 *
 * 职责：
 * - 以对话框形式选择账单标签
 * - 支持多选并限制数量
 * - 在确认时一次性回调选择结果
 */
class TagSelectorDialog internal constructor(
    context: android.content.Context
) : BaseSheetDialog<DialogTagSelectBinding>(context) {

    private lateinit var tagComponent: BillTagComponent
    private val tagHolder = BillInfoModel()
    private var callback: ((List<String>) -> Unit)? = null

    /**
     * 设置已选标签
     * @param tags 标签名称列表
     */
    fun setSelectedTags(tags: List<String>) = apply {
        tagHolder.setTagList(tags)
        if (::tagComponent.isInitialized) {
            tagComponent.setBillInfo(tagHolder)
        }
    }

    /**
     * 设置选择回调
     * @param callback 返回选择的标签名称列表
     */
    fun setCallback(callback: (List<String>) -> Unit) = apply {
        this.callback = callback
    }

    override fun onViewCreated(view: View?) {
        super.onViewCreated(view)

        tagComponent = binding.billTag.bindAs()
        tagComponent.setBillInfo(tagHolder)

        binding.button.setOnClickListener {
            callback?.invoke(tagHolder.getTagList())
            dismiss()
        }
    }
}
