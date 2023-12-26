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


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.database.Db
import net.ankio.auto.database.table.BookName
import net.ankio.auto.databinding.BookSelectDialogBinding
import net.ankio.auto.ui.adapter.BookItemListener
import net.ankio.auto.ui.adapter.BookSelectorAdapter



class BookSelectorDialog(context: Context) : BottomSheetDialog(context) {

    private lateinit var callback: (BookName) -> Unit
    fun show(float: Boolean,callback: (BookName) -> Unit){
        this.callback = callback
        // 创建 BottomSheetDialogFragment
        val dialogFragment = this

        val root = this.onCreateView()

        this.setContentView(root)


        if(float){
            dialogFragment.window?.setType((WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY));
        }
        this.setOnShowListener {
          /*  val bottomSheet =
                dialogFragment.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheet != null) {
                val behavior: BottomSheetBehavior<*> =
                    BottomSheetBehavior.from(bottomSheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.isDraggable = false // 禁用拖动

                // 设置高度为屏幕高度的 50%
                val parent = bottomSheet.parent as View
                val height = parent.height
                behavior.peekHeight = height / 2
            }*/
        }
        // 显示 BottomSheetDialogFragment
        dialogFragment.show()

    }
    private lateinit var binding:BookSelectDialogBinding
    fun onCreateView(): View {
        binding =  BookSelectDialogBinding.inflate(LayoutInflater.from(context))
         val layoutManager = LinearLayoutManager(context)
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


}