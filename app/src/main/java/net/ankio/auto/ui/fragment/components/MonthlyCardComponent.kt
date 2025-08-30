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
import net.ankio.auto.databinding.CardMonthlyBinding
import net.ankio.auto.http.api.BillAPI
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseComponent
import net.ankio.auto.ui.dialog.PeriodSelectorDialog
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.utils.BillTool
import net.ankio.auto.utils.PrefManager
import org.ezbook.server.constant.BillType
import java.util.Calendar
import java.util.Locale

class MonthlyCardComponent(binding: CardMonthlyBinding) :
    BaseComponent<CardMonthlyBinding>(binding) {

    private var onNavigateToAiSummary: ((PeriodSelectorDialog.PeriodData?) -> Unit)? = null

    private lateinit var fragment: Fragment
    /**
     * 设置导航回调
     */
    fun setOnNavigateToAiSummary(callback: (PeriodSelectorDialog.PeriodData?) -> Unit) = apply {
        onNavigateToAiSummary = callback
    }

    fun setFragment(fragment: Fragment) = apply {
        this.fragment = fragment
    }

    override fun onComponentCreate() {
        super.onComponentCreate()
        binding.root.setCardBackgroundColor(SurfaceColors.SURFACE_1.getColor(context))

        // 设置底部操作按钮
        setupBottomButtons()
    }

    /**
     * 设置底部操作按钮
     * 根据配置动态显示AI分析和同步按钮
     */
    private fun setupBottomButtons() {
        val showAiButton = PrefManager.aiMonthlySummary
        val showSyncButton = PrefManager.bookApp != BuildConfig.APPLICATION_ID

        // 设置AI分析按钮
        binding.btnAiAnalysis.visibility = if (showAiButton) View.VISIBLE else View.GONE
        binding.btnAiAnalysis.setOnClickListener {
            showPeriodSelector()
        }

        // 设置同步按钮  
        binding.btnSync.visibility = if (showSyncButton) View.VISIBLE else View.GONE
        binding.btnSync.setOnClickListener {
            performSync()
        }

        // 控制整个按钮容器的显示状态
        // 如果两个按钮都不显示，则隐藏整个容器
        binding.bottomButtonsLayout.visibility = if (showAiButton || showSyncButton) {
            View.VISIBLE
        } else {
            View.GONE
        }

        // 调整按钮布局：只有一个按钮时，让它占满整行
        if (showAiButton && !showSyncButton) {
            // 只显示AI按钮，移除右边距
            binding.btnAiAnalysis.layoutParams =
                (binding.btnAiAnalysis.layoutParams as android.widget.LinearLayout.LayoutParams).apply {
                    weight = 1f
                    marginEnd = 0
                }
        } else if (!showAiButton && showSyncButton) {
            // 只显示同步按钮，移除左边距
            binding.btnSync.layoutParams =
                (binding.btnSync.layoutParams as android.widget.LinearLayout.LayoutParams).apply {
                    weight = 1f
                    marginStart = 0
                }
        } else if (showAiButton && showSyncButton) {
            // 两个按钮都显示，恢复原始边距 (8dp)
            val marginPx = (8 * context.resources.displayMetrics.density).toInt()
            binding.btnAiAnalysis.layoutParams =
                (binding.btnAiAnalysis.layoutParams as android.widget.LinearLayout.LayoutParams).apply {
                    weight = 1f
                    marginEnd = marginPx
                }
            binding.btnSync.layoutParams =
                (binding.btnSync.layoutParams as android.widget.LinearLayout.LayoutParams).apply {
                    weight = 1f
                    marginStart = marginPx
                }
        }
    }

    /**
     * 显示周期选择对话框
     */
    private fun showPeriodSelector() {
        BaseSheetDialog.create<PeriodSelectorDialog>(context)
            .setOnPeriodSelected { periodData ->
                onNavigateToAiSummary?.invoke(periodData)
            }
            .show()
    }

    /**
     * 执行同步操作
     */
    private fun performSync() {
        // TODO 执行同步
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

                // 设置收入金额和颜色
                binding.tvIncomeAmount.text =
                    String.format(Locale.getDefault(), "¥ %.2f", incomeAmount)
                val incomeColor =
                    ContextCompat.getColor(context, BillTool.getColor(BillType.Income))
                binding.tvIncomeAmount.setTextColor(incomeColor)
                binding.ivIncomeIcon.imageTintList =
                    android.content.res.ColorStateList.valueOf(incomeColor)

                // 设置支出金额和颜色
                binding.tvExpenseAmount.text =
                    String.format(Locale.getDefault(), "¥ %.2f", expenseAmount)
                val expenseColor =
                    ContextCompat.getColor(context, BillTool.getColor(BillType.Expend))
                binding.tvExpenseAmount.setTextColor(expenseColor)
                binding.ivExpenseIcon.imageTintList =
                    android.content.res.ColorStateList.valueOf(expenseColor)
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