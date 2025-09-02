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

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentPluginHomeBinding
import net.ankio.auto.ui.activity.MainActivity
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.api.bindAs
import net.ankio.auto.ui.fragment.components.BookCardComponent
import net.ankio.auto.ui.fragment.components.MonthlyCardComponent
import net.ankio.auto.ui.fragment.components.RuleVersionCardComponent
import net.ankio.auto.ui.fragment.components.StatusCardComponent
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.utils.Throttle

class HomeFragment : BaseFragment<FragmentPluginHomeBinding>() {
    private val gson = Gson()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val statusCard: StatusCardComponent = binding.activeCard.bindAs()

        val ruleVersionCard: RuleVersionCardComponent = binding.ruleVersionCard.bindAs()

        val monthlyCard: MonthlyCardComponent = binding.monthlyCard.bindAs()
        monthlyCard.setFragment(this)
            .setOnNavigateToAiSummary { periodData ->
                // 使用Bundle传递周期数据
                val bundle = Bundle()
                if (periodData != null) {
                    bundle.putString("period_data", gson.toJson(periodData))
                }
                // 使用目的地 ID 导航，避免当前目的地识别为 NavGraph 时解析不到 action
                findNavController().navigate(R.id.aiSummaryFragment, bundle)
            }

        val bookCard: BookCardComponent = binding.bookCard.bindAs()
        bookCard.setOnRedirect { navigationId, bundle ->
            findNavController().navigate(navigationId, bundle)
        }
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.title_log -> {
                    // 使用目的地 ID 导航
                    findNavController().navigate(R.id.logFragment)
                    true
                }


                R.id.title_explore -> {
                    PrefManager.introIndex = 0
                    val intent = Intent(requireContext(), MainActivity::class.java)
                    startActivity(intent)
                    requireActivity().finish()
                    true
                }
                else -> false
            }
        }
    }
}