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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentAssetBinding
import net.ankio.auto.http.api.AssetsAPI
import net.ankio.auto.ui.adapter.AssetSelectorAdapter
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.dialog.BottomSheetDialogBuilder
import net.ankio.auto.ui.utils.ListPopupUtilsGeneric
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.utils.PrefManager
import org.ezbook.server.constant.AssetsType
import org.ezbook.server.db.model.AssetsModel

/**
 * 资产管理Fragment
 *
 * 显示所有资产的扁平列表，每个item显示：
 * - 资产图标和名称
 * - 类型标签（普通/信用/借款人/债权人）
 * - 货币标签（CNY/USD等）
 *
 * 设计原则：
 * 1. 扁平列表：无分组头，类型直接显示在item上
 * 2. 简单排序：按类型分组，组内按sort排序
 * 3. 无复杂性：无折叠、无状态管理
 */
class AssetFragment : BaseFragment<FragmentAssetBinding>() {

    /** 资产适配器 */
    private lateinit var adapter: AssetSelectorAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupRecyclerView()
        loadData()
    }

    override fun onResume() {
        super.onResume()
        // 返回时刷新数据（编辑/删除后）
        loadData()
    }

    /**
     * 设置UI组件
     */
    private fun setupViews() {
        // 返回按钮
        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        // 添加按钮
        binding.addButton.setOnClickListener {
            navigateToAssetEdit(0L)
        }

        // 下拉刷新
        binding.statusPage.swipeRefreshLayout?.setOnRefreshListener {
            loadData()
            binding.statusPage.swipeRefreshLayout?.isRefreshing = false
        }
    }

    /**
     * 设置RecyclerView
     */
    private fun setupRecyclerView() {
        val recyclerView = binding.statusPage.contentView!!
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        (recyclerView.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        adapter = AssetSelectorAdapter()
            .setTypeNameMapper(::getTypeName)
            .setOnItemClickListener { asset, _ ->
                navigateToAssetEdit(asset.id)
            }
            .setOnItemLongClickListener { asset, view ->
                showAssetActionMenu(asset, view)
            }

        recyclerView.adapter = adapter
    }

    /**
     * 加载资产数据
     */
    private fun loadData() {
        binding.statusPage.showLoading()

        launch {
            try {
                val assets = AssetsAPI.list()

                if (assets.isEmpty()) {
                    binding.statusPage.showEmpty()
                } else {
                    // 按类型排序，组内按sort排序
                    val sorted = assets.sortedWith(
                        compareBy<AssetsModel> { it.type.ordinal }
                            .thenBy { it.sort }
                    )
                    adapter.replaceItems(sorted)
                    binding.statusPage.showContent()
                }
            } catch (e: Exception) {
                binding.statusPage.showError()
            }
        }
    }

    /**
     * 获取资产类型的显示名称
     */
    private fun getTypeName(type: AssetsType): String {
        return when (type) {
            AssetsType.NORMAL -> getString(R.string.type_normal)
            AssetsType.CREDIT -> getString(R.string.type_credit)
            AssetsType.BORROWER -> getString(R.string.type_borrower)
            AssetsType.CREDITOR -> getString(R.string.type_creditor)
        }
    }

    /**
     * 跳转到资产编辑页面
     */
    private fun navigateToAssetEdit(assetId: Long) {
        val bundle = Bundle().apply {
            putLong("assetId", assetId)
        }
        findNavController().navigate(R.id.assetEditFragment, bundle)
    }

    /**
     * 显示资产操作菜单（编辑/删除）
     */
    private fun showAssetActionMenu(asset: AssetsModel, anchorView: View) {
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
                    "edit" -> navigateToAssetEdit(asset.id)
                    "delete" -> showDeleteConfirmDialog(asset)
                }
            }
            .toggle()
    }

    /**
     * 显示删除确认对话框
     */
    private fun showDeleteConfirmDialog(asset: AssetsModel) {
        BaseSheetDialog.create<BottomSheetDialogBuilder>(requireContext())
            .setTitle(getString(R.string.delete_asset))
            .setMessage(getString(R.string.delete_asset_message, asset.name))
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                launch {
                    AssetsAPI.delete(asset.id)
                    ToastUtils.info(getString(R.string.delete_asset_success))
                    loadData()
                }
            }
            .setNegativeButton(getString(R.string.close)) { _, _ -> }
            .show()
    }
}
