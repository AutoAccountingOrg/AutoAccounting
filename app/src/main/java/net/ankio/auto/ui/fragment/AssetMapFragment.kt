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
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentMapBinding
import net.ankio.auto.http.api.AssetsMapAPI
import net.ankio.auto.ui.adapter.AssetsMapAdapter
import net.ankio.auto.ui.api.BasePageFragment
import net.ankio.auto.ui.components.WrapContentLinearLayoutManager
import net.ankio.auto.ui.dialog.AssetsMapDialog
import org.ezbook.server.db.model.AssetsMapModel

/**
 * 资产映射Fragment
 *
 * 该Fragment负责显示和管理资产映射列表，提供以下功能：
 * - 显示所有资产映射列表
 * - 添加新的资产映射
 * - 编辑资产映射
 * - 重新应用资产映射到历史数据
 * - 分页加载更多数据
 *
 * 继承自BasePageFragment提供分页功能
 */
class AssetMapFragment : BasePageFragment<AssetsMapModel, FragmentMapBinding>() {

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
        return AssetsMapAdapter(requireActivity(), this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 设置RecyclerView布局管理器
        statusPage.contentView?.layoutManager = WrapContentLinearLayoutManager(context)

        // 设置添加按钮点击事件
        binding.addButton.setOnClickListener {
            AssetsMapDialog(this) { model ->
                lifecycleScope.launch {
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
     * 显示重新应用确认对话框
     */
    private fun showReapplyConfirmDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.reapply_confirm_title)
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
        lifecycleScope.launch {
            try {
                // 显示开始提示
                Snackbar.make(binding.root, R.string.reapply_started, Snackbar.LENGTH_SHORT).show()

                // 调用API
                val result = AssetsMapAPI.reapply()

                // 检查结果并显示相应提示
                if (result.has("success") && result.get("success").asBoolean) {
                    Snackbar.make(binding.root, R.string.reapply_success, Snackbar.LENGTH_LONG)
                        .show()
                } else {
                    val message = if (result.has("message")) {
                        result.get("message").asString
                    } else {
                        getString(R.string.reapply_failed)
                    }
                    Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                // 显示错误提示
                Snackbar.make(binding.root, R.string.reapply_failed, Snackbar.LENGTH_LONG).show()
            }
        }
    }
}
