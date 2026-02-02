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

package net.ankio.auto.ui.fragment.components

import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.elevation.SurfaceColors
import net.ankio.auto.BuildConfig
import net.ankio.auto.R
import net.ankio.auto.databinding.CardMonthlyBinding
import net.ankio.auto.http.api.BillAPI
import net.ankio.auto.ui.api.BaseComponent

import net.ankio.auto.ui.theme.DynamicColors
import net.ankio.auto.utils.BillTool
import net.ankio.auto.utils.PrefManager
import org.ezbook.server.constant.BillType
import java.util.Calendar
import java.util.Locale

class MonthlyCardComponent(binding: CardMonthlyBinding) :
    BaseComponent<CardMonthlyBinding>(binding) {

    private var onNavigateToAiSummary: (() -> Unit)? = null
    private var onNavigateToStatistics: (() -> Unit)? = null

    fun setOnNavigateToAiSummary(callback: () -> Unit) = apply {
        onNavigateToAiSummary = callback
    }

    fun setOnNavigateToStatistics(callback: () -> Unit) = apply {
        onNavigateToStatistics = callback
    }

    override fun onComponentCreate() {
        super.onComponentCreate()
        binding.root.setCardBackgroundColor(DynamicColors.SurfaceColor1)
        setupColors()
        setupPrivacyToggle()
        // 设置底部操作按钮
        setupBottomButtons()
    }

    private fun setupPrivacyToggle() {
        val toggleAction = {
            PrefManager.homePrivacyMode = !PrefManager.homePrivacyMode
            refreshData()
        }
        binding.incomeExpenseLayout.setOnClickListener { toggleAction() }
    }

    private fun setupColors() {
        with(binding) {
            // 使用BillTool统一获取样式信息
            val (incomeColor, incomeIcon, incomeBackground) = BillTool.getStyle(BillType.Income)
            val (expenseColor, expenseIcon, expenseBackground) = BillTool.getStyle(BillType.Expend)

            // 设置颜色
            tvIncomeAmount.setTextColor(ContextCompat.getColor(context, incomeColor))
            tvExpenseAmount.setTextColor(ContextCompat.getColor(context, expenseColor))

            ivIncomeIcon.imageTintList = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(context, incomeColor)
            )
            ivExpenseIcon.imageTintList = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(context, expenseColor)
            )

            // 设置图标
            ivIncomeIcon.setImageResource(incomeIcon)
            ivExpenseIcon.setImageResource(expenseIcon)

            // 设置背景
            llIncome.setBackgroundResource(incomeBackground)
            llExpense.setBackgroundResource(expenseBackground)
        }
    }

    private fun setupBottomButtons() {
        with(binding) {
            btnAiAnalysis.apply {
                visibility = if (PrefManager.aiMonthlySummary) View.VISIBLE else View.GONE
                setOnClickListener { onNavigateToAiSummary?.invoke() }
            }
            btnSync.apply {
                visibility =
                    if (PrefManager.bookApp != BuildConfig.APPLICATION_ID) View.VISIBLE else View.GONE
                setOnClickListener { BillTool.syncBills() }
            }

            btnAnalysis.setOnClickListener {
                //跳转账单综合分析页面
                onNavigateToStatistics?.invoke()
            }
        }
    }


    /**
     * 刷新数据
     */
    private fun refreshData() {
        launch {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH is 0-based

            val stats = BillAPI.getMonthlyStats(year, month)
            if (stats != null) {
                val incomeAmount = stats["income"] ?: 0.0
                val expenseAmount = stats["expense"] ?: 0.0

                // 隐私模式处理
                if (PrefManager.homePrivacyMode) {
                    binding.tvIncomeAmount.text = "****"
                    binding.tvExpenseAmount.text = "****"
                } else {
                    binding.tvIncomeAmount.text = BillTool.formatAmount(incomeAmount)
                    binding.tvExpenseAmount.text = BillTool.formatAmount(expenseAmount)
                }
            }
        }
    }


    override fun onComponentResume() {
        super.onComponentResume()

        // 更新底部按钮状态
        setupBottomButtons()

        // 刷新数据
        refreshData()
    }

}