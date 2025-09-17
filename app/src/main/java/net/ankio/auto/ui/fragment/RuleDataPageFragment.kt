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

package net.ankio.auto.ui.fragment

import android.os.Bundle
import android.view.View
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentRuleDataPageBinding
import net.ankio.auto.http.api.RuleManageAPI
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.adapter.DataRuleAdapter
import net.ankio.auto.ui.api.BasePageFragment
import net.ankio.auto.ui.components.WrapContentLinearLayoutManager
import net.ankio.auto.ui.models.RailMenuItem
import net.ankio.auto.utils.getAppInfoFromPackageName
import org.ezbook.server.db.model.RuleModel

/**
 * 数据规则页面Fragment
 *
 * 该Fragment是RuleManageFragment的子页面，负责展示和管理数据规则，包括：
 * - 左侧应用列表展示
 * - 规则筛选（规则类型、创建者）
 * - 搜索功能
 * - 规则管理功能
 *
 * @author ankio
 */
class RuleDataPageFragment : BasePageFragment<RuleModel, FragmentRuleDataPageBinding>() {

    /** 当前选中的应用包名 */
    var app: String = ""

    /** 数据类型筛选（NOTICE/DATA） */
    var type: String = ""

    /** 创建者筛选（USER/OFFICIAL） */
    var creator = ""

    /** 搜索关键词 */
    var searchData = ""

    /**
     * 应用信息数据类 - 统一的数据结构，消除双重维护
     * 按照Linus的"好品味"原则：一个数据结构解决一个问题
     */
    data class AppInfo(
        val packageName: String,
        val name: String,
        val icon: android.graphics.drawable.Drawable?
    )

    /** 应用列表 - 单一数据源，消除索引转换的特殊情况 */
    private val appList = mutableListOf<AppInfo>()

    /**
     * 加载数据的主要方法
     * 根据当前筛选条件从API获取规则数据列表
     *
     * @return 规则数据模型列表
     */
    override suspend fun loadData(): List<RuleModel> =
        RuleManageAPI.list(app, type, creator, page, pageSize, searchData)

    /**
     * 创建数据适配器
     * 配置RecyclerView的布局管理器和适配器
     *
     * @return 配置好的DataRuleAdapter实例
     */
    override fun onCreateAdapter(): RecyclerView.Adapter<*> {
        val recyclerView = binding.statusPage.contentView!!
        recyclerView.layoutManager = WrapContentLinearLayoutManager(requireContext())
        return DataRuleAdapter(this)
    }

    /**
     * Fragment视图创建完成后的初始化
     * 设置左侧数据、下拉框过滤器和搜索功能
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpLeftData()
        setupFilterDropdown()
        setUpSearch()
    }

    /**
     * 设置左侧应用列表数据
     * 消除索引转换 - 直接使用数组索引，无特殊情况
     */
    private fun setUpLeftData() {
        binding.leftList.setOnItemSelectedListener { menuItem ->
            page = 1
            // Linus式简化：无需索引转换，直接使用数组位置
            val index = menuItem.id - 1
            if (index in appList.indices) {
                app = appList[index].packageName
                Logger.d("Selected app: $app (${appList[index].name})")
                reload()
            } else {
                Logger.w("Invalid app selection index: $index, list size: ${appList.size}")
            }
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
     * 简化的单一数据流 - 从API到UI，无重复数据结构
     */
    private fun refreshLeftData() {
        Logger.d("Refreshing left data for rules")
        launch {
            try {
                // 1. 清空单一数据源
                binding.leftList.clear()
                appList.clear()

                // 2. 获取应用数据
                val apiResult = RuleManageAPI.apps()
                Logger.d("Fetched ${apiResult.size()} apps from rule API")

                // 3. 构建应用列表 - 单次遍历，无重复逻辑
                var index = 1
                for (packageName in apiResult.keySet()) {
                    val appInfo = getAppInfoFromPackageName(packageName)
                    if (appInfo == null) {
                        Logger.w("Failed to get app info for package: $packageName")
                        continue
                    }

                    // 创建统一的应用数据对象
                    val app = AppInfo(packageName, appInfo.name, appInfo.icon)
                    appList.add(app)

                    // 添加到UI - 使用统一数据源
                    binding.leftList.addMenuItem(
                        RailMenuItem(index, app.icon!!, app.name)
                    )

                    Logger.d("Added app: ${app.name} ($packageName)")
                    index++
                }

                // 4. 处理空状态 - 简化条件判断
                if (appList.isEmpty() || !binding.leftList.performFirstItem()) {
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
     * 过滤器配置 - Linus式简化：消除重复代码的通用方案
     */
    private data class FilterConfig(
        val dropdown: com.google.android.material.textfield.MaterialAutoCompleteTextView,
        val optionsArrayRes: Int,
        val valueMap: Array<String>,
        val updateValue: (String) -> Unit
    )

    /**
     * 设置过滤下拉框 - 统一的过滤器设置逻辑
     */
    private fun setupFilterDropdown() {
        // Linus原则：用数据结构消除重复代码
        val filters = listOf(
            FilterConfig(
                dropdown = binding.typeDropdown,
                optionsArrayRes = R.array.rule_type_options,
                valueMap = arrayOf("", "NOTICE", "DATA", "OCR"),
                updateValue = { type = it }
            ),
            FilterConfig(
                dropdown = binding.creatorDropdown,
                optionsArrayRes = R.array.creator_options,
                valueMap = arrayOf("", "user", "system"),
                updateValue = { creator = it }
            )
        )

        // 通用设置逻辑 - 无重复代码
        filters.forEach { config ->
            setupSingleFilter(config)
        }
    }

    /**
     * 设置单个过滤器 - 统一的实现，消除所有重复
     */
    private fun setupSingleFilter(config: FilterConfig) {
        val options = resources.getStringArray(config.optionsArrayRes)

        // 设置适配器
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            options
        )
        config.dropdown.setAdapter(adapter)

        // 设置点击监听器 - 通用逻辑
        config.dropdown.setOnItemClickListener { _, _, position, _ ->
            val value = config.valueMap[position]
            config.updateValue(value)
            Logger.d("Filter updated: position=$position, value='$value'")
            reload()
        }

        // 设置默认选中项
        config.dropdown.setText(options[0], false)
    }

    /**
     * 设置搜索功能 - 简化的文本监听器
     */
    private fun setUpSearch() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchData = s?.toString() ?: ""
                Logger.d("Rule search text changed: '$searchData'")
                reload()
            }
        })
    }
}
