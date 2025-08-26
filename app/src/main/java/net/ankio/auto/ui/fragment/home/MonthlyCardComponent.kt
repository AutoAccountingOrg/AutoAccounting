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

package net.ankio.auto.ui.fragment.home

import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import com.google.android.material.elevation.SurfaceColors
import kotlinx.coroutines.launch
import net.ankio.auto.BuildConfig
import net.ankio.auto.R
import net.ankio.auto.databinding.CardMonthlyBinding
import net.ankio.auto.http.api.BillAPI
import net.ankio.auto.ui.api.BaseComponent
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.dialog.PeriodSelectorDialog
import net.ankio.auto.utils.BillTool
import net.ankio.auto.utils.PrefManager
import org.ezbook.server.constant.BillType
import java.lang.ref.WeakReference
import java.util.Calendar
import java.util.Locale

class MonthlyCardComponent(binding: CardMonthlyBinding, private val lifecycle: Lifecycle) :
    BaseComponent<CardMonthlyBinding>(binding, lifecycle) {

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

    override fun init() {
        super.init()
        binding.root.setCardBackgroundColor(SurfaceColors.SURFACE_1.getColor(context))

        // 设置右上角按钮
        setupTopButtons()
    }

    /**
     * 设置右上角按钮
     */
    private fun setupTopButtons() {
        // 设置AI分析按钮
        binding.btnAiAnalysis.visibility = if (PrefManager.aiMonthlySummary) {
            View.VISIBLE
        } else {
            View.GONE
        }
        binding.btnAiAnalysis.setOnClickListener {
            showPeriodSelector()
        }

        // 设置同步按钮
        binding.btnSync.visibility = if (PrefManager.bookApp !== BuildConfig.APPLICATION_ID) {
            View.VISIBLE
        } else {
            View.GONE
        }
        binding.btnSync.setOnClickListener {
            performSync()
        }
    }

    /**
     * 显示周期选择对话框
     */
    private fun showPeriodSelector() {
        PeriodSelectorDialog.create(fragment)
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
        componentScope.launch {
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


    override fun resume() {
        super.resume()

        // 更新右上角按钮状态
        setupTopButtons()

        // 刷新数据
        refreshData()
    }

}