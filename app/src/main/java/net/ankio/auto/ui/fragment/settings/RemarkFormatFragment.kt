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

package net.ankio.auto.ui.fragment.settings

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.view.View
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentRemarkFormatBinding
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.ui.theme.DynamicColors
import net.ankio.auto.utils.PrefManager

/**
 * 备注格式设置页面
 *
 * 结构：
 * - 顶部输入框用于编辑模板
 * - 下方使用 Chip 展示可用占位符，一键插入到光标处
 * - 右下角悬浮保存按钮，点击后写入 Pref 并返回
 *
 * 设计哲学：
 * - 简洁直接：页面只做一件事——编辑备注模板
 * - 无特殊情况：占位符集合固定，点击即插入
 * - 向后兼容：读写使用 PrefManager.noteFormat，与服务端键一致
 */
class RemarkFormatFragment : BaseFragment<FragmentRemarkFormatBinding>() {

    /** 支持的占位符集合（与服务端替换逻辑保持一致） */
    private val placeholders = listOf(
        // 基础信息
        "【商户名称】",
        "【商品名称】",
        "【金额】",
        "【分类】",
        "【账本】",
        "【来源】",
        "【原始资产】",
        "【目标资产】",
        "【渠道】",
        // 扩展信息
        "【规则名称】",
        "【AI】",
        "【货币类型】",
        "【手续费】",
        "【标签】",
        "【交易类型】",
        "【时间】"
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 顶部返回按钮
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        // 初始化输入框内容
        binding.editText.setText(PrefManager.noteFormat)

        // 渲染占位符 Chips
        placeholders.forEach { addPlaceholderChip(it) }

        // 保存按钮：写入偏好并返回
        binding.saveButton.setOnClickListener {
            val text = binding.editText.text?.toString()?.trim() ?: ""
            PrefManager.noteFormat = text
            ToastUtils.info(getString(R.string.js_saved))
            findNavController().popBackStack()
        }
    }

    /**
     * 创建占位符 Chip 并加入视图
     * @param text 占位符文本（如："【商户名称】"）
     */
    private fun addPlaceholderChip(text: String) {
        val chip = Chip(requireContext()).apply {
            this.text = text
            isClickable = true
            isCheckable = false
            isCloseIconVisible = false
            chipStrokeWidth = 0f
            chipBackgroundColor = ColorStateList.valueOf(DynamicColors.PrimaryContainer)
            setOnClickListener { insertAtCursor(text) }
        }
        binding.chipGroup.addView(chip)
    }

    /**
     * 将占位符插入到当前光标位置（或替换选中内容）
     * @param token 占位符文本
     */
    private fun insertAtCursor(token: String) {
        val edit = binding.editText
        val editable: Editable = edit.text ?: return
        val start = edit.selectionStart.coerceAtLeast(0)
        val end = edit.selectionEnd.coerceAtLeast(0)
        val min = minOf(start, end)
        val max = maxOf(start, end)
        editable.replace(min, max, token)
        edit.setSelection(min + token.length)
    }
}


