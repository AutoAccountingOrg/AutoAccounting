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

package net.ankio.auto.ui.fragment.rule

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.App
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentDataRuleBinding
import net.ankio.auto.ui.adapter.DataRuleAdapter
import net.ankio.auto.ui.api.BasePageFragment
import net.ankio.auto.ui.componets.CustomNavigationRail
import net.ankio.auto.ui.utils.RailMenuItem
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.ui.utils.ToolbarMenuItem
import org.ezbook.server.constant.DataType
import org.ezbook.server.db.model.RuleModel

/**
 * 数据规则Fragment
 */
class DataRuleFragment: BasePageFragment<RuleModel>()  {
    private lateinit var binding: FragmentDataRuleBinding
    override val menuList: ArrayList<ToolbarMenuItem>
        get() =
            arrayListOf(
                ToolbarMenuItem(R.string.item_add, R.drawable.menu_item_add) {
                   ToastUtils.error("敬请期待")
                },
                ToolbarMenuItem(R.string.item_notice, R.drawable.menu_item_notice,true) {
                    // TODO 跳转通知监控列表
                    Log.i("DataRuleFragment", "通知监控$searchData")
                },
            )

    /**
     * 初始化View
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentDataRuleBinding.inflate(layoutInflater)
        recyclerView = binding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = DataRuleAdapter(pageData)
        loadDataEvent(binding.refreshLayout)
        loadLeftData(binding.leftList)
        chipEvent()
        return binding.root
    }

    /**
     * App
     */
    private var app = ""

    /**
     * 类型
     */
    private var type = ""

    /**
     * 加载数据
     */
    override suspend fun loadData(callback: (resultData: List<RuleModel>) -> Unit) {
        RuleModel.list(app, type, page, pageSize).let { result ->
            withContext(Dispatchers.Main) {
                callback(result)
            }

        }
    }

    /**
     * 左侧数据
     */
    private var leftData = JsonObject()
    /**
     * 加载左侧数据
     */
    private fun loadLeftData(leftList: CustomNavigationRail){
        lifecycleScope.launch {
            RuleModel.apps().let { result ->
                leftData = result
                var i = 0
                for (key in result.keySet()){
                    i++
                    val app = App.getAppInfoFromPackageName(key) ?: continue
                    leftList.addMenuItem(
                        RailMenuItem(i, app[1] as Drawable, app[0] as String)
                    )

                }
                leftList.triggerFirstItem()
            }
        }

        leftList.setOnItemSelectedListener {
            val id = it.id
            app = leftData.keySet().elementAt(id - 1)
            page = 1
            loadDataInside()
        }
    }

    /**
     * Chip事件
     */
    private fun chipEvent(){
        binding.chipGroup.setOnCheckedStateChangeListener { group, checkedId ->
            val chipId = checkedId.firstOrNull() ?: R.id.chip_all

            when(chipId){
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
            page = 1
            loadDataInside()
        }
    }


}