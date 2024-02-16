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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.database.Db
import net.ankio.auto.database.table.Category
import net.ankio.auto.databinding.AdapterCategoryListBinding
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.ImageUtils


class CategorySelectorAdapter(
    private val dataItems: List<Category>,
    private val listener: CateItemListener
) : RecyclerView.Adapter<CategorySelectorAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            AdapterCategoryListBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ), parent.context
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataItems[position]
        holder.bind(item, position)
    }

    override fun getItemCount(): Int {
        return dataItems.size
    }
    fun setActive(textView: TextView,imageView: ImageView,imageView2: ImageView,boolean: Boolean,context: Context){
        if(boolean){
            textView.setTextColor(AppUtils.getThemeAttrColor(com.google.android.material.R.attr.colorPrimary))
            imageView.setBackgroundResource(R.drawable.rounded_border)
            imageView.setColorFilter(AppUtils.getThemeAttrColor(com.google.android.material.R.attr.colorOnPrimary))
            imageView2.setBackgroundResource(R.drawable.rounded_border2)
            imageView2.setColorFilter(AppUtils.getThemeAttrColor(com.google.android.material.R.attr.colorOnPrimary))
        }else{
            textView.setTextColor(AppUtils.getThemeAttrColor(com.google.android.material.R.attr.colorSecondary))
           imageView.setBackgroundResource(R.drawable.rounded_border_)
            imageView.setColorFilter(AppUtils.getThemeAttrColor(com.google.android.material.R.attr.colorSecondary))
            imageView2.setBackgroundResource(R.drawable.rounded_border_2)
            imageView2.setColorFilter(AppUtils.getThemeAttrColor(com.google.android.material.R.attr.colorSecondary))
        }
    }

    private var itemTextView:TextView? = null
    private var itemImageIcon:ImageView? = null
    private var ivMore:ImageView? = null
    inner class ViewHolder(
        private val binding: AdapterCategoryListBinding,
        private val context: Context
    ) :
        RecyclerView.ViewHolder(binding.root) {

        @OptIn(DelicateCoroutinesApi::class)
        fun bind(item: Category, position: Int) {
            //刚绑定false
            setActive(binding.itemText,binding.itemImageIcon,binding.ivMore,false,context)
            if(item.book==-2){
                binding.icon.visibility = View.GONE
                binding.container.visibility = View.VISIBLE
                renderItem(item.parent,item.remoteId,item.id)
                return
            }
            if (item.parent != -1) {
                binding.ivMore.visibility = View.GONE
            } else {
                GlobalScope.launch {
                    if (Db.get().CategoryDao().count(item.book,item.id) == 0) {
                        launch(Dispatchers.Main) {
                            binding.ivMore.visibility = View.GONE
                        }
                    }
                }
            }

            if (item.icon == null) {
                binding.itemImageIcon.setImageDrawable(ResourcesCompat.getDrawable(
                    context.resources,
                    R.drawable.default_cate,
                    context.theme
                ))
            } else {
                GlobalScope.launch {
                    ImageUtils.get(context, item.icon!!, { drawable ->
                        launch(Dispatchers.Main) {
                            binding.itemImageIcon.setImageDrawable(drawable)
                        }
                    }, { error ->
                        launch(Dispatchers.Main) {
                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                            binding.itemImageIcon.setImageDrawable(ResourcesCompat.getDrawable(
                                context.resources,
                                R.drawable.default_cate,
                                context.theme
                            ))
                        }
                    })
                }
            }

            binding.itemText.text = item.name
            binding.itemImageIcon.setOnClickListener {
                if(itemTextView!==null){
                     setActive(itemTextView!!,itemImageIcon!!,ivMore!!,false,context)


                }
                setActive(binding.itemText,binding.itemImageIcon,binding.ivMore,true,context)

                itemTextView = binding.itemText
                itemImageIcon = binding.itemImageIcon
                ivMore = binding.ivMore

                listener.onClick(item, position,binding.ivMore.visibility==View.VISIBLE,it)
            }
        }

        @OptIn(DelicateCoroutinesApi::class)
        private fun renderItem(parent:Int, book: String?,position: Int){
            val bookIndex = book?.toInt()?:1
            val layoutManager = GridLayoutManager(context, 5)
            binding.recyclerView.layoutManager = layoutManager
            val leftDistanceView2: Int = position
            val layoutParams =  binding.imageView.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.leftMargin = leftDistanceView2 // 设置左边距
           // binding.imageView.layoutParams = leftDistanceView2
            val items = ArrayList<Category>()
            val adapter = CategorySelectorAdapter(items,object: CateItemListener {
                override fun onClick(item: Category, position: Int, hasChild: Boolean,view: View) {
                    listener.onChildClick(item, position)
                }
                override fun onChildClick(item: Category, position: Int) {

                }
            })
            binding.recyclerView.adapter = adapter

            GlobalScope.launch {
                val newData = Db.get().CategoryDao().loadAll(bookIndex,parent)
                val collection = newData?.mapNotNull { it }?.takeIf { it.isNotEmpty() } ?: listOf()
                withContext(Dispatchers.Main) {
                    // 在主线程更新 UI
                    items.addAll(collection)
                    if(collection.isNotEmpty()){
                        adapter.notifyItemInserted(0)
                    }

                }
            }

        }

    }
}

interface CateItemListener {
    fun onClick(item: Category, position: Int,hasChild:Boolean,view: View)
    fun onChildClick(item: Category, position: Int)
}