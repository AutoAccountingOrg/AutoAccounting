/*
 * Copyright (C) 2026 ankio(ankio@ankio.net)
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
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentSummaryWebviewBinding
import net.ankio.auto.http.api.BillAPI
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseWebViewFragment
import net.ankio.auto.utils.CoroutineUtils.withIO
import net.ankio.auto.utils.CoroutineUtils.withMain
import net.ankio.auto.utils.PeriodSelector
import java.util.Calendar

/**
 * 消费分析报告页面 - WebView版本
 * 使用summary.html展示详细的消费分析数据
 */
class SummaryWebViewFragment : BaseWebViewFragment<FragmentSummaryWebviewBinding>() {

    private var startTime: Long = 0L
    private var endTime: Long = 0L
    private var isPrivacyMode = false
    private val gson = Gson()

    override fun getWebView(): WebView = binding.webView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDefaultPeriod()
        setupUI()
    }

    private fun setupUI() {
        binding.topAppBar.apply {
            subtitle = getPeriodName(startTime, endTime)
            setNavigationOnClickListener { findNavController().popBackStack() }
            setOnMenuItemClickListener { onMenuItemClick(it) }
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            if (isWebViewReady) loadSummary() else binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    override fun loadInitialUrl(): String = "file:///android_asset/summary/summary.html"

    override fun onWebViewReady() {
        loadSummary()
    }

    private fun onMenuItemClick(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_filter_period -> {
                PeriodSelector.show(
                    requireContext(),
                    binding.topAppBar.findViewById(R.id.action_filter_period),
                    startTime,
                    endTime
                ) { start, end, label ->
                    setTimeRange(start to end, label)
                }
                true
            }
            R.id.action_privacy_mode -> {
                togglePrivacyMode(item)
                true
            }
            else -> false
        }
    }

    private fun togglePrivacyMode(item: MenuItem) {
        isPrivacyMode = !isPrivacyMode
        item.setIcon(if (isPrivacyMode) R.drawable.ic_visibility_off else R.drawable.ic_visibility)
        binding.webView.evaluateJavascript("togglePrivacyMode($isPrivacyMode)", null)
    }

    private fun initDefaultPeriod() {
        val periodData =
            PeriodSelector.calculatePeriodData(requireContext(), PeriodSelector.Period.THIS_MONTH)
        startTime = periodData.startTime
        endTime = periodData.endTime
    }

    private fun loadSummary() {
        launch {
            try {
                val periodName = getPeriodName(startTime, endTime)
                val summaryData = withIO { BillAPI.summary(startTime, endTime, periodName) }

                withMain {
                    summaryData?.let { injectDataToWebView(it) }
                    binding.swipeRefreshLayout.isRefreshing = false
                }
            } catch (e: Exception) {
                Logger.e("加载消费分析数据失败", e)
                withMain { binding.swipeRefreshLayout.isRefreshing = false }
            }
        }
    }

    private fun getPeriodName(start: Long, end: Long): String {
        val startCal = Calendar.getInstance().apply { timeInMillis = start }
        val endCal = Calendar.getInstance().apply { timeInMillis = end }
        val isFullMonth = startCal.get(Calendar.DAY_OF_MONTH) == 1 &&
                endCal.get(Calendar.DAY_OF_MONTH) == endCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        return if (isFullMonth) {
            "${startCal.get(Calendar.YEAR)}年${startCal.get(Calendar.MONTH) + 1}月"
        } else {
            PeriodSelector.formatRangeLabel(start, end)
        }
    }

    private fun injectDataToWebView(summaryData: Map<String, Any?>) {
        val jsonData = gson.toJson(summaryData)
        binding.webView.evaluateJavascript("window.setJson($jsonData)") {
            Logger.i("数据注入完成: $it")
        }
    }

    private fun setTimeRange(range: Pair<Long, Long>, label: String? = null) {
        startTime = range.first
        endTime = range.second
        binding.topAppBar.subtitle = label ?: getPeriodName(startTime, endTime)
        if (isWebViewReady) loadSummary()
    }
}
