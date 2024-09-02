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

package net.ankio.auto.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import net.ankio.auto.databinding.FragmentMapBinding
import net.ankio.auto.ui.adapter.AssetsMapAdapter
import net.ankio.auto.ui.api.BasePageFragment
import net.ankio.auto.ui.dialog.AssetsMapDialog
import org.ezbook.server.db.model.AssetsMapModel

class AssetMapFragment : BasePageFragment<AssetsMapModel>() {
    private lateinit var binding: FragmentMapBinding

    override suspend fun loadData(callback: (resultData: List<AssetsMapModel>) -> Unit) {
        lifecycleScope.launch {
            val newData = AssetsMapModel.list(page,pageSize)
            callback(newData)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentMapBinding.inflate(layoutInflater)
        statusPage = binding.statusPage
        val recyclerView = statusPage.contentView!!
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = AssetsMapAdapter(pageData,requireActivity())
        scrollView = recyclerView

        binding.addButton.setOnClickListener {
            AssetsMapDialog(requireContext()) { model ->
                lifecycleScope.launch {
                    AssetsMapModel.put(model)
                    statusPage.showLoading()
                    loadDataInside()
                }
            }.show(cancel = true)
        }

        loadDataEvent(binding.refreshLayout)

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        statusPage.showLoading()
        loadDataInside()
    }
}