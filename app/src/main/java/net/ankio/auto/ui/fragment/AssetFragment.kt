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
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.databinding.ComponentAssetBinding
import net.ankio.auto.databinding.FragmentAssetBinding
import net.ankio.auto.http.api.AssetsAPI
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.api.bindAs
import net.ankio.auto.ui.dialog.BottomSheetDialogBuilder
import net.ankio.auto.ui.fragment.components.AssetComponent
import net.ankio.auto.ui.utils.DisplayUtils.dp2px
import net.ankio.auto.ui.utils.ListPopupUtilsGeneric
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.ui.utils.adapterBottom
import net.ankio.auto.utils.PrefManager
import org.ezbook.server.constant.AssetsType
import org.ezbook.server.db.model.AssetsModel
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * 资产管理Fragment
 *
 * 该Fragment负责显示和管理资产列表，提供以下功能：
 * - 使用Tab区分不同类型的资产
 * - 支持资产的增删改查
 * - 使用浮动按钮作为添加资产的按钮
 */
class AssetFragment : BaseFragment<FragmentAssetBinding>() {

    /** Tab页面适配器 */
    private lateinit var pagerAdapter: AssetPagerAdapter

    /** 资产类型列表，根据功能开关动态构建 */
    private val assetTypes: List<AssetsType> by lazy {
        buildList {
            // 基础资产类型始终显示
            add(AssetsType.NORMAL)      // 普通资产
            add(AssetsType.CREDIT)      // 信用资产

            // 只有开启债务功能时才显示借贷相关类型
            if (PrefManager.featureDebt) {
                add(AssetsType.BORROWER)    // 借款人
                add(AssetsType.CREDITOR)    // 债权人
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        initializeAssetTabs()
    }

    /**
     * Fragment恢复时刷新数据
     */
    override fun onResume() {
        super.onResume()
        refreshCurrentPageData()
    }

    /**
     * 设置UI组件
     */
    private fun setupViews() {
        // 设置返回按钮监听器
        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        // 设置添加按钮
        binding.addButton.setOnClickListener {
            navigateToAssetEdit(0L)
        }
    }

    /**
     * 刷新当前页面的资产数据
     */
    private fun refreshCurrentPageData() {
        if (::pagerAdapter.isInitialized) {
            val currentItem = binding.viewPager.currentItem
            val fragment = childFragmentManager.fragments.find {
                it is AssetPageFragment && it.isVisible
            } as? AssetPageFragment
            fragment?.refreshData()
        }
    }

    /**
     * 跳转到资产编辑页面
     */
    private fun navigateToAssetEdit(assetId: Long) {
        val bundle = Bundle().apply {
            putLong("assetId", assetId)
        }
        // 使用目的地 ID 导航，避免当前目的地为 NavGraph 时解析不到 action
        findNavController().navigate(R.id.assetEditFragment, bundle)
    }

    /**
     * 初始化资产Tab页面
     */
    private fun initializeAssetTabs() {
        // 设置ViewPager适配器
        pagerAdapter = AssetPagerAdapter(this, assetTypes)
        binding.viewPager.adapter = pagerAdapter

        // 连接TabLayout和ViewPager2
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            // 根据实际的资产类型列表动态设置Tab标题
            tab.text = when (assetTypes.getOrNull(position)) {
                AssetsType.NORMAL -> getString(R.string.type_normal)
                AssetsType.CREDIT -> getString(R.string.type_credit)
                AssetsType.BORROWER -> getString(R.string.type_borrower)
                AssetsType.CREDITOR -> getString(R.string.type_creditor)
                else -> ""
            }
        }.attach()
    }

    /**
     * 资产Tab页面适配器
     */
    private class AssetPagerAdapter(
        private val parentFragment: Fragment,
        private val assetTypes: List<AssetsType>
    ) : FragmentStateAdapter(parentFragment) {

        override fun getItemCount(): Int = assetTypes.size

        override fun createFragment(position: Int): Fragment {
            return AssetPageFragment.newInstance(
                assetTypes[position],
                parentFragment as AssetFragment
            )
        }
    }

    /**
     * 资产页面Fragment - 显示具体的资产列表
     */
    class AssetPageFragment : BaseFragment<ComponentAssetBinding>() {

        companion object {
            private const val ARG_ASSET_TYPE = "asset_type"

            fun newInstance(
                assetType: AssetsType,
                parentFragment: AssetFragment
            ): AssetPageFragment {
                return AssetPageFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_ASSET_TYPE, assetType.name)
                    }
                    this.parentAssetFragment = parentFragment
                }
            }
        }

        private var parentAssetFragment: AssetFragment? = null
        private lateinit var assetComponent: AssetComponent
        private lateinit var assetType: AssetsType

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            assetType = AssetsType.valueOf(arguments?.getString(ARG_ASSET_TYPE) ?: "NORMAL")
            initAssetComponent()
            binding.statusPage.adapterBottom(requireContext())
        }

        /**
         * 初始化资产组件
         */
        private fun initAssetComponent() {

            assetComponent = binding.bindAs<AssetComponent>()

            // 设置资产选择回调
            assetComponent.setOnAssetSelectedListener { asset ->
                asset.let {
                    logger.debug { "资产被选中: ${it.name}" }
                }
            }

            // 设置长按回调
            assetComponent.setOnAssetLongClickListener { asset, view ->
                showAssetActionDialog(asset, view)
            }

            // 设置资产类型过滤器（这将触发数据加载）
            assetComponent.setAssetTypeFilter(assetType)
        }

        /**
         * 刷新资产数据
         */
        fun refreshData() {
            if (::assetComponent.isInitialized) {
                assetComponent.refreshData()
            }
        }

        /**
         * 显示资产操作选择对话框（编辑或删除）
         */
        private fun showAssetActionDialog(asset: AssetsModel, anchorView: View) {
            val actionMap = hashMapOf(
                getString(R.string.edit) to "edit",
                getString(R.string.delete) to "delete"
            )

            ListPopupUtilsGeneric.create<String>(requireContext())
                .setAnchor(anchorView)
                .setList(actionMap)
                .setSelectedValue("")
                .setOnItemClick { _, _, value ->
                    when (value) {
                        "edit" -> parentAssetFragment?.navigateToAssetEdit(asset.id)
                        "delete" -> showDeleteAssetDialog(asset)
                    }
                }
                .toggle()
        }

        /**
         * 显示删除资产确认对话框
         */
        private fun showDeleteAssetDialog(asset: AssetsModel) {
            BaseSheetDialog.create<BottomSheetDialogBuilder>(requireContext())
                .setTitle(getString(R.string.delete_asset))
                .setMessage(getString(R.string.delete_asset_message, asset.name))
                .setPositiveButton(getString(R.string.ok)) { _, _ ->
                    launch {
                        // 调用删除API
                        AssetsAPI.delete(asset.id)
                        ToastUtils.info(getString(R.string.delete_asset_success))
                        // 刷新数据
                        refreshData()
                    }
                }
                .setNegativeButton(getString(R.string.close)) { _, _ -> }
                .show()
        }
    }
}