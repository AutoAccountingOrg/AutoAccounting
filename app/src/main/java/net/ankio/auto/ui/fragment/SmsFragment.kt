/*
 * Copyright (C) 2024 ankio(ankio@ankio.net)
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
import androidx.viewbinding.ViewBinding
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentSmsEditBinding
import net.ankio.auto.databinding.SettingItemInputBinding
import net.ankio.auto.storage.ConfigUtils
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.dialog.BottomSheetDialogBuilder
import net.ankio.auto.ui.utils.viewBinding
import org.ezbook.server.constant.DefaultData
import org.ezbook.server.constant.Setting

class SmsFragment: BaseFragment() {
    override val binding: FragmentSmsEditBinding by viewBinding(FragmentSmsEditBinding::inflate)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    )= binding.root

    private var chipData = mutableListOf<String>()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.addChip.setOnClickListener {
            showInput("")
        }
        chipData = ConfigUtils.getString(Setting.SMS_FILTER,DefaultData.SMS_FILTER).split(",").filter { it.isNotEmpty() }.toMutableList()
        chipData.forEach {
            addChip(it)
        }

    }
    private fun setChip(oldText: String,newText: String) {
        val index = chipData.indexOf(oldText)
        if (index != -1) {
            chipData[index] = newText
        }
    }

    override fun onStop() {
        super.onStop()
        ConfigUtils.putString(Setting.SMS_FILTER, chipData.joinToString(","))
    }
    private fun addChipData(text: String) {
        chipData.add(text)
    }
    private fun showInput(text: String,chip: Chip? = null) {
        val inputBinding = SettingItemInputBinding.inflate(layoutInflater)
        inputBinding.root.setPadding(16,16,16,16)
        inputBinding.input.setText(text)
        inputBinding.inputLayout.hint = ""


        BottomSheetDialogBuilder(requireContext())
            .setTitleInt(R.string.add_filter)
            .setView(inputBinding.root)
            .setPositiveButton(R.string.sure_msg) { dialog, which ->
                val input = inputBinding.input.text.toString()
                if (input.isNotEmpty()) {
                    if (chip != null) {
                        chip.text = input
                        setChip(text,input)
                    } else {
                        addChip(input,true)
                    }
                }
            }
            .setNegativeButton(R.string.cancel_msg, null)
            .showInFragment(this,false,true)
    }
    // 添加Chip
    private fun addChip(text: String,saveToData:Boolean = false) {
        val chip = Chip(context)
        chip.text = text
        chip.chipStrokeWidth = 0f
        chip.isClickable = true
        chip.isCloseIconVisible = true
        chip.setOnCloseIconClickListener {
            binding.chipGroup.removeView(chip)
            chipData.remove(text)
        }
        chip.setOnClickListener {
            showInput(text,chip)
        }
        binding.chipGroup.addView(chip)
        if (saveToData) {
            addChipData(text)
        }
    }
}