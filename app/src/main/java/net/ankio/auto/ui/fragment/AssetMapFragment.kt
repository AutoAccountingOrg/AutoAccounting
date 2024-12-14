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
import net.ankio.auto.ui.utils.viewBinding
import org.ezbook.server.db.model.AssetsMapModel

class AssetMapFragment : BasePageFragment<AssetsMapModel>() {

    override val binding: FragmentMapBinding by viewBinding(FragmentMapBinding::inflate)

    override suspend fun loadData(callback: (resultData: List<AssetsMapModel>) -> Unit) {
        lifecycleScope.launch {
            val newData = AssetsMapModel.list(page, pageSize)
            callback(newData)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View  = binding.root

    override fun onCreateAdapter() {
        val recyclerView = statusPage.contentView!!
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = AssetsMapAdapter(pageData, requireActivity())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.addButton.setOnClickListener {
            AssetsMapDialog(requireContext()) { model ->
                lifecycleScope.launch {
                    AssetsMapModel.put(model)
                    reload()
                }
            }.showInFragment(this,false,true)
        }
    }

}