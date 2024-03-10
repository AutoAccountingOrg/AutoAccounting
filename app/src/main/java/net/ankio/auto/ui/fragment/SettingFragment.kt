/*
 * Copyright (C) 2023 ankio(ankio@ankio.net)
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
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentSetting2Binding
import net.ankio.auto.databinding.FragmentSettingBinding
import net.ankio.auto.setting.Config
import net.ankio.auto.setting.SettingUtils
import net.ankio.auto.ui.utils.MenuItem


class SettingFragment : BaseFragment() {
    private lateinit var binding: FragmentSettingBinding

    private lateinit var settingRenderUtils: SettingUtils


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingBinding.inflate(layoutInflater)
        val settingItems = Config.setting(requireContext())
        settingRenderUtils = SettingUtils(requireActivity(),binding.container,layoutInflater,settingItems)
        settingRenderUtils.init()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        settingRenderUtils.onResume()
    }

}