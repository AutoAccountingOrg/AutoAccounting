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

package net.ankio.auto.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentAiSummaryBinding
import net.ankio.auto.http.api.AnalysisTaskAPI
import net.ankio.auto.ui.adapter.AnalysisTaskAdapter
import net.ankio.auto.ui.api.BasePageFragment
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.components.WrapContentLinearLayoutManager
import net.ankio.auto.ui.dialog.BottomSheetDialogBuilder
import net.ankio.auto.ui.utils.ListPopupUtilsGeneric
import net.ankio.auto.ui.utils.ToastUtils
import java.util.*
import net.ankio.auto.utils.PrefManager
import org.ezbook.server.db.model.AnalysisTaskModel

/**
 * AI分析任务管理页面
 *
 * 功能概览：
 * 1. 显示所有AI分析任务列表
 * 2. 支持创建新的分析任务
 * 3. 支持查看已完成的分析报告
 * 4. 支持删除任务
 */
class AiSummaryFragment : BasePageFragment<AnalysisTaskModel, FragmentAiSummaryBinding>() {

    private var autoRefreshJob: Job? = null

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

    override suspend fun loadData(): List<AnalysisTaskModel> = AnalysisTaskAPI.getAllTasks()

    override fun onCreateAdapter(): RecyclerView.Adapter<*> {
        val recyclerView = binding.statusPage.contentView

        // 设置布局管理器为垂直线性布局
        recyclerView?.layoutManager = WrapContentLinearLayoutManager(requireContext())

        return AnalysisTaskAdapter()
            .setOnItemClickListener { task ->
                // 点击查看分析结果
                navigateToDetail(task.id)
            }
            .setOnItemLongClickListener { task ->
                // 长按删除任务
                showDeleteConfirmation(task)
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 检查AI月度总结功能是否启用
        if (!PrefManager.aiMonthlySummary) {
            findNavController().popBackStack()
            return
        }

        setupUI()
        startAutoRefresh()
    }

    override fun onDestroyView() {
        stopAutoRefresh()
        super.onDestroyView()
    }

    /**
     * 开始智能自动刷新
     */
    private fun startAutoRefresh() {
        autoRefreshJob = launch {
            var refreshCount = 0
            while (uiReady()) {
                delay(15_000) // 15秒检查一次

                refreshCount++
                // 前5次刷新（用户刚进入页面，可能有处理中的任务）
                // 之后每4次刷新一次（1分钟一次）
                if (refreshCount <= 5 || refreshCount % 4 == 0) {
                    reload()
                }
            }
        }
    }

    /**
     * 停止自动刷新
     */
    private fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
    }

    /**
     * 设置UI组件
     */
    private fun setupUI() {
        // 设置创建任务按钮
        binding.fabCreate.setOnClickListener {
            showPeriodSelector(it)
        }

        // 设置返回按钮
        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        // 设置菜单项点击监听器
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_clear_all -> {
                    showClearAllConfirmation()
                    true
                }

                else -> false
            }
        }
    }

    /**
     * 显示周期选择弹窗
     */
    private fun showPeriodSelector(anchorView: View) {
        val periodMap = mapOf(
            getString(R.string.period_last_week) to Period.LAST_WEEK,
            getString(R.string.period_last_30_days) to Period.LAST_30_DAYS,
            getString(R.string.period_this_month) to Period.THIS_MONTH,
            getString(R.string.period_last_year) to Period.LAST_YEAR,
            getString(R.string.period_this_year) to Period.THIS_YEAR
        )

        ListPopupUtilsGeneric.create<Period>(requireContext())
            .setAnchor(anchorView)
            .setList(periodMap)
            .setSelectedValue(Period.LAST_30_DAYS) // 默认选择最近30天
            .setOnItemClick { _, _, period ->
                val periodData = calculatePeriodData(period)
                createAnalysisTask(periodData)
            }
            .show()
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
                PeriodData(period, startTime, now, getString(R.string.period_last_week))
            }

            Period.LAST_30_DAYS -> {
                // 最近30天
                calendar.add(Calendar.DAY_OF_YEAR, -29)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startTime = calendar.timeInMillis
                PeriodData(period, startTime, now, getString(R.string.period_last_30_days))
            }

            Period.THIS_MONTH -> {
                // 本月1号到现在
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startTime = calendar.timeInMillis
                PeriodData(period, startTime, now, getString(R.string.period_this_month))
            }

            Period.LAST_YEAR -> {
                // 最近365天
                calendar.add(Calendar.DAY_OF_YEAR, -364)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startTime = calendar.timeInMillis
                PeriodData(period, startTime, now, getString(R.string.period_last_year))
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

    /**
     * 创建分析任务
     */
    private fun createAnalysisTask(periodData: PeriodData) {


        launch {
            val taskId = AnalysisTaskAPI.createTask(
                title = periodData.displayName,
                startTime = periodData.startTime,
                endTime = periodData.endTime
            )



            if (taskId != null) {
                ToastUtils.info("分析任务创建成功，正在后台处理...")
                reload() // 刷新列表
            } else {
                ToastUtils.error("创建任务失败，可能已存在相同时间范围的任务")
            }
        }
    }

    /**
     * 导航到详情页面
     */
    private fun navigateToDetail(taskId: Long) {
        val bundle = bundleOf("task_id" to taskId)
        findNavController().navigate(R.id.analysisDetailFragment, bundle)
    }

    /**
     * 显示删除确认对话框
     */
    private fun showDeleteConfirmation(task: AnalysisTaskModel) {
        BaseSheetDialog.create<BottomSheetDialogBuilder>(requireContext())
            .setTitle(getString(R.string.delete_analysis_task))
            .setMessage(getString(R.string.delete_analysis_task_message, task.title))
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                this@AiSummaryFragment.launch {
                    AnalysisTaskAPI.deleteTask(task.id)
                    reload()
                }
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ -> }
            .show()
    }

    /**
     * 显示清空所有任务确认对话框
     */
    private fun showClearAllConfirmation() {
        BaseSheetDialog.create<BottomSheetDialogBuilder>(requireContext())
            .setTitle("清空所有分析")
            .setMessage("确定要删除所有财务分析任务吗？此操作不可恢复。")
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                this@AiSummaryFragment.launch {
                    val success = AnalysisTaskAPI.clearAllTasks()
                    if (success) {
                        ToastUtils.info("已清空所有分析任务")
                        reload()
                    } else {
                        ToastUtils.error("清空失败，请重试")
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ -> }
            .show()
    }

}
