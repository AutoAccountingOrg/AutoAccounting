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
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentAssetEditBinding
import net.ankio.auto.http.api.AssetsAPI
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.adapter.AssetSelectorAdapter
import net.ankio.auto.ui.adapter.CurrencyDropdownAdapter
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.components.WrapContentLinearLayoutManager
import net.ankio.auto.ui.utils.AssetsUtils
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.ui.utils.setAssetIcon
import net.ankio.auto.utils.PrefManager
import org.ezbook.server.constant.AssetsType
import org.ezbook.server.constant.Currency
import org.ezbook.server.db.model.AssetsModel

/**
 * 资产编辑Fragment
 *
 * 主要功能：
 * - 创建/编辑资产
 * - 资产类型选择
 * - 货币类型选择
 * - 图标选择
 * - 优化的性能和用户体验
 */
class AssetEditFragment : BaseFragment<FragmentAssetEditBinding>() {

    /** 当前资产模型 */
    private var currentAsset: AssetsModel = AssetsModel()

    /** 页面参数 */
    private var assetId: Long = 0L

    /** 图标适配器和数据 */
    private lateinit var iconAdapter: AssetSelectorAdapter
    private var allIcons = listOf<AssetsModel>()
    private var selectedIcon: AssetsModel? = null

    /** AutoCompleteTextView适配器和数据 */
    private lateinit var assetTypeAdapter: ArrayAdapter<String>
    private var assetTypesList = listOf<AssetsType>()
    private var currenciesList = listOf<Currency>()

    /** 搜索防抖 */
    private var searchJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        parseArguments()
        setupUI()
        setupIconAdapter()
        loadData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
    }

    /**
     * 解析传入参数
     */
    private fun parseArguments() {
        arguments?.let { bundle ->
            assetId = bundle.getLong("assetId", 0L)
        }
    }

    /**
     * 设置UI界面
     */
    private fun setupUI() = with(binding) {
        // 设置标题和按钮文字
        val isEdit = assetId > 0
        toolbar.title = getString(if (isEdit) R.string.edit_asset else R.string.add_asset)
        saveButton.text = getString(if (isEdit) R.string.btn_save else R.string.btn_create)

        // 设置事件监听
        toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        saveButton.setOnClickListener { saveAsset() }

        // 设置资产类型选择
        setupAssetTypeAutoComplete()

        // 设置货币类型选择
        setupCurrencyAutoComplete()

        // 设置搜索（带防抖）
        iconSearchEditText.addTextChangedListener { text ->
            performSearch(text?.toString() ?: "")
        }


    }

    /**
     * 设置资产类型AutoCompleteTextView
     */
    private fun setupAssetTypeAutoComplete() = with(binding) {
        // 根据功能开关动态构建资产类型数据
        assetTypesList = buildList {
            // 基础资产类型始终显示
            add(AssetsType.NORMAL)      // 普通资产
            add(AssetsType.CREDIT)      // 信用资产

            // 只有开启债务功能时才显示借贷相关类型
            if (PrefManager.featureDebt) {
                add(AssetsType.BORROWER)    // 借款人
                add(AssetsType.CREDITOR)    // 债权人
            }
        }
        
        val assetTypeNames = assetTypesList.map { type ->
            when (type) {
                AssetsType.NORMAL -> getString(R.string.type_normal)
                AssetsType.CREDIT -> getString(R.string.type_credit)
                AssetsType.BORROWER -> getString(R.string.type_borrower)
                AssetsType.CREDITOR -> getString(R.string.type_creditor)
            }
        }

        // 创建适配器
        assetTypeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            assetTypeNames
        )

        // 设置适配器
        assetTypeAutoComplete.setAdapter(assetTypeAdapter)

        // 设置点击监听器，点击时显示所有选项
        assetTypeAutoComplete.setOnClickListener {
            assetTypeAutoComplete.showDropDown()
        }

        // 设置选择监听器
        assetTypeAutoComplete.setOnItemClickListener { _, _, position, _ ->
            if (position < assetTypesList.size) {
                currentAsset.type = assetTypesList[position]
            }
        }
    }

    /**
     * 设置货币类型AutoCompleteTextView
     */
    private fun setupCurrencyAutoComplete() = with(binding) {
        // 准备货币数据
        currenciesList = Currency.entries.toList()

        // 使用带图标的适配器
        val currencyIconAdapter = CurrencyDropdownAdapter(
            requireContext(),
            currenciesList
        )
        currencyAutoComplete.setAdapter(currencyIconAdapter)

        // 设置点击监听器，点击时显示所有选项
        currencyAutoComplete.setOnClickListener {
            currencyAutoComplete.showDropDown()
        }

        // 设置选择监听器
        currencyAutoComplete.setOnItemClickListener { _, _, position, _ ->
            if (position < currenciesList.size) {
                currentAsset.currency = currenciesList[position]
                // 更新显示文本为货币名称而不是代码
                currencyAutoComplete.setText(currentAsset.currency.name(requireContext()), false)
            }
        }
    }

    /**
     * 设置图标适配器
     */
    private fun setupIconAdapter() = with(binding) {
        val recyclerView = statusPage.contentView!!
        recyclerView.layoutManager = WrapContentLinearLayoutManager(requireContext())

        // 初始化适配器，使用链式配置模式
        iconAdapter = AssetSelectorAdapter()
            .setOnItemClickListener { item, _ -> onIconSelected(item) }
            .setShowCurrency(false) // 图标选择不需要展示货币

        recyclerView.adapter = iconAdapter
    }

    /**
     * 加载数据
     */
    private fun loadData() {
        binding.statusPage.showLoading()
        launch {
            loadIconData()
            // 如果是编辑模式，加载资产数据
            if (assetId > 0) {
                loadAssetData()
            } else {
                // 新建模式，设置默认值
                setupNewAsset()
            }

            binding.statusPage.showContent()
        }
    }

    /**
     * 加载图标数据
     */
    private suspend fun loadIconData() {
        allIcons = AssetsUtils.list(requireContext())
        Logger.d("icons: " + allIcons.size.toString())

        // 显示所有图标，不进行类型过滤
        iconAdapter.updateItems(allIcons)

        // 设置默认选中第一个图标
        if (allIcons.isNotEmpty() && selectedIcon == null) {
            onIconSelected(allIcons.first())
        }
    }

    /**
     * 加载资产数据（编辑模式）
     */
    private suspend fun loadAssetData() {
        try {
            val asset = AssetsAPI.getById(assetId)
            if (asset != null) {
                currentAsset = asset

                // 更新UI
                binding.assetNameEditText.setText(asset.name)

                // 设置资产类型AutoCompleteTextView选中项
                val assetTypeIndex = assetTypesList.indexOf(asset.type)
                if (assetTypeIndex >= 0) {
                    val assetTypeName = when (asset.type) {
                        AssetsType.NORMAL -> getString(R.string.type_normal)
                        AssetsType.CREDIT -> getString(R.string.type_credit)
                        AssetsType.BORROWER -> getString(R.string.type_borrower)
                        AssetsType.CREDITOR -> getString(R.string.type_creditor)
                    }
                    binding.assetTypeAutoComplete.setText(assetTypeName, false)
                }

                // 设置货币类型AutoCompleteTextView选中项
                val currencyIndex = currenciesList.indexOf(asset.currency)
                if (currencyIndex >= 0) {
                    binding.currencyAutoComplete.setText(
                        asset.currency.name(requireContext()),
                        false
                    )
                }

                // 设置选中的图标
                asset.icon?.let { iconUrl ->
                    val matchedIcon = allIcons.find { it.icon == iconUrl }
                    matchedIcon?.let { onIconSelected(it) }
                }
            } else {
                ToastUtils.error(getString(R.string.asset_not_found))
                findNavController().popBackStack()
            }
        } catch (e: Exception) {
            Logger.e("Failed to load asset data", e)
            ToastUtils.error(getString(R.string.asset_load_failed))
            findNavController().popBackStack()
        }
    }

    /**
     * 设置新资产的默认值
     */
    private fun setupNewAsset() {
        currentAsset = AssetsModel().apply {
            type = AssetsType.NORMAL
            currency = Currency.CNY
        }

        // 设置默认UI显示
        binding.assetTypeAutoComplete.setText(getString(R.string.type_normal), false)
        binding.currencyAutoComplete.setText(Currency.CNY.name(requireContext()), false)
    }

    /**
     * 处理图标选择
     *
     * 只在新建资产且用户尚未输入名称时，使用图标名作为建议
     * 编辑已有资产时，不会覆盖用户已设置的名称
     */
    private fun onIconSelected(icon: AssetsModel) {
        selectedIcon = icon
        binding.selectedIconImageView.setAssetIcon(icon)

        // 只在新建资产且用户尚未输入名称时，使用图标名作为建议
        if (assetId == 0L && binding.assetNameEditText.text.isNullOrEmpty()) {
            binding.assetNameEditText.setText(icon.name)
        }
    }


    /**
     * 执行搜索（带防抖）
     */
    private fun performSearch(query: String) {
        searchJob?.cancel()

        if (query.isEmpty()) {
            // 搜索为空时，显示所有图标
            iconAdapter.updateItems(allIcons)
            return
        }

        searchJob = launch {
            delay(300) // 防抖延迟

            // 在所有图标中进行搜索
            val searchResults = allIcons.filter { icon ->
                icon.name.contains(query, ignoreCase = true)
            }

            iconAdapter.replaceItems(searchResults)
        }
    }

    /**
     * 保存资产
     */
    private fun saveAsset() {
        val assetName = binding.assetNameEditText.text?.toString()?.trim()

        // 验证输入
        if (assetName.isNullOrEmpty()) {
            ToastUtils.error(getString(R.string.asset_name_empty))
            return
        }

        if (selectedIcon == null) {
            ToastUtils.error(getString(R.string.asset_icon_empty))
            return
        }

        // 更新资产信息
        currentAsset.apply {
            name = assetName
            icon = selectedIcon?.icon ?: ""
        }

        // 保存资产
        lifecycleScope.launch {
            try {
                val savedId = AssetsAPI.save(currentAsset)
                if (savedId > 0) {
                    ToastUtils.info(getString(R.string.save_asset_success))
                    findNavController().popBackStack()
                } else {
                    ToastUtils.error(getString(R.string.save_failed))
                }
            } catch (e: Exception) {
                Logger.e("Failed to save asset", e)
                ToastUtils.error(getString(R.string.save_failed))
            }
        }
    }
}