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
 *  limitations under the License.
 */

package net.ankio.auto.ui.adapter

import android.view.View
import androidx.core.content.ContextCompat
import net.ankio.auto.R
import net.ankio.auto.ui.theme.DynamicColors
import net.ankio.auto.databinding.ItemAnalysisTaskBinding
import org.ezbook.server.constant.AnalysisTaskStatus
import org.ezbook.server.db.model.AnalysisTaskModel
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import android.content.Context
import net.ankio.auto.utils.DateUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * AI分析任务列表适配器
 */
class AnalysisTaskAdapter : BaseAdapter<ItemAnalysisTaskBinding, AnalysisTaskModel>() {

    private var onItemClickListener: ((AnalysisTaskModel) -> Unit)? = null
    private var onItemLongClickListener: ((AnalysisTaskModel) -> Unit)? = null

    /**
     * 设置点击监听器
     */
    fun setOnItemClickListener(listener: (AnalysisTaskModel) -> Unit) = apply {
        onItemClickListener = listener
    }

    /**
     * 设置长按监听器
     */
    fun setOnItemLongClickListener(listener: (AnalysisTaskModel) -> Unit) = apply {
        onItemLongClickListener = listener
    }

    override fun onInitViewHolder(holder: BaseViewHolder<ItemAnalysisTaskBinding, AnalysisTaskModel>) {
        val binding = holder.binding

        // 设置点击事件
        binding.root.setOnClickListener {
            val item = holder.item
            if (item != null && item.status == AnalysisTaskStatus.COMPLETED) {
                onItemClickListener?.invoke(item)
            }
        }

        // 设置长按事件
        binding.root.setOnLongClickListener {
            val item = holder.item
            if (item != null) {
                onItemLongClickListener?.invoke(item)
            }
            true
        }
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder<ItemAnalysisTaskBinding, AnalysisTaskModel>,
        data: AnalysisTaskModel,
        position: Int
    ) {
        val binding = holder.binding
        binding.root.setCardBackgroundColor(DynamicColors.SurfaceColor3)
        // 设置任务标题
        binding.titleText.text = data.title

        // 设置时间范围二级标题
        binding.timeRangeText.text =
            DateUtils.formatTimeRange(binding.root.context, data.startTime, data.endTime)

        // 设置创建时间
        val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        binding.createTimeText.text = dateFormat.format(Date(data.createTime))

        // 设置状态
        setupStatus(binding, data)

        // 设置进度区域（整体显示或隐藏）
        setupProgress(binding, data)

        // 设置错误信息
        setupError(binding, data)

        // 根据状态设置点击效果
        binding.root.isClickable = data.status == AnalysisTaskStatus.COMPLETED
        binding.root.alpha = if (data.status == AnalysisTaskStatus.COMPLETED) 1.0f else 0.7f
    }

    /**
     * 设置进度区域
     */
    private fun setupProgress(binding: ItemAnalysisTaskBinding, data: AnalysisTaskModel) {
        if (data.status == AnalysisTaskStatus.PROCESSING) {
            binding.progressContainer.visibility = View.VISIBLE
            binding.progressBar.progress = data.progress
            binding.progressText.text = "${data.progress}%"
            binding.progressText.setTextColor(DynamicColors.Primary)
        } else {
            binding.progressContainer.visibility = View.GONE
        }
    }

    /**
     * 设置错误信息
     */
    private fun setupError(binding: ItemAnalysisTaskBinding, data: AnalysisTaskModel) {
        if (data.status == AnalysisTaskStatus.FAILED && !data.errorMessage.isNullOrBlank()) {
            binding.errorText.visibility = View.VISIBLE
            binding.errorText.text = data.errorMessage
        } else {
            binding.errorText.visibility = View.GONE
        }
    }

    /**
     * 设置状态显示
     */
    private fun setupStatus(binding: ItemAnalysisTaskBinding, task: AnalysisTaskModel) {
        val context = binding.root.context

        val (iconRes, textRes, color) = when (task.status) {
            AnalysisTaskStatus.PENDING -> Triple(
                R.drawable.ic_auto_schedule,
                R.string.analysis_status_pending,
                DynamicColors.OnSurfaceVariant
            )

            AnalysisTaskStatus.PROCESSING -> Triple(
                R.drawable.ic_autorenew,
                R.string.analysis_status_processing,
                DynamicColors.Primary
            )

            AnalysisTaskStatus.COMPLETED -> Triple(
                R.drawable.ic_check_circle,
                R.string.analysis_status_completed,
                DynamicColors.Primary
            )

            AnalysisTaskStatus.FAILED -> Triple(
                R.drawable.ic_error,
                R.string.analysis_status_failed,
                DynamicColors.Error
            )
        }

        binding.statusIconView.setIcon(ContextCompat.getDrawable(context, iconRes))
        binding.statusIconView.setText(context.getString(textRes))
        binding.statusIconView.setColor(color)
    }

    override fun areItemsSame(oldItem: AnalysisTaskModel, newItem: AnalysisTaskModel): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsSame(oldItem: AnalysisTaskModel, newItem: AnalysisTaskModel): Boolean {
        return oldItem == newItem
    }


}