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
import androidx.core.widget.addTextChangedListener
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentCategoryEditBinding
import net.ankio.auto.http.api.CategoryAPI
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.adapter.CategorySelectorAdapter
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.utils.CategoryUtils
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.ui.utils.adapterBottom
import net.ankio.auto.ui.utils.setCategoryIcon
import org.ezbook.server.constant.BillType
import org.ezbook.server.db.model.CategoryModel

/**
 * 分类编辑Fragment (简化版本)
 *
 * 主要功能：
 * - 创建/编辑分类
 * - 图标选择
 * - 优化的性能和用户体验
 */
class CategoryEditFragment : BaseFragment<FragmentCategoryEditBinding>() {

    /** 当前分类模型 */
    private var currentCategory: CategoryModel = CategoryModel()

    /** 页面参数 */
    private var categoryId: Long = 0L
    private var bookName: String = ""
    private var bookRemoteId: String = ""
    private var billType: BillType = BillType.Expend
    private var parentId: String = "-1"
    private var parentCategoryName: String = ""

    /** 图标适配器和数据 */
    private lateinit var iconAdapter: CategorySelectorAdapter
    private var allIcons = listOf<CategoryModel>()
    private var selectedIcon: CategoryModel? = null

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
            categoryId = bundle.getLong("categoryId", 0L)
            bookName = bundle.getString("bookName", "")
            bookRemoteId = bundle.getString("bookRemoteId", "")
            billType = BillType.valueOf(bundle.getString("billType", "Expend"))
            parentId = bundle.getString("parentId", "-1")
            parentCategoryName = bundle.getString("parentCategoryName", "")
        }
    }

    /**
     * 设置UI界面
     */
    private fun setupUI() = with(binding) {
        // 设置标题和按钮文字
        val isEdit = categoryId > 0
        toolbar.title = getString(if (isEdit) R.string.edit_category else R.string.add_category)
        saveButton.text = getString(if (isEdit) R.string.btn_save else R.string.btn_create)

        // 设置基本信息
        bookNameText.text = bookName
        parentCategoryText.text = if (parentId == "-1") {
            getString(R.string.no_parent_category)
        } else {
            parentCategoryName
        }

        // 设置事件监听
        toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        saveButton.setOnClickListener { saveCategory() }

        // 设置搜索（带防抖）
        iconSearchEditText.addTextChangedListener { text ->
            performSearch(text?.toString() ?: "")
        }
        binding.statusPage.adapterBottom(requireContext())
    }

    /**
     * 设置图标适配器
     */
    private fun setupIconAdapter() = with(binding) {
        val recyclerView = statusPage.contentView!!
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 5)

        // 初始化适配器，只使用基本的图标选择功能 - 链式调用配置
        iconAdapter = CategorySelectorAdapter()
            .editMode(false) // 不使用编辑模式的复杂功能
            .onItemClick { item, _, _, _ -> onIconSelected(item) }
            .onItemChildClick { _, _ -> } // 不使用子项点击

        recyclerView.adapter = iconAdapter
    }

    /**
     * 加载数据
     */
    private fun loadData() {
        binding.statusPage.showLoading()
        launch {
            // 加载图标数据
            loadIconData()

            // 如果是编辑模式，加载分类数据
            if (categoryId > 0) {
                loadCategoryData()
            } else {
                // 新建模式，设置默认值
                setupNewCategory()
            }

            binding.statusPage.showContent()
        }
    }

    /**
     * 加载图标数据
     */
    private suspend fun loadIconData() = withContext(Dispatchers.IO) {
        val categoryUtils = CategoryUtils()
        allIcons = categoryUtils.list(requireContext())

        withContext(Dispatchers.Main) {
            iconAdapter.replaceItems(allIcons)

            // 设置默认选中第一个图标
            if (allIcons.isNotEmpty() && selectedIcon == null) {
                onIconSelected(allIcons.first())
            }
        }
    }

    /**
     * 加载分类数据（编辑模式）
     */
    private suspend fun loadCategoryData() {
        try {
            val category = CategoryAPI.getById(categoryId)
            if (category != null) {
                currentCategory = category

                // 更新UI
                binding.categoryNameEditText.setText(category.name)

                // 设置选中的图标
                category.icon?.let { iconUrl ->
                    val matchedIcon = allIcons.find { it.icon == iconUrl }
                    matchedIcon?.let { onIconSelected(it) }
                }
            } else {
                ToastUtils.error(getString(R.string.category_not_found))
                findNavController().popBackStack()
            }
        } catch (e: Exception) {
            Logger.e("Failed to load category data", e)
            ToastUtils.error(getString(R.string.category_load_failed))
            findNavController().popBackStack()
        }
    }

    /**
     * 设置新分类的默认值
     */
    private fun setupNewCategory() {
        currentCategory = CategoryModel().apply {
            remoteBookId = bookRemoteId
            type = billType
            remoteParentId = parentId
        }
    }

    /**
     * 处理图标选择
     */
    private fun onIconSelected(icon: CategoryModel) {
        selectedIcon = icon
        binding.selectedIconImageView.setCategoryIcon(icon)
        binding.categoryNameInputLayout.editText?.setText(icon.name)
    }

    /**
     * 执行搜索（带防抖）
     */
    private fun performSearch(query: String) {
        searchJob?.cancel()

        if (query.isEmpty()) {
            iconAdapter.replaceItems(allIcons)
            return
        }

        searchJob = launch {
            delay(300) // 防抖延迟

            val filteredIcons = allIcons.filter { icon ->
                icon.name?.contains(query, ignoreCase = true) == true
            }

            iconAdapter.replaceItems(filteredIcons)
        }
    }

    /**
     * 保存分类
     */
    private fun saveCategory() {
        val categoryName = binding.categoryNameEditText.text?.toString()?.trim()

        // 验证输入
        if (categoryName.isNullOrEmpty()) {
            ToastUtils.error(getString(R.string.category_name_empty))
            return
        }

        if (selectedIcon == null) {
            ToastUtils.error(getString(R.string.category_icon_empty))
            return
        }

        // 更新分类信息
        currentCategory.apply {
            name = categoryName
            remoteBookId = bookRemoteId
            type = billType
            remoteParentId = parentId
            icon = selectedIcon?.icon
        }

        // 保存分类
        launch {
            val savedId = CategoryAPI.save(currentCategory)
            if (savedId > 0) {
                ToastUtils.info(getString(R.string.save_category_success))
                findNavController().popBackStack()
            } else {
                ToastUtils.error(getString(R.string.save_failed))
            }
        }
    }
} 