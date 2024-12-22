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

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import net.ankio.auto.databinding.CustomDateTimePickerBinding
import net.ankio.auto.ui.api.BaseSheetDialog
import java.util.Calendar
import kotlin.math.min

class DateTimePickerDialog(
    context: Context,
    private val timeOnly: Boolean = false,
    private val title: String? = null
) : BaseSheetDialog(context) {
    lateinit var binding: CustomDateTimePickerBinding
    private var onDateTimeSelectedListener: ((year: Int, month: Int, day: Int, hour: Int, minute: Int) -> Unit)? = null
    
    // 当前选中的日期时间
    private var currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private var currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
    private var currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    private var currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    private var currentMinute = Calendar.getInstance().get(Calendar.MINUTE)

    override fun onCreateView(inflater: LayoutInflater): View {
        binding = CustomDateTimePickerBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        
        // 设置标题
        title?.let {
            binding.dialogTitle.text = it
            binding.dialogTitle.visibility = View.VISIBLE
        } ?: run {
            binding.dialogTitle.visibility = View.GONE
        }
        
        if (timeOnly) {
            binding.llDate.visibility = View.GONE
            binding.llDateTitle.visibility = View.GONE
        }
        
        initializePickers()
        setupListeners()
    }

    private fun initializePickers() {
        // 设置年份范围
        binding.npYear.apply {
            minValue = 1900
            maxValue = 2100
            value = currentYear
        }

        // 设置月份范围
        binding.npMonth.apply {
            minValue = 1
            maxValue = 12
            value = currentMonth
        }

        // 设置日期范围
        updateDayPicker()

        // 设置小时范围
        binding.npHour.apply {
            minValue = 0
            maxValue = 23
            value = currentHour
        }

        // 设置分钟范围
        binding.npMinute.apply {
            minValue = 0
            maxValue = 59
            value = currentMinute
        }
    }

    private fun setupListeners() {
        // 月份变化时更新天数
        binding.npMonth.setOnValueChangedListener { _, _, newVal ->
            currentMonth = newVal
            updateDayPicker()
        }

        binding.npYear.setOnValueChangedListener { _, _, newVal ->
            currentYear = newVal
            updateDayPicker()
        }

        // 确定按钮点击事件
        binding.positiveButton.setOnClickListener {
            onDateTimeSelectedListener?.invoke(
                binding.npYear.value,
                binding.npMonth.value,
                binding.npDay.value,
                binding.npHour.value,
                binding.npMinute.value
            )
            dismiss()
        }

        // 取消按钮点击事件
        binding.negativeButton.setOnClickListener {
            dismiss()
        }
    }

    private fun updateDayPicker() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, currentYear)
        calendar.set(Calendar.MONTH, currentMonth - 1)
        
        val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        binding.npDay.apply {
            minValue = 1
            maxValue = maxDays
            // 确保当前选中的日期不超过该月的最大天数
            value = min(currentDay, maxDays)
        }
    }

    fun setOnDateTimeSelectedListener(listener: (year: Int, month: Int, day: Int, hour: Int, minute: Int) -> Unit) {
        onDateTimeSelectedListener = listener
    }

    // 设置初始日期时间的方法
    fun setDateTime(year: Int, month: Int, day: Int, hour: Int, minute: Int) {
        currentYear = year
        currentMonth = month
        currentDay = day
        currentHour = hour
        currentMinute = minute
        
        if (::binding.isInitialized) {
            binding.npYear.value = year
            binding.npMonth.value = month
            binding.npDay.value = day
            binding.npHour.value = hour
            binding.npMinute.value = minute
        }
    }

    companion object {
        // 创建当前时间的对话框
        fun withCurrentTime(
            context: Context, 
            timeOnly: Boolean = false,
            title: String? = null
        ): DateTimePickerDialog {
            return DateTimePickerDialog(context, timeOnly, title).apply {
                val calendar = Calendar.getInstance()
                setDateTime(
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.DAY_OF_MONTH),
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE)
                )
            }
        }
    }
}