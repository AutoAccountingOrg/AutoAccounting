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

package net.ankio.auto.ui.fragment

import android.os.Bundle
import android.view.View
import net.ankio.auto.databinding.FragmentStatisticBinding
import net.ankio.auto.http.api.BillAPI
import net.ankio.auto.storage.Logger
import org.ezbook.server.models.CategoryItemDto
import org.ezbook.server.models.SummaryDto
import org.ezbook.server.models.TrendDto
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.theme.DynamicColors
import net.ankio.auto.utils.BillTool
import net.ankio.auto.utils.CoroutineUtils.withIO
import net.ankio.auto.utils.CoroutineUtils.withMain
import net.ankio.auto.utils.PrefManager
import org.ezbook.server.constant.BillType
import androidx.core.content.ContextCompat
import android.content.Context
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import net.ankio.auto.R
import net.ankio.auto.ui.adapter.CategoryStatsAdapter
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.dialog.DateTimePickerDialog
import java.util.Calendar
import java.util.Locale

/**
 * 账单统计页面
 * - 顶部时间筛选（预设与自定义）
 * - 汇总卡片：总支出、总收入、结余、日均支出
 * - 折线图：收入/支出趋势
 * - 分类报表：按一级分类百分比，支持展开二级
 */
class StatisticFragment : BaseFragment<FragmentStatisticBinding>() {

    // 当前筛选时间范围（毫秒）
    private var startTime: Long = 0L
    private var endTime: Long = 0L

    // 分类统计适配器
    private val categoryAdapter = CategoryStatsAdapter()

    // 当前分类数据
    private var currentExpenseCategories: List<CategoryItemDto> = emptyList()
    private var currentIncomeCategories: List<CategoryItemDto> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDefaultPeriod()
        setupUI()
        bindPeriodChips()
        refreshAll()
    }

    /** 初始化UI样式 */
    private fun setupUI() {
        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        setupCardColors()
        setupAmountColors()
        setupCategoryRecyclerView()
        setupCustomRangeButton()
    }

    /** 设置分类统计RecyclerView */
    private fun setupCategoryRecyclerView() {
        binding.categoryRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryAdapter
        }

        // 设置分类切换按钮
        setupCategoryToggle()
    }

    /** 设置分类切换功能 */
    private fun setupCategoryToggle() {
        // 默认选中支出
        binding.btnExpenseCategory.isChecked = true

        // 设置切换监听器
        binding.categoryToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    binding.btnExpenseCategory.id -> {
                        categoryAdapter.setCategoryData(currentExpenseCategories)
                    }

                    binding.btnIncomeCategory.id -> {
                        categoryAdapter.setCategoryData(currentIncomeCategories)
                    }
                }
            }
        }
    }

    /** 设置所有卡片的背景色 */
    private fun setupCardColors() {
        val cardColor = DynamicColors.SurfaceColor1
        listOf(
            binding.filterCard,
            binding.summaryCard,
            binding.lineCard,
            binding.categoryCard
        ).forEach { it.setCardBackgroundColor(cardColor) }
    }

    /** 根据用户偏好设置收支颜色和图标 */
    private fun setupAmountColors() {
        val ctx = requireContext()
        val (incomeColor, incomeIcon, _) = BillTool.getStyle(BillType.Income)
        val (expenseColor, expenseIcon, _) = BillTool.getStyle(BillType.Expend)

        // 获取实际颜色值
        val incomeColorValue = ContextCompat.getColor(ctx, incomeColor)
        val expenseColorValue = ContextCompat.getColor(ctx, expenseColor)

        with(binding) {
            // 收入样式设置
            setupBillTypeStyle(
                icon = ivIncomeIcon,
                label = tvIncomeLabel,
                amount = tvTotalIncome,
                indicator = incomeIndicator,
                iconRes = incomeIcon,
                colorValue = incomeColorValue
            )

            // 支出样式设置  
            setupBillTypeStyle(
                icon = ivExpenseIcon,
                label = tvExpenseLabel,
                amount = tvTotalExpense,
                indicator = expenseIndicator,
                iconRes = expenseIcon,
                colorValue = expenseColorValue
            )
        }
    }

    private fun setupBillTypeStyle(
        icon: android.widget.ImageView,
        label: com.google.android.material.textview.MaterialTextView,
        amount: com.google.android.material.textview.MaterialTextView,
        indicator: View,
        iconRes: Int,
        colorValue: Int
    ) {
        icon.setImageResource(iconRes)
        icon.setColorFilter(colorValue)
        label.setTextColor(colorValue)
        amount.setTextColor(colorValue)
        indicator.setBackgroundColor(colorValue)
    }

    /** 使用"本月"作为默认筛选周期 */
    private fun initDefaultPeriod() {
        val (start, end) = TimeRangeCalculator.thisMonth()
        startTime = start
        endTime = end
    }

    /** 时间范围计算工具 */
    private object TimeRangeCalculator {
        private fun Calendar.resetTime() = apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        fun thisWeek(): Pair<Long, Long> {
            val cal = Calendar.getInstance().resetTime()
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            val offset = ((dayOfWeek + 5) % 7) // 周一=0
            cal.add(Calendar.DAY_OF_YEAR, -offset)
            return cal.timeInMillis to System.currentTimeMillis()
        }

        fun thisMonth(): Pair<Long, Long> {
            val cal = Calendar.getInstance().resetTime()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            return cal.timeInMillis to System.currentTimeMillis()
        }

        fun lastWeek(): Pair<Long, Long> {
            val cal = Calendar.getInstance().resetTime()
            cal.add(Calendar.DAY_OF_YEAR, -6)
            return cal.timeInMillis to System.currentTimeMillis()
        }

        fun last30Days(): Pair<Long, Long> {
            val cal = Calendar.getInstance().resetTime()
            cal.add(Calendar.DAY_OF_YEAR, -29)
            return cal.timeInMillis to System.currentTimeMillis()
        }

        fun thisYear(): Pair<Long, Long> {
            val cal = Calendar.getInstance().resetTime()
            cal.set(Calendar.MONTH, Calendar.JANUARY)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            return cal.timeInMillis to System.currentTimeMillis()
        }

        fun lastYear(): Pair<Long, Long> {
            val cal = Calendar.getInstance().resetTime()
            cal.add(Calendar.DAY_OF_YEAR, -364)
            return cal.timeInMillis to System.currentTimeMillis()
        }
    }

    private fun bindPeriodChips() {
        // 默认选中"本月"
        binding.chipThisMonth.isChecked = true
        binding.btnCustomRange.isChecked = false

        // 数据驱动的chip配置
        val chipConfigs = mapOf(
            binding.chipThisWeek to TimeRangeCalculator::thisWeek,
            binding.chipThisMonth to TimeRangeCalculator::thisMonth,
            binding.chipLastWeek to TimeRangeCalculator::lastWeek,
            binding.chipLast30Days to TimeRangeCalculator::last30Days,
            binding.chipThisYear to TimeRangeCalculator::thisYear,
            binding.chipLastYear to TimeRangeCalculator::lastYear
        )

        chipConfigs.forEach { (chip, calculator) ->
            chip.setOnClickListener {
                // 选择预设周期时，取消自定义区间的选中状态
                binding.btnCustomRange.isChecked = false
                val (start, end) = calculator()
                setTimeRange(start, end)
            }
        }

        // chipGroup 发生选择时，确保自定义区间处于未选中
        binding.chipGroupPeriod.setOnCheckedStateChangeListener { _, _ ->
            binding.btnCustomRange.isChecked = false
        }
    }

    /** 自定义时间区间按钮 */
    private fun setupCustomRangeButton() {
        binding.btnCustomRange.setOnClickListener {
            // 选择自定义区间时，清空 chipGroup 的选中状态
            binding.chipGroupPeriod.clearCheck()
            binding.btnCustomRange.isChecked = true
            showCustomRangePicker()
        }
    }

    /**
     * 显示自定义时间区间选择器
     */
    private fun showCustomRangePicker() {
        // 使用内置选择器的区间模式，避免弹两次对话框
        BaseSheetDialog.create<DateTimePickerDialog>(requireContext())
            .setTitle(getString(R.string.period_custom_range))
            .setRangeMode(true)
            .setOnDateRangeSelected { start, end ->
                if (end < start) {
                    ToastUtils.error(getString(R.string.time_range_invalid))
                    // 无效区间时，不保持自定义选中
                    binding.btnCustomRange.isChecked = false
                    return@setOnDateRangeSelected
                }
                // 有效区间：保持自定义选中
                binding.btnCustomRange.isChecked = true
                setTimeRange(start, end)
            }
            .show()
    }

    private fun setTimeRange(start: Long, end: Long) {
        startTime = start
        endTime = end
        refreshAll()
    }

    /** 刷新所有统计数据 */
    private fun refreshAll() {
        launch {
            val stats = withIO { BillAPI.stats(startTime, endTime) }
            if (stats == null) {
                Logger.e("统计数据为空")
                return@launch
            }
            withMain {
                renderSummary(stats.summary)
                renderTrend(stats.trend)
                renderCategory(stats.expenseCategories, stats.incomeCategories)
            }
        }
    }

    /** 渲染汇总数据 */
    private fun renderSummary(summary: SummaryDto) {
        with(binding) {
            tvTotalExpense.text = BillTool.formatAmount(summary.totalExpense)
            tvTotalIncome.text = BillTool.formatAmount(summary.totalIncome)
            tvNetBalance.text = BillTool.formatAmount(summary.net)
            tvDailyAvgExpense.text = BillTool.formatAmount(summary.dailyAvgExpense)
        }
    }

    /** 渲势图表 */
    private fun renderTrend(trend: TrendDto) {
        if (trend.labels.isEmpty()) return

        // 根据用户偏好获取收入和支出颜色
        val incomeColor =
            ContextCompat.getColor(requireContext(), BillTool.getColor(BillType.Income))
        val expenseColor =
            ContextCompat.getColor(requireContext(), BillTool.getColor(BillType.Expend))

        // 使用通用的双折线图API
        binding.lineChart.setDualLines(
            trend.labels,
            trend.incomes, incomeColor,
            trend.expenses, expenseColor
        )
    }


    /** 渲染分类统计 */
    private fun renderCategory(
        expenseCategories: List<CategoryItemDto>,
        incomeCategories: List<CategoryItemDto>
    ) {
        // 保存数据
        currentExpenseCategories = expenseCategories
        currentIncomeCategories = incomeCategories

        // 根据当前选择显示对应分类
        val currentCategories = if (binding.btnExpenseCategory.isChecked) {
            expenseCategories
        } else {
            incomeCategories
        }
        categoryAdapter.setCategoryData(currentCategories)
    }
}
