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

package net.ankio.auto.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import net.ankio.auto.App
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentMapBinding
import net.ankio.auto.http.api.AssetsMapAPI
import net.ankio.auto.ui.adapter.AssetsMapAdapter
import net.ankio.auto.ui.api.BasePageFragment
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.components.WrapContentLinearLayoutManager
import net.ankio.auto.ui.dialog.AssetsMapDialog
import net.ankio.auto.ui.dialog.BottomSheetDialogBuilder
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.ui.utils.ToastUtils
import org.ezbook.server.db.model.AssetsMapModel

/**
 * 资产映射Fragment
 *
 * 该Fragment负责显示和管理资产映射列表，提供以下功能：
 * - 显示所有资产映射列表
 * - 添加新的资产映射
 * - 编辑资产映射
 * - 拖拽调整映射规则顺序
 * - 重新应用资产映射到历史数据
 * - 分页加载更多数据
 *
 * 继承自BasePageFragment提供分页功能
 */
class AssetMapFragment : BasePageFragment<AssetsMapModel, FragmentMapBinding>() {

    /** 资产映射适配器 */
    private lateinit var assetsMapAdapter: AssetsMapAdapter

    /** 拖拽排序助手 */
    private lateinit var itemTouchHelper: ItemTouchHelper

    /** 标记是否发生了排序变化 */
    private var sortChanged = false

    /**
     * 加载资产映射数据
     * @return 当前页的资产映射列表
     */
    override suspend fun loadData(): List<AssetsMapModel> {
        return AssetsMapAPI.list(page, pageSize)
    }

    /**
     * 创建RecyclerView适配器
     * @return AssetsMapAdapter实例
     */
    override fun onCreateAdapter(): RecyclerView.Adapter<*> {
        assetsMapAdapter = AssetsMapAdapter()
            .setOnEditClick { item, position -> handleEditAssetsMap(item, position) }
            .setOnDeleteClick { item -> handleDeleteAssetsMap(item) }

        return assetsMapAdapter
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 设置RecyclerView布局管理器
        statusPage.contentView?.layoutManager = WrapContentLinearLayoutManager(context)

        // 初始化拖拽排序
        setupDragSort()

        // 设置返回按钮点击事件
        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        // 设置添加按钮点击事件
        binding.addButton.setOnClickListener {
            BaseSheetDialog.create<AssetsMapDialog>(requireContext()).setOnClose { model ->
                launch {
                    AssetsMapAPI.put(model)
                    reload()
                }

            }.show()
        }

        // 设置工具栏菜单点击事件
        binding.topAppBar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_reapply -> {
                    showReapplyConfirmDialog()
                    true
                }

                else -> false
            }
        }
    }

    /**
     * 初始化拖拽排序和滑动删除功能
     */
    private fun setupDragSort() {
        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT  // 支持左滑删除
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.bindingAdapterPosition
                val toPosition = target.bindingAdapterPosition
                if (fromPosition != RecyclerView.NO_POSITION && toPosition != RecyclerView.NO_POSITION) {
                    assetsMapAdapter.swapItems(fromPosition, toPosition)
                    sortChanged = true
                    return true
                }
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // 左滑删除
                val position = viewHolder.bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = assetsMapAdapter.getItems()[position]
                    // 先恢复item位置，等用户确认后再真正删除
                    assetsMapAdapter.notifyItemChanged(position)
                    assetsMapAdapter.triggerDelete(item)
                }
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                // 拖拽时提升视图层级
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    viewHolder?.itemView?.alpha = 0.9f
                    viewHolder?.itemView?.elevation = 8f
                }
            }

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                super.clearView(recyclerView, viewHolder)
                // 恢复视图状态
                viewHolder.itemView.alpha = 1.0f
                viewHolder.itemView.elevation = 0f
                // 拖拽结束后保存排序
                if (sortChanged) {
                    saveSortOrder()
                    sortChanged = false
                }
            }

            override fun isLongPressDragEnabled(): Boolean = true
        }

        itemTouchHelper = ItemTouchHelper(callback)
        statusPage.contentView?.let { itemTouchHelper.attachToRecyclerView(it) }
    }

    /**
     * 保存排序到服务器
     */
    private fun saveSortOrder() {
        val items = assetsMapAdapter.getItems()
        val sortItems = items.mapIndexed { index, model ->
            AssetsMapAPI.SortItem(model.name, index)
        }

        launch {
            AssetsMapAPI.updateSort(sortItems)
        }
    }

    /**
     * 显示重新应用确认对话框
     */
    private fun showReapplyConfirmDialog() {
        BaseSheetDialog.create<BottomSheetDialogBuilder>(requireContext())
            .setTitleInt(R.string.reapply_confirm_title)
            .setMessage(R.string.reapply_confirm_message)
            .setPositiveButton(R.string.sure_msg) { _, _ ->
                reapplyAssetMapping()
            }
            .setNegativeButton(R.string.cancel_msg, null)
            .show()
    }

    /**
     * 执行重新应用资产映射操作
     */
    private fun reapplyAssetMapping() {
        launch {
            val loadingUtils = LoadingUtils(requireActivity())
            // 显示加载对话框
            loadingUtils.show(R.string.reapply_started)

            // 调用API
            AssetsMapAPI.reapply()

            ToastUtils.info(R.string.reapply_success)

            loadingUtils.close()
        }
    }

    /**
     * 处理编辑资产映射事件
     * @param item 要编辑的资产映射项
     * @param position 项目在列表中的位置
     */
    private fun handleEditAssetsMap(item: AssetsMapModel, position: Int) {
        BaseSheetDialog.create<AssetsMapDialog>(requireContext())
            .setAssetsMapModel(item)
            .setOnClose { changedAssetsMap ->
                launch {
                    AssetsMapAPI.put(changedAssetsMap)
                    // 更新适配器中的数据
                    updateItem(position, item)
                }
            }
            .show()
    }

    /**
     * 处理删除资产映射事件
     * @param item 要删除的资产映射项
     */
    private fun handleDeleteAssetsMap(item: AssetsMapModel) {
        BaseSheetDialog.create<BottomSheetDialogBuilder>(requireContext())
            .setTitleInt(R.string.delete_title)
            .setMessage(getString(R.string.delete_message, item.name))
            .setNegativeButton(R.string.cancel_msg) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(R.string.delete) { _, _ ->
                this@AssetMapFragment.launch {
                    AssetsMapAPI.remove(item.id)
                    // 从适配器中移除数据
                    removeItem(item)
                }
            }
            .show()
    }


    override fun onStop() {
        super.onStop()
        App.launch {
            AssetsMapAPI.reapply()
        }
    }
}
