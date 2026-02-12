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

import android.view.View
import androidx.core.view.isVisible
import com.google.android.material.elevation.SurfaceColors
import net.ankio.auto.R
import net.ankio.auto.databinding.AdapterDataBinding
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import net.ankio.auto.ui.theme.DynamicColors
import net.ankio.auto.ui.utils.load
import net.ankio.auto.utils.DateUtils
import net.ankio.auto.utils.getAppInfoFromPackageName
import org.ezbook.server.constant.DataType
import org.ezbook.server.db.model.AppDataModel

class AppDataAdapter : BaseAdapter<AdapterDataBinding, AppDataModel>() {

    companion object {
        private val RULE_NAME_REGEX = "\\[(.*?)]".toRegex()
    }

    // 事件监听器 - 让外部处理业务逻辑
    var onTestRuleClick: ((AppDataModel) -> Unit)? = null
    var onTestRuleLongClick: ((AppDataModel) -> Unit)? = null
    var onContentClick: ((AppDataModel) -> Unit)? = null

    /** 图片内容点击，仅当 data.image 非空时触发 */
    var onImageClick: ((AppDataModel) -> Unit)? = null
    var onCreateRuleClick: ((AppDataModel) -> Unit)? = null
    var onUploadDataClick: ((AppDataModel) -> Unit)? = null
    var onDeleteClick: ((AppDataModel) -> Unit)? = null

    /**
     * 从规则字符串中提取规则名称
     */
    private fun extractRuleName(rule: String): String {
        return RULE_NAME_REGEX.find(rule)?.destructured?.component1() ?: rule
    }

    override fun onInitViewHolder(holder: BaseViewHolder<AdapterDataBinding, AppDataModel>) {
        val binding = holder.binding
        val context = holder.context

        // 简单的事件转发 - 让外部处理具体逻辑
        binding.testRule.setOnClickListener {
            holder.item?.let { onTestRuleClick?.invoke(it) }
        }

        // 长按测试按钮：强制使用 AI 识别
        binding.testRule.setOnLongClickListener {
            holder.item?.let { onTestRuleLongClick?.invoke(it) }
            true
        }

        binding.content.setOnClickListener {
            holder.item?.let { onContentClick?.invoke(it) }
        }
        binding.contentImage.setOnClickListener {
            holder.item?.let { onImageClick?.invoke(it) }
        }

        binding.createRule.setOnClickListener {
            holder.item?.let { onCreateRuleClick?.invoke(it) }
        }

        binding.uploadData.setOnClickListener {
            holder.item?.let { item ->
                // 直接回调上传逻辑，不再限制 AI 生成规则
                onUploadDataClick?.invoke(item)
            }
        }

        binding.root.setOnLongClickListener {
            holder.item?.let { onDeleteClick?.invoke(it) }
            true
        }

        binding.groupCard.setCardBackgroundColor(DynamicColors.SurfaceColor1)
    }





    override fun onBindViewHolder(
        holder: BaseViewHolder<AdapterDataBinding, AppDataModel>,
        data: AppDataModel,
        position: Int
    ) {
        val binding = holder.binding
        val context = holder.context

        // 应用信息：图标和名称
        val appInfo = getAppInfoFromPackageName(data.app)
        binding.appIcon.setImageDrawable(appInfo?.icon)
        binding.appName.text = appInfo?.name ?: data.app

        // 时间（无图标）
        binding.time.text = DateUtils.stampToDate(data.time)

        // 数据类型badge
        binding.typeBadge.text = when (data.type) {
            DataType.NOTICE -> context.getString(R.string.data_type_notice)
            DataType.DATA -> context.getString(R.string.data_type_app)
            DataType.OCR -> context.getString(R.string.data_type_ocr)
        }

        // 内容：有图片时展示图片，否则展示文本
        val hasImage = data.image.isNotBlank()
        binding.content.isVisible = !hasImage
        binding.contentImage.isVisible = hasImage
        if (hasImage) {
            val imgSrc =
                if (data.image.startsWith("data:image")) data.image else "data:image/png;base64,${data.image}"
            binding.contentImage.load(imgSrc)
        } else {
            binding.content.text = data.data
        }

        // 规则相关UI状态
        val hasRule = data.isMatched()
        binding.ruleName.setText(if (hasRule) extractRuleName(data.rule) else "")
        binding.ruleName.visibility = if (hasRule) View.VISIBLE else View.INVISIBLE
        binding.uploadData.isVisible = true

        // 根据匹配状态设置按钮文案：有效匹配显示"反馈"，无效匹配显示"适配"
        binding.uploadData.setText(
            if (data.hasValidMatch()) R.string.btn_upload_feedback else R.string.btn_upload_adapter
        )
    }


    override fun areItemsSame(oldItem: AppDataModel, newItem: AppDataModel): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsSame(oldItem: AppDataModel, newItem: AppDataModel): Boolean {
        return oldItem == newItem
    }
}
