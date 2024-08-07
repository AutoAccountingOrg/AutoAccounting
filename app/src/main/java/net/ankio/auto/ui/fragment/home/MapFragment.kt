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

package net.ankio.auto.ui.fragment.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentMapBinding
//import net.ankio.auto.ui.adapter.MapAdapter
import net.ankio.auto.ui.dialog.MapDialog
import net.ankio.auto.ui.fragment.BaseFragment
import net.ankio.auto.ui.utils.MenuItem
import net.ankio.auto.utils.server.model.AssetsMapModel

class MapFragment : BaseFragment() {
    private lateinit var binding: FragmentMapBinding

  //  private lateinit var adapter: MapAdapter

    private var dataItems = mutableListOf<AssetsMapModel>()
    override val menuList: ArrayList<MenuItem>
        get() =
            arrayListOf(
                MenuItem(R.string.item_add, R.drawable.menu_item_add) {
                    MapDialog(requireContext(), onClose = {
                        dataItems.add(it)
                   //     adapter.notifyItemInserted(dataItems.size - 1)
                    }).show(cancel = true)
                },
            )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentMapBinding.inflate(layoutInflater)
        val layoutManager = LinearLayoutManager(context)
        binding.recyclerView.layoutManager = layoutManager

       /* adapter =
            MapAdapter(
                dataItems,
                onClick = { adapter, item, pos ->
                    MapDialog(requireContext(), item) { changedAssetsMap ->
                        dataItems[pos] = changedAssetsMap
                        adapter.notifyItemChanged(pos)
                    }.show(cancel = true)
                },
                onLongClick = { adapter, item, pos ->
                    // 弹出Material提示框提示是否删除
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.delete_title)
                        .setMessage(getString(R.string.delete_message, item.name))
                        .setNegativeButton(R.string.cancel) { dialog, which ->
                            // 用户点击了取消按钮，不做任何操作
                            dialog.dismiss()
                        }
                        .setPositiveButton(R.string.delete) { _, _ ->
                            // 用户点击了删除按钮，执行删除操作

                            lifecycleScope.launch {
                                withContext(Dispatchers.IO) {
                                    AssetsMap.remove(item.id)
                                }
                            }

                            dataItems.removeAt(pos)
                            adapter.notifyItemRemoved(pos)
                        }
                        .show()
                },
            )

        binding.recyclerView.adapter = adapter*/
        scrollView = binding.recyclerView
        lifecycleScope.launch {
            val newData = AssetsMapModel.get()

            val collection = newData.takeIf { it.isNotEmpty() } ?: listOf()

            dataItems.addAll(collection)


            withContext(Dispatchers.Main) {
             //   adapter.notifyDataSetChanged()
                binding.empty.root.visibility = if (dataItems.isEmpty()) View.VISIBLE else View.GONE

            }
        }

        return binding.root
    }
}
