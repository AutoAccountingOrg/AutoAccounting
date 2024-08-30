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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentDataBinding
import net.ankio.auto.ui.adapter.AppDataAdapter
import net.ankio.auto.ui.api.BaseActivity
import net.ankio.auto.ui.api.BasePageFragment
import net.ankio.auto.ui.models.ToolbarMenuItem
import org.ezbook.server.db.model.AppDataModel

class DataFragment : BasePageFragment<AppDataModel>() {
    private lateinit var binding: FragmentDataBinding
    var app:String = ""
    var type:String = ""
    override suspend fun loadData(callback: (resultData: List<AppDataModel>) -> Unit) {
        AppDataModel.list(app, type, page,pageSize).let { result ->
            withContext(Dispatchers.Main) {
                callback(result)
            }
        }
    }

    override val menuList: ArrayList<ToolbarMenuItem> =
        arrayListOf(
            ToolbarMenuItem(R.string.item_clear, R.drawable.menu_icon_clear) {
                lifecycleScope.launch {
                    AppDataModel.clear()
                    page = 1
                    loadDataInside()
                }
            },
        )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentDataBinding.inflate(layoutInflater)
        recyclerView = binding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        scrollView = recyclerView


        recyclerView.adapter = AppDataAdapter(pageData, requireActivity() as BaseActivity)

        return binding.root
    }

}
