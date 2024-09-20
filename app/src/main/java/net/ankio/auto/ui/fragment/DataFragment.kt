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

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.App
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentDataRuleBinding
import net.ankio.auto.ui.adapter.AppDataAdapter
import net.ankio.auto.ui.api.BaseActivity
import net.ankio.auto.ui.api.BasePageFragment
import net.ankio.auto.ui.componets.CustomNavigationRail
import net.ankio.auto.ui.models.RailMenuItem
import net.ankio.auto.ui.models.ToolbarMenuItem
import org.ezbook.server.constant.DataType
import org.ezbook.server.db.model.AppDataModel
import java.lang.ref.WeakReference

class DataFragment : BasePageFragment<AppDataModel>() {
    private lateinit var binding: FragmentDataRuleBinding
    var app: String = ""
    var type: String = ""
    override suspend fun loadData(callback: (resultData: List<AppDataModel>) -> Unit) {
        AppDataModel.list(app, type, page, pageSize, searchData).let { result ->
            withContext(Dispatchers.Main) {
                callback(result)
            }
        }
    }

    override val menuList: ArrayList<ToolbarMenuItem> =
        arrayListOf(
            ToolbarMenuItem(R.string.item_search, R.drawable.menu_icon_search, true) {
                loadDataInside()
            },
            ToolbarMenuItem(R.string.item_clear, R.drawable.menu_icon_clear) {
                MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(requireActivity().getString(R.string.delete_data))
                    .setMessage(requireActivity().getString(R.string.delete_msg))
                    .setNegativeButton(requireActivity().getString(R.string.sure_msg)) { _, _ ->
                        lifecycleScope.launch {
                            AppDataModel.clear()
                            page = 1
                            loadDataInside()
                        }
                    }
                    .setPositiveButton(requireActivity().getString(R.string.cancel_msg)) { _, _ -> }
                    .show()

            },
        )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentDataRuleBinding.inflate(layoutInflater)
        statusPage = binding.statusPage
        val recyclerView = binding.statusPage.contentView!!
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        //scrollView = WeakReference(recyclerView)

        recyclerView.adapter = AppDataAdapter(pageData, requireActivity() as BaseActivity)
        loadDataEvent(binding.refreshLayout)
        loadLeftData(binding.leftList)
        chipEvent()
        return binding.root
    }

    private var leftData = JsonObject()
    private fun loadLeftData(leftList: CustomNavigationRail) {
        lifecycleScope.launch {
            AppDataModel.apps().let { result ->
                leftData = result
                var i = 0
                for (key in result.keySet()) {
                    i++
                    val app = App.getAppInfoFromPackageName(key) ?: continue
                    leftList.addMenuItem(
                        RailMenuItem(i, app[1] as Drawable, app[0] as String)
                    )

                }
                if (!leftList.triggerFirstItem()) {
                    statusPage.showEmpty()
                }
            }
        }

        leftList.setOnItemSelectedListener {
            val id = it.id
            page = 1
            app = leftData.keySet().elementAt(id - 1)
            statusPage.showLoading()
            loadDataInside()
        }
    }

    /**
     * Chip事件
     */
    private fun chipEvent() {
        binding.chipGroup.setOnCheckedStateChangeListener { group, checkedId ->
            val chipId = checkedId.firstOrNull() ?: R.id.chip_all

            when (chipId) {
                R.id.chip_all -> {
                    type = ""
                }

                R.id.chip_notify -> {
                    type = DataType.NOTICE.name
                }

                R.id.chip_data -> {
                    type = DataType.DATA.name
                }
            }
            loadDataInside()
        }
    }


}
