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
 *//*


package net.ankio.auto.ui.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.databinding.AdapterCategoryListBinding
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.storage.ImageUtils
import net.ankio.auto.utils.server.model.Category

*/
/**
 * 分类选择器适配器
 * @property dataItems 分类数据列表
 * @property onItemClick 点击事件回调
 * @property onItemChildClick 子项点击事件回调
 *//*

class CategorySelectorAdapter(
    override val dataItems: List<Category>,
    private val onItemClick: (item: Category, pos: Int, hasChild: Boolean, view: View) -> Unit,
    private val onItemChildClick: (item: Category, pos: Int) -> Unit,
) : BaseAdapter(dataItems, AdapterCategoryListBinding::class.java) {
    override fun onBindViewHolder(
        holder: BaseViewHolder,
        position: Int,
        payloads: MutableList<Any>,
    ) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            // 如果有 payload，根据 payload 更新部分内容
            val category = payloads[0] as Category
            val viewHolder = holder as ViewHolder
            viewHolder.item = category
            viewHolder.updatePanel(category)
        }
    }

    override fun onBindView(
        holder: BaseViewHolder,
        item: Any,
    ) {
        val it = item as Category
        val viewHolder = holder as ViewHolder
        viewHolder.bind(it)
    }

    override fun onInitView(holder: BaseViewHolder) {
        val viewHolder = holder as ViewHolder

        val binding = viewHolder.binding
        binding.itemImageIcon.setOnClickListener {
            if (itemTextView != null) {
                viewHolder.setActive(itemTextView!!, itemImageIcon!!, ivMore!!, false)
            }
            viewHolder.setActive(binding.itemText, binding.itemImageIcon, binding.ivMore, true)

            itemTextView = binding.itemText
            itemImageIcon = binding.itemImageIcon
            ivMore = binding.ivMore

            val item = holder.item as Category
            onItemClick(item, getHolderIndex(holder), binding.ivMore.visibility == View.VISIBLE, it)
        }
    }

    override fun wrapHolder(viewBinding: ViewBinding): BaseViewHolder {
        return ViewHolder(viewBinding as AdapterCategoryListBinding, viewBinding.root.context)
    }

    private var itemTextView: TextView? = null
    private var itemImageIcon: ImageView? = null
    private var ivMore: ImageView? = null

    */
/**
     * ViewHolder内部类
     * @property binding 视图绑定
     * @property context 上下文
     *//*

    inner class ViewHolder(
        override val binding: AdapterCategoryListBinding,
        override val context: Context,
    ) : BaseViewHolder(binding, context) {
        private lateinit var adapter: CategorySelectorAdapter

        */
/**
         * 设置激活状态
         *//*

        fun setActive(
            textView: TextView,
            imageView: ImageView,
            imageView2: ImageView,
            isActive: Boolean,
        ) {
            val (textColor, imageBackground, imageColorFilter) =
                if (isActive) {
                    Triple(
                        AppUtils.getThemeAttrColor(com.google.android.material.R.attr.colorPrimary),
                        R.drawable.rounded_border,
                        AppUtils.getThemeAttrColor(com.google.android.material.R.attr.colorOnPrimary),
                    )
                } else {
                    Triple(
                        AppUtils.getThemeAttrColor(com.google.android.material.R.attr.colorSecondary),
                        R.drawable.rounded_border_,
                        AppUtils.getThemeAttrColor(com.google.android.material.R.attr.colorSecondary),
                    )
                }

            textView.setTextColor(textColor)
            imageView.apply {
                setBackgroundResource(imageBackground)
                setColorFilter(imageColorFilter)
            }
            imageView2.apply {
                setBackgroundResource(imageBackground)
                setColorFilter(imageColorFilter)
            }
        }

        fun bind(item: Category) {
            setActive(binding.itemText, binding.itemImageIcon, binding.ivMore, false)
            if (item.isPanel()) { // -2表示他是一个面板，而非item，需要切换为面板视图
                binding.icon.visibility = View.GONE
                binding.container.visibility = View.VISIBLE
                adapter =
                    CategorySelectorAdapter(items, { childItem, pos, _, _ ->
                        onItemChildClick(childItem, pos)
                    }, { _, _ ->
                        // 因为二级分类下面不会再有子类，所以子类点击直接忽略。
                    })
                // 渲染面板视图
                renderPanel(item)
            } else {
                renderItem(item)
            }
        }

        */
/**
         * item渲染
         *//*

        fun renderItem(item: Category) {
            if (item.parent != 0) {
                binding.ivMore.visibility = View.GONE
            } else {
                scope.launch {
                    val count = Category.getAll(item.book, item.type, item.id).size
                    if (count == 0) {
                        withContext(Dispatchers.Main) {
                            binding.ivMore.visibility = View.GONE
                        }
                    }
                }
            }

            if (item.icon.isNullOrEmpty()) {
                binding.itemImageIcon.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        context.resources,
                        R.drawable.default_cate,
                        context.theme,
                    ),
                )
            } else {
                scope.launch {
                    // 自动切回主线程
                    ImageUtils.get(context, item.icon!!, R.drawable.default_cate).let {
                        binding.itemImageIcon.setImageDrawable(it)
                    }
                }
            }

            binding.itemText.text = item.name
        }

        private val items = ArrayList<Category>()

        */
/**
         * 渲染项目
         *//*

        private fun renderPanel(item: Category) {
            val layoutManager = GridLayoutManager(context, 5)
            binding.recyclerView.layoutManager = layoutManager

            binding.recyclerView.adapter = adapter

            updatePanel(item)
        }

        */
/**
         * 更新面板内容，由于面板复用的时候是全部内容替换，所以使用NotifyDataSetChanged
         *//*

        fun updatePanel(item: Category) {
            val leftDistanceView2: Int = item.id
            val layoutParams = binding.imageView.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.leftMargin = leftDistanceView2 // 设置左边距
            scope.launch {
                withContext(Dispatchers.IO) {
                    val newData =
                        Category.getAll(
                            item.book,
                            item.type,
                            item.parent,
                        )

                    val collection = newData.map { it }.takeIf { it.isNotEmpty() } ?: listOf()

                    if (collection.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            items.clear()
                            items.addAll(collection)
                            adapter.notifyDataSetChanged()
                        }
                    }
                }
            }
        }
    }
}
*/
