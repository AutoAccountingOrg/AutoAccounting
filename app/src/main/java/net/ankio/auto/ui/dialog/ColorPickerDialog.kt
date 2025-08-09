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

package net.ankio.auto.ui.dialog

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleService
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.ankio.auto.databinding.DialogColorPickerBinding
import net.ankio.auto.databinding.ItemColorBinding
import net.ankio.auto.ui.api.BaseSheetDialog

/**
 * 颜色选择对话框
 *
 * 提供预设颜色和自定义颜色输入功能
 */
class ColorPickerDialog : BaseSheetDialog<DialogColorPickerBinding> {

    /** 初始颜色值 */
    private var initialColor: String = "#2196F3"

    /** 颜色选择回调函数 */
    private var onColorSelected: ((String) -> Unit)? = null

    /** 当前选中的颜色 */
    private var selectedColor: String = initialColor

    /** 颜色网格适配器 */
    private lateinit var colorAdapter: ColorAdapter

    /**
     * 使用Activity构造颜色选择对话框
     *
     * @param activity 宿主Activity
     */
    constructor(activity: Activity) : super(activity)

    /**
     * 使用Fragment构造颜色选择对话框
     *
     * @param fragment 宿主Fragment
     */
    constructor(fragment: Fragment) : super(fragment)

    /**
     * 使用LifecycleService构造颜色选择对话框（悬浮窗模式）
     *
     * @param service 宿主Service
     */
    constructor(service: LifecycleService) : super(service)

    // 预设颜色列表
    private val presetColors = listOf(
        "#F44336", "#E91E63", "#9C27B0", "#673AB7",
        "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4",
        "#009688", "#4CAF50", "#8BC34A", "#CDDC39",
        "#FFEB3B", "#FFC107", "#FF9800", "#FF5722",
        "#795548", "#9E9E9E", "#607D8B", "#000000"
    )

    /**
     * 设置初始颜色和颜色选择回调
     *
     * @param initialColor 初始颜色值，默认为蓝色
     * @param onColorSelected 颜色选择完成时的回调函数
     * @return ColorPickerDialog 当前实例，支持链式调用
     */
    fun setColorConfig(
        initialColor: String = "#2196F3",
        onColorSelected: (String) -> Unit
    ): ColorPickerDialog {
        this.initialColor = initialColor
        this.selectedColor = initialColor
        this.onColorSelected = onColorSelected
        return this
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        setupViews()
        setupColorGrid()
        setupCustomColorInput()
        setupButtons()

        // 设置初始颜色
        updateSelectedColor(initialColor)
    }

    /**
     * 设置视图
     */
    private fun setupViews() {
        // 初始化当前颜色显示
        updateColorPreview(selectedColor)
    }

    /**
     * 设置颜色网格
     */
    private fun setupColorGrid() {
        colorAdapter = ColorAdapter(presetColors) { color ->
            updateSelectedColor(color)
        }

        binding.colorGrid.apply {
            layoutManager = GridLayoutManager(context, 5)
            adapter = colorAdapter
        }
    }

    /**
     * 设置自定义颜色输入
     */
    private fun setupCustomColorInput() {
        binding.customColorInput.apply {
            setText(selectedColor)

            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val input = s.toString().trim()
                    if (isValidHexColor(input)) {
                        updateSelectedColor(input)
                    }
                }
            })
        }
    }

    /**
     * 设置按钮
     */
    private fun setupButtons() {
        binding.cancelButton.setOnClickListener {
            dismiss()
        }

        binding.confirmButton.setOnClickListener {
            if (isValidHexColor(selectedColor)) {
                onColorSelected?.invoke(selectedColor)
                dismiss()
            }
        }
    }

    /**
     * 更新选中的颜色
     */
    private fun updateSelectedColor(color: String) {
        if (!isValidHexColor(color)) return

        selectedColor = color
        updateColorPreview(color)
        colorAdapter.updateSelectedColor(color)

        // 更新自定义颜色输入框（如果不是用户输入触发的）
        if (binding.customColorInput.text.toString() != color) {
            binding.customColorInput.setText(color)
        }
    }

    /**
     * 更新颜色预览
     */
    private fun updateColorPreview(color: String) {
        try {
            val colorInt = Color.parseColor(color)
            binding.currentColorPreview.backgroundTintList = ColorStateList.valueOf(colorInt)
            binding.currentColorText.text = color.uppercase()
        } catch (e: Exception) {
            // 无效颜色，使用默认颜色
            val defaultColor = Color.parseColor("#2196F3")
            binding.currentColorPreview.backgroundTintList = ColorStateList.valueOf(defaultColor)
            binding.currentColorText.text = "#2196F3"
        }
    }

    /**
     * 验证十六进制颜色是否有效
     */
    private fun isValidHexColor(color: String): Boolean {
        return try {
            Color.parseColor(color)
            color.matches(Regex("^#[0-9A-Fa-f]{6}$"))
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 颜色网格适配器
     */
    private inner class ColorAdapter(
        private val colors: List<String>,
        private val onColorClick: (String) -> Unit
    ) : RecyclerView.Adapter<ColorAdapter.ColorViewHolder>() {

        private var selectedColor: String = initialColor

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
            val binding = ItemColorBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ColorViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
            holder.bind(colors[position])
        }

        override fun getItemCount(): Int = colors.size

        fun updateSelectedColor(color: String) {
            val oldSelected = selectedColor
            selectedColor = color

            // 刷新相关项
            colors.forEachIndexed { index, c ->
                if (c == oldSelected || c == color) {
                    notifyItemChanged(index)
                }
            }
        }

        inner class ColorViewHolder(private val binding: ItemColorBinding) :
            RecyclerView.ViewHolder(binding.root) {

            fun bind(color: String) {
                try {
                    val colorInt = Color.parseColor(color)
                    binding.colorCircle.backgroundTintList = ColorStateList.valueOf(colorInt)

                    // 显示/隐藏选中指示器
                    binding.selectedIndicator.isVisible = (color == selectedColor)

                    // 设置点击事件
                    binding.colorItem.setOnClickListener {
                        onColorClick(color)
                    }
                } catch (e: Exception) {
                    // 无效颜色，隐藏此项
                    binding.root.isVisible = false
                }
            }
        }
    }
}
