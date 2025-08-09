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
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleService
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.ankio.auto.databinding.DialogColorPickerBinding
import net.ankio.auto.databinding.ItemColorBinding
import net.ankio.auto.ui.api.BaseSheetDialog
import androidx.core.graphics.toColorInt

/**
 * 简化的颜色选择对话框
 *
 * 只提供颜色圆圈选择功能，选中状态通过外圈高亮显示
 */
class ColorPickerDialog : BaseSheetDialog<DialogColorPickerBinding> {

    /** 颜色选择回调函数 */
    private var onColorSelected: ((String) -> Unit)? = null

    /** 当前选中的颜色 */
    private var selectedColor: String = "#2196F3"

    /** 颜色网格适配器 */
    private lateinit var colorAdapter: ColorAdapter

    /**
     * 构造函数
     */
    constructor(activity: Activity) : super(activity)
    constructor(fragment: Fragment) : super(fragment)
    constructor(service: LifecycleService) : super(service)

    // 预设颜色列表 - 优化为适合标签底色的颜色
    private val colors = listOf(
        // 红色系 - 温和的红色调
        "#E57373", "#F06292", "#EF5350", "#FF7043",
        // 粉色系 - 柔和的粉色
        "#F48FB1", "#CE93D8", "#BA68C8", "#AB47BC",
        // 紫色系 - 优雅的紫色
        "#9575CD", "#7986CB", "#64B5F6", "#42A5F5",
        // 蓝色系 - 舒适的蓝色
        "#29B6F6", "#26C6DA", "#26A69A", "#66BB6A",
        // 绿色系 - 自然的绿色
        "#81C784", "#AED581", "#DCE775", "#FFF176",
        // 黄色系 - 温暖但不刺眼的黄色
        "#FFD54F", "#FFCC02", "#FFB74D", "#FF8A65",
        // 橙色系 - 活力的橙色
        "#A1887F", "#90A4AE", "#78909C", "#546E7A",
        // 中性色系 - 优雅的灰色调
        "#8D6E63", "#795548", "#607D8B", "#455A64"
    )

    /**
     * 设置颜色配置
     */
    fun setColorConfig(
        initialColor: String = "#2196F3",
        onColorSelected: (String) -> Unit
    ): ColorPickerDialog {
        this.selectedColor = initialColor
        this.onColorSelected = onColorSelected
        return this
    }

    override fun onViewCreated(view: View?) {
        super.onViewCreated(view)
        setupColorGrid()
        setupConfirmButton()

        // 确保初始选中状态正确显示
        colorAdapter.updateSelectedColor(selectedColor)
    }

    /**
     * 设置颜色网格
     */
    private fun setupColorGrid() {
        colorAdapter = ColorAdapter(colors) { color ->
            selectedColor = color
            colorAdapter.updateSelectedColor(color)
        }

        binding.colorGrid.apply {
            layoutManager = GridLayoutManager(context, 5) // 5列更合适的布局
            adapter = colorAdapter
            // 禁用RecyclerView的滚动，让ScrollView处理滚动
            isNestedScrollingEnabled = false
        }
    }

    /**
     * 设置确认按钮
     */
    private fun setupConfirmButton() {
        binding.confirmButton.setOnClickListener {
            onColorSelected?.invoke(selectedColor)
            dismiss()
        }
    }

    /**
     * 根据背景颜色计算最佳对比色
     * @param backgroundColor 背景颜色
     * @return 对比色（黑色或白色）
     */
    private fun getContrastColor(backgroundColor: Int): Int {
        // 计算颜色的相对亮度
        val luminance = ColorUtils.calculateLuminance(backgroundColor)
        // 如果亮度大于0.5，使用黑色；否则使用白色
        return if (luminance > 0.5) Color.BLACK else Color.WHITE
    }

    /**
     * 颜色网格适配器
     */
    private inner class ColorAdapter(
        private val colors: List<String>,
        private val onColorClick: (String) -> Unit
    ) : RecyclerView.Adapter<ColorAdapter.ColorViewHolder>() {

        private var selectedColor: String = this@ColorPickerDialog.selectedColor

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

        /**
         * 更新选中颜色
         */
        fun updateSelectedColor(color: String) {
            val oldSelected = selectedColor
            selectedColor = color

            // 只刷新相关的项目
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
                    val colorInt = color.toColorInt()

                    // 创建圆形背景
                    val circleDrawable = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(colorInt)
                    }
                    binding.colorCircle.background = circleDrawable

                    // 选中效果 - 显示对号
                    val isSelected = (color == selectedColor)
                    binding.checkIcon.isVisible = isSelected

                    // 根据背景颜色计算对比色，设置对号颜色
                    if (isSelected) {
                        val contrastColor = getContrastColor(colorInt)
                        binding.checkIcon.setColorFilter(contrastColor)
                    }

                    // 设置点击事件
                    binding.colorItem.setOnClickListener {
                        onColorClick(color)
                    }
                } catch (e: Exception) {
                    // 无效颜色则隐藏
                    binding.root.isVisible = false
                }
            }
        }
    }
}
