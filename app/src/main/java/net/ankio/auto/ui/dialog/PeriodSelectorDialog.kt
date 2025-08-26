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

import android.app.Activity
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import net.ankio.auto.databinding.DialogPeriodSelectorBinding
import net.ankio.auto.ui.api.BaseActivity
import net.ankio.auto.ui.api.BaseSheetDialog
import java.util.*

/**
 * 周期选择对话框
 *
 * 用于选择AI分析的时间周期，支持多种预设周期选项
 *
 * 使用方式：
 * ```kotlin
 * PeriodSelectorDialog.create(activity)
 *     .setOnPeriodSelected { period ->
 *         // 处理选择结果
 *     }
 *     .show()
 * ```
 */
class PeriodSelectorDialog private constructor(
    context: android.content.Context,
    lifecycleOwner: LifecycleOwner?,
    isOverlay: Boolean
) : BaseSheetDialog<DialogPeriodSelectorBinding>(context, lifecycleOwner, isOverlay) {

    /**
     * 周期类型枚举
     */
    enum class Period {
        LAST_WEEK,      // 最近一周
        LAST_30_DAYS,   // 最近30天
        THIS_MONTH,     // 这个月
        LAST_YEAR,      // 最近一年
        THIS_YEAR       // 今年
    }

    /**
     * 周期数据类
     */
    data class PeriodData(
        val type: Period,
        val startTime: Long,
        val endTime: Long,
        val displayName: String
    )

    private var onPeriodSelected: ((PeriodData) -> Unit)? = null
    private var selectedPeriod: Period = Period.LAST_30_DAYS // 默认选择最近30天

    companion object {
        /**
         * 创建对话框实例 - Activity环境
         */
        fun create(activity: BaseActivity): PeriodSelectorDialog {
            return PeriodSelectorDialog(activity, activity, false)
        }

        /**
         * 创建对话框实例 - Fragment环境
         */
        fun create(fragment: Fragment): PeriodSelectorDialog {
            return PeriodSelectorDialog(fragment.requireContext(), fragment, false)
        }

        /**
         * 创建对话框实例 - Service环境（悬浮窗）
         */
        fun create(service: LifecycleService): PeriodSelectorDialog {
            return PeriodSelectorDialog(service, service, true)
        }
    }

    override fun onViewCreated(view: View?) {
        super.onViewCreated(view)
        setupClickListeners()
        updateSelectedState() // 设置默认选择状态
    }

    /**
     * 设置周期选择回调
     */
    fun setOnPeriodSelected(callback: (PeriodData) -> Unit): PeriodSelectorDialog {
        onPeriodSelected = callback
        return this
    }

    /**
     * 设置点击事件
     */
    private fun setupClickListeners() {
        binding.btnLastWeek.setOnClickListener {
            selectPeriod(Period.LAST_WEEK)
        }

        binding.btnLast30Days.setOnClickListener {
            selectPeriod(Period.LAST_30_DAYS)
        }

        binding.btnThisMonth.setOnClickListener {
            selectPeriod(Period.THIS_MONTH)
        }

        binding.btnLastYear.setOnClickListener {
            selectPeriod(Period.LAST_YEAR)
        }

        binding.btnThisYear.setOnClickListener {
            selectPeriod(Period.THIS_YEAR)
        }
    }

    /**
     * 更新选择状态
     */
    private fun updateSelectedState() {
        // 重置所有按钮状态
        listOf(
            binding.btnLastWeek,
            binding.btnLast30Days,
            binding.btnThisMonth,
            binding.btnLastYear,
            binding.btnThisYear
        ).forEach { button ->
            button.isSelected = false
        }

        // 设置选中状态
        when (selectedPeriod) {
            Period.LAST_WEEK -> binding.btnLastWeek.isSelected = true
            Period.LAST_30_DAYS -> binding.btnLast30Days.isSelected = true
            Period.THIS_MONTH -> binding.btnThisMonth.isSelected = true
            Period.LAST_YEAR -> binding.btnLastYear.isSelected = true
            Period.THIS_YEAR -> binding.btnThisYear.isSelected = true
        }
    }

    /**
     * 处理周期选择
     */
    private fun selectPeriod(period: Period) {
        selectedPeriod = period
        updateSelectedState()
        val periodData = calculatePeriodData(period)
        onPeriodSelected?.invoke(periodData)
        dismiss()
    }

    /**
     * 计算周期的时间范围
     */
    private fun calculatePeriodData(period: Period): PeriodData {
        val calendar = Calendar.getInstance()
        val now = System.currentTimeMillis()

        return when (period) {
            Period.LAST_WEEK -> {
                // 最近7天
                calendar.add(Calendar.DAY_OF_YEAR, -6)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startTime = calendar.timeInMillis

                PeriodData(period, startTime, now, "最近一周")
            }

            Period.LAST_30_DAYS -> {
                // 最近30天
                calendar.add(Calendar.DAY_OF_YEAR, -29)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startTime = calendar.timeInMillis

                PeriodData(period, startTime, now, "最近30天")
            }

            Period.THIS_MONTH -> {
                // 本月1号到现在
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startTime = calendar.timeInMillis

                PeriodData(period, startTime, now, "这个月")
            }

            Period.LAST_YEAR -> {
                // 最近365天
                calendar.add(Calendar.DAY_OF_YEAR, -364)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startTime = calendar.timeInMillis

                PeriodData(period, startTime, now, "最近一年")
            }

            Period.THIS_YEAR -> {
                // 今年1月1号到现在
                calendar.set(Calendar.MONTH, Calendar.JANUARY)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startTime = calendar.timeInMillis

                PeriodData(period, startTime, now, "今年")
            }
        }
    }
}
