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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentBookBinding
import net.ankio.auto.http.api.BookNameAPI
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.api.bindAs
import net.ankio.auto.ui.dialog.BottomSheetDialogBuilder
import net.ankio.auto.ui.fragment.components.BookComponent
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.utils.PrefManager
import org.ezbook.server.db.model.BookNameModel

/**
 * 账本管理Fragment
 *
 * 该Fragment负责显示和管理账本列表，提供以下功能：
 * - 显示所有账本列表
 * - 添加新账本
 * - 管理账本
 * - 选择账本进行操作
 *
 * 使用BookComponent组件来处理账本列表的显示和交互
 */
class BookFragment : BaseFragment<FragmentBookBinding>() {

    private lateinit var bookComponent: BookComponent

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化账本组件
        bookComponent = binding.book.bindAs()
        bookComponent.setShowOption(false, true)

        // 设置账本操作回调
        bookComponent.setOnBookSelectedListener { bookNameModel, billType ->
            when (billType) {
                "edit" -> navigateToBookEdit(bookNameModel.id)
                "setDefault" -> {
                    PrefManager.defaultBook = bookNameModel.name
                    ToastUtils.info(R.string.set_default_book_success)
                    refreshBookList()
                }
                "delete" -> showDeleteBookDialog(bookNameModel)
            }
        }

        // 设置刷新逻辑
        bookComponent.binding.statusPage.swipeRefreshLayout = binding.swipeRefreshLayout
        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshBookList()
            binding.swipeRefreshLayout.isRefreshing = false
        }

        // 设置UI监听器
        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.addButton.setOnClickListener {
            navigateToBookEdit(0L)
        }
    }

    /**
     * 跳转到账本编辑页面
     * @param bookId 账本ID，0表示新建账本
     */
    private fun navigateToBookEdit(bookId: Long) {
        val bundle = Bundle().apply {
            putLong("bookId", bookId)
        }
        findNavController().navigate(
            R.id.action_bookFragment_to_bookEditFragment,
            bundle
        )
    }


    /**
     * 显示删除账本确认对话框
     * @param bookModel 要删除的账本模型
     */
    private fun showDeleteBookDialog(bookModel: BookNameModel) {
        BaseSheetDialog.create<BottomSheetDialogBuilder>(requireContext())
            .setTitle(getString(R.string.delete))
            .setMessage(getString(R.string.delete_book_message, bookModel.name))
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                deleteBook(bookModel)
            }
            .setNegativeButton(getString(R.string.close)) { _, _ -> }
            .show()
    }

    /**
     * 删除账本
     * @param bookModel 要删除的账本模型
     */
    private fun deleteBook(bookModel: BookNameModel) {
        lifecycleScope.launch {
            try {
                BookNameAPI.delete(bookModel.id)
                ToastUtils.info(R.string.delete_book_success)
                refreshBookList()
            } catch (e: Exception) {
                ToastUtils.error("删除账本失败: ${e.message}")
            }
        }
    }

    /**
     * 刷新账本列表
     */
    private fun refreshBookList() {
        if (::bookComponent.isInitialized) {
            // 重新加载账本数据
            lifecycleScope.launch {
                bookComponent.refreshData()
            }
        }
    }
} 