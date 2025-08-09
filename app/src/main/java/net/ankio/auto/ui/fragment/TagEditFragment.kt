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
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentTagEditBinding
import net.ankio.auto.http.api.TagAPI
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.dialog.ColorPickerDialog
import net.ankio.auto.ui.utils.ToastUtils
import org.ezbook.server.db.model.TagModel

/**
 * 标签编辑Fragment
 *
 * 用于创建或编辑标签，提供标签名称输入和颜色选择功能
 */
class TagEditFragment : BaseFragment<FragmentTagEditBinding>() {

    private lateinit var tagModel: TagModel
    private var isEditing = false
    private var tagId: Long = 0
    private var availableGroups: List<String> = emptyList()
    private lateinit var groupAdapter: ArrayAdapter<String>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeTag()
        setupViews()
        setupGroupSelection()
        setupListeners()
    }

    val defaultColor = "#E57373"

    /**
     * 初始化标签数据
     */
    private fun initializeTag() {
        // 从arguments获取tagId
        tagId = arguments?.getLong("tagId", 0) ?: 0
        isEditing = tagId > 0

        if (isEditing) {
            // 编辑模式，加载现有标签
            lifecycleScope.launch {
                try {
                    tagModel = TagAPI.getById(tagId) ?: TagModel().apply {
                        color = defaultColor
                    }
                    setupViewsWithData()
                } catch (e: Exception) {
                    ToastUtils.error(R.string.load_tag_failed)
                    findNavController().popBackStack()
                }
            }
        } else {
            // 新建模式
            tagModel = TagModel().apply {
                color = defaultColor
            }
            setupViewsWithData()
        }
    }

    /**
     * 设置视图
     */
    private fun setupViews() {
        // 设置标题
        binding.topAppBar.title = getString(
            if (isEditing) R.string.edit_tag else R.string.add_tag
        )

        // 设置返回按钮
        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    /**
     * 使用数据设置视图
     */
    private fun setupViewsWithData() {
        binding.tagNameInput.setText(tagModel.name)
        binding.tagGroupInput.setText(tagModel.group, false)
        updateColorDisplay(tagModel.color)
    }

    /**
     * 设置分组选择功能
     */
    private fun setupGroupSelection() {
        lifecycleScope.launch {
            availableGroups = TagAPI.getGroups().toMutableList()

            // 添加预定义分组选项（确保所有常用分组都可选择）
            val predefinedGroups = listOf(
                "场景",
                "角色",
                "属性",
                "时间",
                "项目",
                "情绪",
                "地点",
                "平台",
                "游戏",
                "其他"
            )
            val allGroups = (predefinedGroups + availableGroups).distinct()

            // 创建适配器，支持筛选
            groupAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                allGroups
            )

            binding.tagGroupInput.setAdapter(groupAdapter)

            // 设置当前标签的分组
            binding.tagGroupInput.setText(tagModel.group, false)

            // 设置阈值为0，这样即使输入框为空也会显示所有选项
            binding.tagGroupInput.threshold = 0
        }
    }

    /**
     * 设置监听器
     */
    private fun setupListeners() {
        // 标签名称输入监听
        binding.tagNameInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val name = s.toString().trim()
                tagModel.name = name
                validateForm()
            }
        })

        // 标签分组选择监听（从下拉列表选择时）
        binding.tagGroupInput.setOnItemClickListener { _, _, _, _ ->
            val selectedGroup = binding.tagGroupInput.text.toString().trim()
            tagModel.group = selectedGroup
        }

        // 分组输入文本变化监听（支持自定义分组输入）
        binding.tagGroupInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val group = s.toString().trim()
                tagModel.group = group
            }
        })

        // 点击分组输入框时显示下拉列表
        binding.tagGroupInput.setOnClickListener {
            binding.tagGroupInput.showDropDown()
        }

        // 获得焦点时也显示下拉列表
        binding.tagGroupInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.tagGroupInput.showDropDown()
            }
        }

        // 颜色选择项点击 - 简化为单一点击事件
        binding.colorSelectionItem.setOnClickListener {
            showColorPicker()
        }

        // 保存按钮点击
        binding.saveButton.setOnClickListener {
            saveTag()
        }
    }

    /**
     * 显示颜色选择器
     */
    private fun showColorPicker() {
        ColorPickerDialog(requireActivity())
            .setColorConfig(tagModel.color) { selectedColor ->
                tagModel.color = selectedColor
                updateColorDisplay(selectedColor)
            }
            .show()
    }

    /**
     * 更新颜色显示
     */
    private fun updateColorDisplay(color: String) {
        try {
            val colorInt = color.toColorInt()

            // 通过include布局访问内部的颜色圆圈
            val colorCircle = binding.colorItemLayout.colorCircle
            val checkIcon = binding.colorItemLayout.checkIcon

            // 创建圆形背景并设置颜色
            val circleDrawable = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(colorInt)
            }
            colorCircle.background = circleDrawable

            // 隐藏选中图标（在标签编辑页面不需要显示）
            checkIcon.isVisible = false

            // 更新颜色文本显示
            binding.colorText.text = color.uppercase()
        } catch (e: Exception) {
            // 使用默认颜色
            val colorCircle = binding.colorItemLayout.colorCircle
            val checkIcon = binding.colorItemLayout.checkIcon

            val circleDrawable = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(defaultColor.toColorInt())
            }
            colorCircle.background = circleDrawable
            checkIcon.isVisible = false

            binding.colorText.text = defaultColor
            tagModel.color = defaultColor
        }
    }

    /**
     * 验证表单
     */
    private fun validateForm() {
        val name = binding.tagNameInput.text.toString().trim()

        if (name.isEmpty()) {
            binding.tagNameLayout.error = getString(R.string.tag_name_required)
            binding.saveButton.isEnabled = false
            return
        }

        if (name.length > 50) {
            binding.tagNameLayout.error = getString(R.string.tag_name_too_long)
            binding.saveButton.isEnabled = false
            return
        }

        // 检查名称重复（异步）
        lifecycleScope.launch {
            try {
                val isAvailable = TagAPI.checkNameAvailable(name, tagModel.id)
                if (!isAvailable) {
                    binding.tagNameLayout.error = getString(R.string.tag_name_exists)
                    binding.saveButton.isEnabled = false
                } else {
                    binding.tagNameLayout.error = null
                    binding.saveButton.isEnabled = true
                }
            } catch (e: Exception) {
                // 网络错误时允许保存，让服务端处理
                binding.tagNameLayout.error = null
                binding.saveButton.isEnabled = true
            }
        }
    }

    /**
     * 保存标签
     */
    private fun saveTag() {
        val name = binding.tagNameInput.text.toString().trim()

        if (name.isEmpty()) {
            ToastUtils.error(R.string.tag_name_required)
            return
        }

        tagModel.name = name

        lifecycleScope.launch {
            val result = TagAPI.put(tagModel)

            if (result.get("code").asInt == 200) {
                ToastUtils.info(
                    if (isEditing) R.string.tag_updated_successfully
                    else R.string.tag_created_successfully
                )
                findNavController().popBackStack()
            } else {
                ToastUtils.error(result.get("msg").asString)
            }
        }
    }
}
