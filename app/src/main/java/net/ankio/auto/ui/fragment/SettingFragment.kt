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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentSettingBinding
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.utils.viewBinding

class SettingFragment : BaseFragment(), View.OnClickListener {
    override val binding: FragmentSettingBinding by viewBinding(FragmentSettingBinding::inflate)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.settingBill.setOnClickListener(this)
        binding.settingPopup.setOnClickListener(this)
        binding.settingFeatures.setOnClickListener(this)
        binding.settingAppearance.setOnClickListener(this)
        binding.settingExperimental.setOnClickListener(this)
        binding.settingBackup.setOnClickListener(this)
        binding.settingOthers.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        if (v == null) return
        when (v.id) {
            R.id.setting_bill-> {
                navigate(R.id.action_settingFragment_to_settingDetailFragment,Bundle().apply {
                    putString("title",getString(R.string.setting_title_bill))
                    putInt("id",R.id.setting_bill)
                })
            }
            R.id.setting_popup-> {
                navigate(R.id.action_settingFragment_to_settingDetailFragment,Bundle().apply {
                    putString("title",getString(R.string.setting_title_popup))
                    putInt("id",R.id.setting_popup)
                })
            }
            R.id.setting_features-> {
                navigate(R.id.action_settingFragment_to_settingDetailFragment,Bundle().apply {
                    putString("title",getString(R.string.setting_title_features))
                    putInt("id",R.id.setting_features)
                })
            }
            R.id.setting_appearance-> {
                navigate(R.id.action_settingFragment_to_settingDetailFragment,Bundle().apply {
                    putString("title",getString(R.string.setting_title_appearance))
                    putInt("id",R.id.setting_appearance)
                })
            }
            R.id.setting_experimental-> {
                navigate(R.id.action_settingFragment_to_settingDetailFragment,Bundle().apply {
                    putString("title",getString(R.string.setting_title_experimental))
                    putInt("id",R.id.setting_experimental)
                })
            }
            R.id.setting_backup-> {
                navigate(R.id.action_settingFragment_to_settingDetailFragment,Bundle().apply {
                    putString("title",getString(R.string.setting_title_backup))
                    putInt("id",R.id.setting_backup)
                })
            }
            R.id.setting_others-> {
                navigate(R.id.action_settingFragment_to_settingDetailFragment,Bundle().apply {
                    putString("title",getString(R.string.setting_title_others))
                    putInt("id",R.id.setting_others)
                })
            }

        }
    }
}
