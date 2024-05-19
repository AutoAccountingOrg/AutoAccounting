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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentDataBinding
import net.ankio.auto.ui.adapter.RuleAdapter
import net.ankio.auto.ui.fragment.BaseFragment
import net.ankio.auto.ui.utils.MenuItem
import net.ankio.auto.utils.server.model.Regular

class RuleFragment : BaseFragment() {
    private lateinit var binding: FragmentDataBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RuleAdapter
    private lateinit var layoutManager: LinearLayoutManager
    private val dataItems = mutableListOf<Regular>()
    override val menuList: ArrayList<MenuItem>
        get() =
            arrayListOf(
                MenuItem(R.string.item_add, R.drawable.menu_item_add) {
                    it.navigate(R.id.editFragment)
                },
            )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentDataBinding.inflate(layoutInflater)
        recyclerView = binding.recyclerView
        layoutManager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = layoutManager
        scrollView = recyclerView
        adapter =
            RuleAdapter(
                dataItems,
                onClickEdit = { item, position ->
                    val bundle =
                        Bundle().apply {
                            putSerializable("regular", item)
                        }
                    findNavController().navigate(R.id.editFragment, bundle)
                },
                onClickDelete = { item, position ->
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(requireContext().getString(R.string.delete_data))
                        .setMessage(requireContext().getString(R.string.delete_msg))
                        .setNegativeButton(requireContext().getString(R.string.sure_msg)) { _, _ ->
                            lifecycleScope.launch {
                                withContext(Dispatchers.IO) {
                                    Regular.remove(item.id)
                                }
                                dataItems.removeAt(position)
                                adapter.notifyItemRemoved(position)
                            }
                        }
                        .setPositiveButton(requireContext().getString(R.string.cancel_msg)) { _, _ -> }
                        .show()
                },
            )

        recyclerView.adapter = adapter

        return binding.root
    }

    private fun loadData() {
        lifecycleScope.launch {
            val newData = Regular.get(500)
            val collection: Collection<Regular> = newData
            dataItems.clear()
            dataItems.addAll(collection)
            adapter.notifyDataSetChanged()
            binding.empty.root.visibility = if (collection.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        // 加载数据

        loadData()
    }
}
