/*
 * Copyright (C) 2025 ankio(ankio@ankio.net)
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

package net.ankio.auto.ui.fragment.plugin

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentPluginDataBinding
import net.ankio.auto.http.api.AppDataAPI
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.adapter.AppDataAdapter
import net.ankio.auto.ui.api.BaseActivity
import net.ankio.auto.ui.api.BasePageFragment
import net.ankio.auto.ui.components.MaterialSearchView
import net.ankio.auto.ui.components.WrapContentLinearLayoutManager
import net.ankio.auto.ui.dialog.BottomSheetDialogBuilder
import net.ankio.auto.ui.models.RailMenuItem
import net.ankio.auto.utils.getAppInfoFromPackageName
import org.ezbook.server.constant.DataType
import org.ezbook.server.db.model.AppDataModel

/**
 * 插件数据管理Fragment
 *
 * 该Fragment负责展示和管理应用数据，包括：
 * - 左侧应用列表展示
 * - 数据筛选（通知数据、应用数据、匹配状态）
 * - 搜索功能
 * - 数据清理功能
 *
 * @author ankio
 */
class DataFragment : BasePageFragment<AppDataModel, FragmentPluginDataBinding>(),
    Toolbar.OnMenuItemClickListener {

    /** 当前选中的应用包名 */
    var app: String = ""

    /** 数据类型筛选（NOTICE/DATA） */
    var type: String = ""

    /** 是否只显示匹配的数据 */
    var match = false

    /** 搜索关键词 */
    var searchData = ""

    /** 左侧应用数据缓存 */
    private var leftData = JsonObject()

    /**
     * 加载数据的主要方法
     * 根据当前筛选条件从API获取应用数据列表
     *
     * @return 应用数据模型列表
     */
    override suspend fun loadData(): List<AppDataModel> {
        Logger.d("Loading data with params: app=$app, type=$type, match=$match, page=$page, pageSize=$pageSize, searchData='$searchData'")
        val result = AppDataAPI.list(app, type, match, page, pageSize, searchData)
        Logger.d("Loaded ${result.size} data items")
        return result
    }

    /**
     * 创建数据适配器
     * 配置RecyclerView的布局管理器和适配器
     *
     * @return 配置好的AppDataAdapter实例
     */
    override fun onCreateAdapter(): AppDataAdapter {
        val recyclerView = binding.statusPage.contentView!!
        recyclerView.layoutManager = WrapContentLinearLayoutManager(requireContext())
        return AppDataAdapter(requireActivity() as BaseActivity)
    }

    /**
     * Fragment视图创建完成后的初始化
     * 设置左侧数据、芯片事件、工具栏菜单和搜索功能
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpLeftData()
        setupChipEvent()
        binding.toolbar.setOnMenuItemClickListener(this)
        setUpSearch()

    }

    /**
     * 设置左侧应用列表数据
     * 配置应用选择监听器和刷新数据
     */
    private fun setUpLeftData() {

        binding.leftList.setOnItemSelectedListener {
            val id = it.id
            page = 1
            app = leftData.keySet().elementAt(id - 1)
            Logger.d("Selected app: $app (id: $id)")
            reload()
        }
    }

    /**
     * Fragment恢复时的处理
     */
    override fun onResume() {
        super.onResume()
        refreshLeftData()
    }

    /**
     * 刷新左侧应用数据
     * 从API获取应用列表并更新UI
     */
    private fun refreshLeftData() {
        Logger.d("Refreshing left data")
        lifecycleScope.launch {
            try {
                // 1. 清空列表
                binding.leftList.clear()

                // 2. 拉取 app 数据
                val result = AppDataAPI.apps()
                leftData = result
                Logger.d("Fetched ${result.size()} apps from API")

                var index = 1
                // 3. 遍历所有 app 包名
                for (packageName in result.keySet()) {
                    val app = getAppInfoFromPackageName(packageName)
                    if (app == null) {
                        Logger.w("Failed to get app info for package: $packageName")
                        continue
                    }

                    binding.leftList.addMenuItem(
                        RailMenuItem(index, app.icon!!, app.name)
                    )
                    Logger.d("Added app to left list: ${app.name} ($packageName)")
                    index++
                }

                // 4. 若没有任何 app，展示空状态页
                if (!binding.leftList.performFirstItem()) {
                    Logger.w("No apps available, showing empty state")
                    statusPage.showEmpty()
                }
            } catch (e: Exception) {
                Logger.e("Error refreshing left data", e)
                statusPage.showError()
            }
        }
    }

    /**
     * 设置筛选芯片事件监听
     * 处理数据类型和匹配状态的筛选
     */
    private fun setupChipEvent() {

        binding.chipGroup.setOnCheckedStateChangeListener { group, checkedId ->

            match = false
            type = ""

            if (R.id.chip_match in checkedId) {
                match = true
            }

            if (R.id.chip_notify in checkedId) {
                type = DataType.NOTICE.name
            }

            if (R.id.chip_data in checkedId) {
                type = DataType.DATA.name
            }

            if (R.id.chip_notify in checkedId && R.id.chip_data in checkedId) {
                type = ""
            }

            Logger.d("Filter updated: match=$match, type='$type'")
            reload()
        }
    }

    /**
     * 设置搜索功能
     * 配置搜索视图的查询文本监听器
     */
    private fun setUpSearch() {


        val searchItem = binding.toolbar.menu.findItem(R.id.action_search)
        if (searchItem != null) {
            val searchView = searchItem.actionView as MaterialSearchView
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    Logger.d("Search submitted: '$query'")
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    searchData = newText ?: ""
                    Logger.d("Search text changed: '$searchData'")
                    reload()
                    return true
                }
            })
        } else {
            Logger.w("Search menu item not found")
        }
    }

    /**
     * 工具栏菜单项点击处理
     * 处理数据清理等菜单操作
     *
     * @param item 被点击的菜单项
     * @return 是否处理了菜单点击事件
     */
    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.item_clear -> {
                // 检查Fragment View是否仍然有效
                if (view == null || !isAdded || isDetached) {
                    Logger.w("Fragment is not in valid state, cannot show dialog")
                    return true
                }

                BottomSheetDialogBuilder(this)
                    .setTitle(requireActivity().getString(R.string.delete_data))
                    .setMessage(requireActivity().getString(R.string.delete_msg))
                    .setPositiveButton(requireActivity().getString(R.string.sure_msg)) { _, _ ->
                        lifecycleScope.launch {
                            try {
                                AppDataAPI.clear()
                                Logger.i("Data cleared successfully")
                                page = 1
                                reload()
                            } catch (e: Exception) {
                                Logger.e("Error clearing data", e)
                            }
                        }
                    }
                    .setNegativeButton(requireActivity().getString(R.string.cancel_msg)) { _, _ ->
                        Logger.d("User cancelled data clear")
                    }
                    .show()
                return true
            }

            else -> {
                Logger.d("Unknown menu item clicked: ${item?.itemId}")
                return false
            }
        }
    }
}