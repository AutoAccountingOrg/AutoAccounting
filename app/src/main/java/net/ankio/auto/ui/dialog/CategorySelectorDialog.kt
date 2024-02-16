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

package net.ankio.auto.ui.dialog

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.database.Db
import net.ankio.auto.database.table.Category
import net.ankio.auto.databinding.CategorySelectDialogBinding
import net.ankio.auto.ui.adapter.CateItemListener
import net.ankio.auto.ui.adapter.CategorySelectorAdapter


class CategorySelectorDialog(private val context: Context,private var book: Int = 0,private val callback: (Category?,Category?) -> Unit) : BaseSheetDialog(context) {

    //父类
    private var category1: Category? = null
    //子类
    private var category2: Category? = null

    private var expand = false //显示状态
    private var line = 5 //默认一行5个


    private lateinit var binding: CategorySelectDialogBinding


    private var items = ArrayList<Category>()

    internal inner class SpecialSpanSizeLookup : SpanSizeLookup() {
        override fun getSpanSize(i: Int): Int {
            val category: Category = items[i]
            return if (category.book == -2) 5 else 1
        }
    }

    private var lastPosition = -1
    private lateinit var adapter: CategorySelectorAdapter

    //计算滑块插入哪一行
    private fun getPanelIndex(position: Int): Int {
        var line = (position + 1) / 5
        if ((position + 1) % 5 != 0) {
            line++
        }
        return line * 5
    }
    //计算滑块偏移距离，生成假的category
    private fun getPanelData(view: View,item:Category): Category {
        val category = Category()
        category.remoteId = item.book.toString()
        category.parent = item.id
        category.book = -2
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val params = view.layoutParams as MarginLayoutParams
        val leftDistanceWithMargin = location[0] + view.paddingLeft + params.leftMargin - 16
        category.id = leftDistanceWithMargin - view.width / 2 + 14
        return category
    }


    override fun onCreateView(inflater: LayoutInflater): View {
        binding = CategorySelectDialogBinding.inflate(inflater)

        this.cardView = binding.cardView

        val layoutManager = GridLayoutManager(context, line)
        layoutManager.spanSizeLookup = SpecialSpanSizeLookup()
        binding.recyclerView.layoutManager = layoutManager
        expand = false
        adapter = CategorySelectorAdapter(items, object : CateItemListener {
            override fun onClick(item: Category, position: Int, hasChild: Boolean, view: View) {
                category1 = item
                val panelPosition = getPanelIndex(position)
                val lastPanelPosition = getPanelIndex(lastPosition)
                if (position == lastPosition) {
                    if(hasChild){
                        //点击两次，说明需要删除
                        items.removeAt(panelPosition)
                        adapter.notifyItemRemoved(panelPosition)
                        lastPosition = -1//归位
                        expand = false
                        category2 = null
                        return
                    }
                    lastPosition = -1
                }
                //构造数据
                val category = getPanelData(view,item)
                // 同一行，不需要做删除，直接更新数据即可
                if(panelPosition==lastPanelPosition){
                    if (hasChild){
                        if(expand){
                            items[panelPosition] = category
                            adapter.notifyItemChanged(panelPosition)
                        }else{
                            items.add(panelPosition, category)
                            adapter.notifyItemInserted(panelPosition)
                            expand = true
                        }
                    }else{
                        //没有就移除
                        if (lastPosition != -1 && expand) {
                            items.removeAt(lastPanelPosition)
                            adapter.notifyItemRemoved(lastPanelPosition)
                            lastPosition = -1//归位
                            expand = false
                            return
                        }
                    }
                }else{
                    //不同行的需要先删除
                    if (lastPosition != -1 && expand) {
                        items.removeAt(lastPanelPosition)
                        adapter.notifyItemRemoved(lastPanelPosition)
                        expand = false
                    }
                    if (hasChild) {
                        items.add(panelPosition, category)
                        adapter.notifyItemInserted(panelPosition)
                        expand = true
                    }
                }
                lastPosition = position

            }

            override fun onChildClick(item: Category, position: Int) {
                category2 = item
            }
        })
        binding.recyclerView.adapter = adapter
        binding.button.setOnClickListener {
            callback(category1,category2)
            dismiss()
        }



        lifecycleScope.launch {
            val newData = withContext(Dispatchers.IO) {
                 Db.get().CategoryDao().loadAll(book)
            }
            val defaultCategory = Category()
            defaultCategory.name = "其他"

            val collection =
                newData?.mapNotNull { it }?.takeIf { it.isNotEmpty() } ?: listOf(defaultCategory)
            withContext(Dispatchers.Main) {
                // 在主线程更新 UI
                items.addAll(collection)
                if (collection.isNotEmpty()) {
                    adapter.notifyItemInserted(0)
                }

            }
        }
        return binding.root
    }


}

