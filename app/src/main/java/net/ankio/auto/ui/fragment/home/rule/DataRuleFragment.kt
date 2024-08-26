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

package net.ankio.auto.ui.fragment.home.rule


import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.navigationrail.NavigationRailView
import com.google.gson.JsonObject
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.ClassicsHeader
import com.scwang.smart.refresh.layout.api.RefreshLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.App
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentDataRuleBinding
import net.ankio.auto.ui.adapter.DataRuleAdapter
import net.ankio.auto.ui.fragment.BaseFragment
import net.ankio.auto.ui.utils.MenuItem
import org.ezbook.server.db.model.RuleModel


class DataRuleFragment: BaseFragment()  {
    private lateinit var binding: FragmentDataRuleBinding
    override val menuList: ArrayList<MenuItem>
        get() =
            arrayListOf(
                MenuItem(R.string.item_add, R.drawable.menu_item_add) {
                    it.navigate(R.id.editFragment)
                },
            )



    lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentDataRuleBinding.inflate(layoutInflater)
        //TODO 默认左边选中微信
        //TODO 左侧使用 微信、支付宝、QQ、短信、云闪付、京东、银行卡这样的顺序进行排列，默认为微信
        loadDataEvent(binding.refreshLayout)
        loadLeftData(binding.navigationRail)
        chipEvent()
        recyclerView = binding.recyclerView
        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        recyclerView.adapter = DataRuleAdapter(pageData)
        return binding.root
    }


    private var page = 1
    private val pageSize = 20
    private var search = ""
    private var app = ""
    private var type = ""
    private var pageData = mutableListOf<RuleModel>()
    /**
     * 加载数据，如果数据为空或者加载失败返回false
     */
     private  fun loadData(callback:(Boolean, Boolean)->Unit){
         lifecycleScope.launch {
             RuleModel.list(app,type,page,pageSize).let { result ->
                 withContext(Dispatchers.Main){
                     if (result.isEmpty()){
                         callback(false,false)
                     }else{
                         callback(true,result.size == pageSize)
                     }

                     if (page == 1){
                        pageData.clear()
                     }
                     pageData.addAll(result)

                 }

             }
         }

    }

    /**
     * 加载数据事件
     */
    private fun loadDataEvent(refreshLayout: RefreshLayout){

        refreshLayout.setRefreshHeader(ClassicsHeader(requireContext()))
        refreshLayout.setRefreshFooter(ClassicsFooter(requireContext()))
        refreshLayout.setOnRefreshListener {
            page = 1
            loadData{ success,hasMore->
                it.finishRefresh(2000,success,hasMore) //传入false表示刷新失败
                recyclerView.adapter?.notifyDataSetChanged()
            }
        }
        refreshLayout.setOnLoadMoreListener {
            page++
            loadData{ success,hasMore->
                if (!success){
                    page--
                }
                it.finishLoadMore(2000,success,hasMore) //传入false表示加载失败
                if (success){
                    recyclerView.adapter?.notifyItemRangeInserted(pageData.size - pageSize,pageSize)
                }

            }
        }
    }


    private var leftData = JsonObject()
    private fun loadLeftData(navigation: NavigationRailView){
        val menu = navigation.menu
        //TODO 左侧使用 微信、支付宝、QQ、短信、云闪付、京东、银行卡这样的顺序进行排列，默认为微信
        lifecycleScope.launch {
            RuleModel.apps().let { result ->
                leftData = result
                menu.clear()
                var i = 0
                for (key in result.keySet()){
                    i++;
                    val app = App.getAppInfoFromPackageName(key,requireContext()) ?: continue

                    // 动态添加菜单项
                    val item1 = menu.add(Menu.NONE,i , Menu.NONE, app[0] as String)
                    item1.setIcon(app[1] as Drawable)
                    val badge = navigation.getOrCreateBadge(i)
                    badge.isVisible = true
                    badge.number = result.get(key).asInt
                }
            }
        }

        navigation.setOnItemSelectedListener {
            val id = it.itemId
            app = leftData.keySet().elementAt(id - 1)
            page = 1
            loadData{ success,hasMore->
                if (success){
                    recyclerView.adapter?.notifyDataSetChanged()
                }
            }
            true
        }
    }


    private fun chipEvent(){
        binding.chipGroup.setOnCheckedStateChangeListener { group, checkedId ->
            val chipId = checkedId.firstOrNull() ?: R.id.chip_all

            //将其他的设置为未选中
            for (id in group.checkedChipIds){
                if (id != chipId){
                    group.findViewById<Chip>(id).isChecked = false
                }
            }

            when(chipId){
                R.id.chip_all -> {
                    type = ""
                }
                R.id.chip_notify -> {
                    type = "notify"
                }
                R.id.chip_data -> {
                    type = "data"
                }
            }
            page = 1
            loadData{ success,hasMore->
                if (success){
                   recyclerView.adapter?.notifyDataSetChanged()
                }
            }
        }
    }


}