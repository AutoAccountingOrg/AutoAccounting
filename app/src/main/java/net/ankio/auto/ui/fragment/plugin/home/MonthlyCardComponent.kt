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

package net.ankio.auto.ui.fragment.plugin.home

import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.launch
import net.ankio.auto.BuildConfig
import net.ankio.auto.R
import net.ankio.auto.constant.WorkMode
import net.ankio.auto.databinding.CardMonthlyBinding
import net.ankio.auto.http.api.BillAPI
import net.ankio.auto.ui.api.BaseComponent
import net.ankio.auto.utils.PrefManager
import java.util.Calendar
import java.util.Locale

class MonthlyCardComponent(binding: CardMonthlyBinding, private val lifecycle: Lifecycle) :
    BaseComponent<CardMonthlyBinding>(binding, lifecycle) {

    override fun init() {
        super.init()
        //tv_pending_sync
        binding.tvPendingSync.visibility = if (PrefManager.bookApp === BuildConfig.APPLICATION_ID) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }


    override fun resume() {
        super.resume()
        lifecycle.coroutineScope.launch {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH is 0-based

            val stats = BillAPI.getMonthlyStats(year, month)
            if (stats != null) {
                binding.tvIncomeAmount.text =
                    String.format(Locale.getDefault(), "¥ %.2f", stats["income"] ?: 0.0)
                binding.tvExpenseAmount.text =
                    String.format(Locale.getDefault(), "¥ %.2f", stats["expense"] ?: 0.0)
            }

            val syncCount = BillAPI.sync().size

            binding.tvPendingSync.text = context.getString(R.string.pending_sync, syncCount)
        }
    }

}