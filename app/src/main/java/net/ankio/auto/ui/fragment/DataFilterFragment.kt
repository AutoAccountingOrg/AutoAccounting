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
import android.view.View
import android.content.res.ColorStateList
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentDataFilterBinding
import net.ankio.auto.storage.ConfigUtils
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.dialog.EditorDialogBuilder
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.utils.toThemeColor
import org.ezbook.server.constant.DefaultData
import org.ezbook.server.constant.Setting

/**
 * 短信过滤条件管理 Fragment
 *
 * 展示与编辑短信关键词过滤白名单：
 * - 使用 Chip 展示已配置的过滤关键词
 * - 支持新增、编辑、删除关键词
 * - 变更在页面离开时统一持久化到配置项 [Setting.SMS_FILTER]
 */
class DataFilterFragment : BaseFragment<FragmentDataFilterBinding>() {

    /** 当前的过滤关键词集合（以 Chip 展示） */
    private var chipData: MutableList<String> = mutableListOf()

    /**
     * 视图创建完成后的初始化逻辑
     * - 绑定返回按钮
     * - 初始化关键词 Chip
     * - 绑定新增按钮
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 返回按钮
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        // 初始化数据并渲染现有 Chip
        chipData = PrefManager.dataFilter
        chipData.forEach { addChip(it) }

        // 新增按钮
        binding.addChip.setOnClickListener { showInput("") }
    }

    /**
     * 替换集合中的旧关键词为新关键词
     */
    private fun setChip(oldText: String, newText: String) {
        val index = chipData.indexOf(oldText)
        if (index != -1) chipData[index] = newText
    }

    /**
     * 生命周期回调：离开页面时持久化保存
     */
    override fun onStop() {
        super.onStop()
        PrefManager.dataFilter = chipData
    }

    /**
     * 将新关键词加入集合
     */
    private fun addChipData(text: String) {
        chipData.add(text)
    }

    /**
     * 弹出输入框，新增或编辑关键词
     * @param text 预填的文本
     * @param chip 传入则为编辑模式，否则为新增
     */
    private fun showInput(text: String, chip: Chip? = null) {
        // 使用统一的 EditorDialogBuilder 构建输入对话框
        EditorDialogBuilder(this)
            .setTitleInt(R.string.add_filter)
            .setMessage(text)
            .setEditorPositiveButton(R.string.sure_msg) { input ->
                val value = input.trim()
                if (value.isNotEmpty()) {
                    if (chip != null) {
                        chip.text = value
                        setChip(text, value)
                    } else {
                        addChip(value, saveToData = true)
                    }
                }
            }
            .setNegativeButton(R.string.cancel_msg, null)
            .show()
    }

    /**
     * 创建并添加一个 Chip 到界面
     * @param text 显示的关键词
     * @param saveToData 是否同步写入集合
     */
    private fun addChip(text: String, saveToData: Boolean = false) {
        val chip = Chip(requireContext()).apply {
            this.text = text
            chipStrokeWidth = 0f
            chipBackgroundColor = ColorStateList.valueOf(
                com.google.android.material.R.attr.colorPrimaryContainer.toThemeColor()
            )
            isClickable = true
            isCloseIconVisible = true
            setOnCloseIconClickListener {
                binding.chipGroup.removeView(this)
                chipData.remove(text)
            }
            setOnClickListener { showInput(this.text.toString(), this) }
        }
        binding.chipGroup.addView(chip)
        if (saveToData) addChipData(text)
    }
}
