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


package net.ankio.auto.ui.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.App
import net.ankio.auto.R
import net.ankio.auto.databinding.AdapterCategoryListBinding
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import net.ankio.auto.ui.scope.autoDisposeScope
import net.ankio.auto.ui.utils.ResourceUtils
import org.ezbook.server.db.model.CategoryModel

/**
 * 分类选择器适配器
 * @property dataItems 分类数据列表
 * @property onItemClick 点击事件回调
 * @property onItemChildClick 子项点击事件回调
 */

class CategorySelectorAdapter(
    val dataItems: MutableList<CategoryModel>,
    private val onItemClick: (item: CategoryModel, pos: Int, hasChild: Boolean, view: View) -> Unit,
    private val onItemChildClick: (item: CategoryModel, pos: Int) -> Unit,
) : BaseAdapter<AdapterCategoryListBinding, CategoryModel>(
    AdapterCategoryListBinding::class.java,
    dataItems
) {
    override fun onInitViewHolder(holder: BaseViewHolder<AdapterCategoryListBinding, CategoryModel>) {
    }

    /**
     * 二级分类缓存
     */
    private val level2 = hashMapOf<Long, MutableList<CategoryModel>>()

    /**
     * 当前面板对应的Item
     */
    private var panelItem: CategoryModel? = null

    /**
     * 加载二级分类的数据
     */
    private fun loadData(
        data: CategoryModel,
        binding: AdapterCategoryListBinding,
        callback: (MutableList<CategoryModel>) -> Unit
    ) {
        if (!level2.containsKey(data.id)) {
            //获取二级菜单
            binding.root.autoDisposeScope.launch {
                val list = CategoryModel.list(data.remoteBookId, data.type, data.remoteId)
                val items = list.toMutableList()
                level2[data.id] = items
                withContext(Dispatchers.Main) {
                    callback(items)
                }
            }
        } else {
            callback(level2[data.id]!!)
        }
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder<AdapterCategoryListBinding, CategoryModel>,
        data: CategoryModel,
        position: Int
    ) {
        val binding = holder.binding
        setActive(binding, false)
        if (data.isPanel()) {
            renderPanel(binding, data, holder.context)
        } else {
            renderCategory(binding, data, position)
        }

    }

    /**
     * 渲染面板
     */
    private fun renderPanel(
        binding: AdapterCategoryListBinding,
        data: CategoryModel,
        context: Context
    ) {
        binding.icon.visibility = View.GONE
        binding.container.visibility = View.VISIBLE
        binding.recyclerView.layoutManager = GridLayoutManager(context, 5)
        val categories = mutableListOf<CategoryModel>()
        val adapter = CategorySelectorAdapter(categories, { childItem, pos, _, _ ->
            onItemChildClick(childItem, pos)
        }, { _, _ ->
            // 因为二级分类下面不会再有子类，所以子类点击直接忽略。
        })
        binding.recyclerView.adapter = adapter

        // 面板没有子类，所以无法渲染~

        loadData(panelItem!!, binding) {
            categories.clear()
            categories.addAll(it)
            adapter.notifyDataSetChanged()
            val leftDistanceView2: Int = data.id.toInt()
            val layoutParams = binding.imageView.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.leftMargin = leftDistanceView2 // 设置左边距
        }
    }

    private var prevBinding: AdapterCategoryListBinding? = null

    /**
     * 渲染分类图标
     */
    private fun renderCategory(
        binding: AdapterCategoryListBinding,
        data: CategoryModel,
        position: Int
    ) {

        binding.icon.visibility = View.VISIBLE
        binding.container.visibility = View.GONE
        binding.root.autoDisposeScope.launch {
            ResourceUtils.getCategoryDrawable(data, binding.itemImageIcon)
        }
        binding.itemText.text = data.name

        binding.root.setOnClickListener {
            if (data.isPanel()) return@setOnClickListener
            val hasChild = binding.ivMore.visibility == View.VISIBLE
            if (prevBinding != null) {
                setActive(prevBinding!!, false)
            }
            prevBinding = binding
            setActive(binding, true)
            panelItem = data
            onItemClick(data, position, hasChild, binding.itemImageIcon)
        }
        // 本身就是二级菜单，无需继续获取二级菜单
        if (data.isChild()) {
            renderMoreItem(binding, false)
            return
        }
        loadData(data, binding) {
            renderMoreItem(binding, it.isNotEmpty())
        }

    }

    private fun renderMoreItem(binding: AdapterCategoryListBinding, hasChild: Boolean) {
        binding.ivMore.visibility = if (hasChild) View.VISIBLE else View.GONE

    }

    private fun setActive(
        binding: AdapterCategoryListBinding,
        isActive: Boolean,
    ) {
        val (textColor, imageBackground, imageColorFilter) =
            if (isActive) {
                Triple(
                    App.getThemeAttrColor(com.google.android.material.R.attr.colorPrimary),
                    R.drawable.rounded_border,
                    App.getThemeAttrColor(com.google.android.material.R.attr.colorOnPrimary),
                )
            } else {
                Triple(
                    App.getThemeAttrColor(com.google.android.material.R.attr.colorSecondary),
                    R.drawable.rounded_border_,
                    App.getThemeAttrColor(com.google.android.material.R.attr.colorSecondary),
                )
            }

        binding.itemText.setTextColor(textColor)
        binding.itemImageIcon.apply {
            setBackgroundResource(imageBackground)
            setColorFilter(imageColorFilter)
        }
        binding.ivMore.apply {
            setBackgroundResource(imageBackground)
            setColorFilter(imageColorFilter)
        }
    }

}

