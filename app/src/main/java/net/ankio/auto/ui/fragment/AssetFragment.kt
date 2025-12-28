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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

    /** 拖拽排序助手 */
    private lateinit var itemTouchHelper: ItemTouchHelper

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

        adapter = AssetSelectorAdapter()
            .setTypeNameMapper(::getTypeName)
            .setOnItemClickListener { asset, _ ->
                navigateToAssetEdit(asset.id)
            }
            .setOnItemLongClickListener { asset, view ->
                showAssetActionMenu(asset, view)
            }
            .setOnDeleteClickListener { asset ->
                // 滑动删除触发
                showDeleteConfirmDialog(asset)
            }

        recyclerView.adapter = adapter

        // 添加拖拽排序和滑动删除
        setupItemTouchHelper(recyclerView)
    }

    /**
     * 设置拖拽排序和滑动删除
     */
    private fun setupItemTouchHelper(recyclerView: RecyclerView) {
        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT  // 支持左滑删除
        ) {
            /**
             * 拖拽移动
             */
            override fun onMove(
                recyclerView: RecyclerView,
                source: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = source.bindingAdapterPosition
                val toPos = target.bindingAdapterPosition

                if (fromPos < 0 || toPos < 0) return false

                val items = adapter.getItems()
                if (fromPos >= items.size || toPos >= items.size) return false

                // 防止跨类型拖拽：只能在同类型资产间移动
                if (items[fromPos].type != items[toPos].type) {
                    return false
                }

                // 交换位置
                adapter.swapItems(fromPos, toPos)
                return true
            }

            /**
             * 拖拽状态改变
             */
            override fun onSelectedChanged(
                viewHolder: RecyclerView.ViewHolder?,
                actionState: Int
            ) {
                super.onSelectedChanged(viewHolder, actionState)
                // 拖拽时提升视图
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    viewHolder?.itemView?.alpha = 0.9f
                    viewHolder?.itemView?.elevation = 8f
                }
            }

            /**
             * 拖拽结束：恢复视图并更新排序到数据库
             */
            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                super.clearView(recyclerView, viewHolder)
                // 恢复视图状态
                viewHolder.itemView.alpha = 1.0f
                viewHolder.itemView.elevation = 0f
                // 更新排序
                updateAssetsSort()
            }

            /**
             * 滑动删除
             */
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // 左滑删除
                val position = viewHolder.bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = adapter.getItems()[position]
                    // 先恢复item位置，等用户确认后再真正删除
                    adapter.notifyItemChanged(position)
                    adapter.triggerDelete(item)
                }
            }

            /**
             * 启用长按拖拽
             */
            override fun isLongPressDragEnabled(): Boolean = true
        }

        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
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
                    // 刷新列表
                    loadData()
                }
            }
            .setNegativeButton(getString(R.string.close)) { _, _ ->
                // 取消删除，item已经通过notifyItemChanged恢复
            }
            .show()
    }

    /**
     * 更新资产排序
     * 拖拽结束后，批量更新所有资产的sort字段
     */
    private fun updateAssetsSort() {
        launch {
            try {
                val items = adapter.getItems()
                // 重新计算sort值（按当前列表顺序）
                items.forEachIndexed { index, asset ->
                    asset.sort = index
                }
                // 批量更新到数据库
                items.forEach { asset ->
                    AssetsAPI.save(asset)
                }
            } catch (e: Exception) {
                // 更新失败，重新加载数据恢复原状
                loadData()
            }
        }
    }
}
