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
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentPluginHomeBinding
import net.ankio.auto.ui.activity.MainActivity
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.api.bindAs
import net.ankio.auto.ui.fragment.home.BookCardComponent
import net.ankio.auto.ui.fragment.home.MonthlyCardComponent
import net.ankio.auto.ui.fragment.home.RuleVersionCardComponent
import net.ankio.auto.ui.fragment.home.StatusCardComponent
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.utils.Throttle

class HomeFragment : BaseFragment<FragmentPluginHomeBinding>() {

    private val nav = Throttle.asFunction<Int>(300) {
        findNavController().navigate(it)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.activeCard.bindAs<StatusCardComponent>(lifecycle)
        binding.ruleVersionCard.bindAs<RuleVersionCardComponent>(lifecycle, requireActivity())
        binding.monthlyCard.bindAs<MonthlyCardComponent>(lifecycle)
        val bookCard = binding.bookCard.bindAs<BookCardComponent>(lifecycle)
        bookCard.setOnRedirect {
            nav.invoke(it)
        }
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.title_log -> {
                    findNavController().navigate(R.id.action_homeFragment_to_logFragment)
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