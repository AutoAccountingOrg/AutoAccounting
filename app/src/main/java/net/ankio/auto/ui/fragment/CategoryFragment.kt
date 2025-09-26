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
import android.view.MenuItem
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.google.gson.Gson
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.databinding.ComponentCategoryBinding
import net.ankio.auto.databinding.FragmentCategoryBinding
import net.ankio.auto.http.api.CategoryAPI
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.api.bindAs
import net.ankio.auto.ui.dialog.BookSelectorDialog
import net.ankio.auto.ui.dialog.BottomSheetDialogBuilder
import net.ankio.auto.ui.fragment.components.CategoryComponent
import net.ankio.auto.ui.utils.CategoryUtils
import net.ankio.auto.ui.utils.DisplayUtils.dp2px
import net.ankio.auto.ui.utils.ListPopupUtilsGeneric
import net.ankio.auto.ui.utils.ToastUtils
import org.ezbook.server.constant.BillType
import org.ezbook.server.db.model.BookNameModel
import org.ezbook.server.db.model.CategoryModel
import org.ezbook.server.tools.MD5HashTable

/**
 * 分类管理Fragment
 *
 * 该Fragment负责显示和管理分类列表，提供以下功能：
 * - 使用Tab区分收入和支出分类
 * - 选择账本后显示对应分类
 * - 支持分类的增删改查
 * - 右上角提供账本切换功能
 */
class CategoryFragment : BaseFragment<FragmentCategoryBinding>(),
    androidx.appcompat.widget.Toolbar.OnMenuItemClickListener {

    companion object {
        // 用于传递和保存账本信息的参数键（Gson序列化）
        const val ARG_BOOK_MODEL = "arg_book_model"
    }

    /** 当前选中的账本 */
    private var selectedBook: BookNameModel? = null

    /** Tab页面适配器 */
    private lateinit var pagerAdapter: CategoryPagerAdapter

    // TabLayoutMediator 引用，便于在销毁时解除绑定，避免内存泄漏
    private var tabMediator: TabLayoutMediator? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 设置工具栏与FAB
        setupToolbar()

        // 解析初始账本并渲染
        if (!resolveSelectedBook(savedInstanceState)) {
            findNavController().popBackStack()
            return
        }
        selectedBook?.let { setSelectedBook(it) }
    }

    /**
     * 保存Fragment状态
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        selectedBook?.let { book ->
            outState.putString(ARG_BOOK_MODEL, Gson().toJson(book))
        }
    }

    /**
     * Fragment恢复时刷新数据
     */
    override fun onResume() {
        super.onResume()
        if (selectedBook != null && ::pagerAdapter.isInitialized) {
            refreshCurrentPageData()
        }
    }

    /**
     * 解析初始账本：先取 savedInstanceState，再退化到 arguments
     * @return 是否解析到账本
     */
    private fun resolveSelectedBook(savedInstanceState: Bundle?): Boolean {
        // 从保存状态恢复
        savedInstanceState?.getString(ARG_BOOK_MODEL)?.let { json ->
            selectedBook =
                runCatching { Gson().fromJson(json, BookNameModel::class.java) }.getOrNull()
        }
        if (selectedBook != null) return true
        // 从导航参数恢复
        arguments?.getString(ARG_BOOK_MODEL)?.let { json ->
            selectedBook =
                runCatching { Gson().fromJson(json, BookNameModel::class.java) }.getOrNull()
        }
        return selectedBook != null
    }

    /**
     * 设置当前账本，并同步标题与Tab内容
     */
    private fun setSelectedBook(book: BookNameModel) {
        selectedBook = book
        updateToolbarSubtitle(book)
        initializeCategoryTabs(book)
    }

    /**
     * 更新标题：主标题固定，子标题显示账本名称
     */
    private fun updateToolbarSubtitle(book: BookNameModel) {
        binding.topAppBar.title = getString(R.string.title_category)
        binding.topAppBar.subtitle = book.name
    }

    /**
     * 刷新当前页面的分类数据
     */
    private fun refreshCurrentPageData() {
        val fragment = childFragmentManager.fragments.find {
            it is CategoryPageFragment && it.isVisible
        } as? CategoryPageFragment
        fragment?.refreshData()
    }

    /**
     * 设置工具栏与FAB
     */
    private fun setupToolbar() {
        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        binding.topAppBar.setOnMenuItemClickListener(this)

    }

    /**
     * 显示账本选择对话框
     */
    private fun showBookSelectorDialog() {
        BaseSheetDialog.create<BookSelectorDialog>(requireContext())
            .setShowSelect(false)
            .setCallback { bookModel, _ ->
                setSelectedBook(bookModel)
            }
            .show()
    }

    /**
     * 初始化分类Tab页面（传入账本，避免到处读/判空）
     */
    private fun initializeCategoryTabs(book: BookNameModel) {
        // 显示Tab与ViewPager
        binding.tabLayout.visibility = View.VISIBLE
        binding.viewPager.visibility = View.VISIBLE

        // 解绑旧Mediator与Adapter
        tabMediator?.detach()
        tabMediator = null
        binding.viewPager.adapter = null

        // 设置新适配器
        pagerAdapter = CategoryPagerAdapter(this, book)
        binding.viewPager.adapter = pagerAdapter

        // 绑定TabLayoutMediator
        tabMediator = TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.expend_category)
                1 -> getString(R.string.income_category)
                else -> ""
            }
        }.apply { attach() }
    }

    override fun onDestroyView() {
        try {
            tabMediator?.detach()
        } catch (_: Exception) {
        }
        tabMediator = null
        try {
            binding.viewPager.adapter = null
        } catch (_: Exception) {
        }
        super.onDestroyView()
    }

    /**
     * 处理工具栏菜单点击
     */
    override fun onMenuItemClick(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.action_restore_default_categories -> {
                showRestoreDefaultCategoriesDialog()
                true
            }
            else -> false
        }
    }

    /**
     * 显示恢复默认分类确认弹窗
     */
    private fun showRestoreDefaultCategoriesDialog() {
        if (selectedBook == null) {
            ToastUtils.error(getString(R.string.select_book_first))
            return
        }
        BaseSheetDialog.create<BottomSheetDialogBuilder>(requireContext())
            .setTitle(getString(R.string.restore_default_categories_title))
            .setMessage(getString(R.string.restore_default_categories_message))
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                restoreDefaultCategories()
            }
            .setNegativeButton(getString(R.string.close)) { _, _ -> }
            .show()
    }

    /**
     * 恢复默认分类：清空当前账本全部分类并按内置默认分类重建
     */
    private fun restoreDefaultCategories() {
        val book = selectedBook ?: return
        launch {
            val defaults = CategoryUtils().setDefaultCategory(book)
            CategoryAPI.put(defaults, MD5HashTable.md5(Gson().toJson(defaults)))
            ToastUtils.info(getString(R.string.restore_default_categories_success))
            refreshCurrentPageData()
        }
    }

    /**
     * 分类Tab页面适配器
     */
    private class CategoryPagerAdapter(
        fragment: Fragment,
        private val book: BookNameModel,
    ) : FragmentStateAdapter(fragment) {

        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            val billType = when (position) {
                0 -> BillType.Expend  // 支出
                1 -> BillType.Income  // 收入
                else -> BillType.Expend
            }
            return CategoryPageFragment.newInstance(book, billType)
        }
    }

    /**
     * 分类页面Fragment - 显示具体的分类列表
     */
    class CategoryPageFragment : BaseFragment<ComponentCategoryBinding>() {

        companion object {
            private const val ARG_BOOK_NAME = "book_name"
            private const val ARG_REMOTE_BOOK_ID = "book_remote_id"
            private const val ARG_BILL_TYPE = "bill_type"

            fun newInstance(
                book: BookNameModel,
                billType: BillType
            ): CategoryPageFragment {
                return CategoryPageFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_REMOTE_BOOK_ID, book.remoteId)
                        putString(ARG_BOOK_NAME, book.name)
                        putString(ARG_BILL_TYPE, billType.name)
                    }
                }
            }
        }

        // 使用可空引用，onDestroyView 时置空以避免持有已销毁视图
        private var categoryComponent: CategoryComponent? = null
        private lateinit var bookName: String
        private lateinit var bookRemoteId: String
        private lateinit var billType: BillType

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            // 获取参数
            bookRemoteId = arguments?.getString(ARG_REMOTE_BOOK_ID) ?: ""
            bookName = arguments?.getString(ARG_BOOK_NAME) ?: ""
            billType = BillType.valueOf(arguments?.getString(ARG_BILL_TYPE) ?: BillType.Expend.name)

            // 初始化分类组件
            initCategoryComponent()
        }

        /**
         * 初始化分类组件
         */
        private fun initCategoryComponent() {
            binding.root.setPadding(dp2px(20f))
            // 使用bindAs创建CategoryComponent实例
            categoryComponent = binding.bindAs()

            // 设置账本信息
            categoryComponent?.setBookInfo(bookRemoteId, billType, true)

            // 设置分类选择回调
            categoryComponent?.setOnCategorySelectedListener { parent, child ->
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
            categoryComponent?.setOnCategoryLongClickListener { category, position, view ->
                // 长按弹出编辑或删除选择对话框
                showCategoryActionDialog(category, view)
            }
        }

        /**
         * 刷新分类数据
         */
        fun refreshData() {
            val comp = categoryComponent ?: return
            launch {
                comp.refreshData()
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
                putString("bookRemoteId", bookRemoteId)
                putString("billType", billType.name)
                putString("parentId", parentId)
                // 父分类名称将在编辑页面中根据parentId动态获取
                putString("parentCategoryName", parentCategoryName)
            }

            // 使用目的地 ID 导航，避免当前目的地为 NavGraph 时解析不到 action
            findNavController().navigate(R.id.categoryEditFragment, bundle)
        }

        /**
         * 显示分类操作选择对话框（编辑或删除）
         */
        private fun showCategoryActionDialog(category: CategoryModel, anchorView: View) {
            val actionMap = hashMapOf<String, String>(
                getString(R.string.edit) to "edit",
                getString(R.string.delete) to "delete"
            )

            ListPopupUtilsGeneric.create<String>(requireContext())
                .setAnchor(anchorView)
                .setList(actionMap)
                .setSelectedValue("")
                .setOnItemClick { position, key, value ->
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
            BaseSheetDialog.create<BottomSheetDialogBuilder>(requireContext())
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
            launch {
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

        override fun onDestroyView() {
            // 释放对视图及其子View的引用
            categoryComponent = null
            super.onDestroyView()
        }
    }
}