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
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.elevation.SurfaceColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.database.Db
import net.ankio.auto.database.table.BookName
import net.ankio.auto.databinding.BookSelectDialogBinding
import net.ankio.auto.ui.adapter.BookItemListener
import net.ankio.auto.ui.adapter.BookSelectorAdapter
import okhttp3.internal.notifyAll


class BookSelectorDialog: BottomSheetDialogFragment() {

    private lateinit var callback: (BookName) -> Unit
    fun show(context: Activity,float: Boolean,callback: (BookName) -> Unit){
        this.callback = callback
        // 创建 BottomSheetDialogFragment
        val dialogFragment = this

        if(float){
            dialog?.window?.setType((WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY));
        }
        // 显示 BottomSheetDialogFragment
        dialogFragment.show((context as AppCompatActivity).supportFragmentManager, dialogFragment.tag)

    }
    private lateinit var binding:BookSelectDialogBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =  BookSelectDialogBinding.inflate(inflater)
         val layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.layoutManager = layoutManager
        val dataItems = mutableListOf<BookName>()
        val adapter = BookSelectorAdapter(dataItems,object:BookItemListener{
            override fun onClick(item: BookName, position: Int) {
                callback(item)
                this@BookSelectorDialog.dismiss()
            }
        })
        //binding.recyclerView.setBackgroundColor(SurfaceColors.SURFACE_1.getColor(requireContext()))
        binding.recyclerView.adapter = adapter
        lifecycleScope.launch {
            val newData = Db.get().BookNameDao().loadAll()
            val defaultBook = BookName()
            defaultBook.name = "默认账本"
            defaultBook.id = 1
            val collection = newData?.mapNotNull { it }?.takeIf { it.isNotEmpty() } ?: listOf(defaultBook)
            withContext(Dispatchers.Main) {
                // 在主线程更新 UI
                dataItems.addAll(collection)
                if(collection.isNotEmpty()){
                    adapter.notifyItemInserted(0)
                }

            }
        }

        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 禁止用户通过下滑手势关闭对话框
        dialog?.setCancelable(false)

        // 允许用户通过点击空白处关闭对话框
        dialog?.setCanceledOnTouchOutside(true)

        // Get the display metrics using the DisplayMetrics directly
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels

        // Calculate and set dialog height as a percentage of screen height
        val dialogHeight = (screenHeight * 0.5).toInt() // 50% height
        view.layoutParams.height = dialogHeight
    }

}