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
import net.ankio.auto.ui.api.bindAs
import net.ankio.auto.ui.dialog.BottomSheetDialogBuilder
import net.ankio.auto.ui.fragment.book.BookComponent
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
        initBookComponent()

        // 设置工具栏监听器
        setupToolbar()

        // 设置添加按钮监听器
        setupAddButton()
    }

    /**
     * 初始化账本组件
     */
    private fun initBookComponent() {
        bookComponent = binding.book.bindAs<BookComponent>(lifecycle)
        // 不显示收入/支出选择按钮，因为这里只是显示账本列表
        bookComponent.setShowOption(false, true)

        bookComponent.setOnBookSelectedListener { bookNameModel, billType ->
            when (billType) {
                "edit" -> {
                    // 跳转到编辑页面
                    navigateToBookEdit(bookNameModel.id)
                }

                "setDefault" -> {
                    // 更新默认账本设置
                    PrefManager.defaultBook = bookNameModel.name

                    // 显示成功提示
                    ToastUtils.info(R.string.set_default_book_success)

                    // 刷新列表以更新UI状态
                    refreshBookList()
                }

                "delete" -> {
                    showDeleteBookDialog(bookNameModel)
                }
            }
        }

        // 将外层 SwipeRefreshLayout 注入到内部 StatusPage
        bookComponent.binding.statusPage.swipeRefreshLayout = binding.swipeRefreshLayout

        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshBookList()
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    /**
     * 设置工具栏
     */
    private fun setupToolbar() {
        // 设置返回按钮监听器
        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    /**
     * 设置添加按钮
     */
    private fun setupAddButton() {
        binding.addButton.setOnClickListener {
            showAddBookDialog()
        }
    }


    /**
     * 显示添加账本对话框
     */
    private fun showAddBookDialog(bookModel: BookNameModel? = null) {
        // 跳转到账本编辑页面
        val bookId = bookModel?.id ?: 0L
        navigateToBookEdit(bookId)
    }

    /**
     * 跳转到账本编辑页面
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
        BottomSheetDialogBuilder.create(this)
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
            // 调用API删除账本
            BookNameAPI.delete(bookModel.id)

            // 显示成功提示
            ToastUtils.info(R.string.delete_book_success)

            // 刷新列表
            refreshBookList()
        }
    }

    /**
     * 刷新账本列表
     */
    fun refreshBookList() {
        if (::bookComponent.isInitialized) {
            // 重新加载账本数据
            lifecycleScope.launch {
                bookComponent.refreshData()
            }
        }
    }
} 