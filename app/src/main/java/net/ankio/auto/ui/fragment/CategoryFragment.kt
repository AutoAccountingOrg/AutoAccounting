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
import net.ankio.auto.databinding.ComponentCategoryBinding
import net.ankio.auto.databinding.FragmentCategoryBinding
import net.ankio.auto.http.api.CategoryAPI
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.api.bindAs
import net.ankio.auto.ui.dialog.BookSelectorDialog
import net.ankio.auto.ui.dialog.BottomSheetDialogBuilder
import net.ankio.auto.ui.fragment.book.CategoryComponent
import net.ankio.auto.ui.utils.DisplayUtils.dp2px
import net.ankio.auto.ui.utils.ListPopupUtils
import net.ankio.auto.ui.utils.ToastUtils
import org.ezbook.server.constant.BillType
import org.ezbook.server.db.model.BookNameModel
import org.ezbook.server.db.model.CategoryModel

/**
 * 分类管理Fragment
 *
 * 该Fragment负责显示和管理分类列表，提供以下功能：
 * - 使用Tab区分收入和支出分类
 * - 选择账本后显示对应分类
 * - 支持分类的增删改查
 * - 右上角提供账本切换功能
 */
class CategoryFragment : BaseFragment<FragmentCategoryBinding>() {

    companion object {
        private const val KEY_SELECTED_BOOK_ID = "selected_book_id"
        private const val KEY_SELECTED_BOOK_NAME = "selected_book_name"
        private const val KEY_SELECTED_BOOK_REMOTE_ID = "selected_book_remote_id"
        private const val KEY_SELECTED_BOOK_ICON = "selected_book_icon"
    }

    /** 当前选中的账本 */
    private var selectedBook: BookNameModel? = null

    /** Tab页面适配器 */
    private lateinit var pagerAdapter: CategoryPagerAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 设置工具栏
        setupToolbar()

        // 恢复保存的账本状态
        restoreBookState(savedInstanceState)

        // 如果没有选中的账本，显示账本选择对话框
        if (selectedBook == null) {
            showBookSelectorDialog()
        } else {
            // 如果已有选中的账本，直接初始化分类Tab
            initializeCategoryTabs()
        }
    }

    /**
     * 保存Fragment状态
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        selectedBook?.let { book ->
            outState.putLong(KEY_SELECTED_BOOK_ID, book.id)
            outState.putString(KEY_SELECTED_BOOK_NAME, book.name)
            outState.putString(KEY_SELECTED_BOOK_REMOTE_ID, book.remoteId)
            outState.putString(KEY_SELECTED_BOOK_ICON, book.icon)
        }
    }

    /**
     * 恢复账本状态
     */
    private fun restoreBookState(savedInstanceState: Bundle?) {
        savedInstanceState?.let { bundle ->
            val bookId = bundle.getLong(KEY_SELECTED_BOOK_ID, 0L)
            val bookName = bundle.getString(KEY_SELECTED_BOOK_NAME)
            val bookRemoteId = bundle.getString(KEY_SELECTED_BOOK_REMOTE_ID)
            val bookIcon = bundle.getString(KEY_SELECTED_BOOK_ICON)

            // 如果有保存的账本信息，重新创建BookNameModel对象
            if (bookId > 0L && !bookName.isNullOrEmpty()) {
                selectedBook = BookNameModel().apply {
                    id = bookId
                    name = bookName
                    remoteId = bookRemoteId ?: ""
                    icon = bookIcon ?: ""
                }
            }
        }
    }

    /**
     * Fragment恢复时刷新数据
     */
    override fun onResume() {
        super.onResume()
        // 如果已有选中的账本且已初始化ViewPager，刷新当前页面的数据
        if (selectedBook != null && ::pagerAdapter.isInitialized) {
            refreshCurrentPageData()
        }
    }

    /**
     * 刷新当前页面的分类数据
     */
    private fun refreshCurrentPageData() {
        val currentItem = binding.viewPager.currentItem
        val fragment = childFragmentManager.fragments.find {
            it is CategoryPageFragment && it.isVisible
        } as? CategoryPageFragment
        fragment?.refreshData()
    }

    /**
     * 设置工具栏
     */
    private fun setupToolbar() {
        // 设置返回按钮监听器
        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        // 设置菜单项点击监听器
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_switch_book -> {
                    showBookSelectorDialog()
                    true
                }

                else -> false
            }
        }
    }

    /**
     * 显示账本选择对话框
     */
    private fun showBookSelectorDialog() {
        val dialog = BookSelectorDialog(
            showSelect = false,
            callback = { bookModel, _ ->
                selectedBook = bookModel
                initializeCategoryTabs()
            },
            activity = requireActivity()
        )
        dialog.show()
    }

    /**
     * 初始化分类Tab页面
     */
    private fun initializeCategoryTabs() {
        selectedBook?.let { book ->
            // 隐藏选择账本提示，显示Tab内容
            binding.tabLayout.visibility = View.VISIBLE
            binding.viewPager.visibility = View.VISIBLE

            // 更新工具栏标题
            binding.topAppBar.title = "${getString(R.string.title_category)} - ${book.name}"

            // 设置ViewPager适配器
            pagerAdapter = CategoryPagerAdapter(this, book.name)
            binding.viewPager.adapter = pagerAdapter

            // 连接TabLayout和ViewPager2
            TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
                tab.text = when (position) {
                    0 -> getString(R.string.expend_category)
                    1 -> getString(R.string.income_category)
                    else -> ""
                }
            }.attach()
        }
    }

    /**
     * 分类Tab页面适配器
     */
    private class CategoryPagerAdapter(
        fragment: Fragment,
        private val bookName: String
    ) : FragmentStateAdapter(fragment) {

        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            val billType = when (position) {
                0 -> BillType.Expend  // 支出
                1 -> BillType.Income  // 收入
                else -> BillType.Expend
            }
            return CategoryPageFragment.newInstance(bookName, billType)
        }
    }

    /**
     * 分类页面Fragment - 显示具体的分类列表
     */
    class CategoryPageFragment : BaseFragment<ComponentCategoryBinding>() {

        companion object {
            private const val ARG_BOOK_NAME = "book_name"
            private const val ARG_BILL_TYPE = "bill_type"

            fun newInstance(bookName: String, billType: BillType): CategoryPageFragment {
                return CategoryPageFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_BOOK_NAME, bookName)
                        putString(ARG_BILL_TYPE, billType.name)
                    }
                }
            }
        }

        private lateinit var categoryComponent: CategoryComponent
        private lateinit var bookName: String
        private lateinit var billType: BillType

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            // 获取参数
            bookName = arguments?.getString(ARG_BOOK_NAME) ?: ""
            billType = BillType.valueOf(arguments?.getString(ARG_BILL_TYPE) ?: "Expend")

            // 初始化分类组件
            initCategoryComponent()
        }

        /**
         * 初始化分类组件
         */
        private fun initCategoryComponent() {
            binding.root.setPadding(dp2px(20f))
            // 使用bindAs创建CategoryComponent实例
            categoryComponent = binding.bindAs<CategoryComponent>(lifecycle)

            // 设置账本信息
            categoryComponent.setBookInfo(bookName, billType, true)

            // 设置分类选择回调
            categoryComponent.setOnCategorySelectedListener { parent, child ->
                // 处理分类选择事件
                when {

                    child?.isAddBtn() == true -> {
                        // 添加二级分类，跳转到编辑页面
                        navigateToCategoryEdit(
                            categoryId = 0L,
                            parentId = parent?.remoteId ?: "-1",
                            parentCategoryName = parent?.name ?: ""
                        )
                    }

                    parent?.isAddBtn() == true -> {
                        // 添加一级分类，跳转到编辑页面
                        navigateToCategoryEdit(
                            categoryId = 0L,
                            parentId = "-1",
                            parentCategoryName = ""
                        )
                    }
                }
            }

            // 设置长按回调
            categoryComponent.setOnCategoryLongClickListener { category, position, view ->
                // 长按弹出编辑或删除选择对话框
                showCategoryActionDialog(category, view)
            }
        }

        /**
         * 刷新分类数据
         */
        fun refreshData() {
            if (::categoryComponent.isInitialized) {
                lifecycleScope.launch {
                    categoryComponent.refreshData()
                }
            }
        }

        /**
         * 跳转到分类编辑页面
         */
        private fun navigateToCategoryEdit(
            categoryId: Long,
            parentId: String,
            parentCategoryName: String
        ) {
            val bundle = Bundle().apply {
                putLong("categoryId", categoryId)
                putString("bookName", bookName)
                putString("billType", billType.name)
                putString("parentId", parentId)
                // 父分类名称将在编辑页面中根据parentId动态获取
                putString("parentCategoryName", parentCategoryName)
            }
            findNavController().navigate(
                R.id.action_categoryFragment_to_categoryEditFragment,
                bundle
            )
        }

        /**
         * 显示分类操作选择对话框（编辑或删除）
         */
        private fun showCategoryActionDialog(category: CategoryModel, anchorView: View) {
            val actionMap = hashMapOf<String, String>(
                getString(R.string.edit) to "edit",
                getString(R.string.delete) to "delete"
            )

            ListPopupUtils(
                context = requireContext(),
                anchor = anchorView,
                list = actionMap as HashMap<String, Any>,
                value = "",
                lifecycle = lifecycle
            ) { position, key, value ->
                when (value) {
                    "edit" -> {
                        // 编辑分类 - 跳转到编辑页面
                        navigateToCategoryEdit(
                            categoryId = category.id,
                            parentId = category.remoteParentId,
                            parentCategoryName = "" // 父分类名称将在编辑页面中根据parentId动态获取
                        )
                    }

                    "delete" -> {
                        // 删除分类 - 显示删除确认对话框
                        showDeleteCategoryDialog(category)
                    }
                }
            }.toggle()
        }

        /**
         * 显示删除分类确认对话框
         */
        private fun showDeleteCategoryDialog(category: CategoryModel) {
            BottomSheetDialogBuilder(this)
                .setTitle(getString(R.string.delete_category))
                .setMessage(getString(R.string.delete_category_message, category.name))
                .setPositiveButton(getString(R.string.ok)) { _, _ ->
                    deleteCategory(category)
                }
                .setNegativeButton(getString(R.string.close)) { _, _ -> }
                .show()
        }

        /**
         * 删除分类
         */
        private fun deleteCategory(category: CategoryModel) {
            lifecycleScope.launch {
                val deletedId = CategoryAPI.delete(category.id)
                if (deletedId > 0) {
                    ToastUtils.info(getString(R.string.delete_category_success))
                    // 刷新数据
                    refreshData()
                } else {
                    ToastUtils.error(getString(R.string.delete_failed))
                }
            }
        }
    }
}