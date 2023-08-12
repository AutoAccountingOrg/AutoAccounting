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

package net.ankio.auto.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.database.Db
import net.ankio.auto.database.table.AppData
import net.ankio.auto.database.table.Regular
import net.ankio.auto.databinding.FragmentDataBinding
import net.ankio.auto.ui.adapter.DataAdapter
import net.ankio.auto.ui.adapter.RuleAdapter
import net.ankio.auto.ui.adapter.RuleItemListener


class RuleFragment : Fragment() {
    private lateinit var binding: FragmentDataBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RuleAdapter
    private lateinit var layoutManager: LinearLayoutManager
    private val dataItems = mutableListOf<Regular>()
    private var currentPage = 0
    private val itemsPerPage = 10

    override fun onCreateView(   inflater: LayoutInflater,
                                 container: ViewGroup?,
                                 savedInstanceState: Bundle?
    ): View {
        binding = FragmentDataBinding.inflate(layoutInflater)
        recyclerView = binding.recyclerView
        layoutManager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = layoutManager

        adapter = RuleAdapter(dataItems,object : RuleItemListener {



            override fun onClickDelete(item: Regular, position: Int) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(requireContext().getString(R.string.delete_data))
                    .setMessage(requireContext().getString(R.string.delete_msg))
                    .setNegativeButton(requireContext().getString(R.string.sure_msg)) { _, _ ->
                        lifecycleScope.launch {
                            Db.get().RegularDao().del(item.id)
                            withContext(Dispatchers.Main) {
                                adapter.notifyItemRemoved(position)
                            }
                        }
                    }
                    .setPositiveButton(requireContext().getString(R.string.cancel_msg)) { _, _ -> }
                    .show()
            }

            override fun onClickEditData(item: Regular, position: Int) {
               //TODO 规则编辑功能
            }


        })

        recyclerView.adapter = adapter

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (visibleItemCount + firstVisibleItemPosition >= totalItemCount
                    && firstVisibleItemPosition >= 0
                ) {
                    loadMoreData()
                }
            }
        })

        loadMoreData()



        return binding.root
    }

    private fun loadMoreData() {

        lifecycleScope.launch {
            val newData = Db.get().RegularDao().loadAll(currentPage * itemsPerPage +1 ,itemsPerPage )
            val collection: Collection<Regular> = newData?.filterNotNull() ?: emptyList()
            withContext(Dispatchers.Main) {
                // 在主线程更新 UI
                dataItems.addAll(collection)
                if(!collection.isEmpty()){
                    adapter.notifyItemRangeInserted(currentPage * itemsPerPage +1, itemsPerPage )
                    currentPage++
                }

            }
        }



    }

    override fun onResume() {
        super.onResume()
        //加载数据
    }

}