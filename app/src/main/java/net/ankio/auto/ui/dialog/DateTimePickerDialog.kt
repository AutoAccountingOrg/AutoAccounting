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

import android.app.Activity
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import net.ankio.auto.databinding.CustomDateTimePickerBinding
import net.ankio.auto.ui.api.BaseSheetDialog
import java.util.Calendar
import kotlin.math.min

/**
 * 日期时间选择器对话框
 *
 * 支持以下功能：
 * - 年月日时分选择
 * - 仅时间选择模式
 * - 自定义标题
 * - 自动处理月份天数变化（闰年等）
 * - 生命周期安全管理
 *
 * 使用方式：
 * ```kotlin
 * DateTimePickerDialog.create(activity)
 *     .setTimeOnly(true)
 *     .setTitle("选择时间")
 *     .setOnDateTimeSelected { year, month, day, hour, minute -> }
 *     .show()
 * ```
 */
class DateTimePickerDialog internal constructor(
    context: android.content.Context
) : BaseSheetDialog<CustomDateTimePickerBinding>(context) {

    // 配置参数
    private var timeOnly: Boolean = false

    // 仅选择年月模式：隐藏日、时、分，仅保留年、月选择
    private var yearMonthOnly: Boolean = false
    private var title: String? = null

    // 回调函数
    private var onDateTimeSelectedListener: ((year: Int, month: Int, day: Int, hour: Int, minute: Int) -> Unit)? =
        null

    // 当前选中的日期时间
    private var currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private var currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
    private var currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    private var currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    private var currentMinute = Calendar.getInstance().get(Calendar.MINUTE)

    /**
     * 设置是否仅显示时间选择器
     * @param timeOnly true表示仅时间，false表示日期+时间
     * @return 当前对话框实例，支持链式调用
     */
    fun setTimeOnly(timeOnly: Boolean) = apply {
        this.timeOnly = timeOnly
    }

    /**
     * 设置是否仅选择 年+月（隐藏日/时/分），用于月份筛选等场景
     */
    fun setYearMonthOnly(yearMonthOnly: Boolean) = apply {
        this.yearMonthOnly = yearMonthOnly
    }

    /**
     * 设置对话框标题
     * @param title 标题文本，null表示不显示标题
     * @return 当前对话框实例，支持链式调用
     */
    fun setTitle(title: String?) = apply {
        this.title = title
    }

    /**
     * 设置日期时间选择回调
     * @param listener 选择后的回调函数
     * @return 当前对话框实例，支持链式调用
     */
    fun setOnDateTimeSelected(listener: (year: Int, month: Int, day: Int, hour: Int, minute: Int) -> Unit) =
        apply {
            this.onDateTimeSelectedListener = listener
    }

    /**
     * 设置初始日期时间
     * @param year 年份
     * @param month 月份（1-12）
     * @param day 日期
     * @param hour 小时（0-23）
     * @param minute 分钟（0-59）
     * @return 当前对话框实例，支持链式调用
     */
    fun setDateTime(year: Int, month: Int, day: Int, hour: Int, minute: Int) = apply {
        this.currentYear = year
        this.currentMonth = month
        this.currentDay = day
        this.currentHour = hour
        this.currentMinute = minute
    }

    override fun onViewCreated(view: View?) {
        super.onViewCreated(view)

        // 设置标题
        title?.let {
            binding.dialogTitle.text = it
            binding.dialogTitle.visibility = View.VISIBLE
        } ?: run {
            binding.dialogTitle.visibility = View.GONE
        }

        // 仅时间模式：隐藏日期
        if (timeOnly) {
            binding.llDateTitle.visibility = View.GONE
            binding.llDate.visibility = View.GONE
        }

        // 年月模式：隐藏日、时、分，仅保留年与月
        if (yearMonthOnly) {
            // 显示日期容器，但仅展示年/月，隐藏日与时间容器
            binding.llDateTitle.visibility = View.VISIBLE
            binding.llDate.visibility = View.VISIBLE

            // 隐藏“日”标题与选择器
            binding.tvDay.visibility = View.GONE
            binding.npDay.visibility = View.GONE
            binding.lTimeSelect.visibility = View.GONE
            binding.tvTime.visibility = View.GONE

        }

        // 初始化选择器
        setupPickers()

        // 设置确认按钮
        binding.positiveButton.setOnClickListener {
            onDateTimeSelectedListener?.invoke(
                currentYear,
                currentMonth,
                currentDay,
                currentHour,
                currentMinute
            )
            dismiss()
        }

        // 设置取消按钮
        binding.negativeButton.setOnClickListener {
            dismiss()
        }
    }

    /**
     * 初始化所有选择器
     */
    private fun setupPickers() {
        if (!timeOnly) {
            setupDatePickers()
        }
        if (!yearMonthOnly) {
            setupTimePickers()
        }
    }

    /**
     * 设置日期选择器
     */
    private fun setupDatePickers() {
        // 年份选择器（1900-2100）
        binding.npYear.apply {
            minValue = 1900
            maxValue = 2100
            value = currentYear
            setOnValueChangedListener { _, _, newVal ->
                currentYear = newVal
                updateDayPicker()
            }
        }

        // 月份选择器（1-12）
        binding.npMonth.apply {
            minValue = 1
            maxValue = 12
            value = currentMonth
            setOnValueChangedListener { _, _, newVal ->
                currentMonth = newVal
                updateDayPicker()
            }
        }

        // 日期选择器
        updateDayPicker()
    }

    /**
     * 设置时间选择器
     */
    private fun setupTimePickers() {
        // 小时选择器（0-23）
        binding.npHour.apply {
            minValue = 0
            maxValue = 23
            value = currentHour
            setFormatter { String.format("%02d", it) }
            setOnValueChangedListener { _, _, newVal ->
                currentHour = newVal
            }
        }

        // 分钟选择器（0-59）
        binding.npMinute.apply {
            minValue = 0
            maxValue = 59
            value = currentMinute
            setFormatter { String.format("%02d", it) }
            setOnValueChangedListener { _, _, newVal ->
                currentMinute = newVal
            }
        }
    }

    /**
     * 更新日期选择器的最大值
     * 根据当前年月计算该月的最大天数
     */
    private fun updateDayPicker() {
        val maxDay = getMaxDayOfMonth(currentYear, currentMonth)
        
        binding.npDay.apply {
            minValue = 1
            maxValue = maxDay

            // 如果当前选中的日期超过了新的最大值，调整为最大值
            if (currentDay > maxDay) {
                currentDay = maxDay
            }

            value = currentDay

            setOnValueChangedListener { _, _, newVal ->
                currentDay = newVal
            }
        }
    }

    /**
     * 获取指定年月的最大天数
     * @param year 年份
     * @param month 月份（1-12）
     * @return 该月的最大天数
     */
    private fun getMaxDayOfMonth(year: Int, month: Int): Int {
        return when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if (isLeapYear(year)) 29 else 28
            else -> 30
        }
    }

    /**
     * 判断是否为闰年
     * @param year 年份
     * @return true表示闰年，false表示平年
     */
    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    /**
     * 获取当前选中的时间戳
     * @return 时间戳（毫秒）
     */
    fun getSelectedTimeInMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(currentYear, currentMonth - 1, currentDay, currentHour, currentMinute, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * 从时间戳设置日期时间
     * @param timeInMillis 时间戳（毫秒）
     */
    fun setDateTimeFromMillis(timeInMillis: Long) = apply {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeInMillis

        currentYear = calendar.get(Calendar.YEAR)
        currentMonth = calendar.get(Calendar.MONTH) + 1
        currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        currentMinute = calendar.get(Calendar.MINUTE)
    }



}