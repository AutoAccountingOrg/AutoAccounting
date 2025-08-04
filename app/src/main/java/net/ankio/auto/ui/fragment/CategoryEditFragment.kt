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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentCategoryEditBinding
import net.ankio.auto.http.api.CategoryAPI
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.adapter.CategorySelectorAdapter
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.dialog.BottomSheetDialogBuilder
import net.ankio.auto.ui.utils.CategoryUtils
import net.ankio.auto.ui.utils.ToastUtils
import org.ezbook.server.constant.BillType
import org.ezbook.server.db.model.CategoryModel

/**
 * 分类编辑Fragment
 *
 * 该Fragment负责分类的创建和编辑功能，提供以下功能：
 * - 创建新分类
 * - 编辑现有分类
 * - 展示所属账本和父分类信息
 * - 使用CategoryUtils获取系统分类图标并支持搜索
 * - 保存和删除分类
 */
class CategoryEditFragment : BaseFragment<FragmentCategoryEditBinding>() {

    /** 当前分类模型 */
    private var currentCategoryModel: CategoryModel = CategoryModel()

    /** 是否为编辑模式 */
    private var isEditMode = false

    /** 分类ID */
    private var categoryId: Long = 0L

    /** 账本名称 */
    private var bookName: String = ""

    /** 账单类型 */
    private var billType: BillType = BillType.Expend

    /** 父分类ID */
    private var parentId: String = "-1"

    /** 父分类名称 */
    private var parentCategoryName: String = ""

    /** 分类工具类 */
    private val categoryUtils = CategoryUtils()

    /** 图标适配器 */
    private lateinit var iconAdapter: CategorySelectorAdapter

    /** 所有图标数据 */
    private var allCategories = mutableListOf<CategoryModel>()

    /** 过滤后的图标数据 */
    private var filteredCategories = mutableListOf<CategoryModel>()

    /** 当前选中的分类 */
    private var selectedCategory: CategoryModel? = null

    /** 搜索防抖延迟 */
    private var searchJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 获取参数并判断编辑模式
        arguments?.let { bundle ->
            categoryId = bundle.getLong("categoryId", 0L)
            bookName = bundle.getString("bookName", "")
            billType = BillType.valueOf(bundle.getString("billType", "Expend"))
            parentId = bundle.getString("parentId", "-1")
            parentCategoryName = bundle.getString("parentCategoryName", "")
            isEditMode = categoryId > 0
        }

        setupUI()
        setupEvents()
        setupCategoryGrid()
        loadIconData()

        // 加载现有数据（编辑模式）
        if (isEditMode) {
            loadCategoryData()
        } else {
            // 创建模式，设置默认值
            initializeNewCategory()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 取消搜索任务，避免内存泄漏
        searchJob?.cancel()
    }

    /**
     * 设置UI界面
     */
    private fun setupUI() = with(binding) {
        // 设置标题
        toolbar.title = getString(if (isEditMode) R.string.edit_category else R.string.add_category)
        saveButton.text = getString(if (isEditMode) R.string.btn_save else R.string.btn_create)

        // 设置账本信息
        bookNameText.text = bookName

        // 设置父分类信息
        loadParentCategoryInfo()

        // 返回按钮
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    /**
     * 设置分类图标网格
     */
    private fun setupCategoryGrid() = with(binding) {
        // 直接使用statusPage
        val recyclerView = statusPage.contentView!!

        // 设置网格布局管理器（5列）
        val layoutManager = GridLayoutManager(requireContext(), 5)
        recyclerView.layoutManager = layoutManager

        // 初始化适配器
        iconAdapter = CategorySelectorAdapter(
            onItemClick = { item, pos, hasChild, view ->
                onCategorySelected(item)
            },
            onItemChildClick = { _, _ -> },
            onItemLongClick = null,
            isEditMode = false
        )

        recyclerView.adapter = iconAdapter
    }

    /**
     * 设置事件监听器
     */
    private fun setupEvents() = with(binding) {
        // 保存按钮
        saveButton.setOnClickListener { saveCategory() }

        // 图标搜索（带防抖）
        iconSearchEditText.addTextChangedListener { text ->
            val query = text?.toString() ?: ""
            performSearchWithDebounce(query)
        }
    }

    /**
     * 加载图标数据
     */
    private fun loadIconData() {
        lifecycleScope.launch {
            try {
                // 在IO线程中加载分类数据
                val categories = withContext(Dispatchers.IO) {
                    categoryUtils.list(requireContext()).map { categoryItem ->
                        CategoryModel().apply {
                            name = categoryItem.name
                            icon = categoryItem.icon
                            remoteId = categoryItem.remoteId
                        }
                    }
                }

                // 在主线程中更新UI
                allCategories.clear()
                allCategories.addAll(categories)

                filteredCategories.clear()
                filteredCategories.addAll(allCategories)

                // 显示加载的数据
                binding.statusPage.showContent()
                iconAdapter.submitItems(filteredCategories)

                // 设置默认图标
                if (allCategories.isNotEmpty() && selectedCategory == null) {
                    onCategorySelected(allCategories.first())
                }
            } catch (e: Exception) {
                Logger.e("Failed to load icon data", e)
                ToastUtils.error(getString(R.string.category_load_failed))
                binding.statusPage.showError()
            }
        }
    }

    /**
     * 处理分类选择
     */
    private fun onCategorySelected(category: CategoryModel) {
        selectedCategory = category

        // 加载图标图片
        Glide.with(this@CategoryEditFragment)
            .load(category.icon)
            .placeholder(R.drawable.default_cate)
            .error(R.drawable.default_cate)
            .into(binding.selectedIconImageView)
    }

    /**
     * 带防抖的搜索
     */
    private fun performSearchWithDebounce(query: String) {
        // 取消之前的搜索任务
        searchJob?.cancel()

        // 如果查询为空，立即显示所有数据
        if (query.isEmpty()) {
            filterCategories(query)
            return
        }

        // 创建新的搜索任务，延迟300ms执行
        searchJob = lifecycleScope.launch {
            delay(300) // 防抖延迟
            filterCategories(query)
        }
    }

    /**
     * 过滤分类数据（优化版本）
     */
    private fun filterCategories(query: String) {
        val newFilteredList = if (query.isEmpty()) {
            allCategories
        } else {
            allCategories.filter { category ->
                category.name?.contains(query, ignoreCase = true) == true
            }
        }

        // 只在数据真正改变时才更新
        if (filteredCategories.size != newFilteredList.size ||
            !filteredCategories.containsAll(newFilteredList)
        ) {
            filteredCategories.clear()
            filteredCategories.addAll(newFilteredList)

            // 使用 notifyDataSetChanged 避免视图复用问题
            iconAdapter.submitItems(filteredCategories)
        }
    }

    /**
     * 加载父分类信息
     */
    private fun loadParentCategoryInfo() {
        if (parentId == "-1") {
            binding.parentCategoryText.text = getString(R.string.no_parent_category)
            return
        }

        binding.parentCategoryText.text = parentCategoryName
    }

    /**
     * 初始化新分类
     */
    private fun initializeNewCategory() {
        currentCategoryModel = CategoryModel().apply {
            remoteBookId = bookName
            type = billType
            remoteParentId = parentId
        }

        // 设置默认图标信息
        updateUIWithDefaultIcon()
    }

    /**
     * 更新UI显示默认图标
     */
    private fun updateUIWithDefaultIcon() = with(binding) {
        selectedIconImageView.setImageResource(R.drawable.default_cate)
    }

    /**
     * 加载现有分类数据
     */
    private fun loadCategoryData() {
        lifecycleScope.launch {
            try {
                val category = CategoryAPI.getById(categoryId)
                category?.let {
                    currentCategoryModel = it
                    binding.categoryNameEditText.setText(it.name)
                    parentId = it.remoteParentId

                    // 如果有图标，设置选中状态
                    it.icon?.let { iconUrl ->
                        val iconItem = allCategories.find { icon -> icon.icon == iconUrl }
                        iconItem?.let { item ->
                            onCategorySelected(item)
                        }
                    }
                } ?: run {
                    ToastUtils.error(getString(R.string.category_not_found))
                    findNavController().popBackStack()
                }
            } catch (e: Exception) {
                Logger.e("Failed to load category data", e)
                ToastUtils.error(getString(R.string.category_load_failed))
                findNavController().popBackStack()
            }
        }
    }

    /**
     * 保存分类
     */
    private fun saveCategory() {
        val categoryName = binding.categoryNameEditText.text?.toString()?.trim()

        if (categoryName.isNullOrEmpty()) {
            ToastUtils.error(getString(R.string.category_name_empty))
            return
        }

        if (selectedCategory == null) {
            ToastUtils.error(getString(R.string.category_icon_empty))
            return
        }

        // 更新分类信息
        currentCategoryModel.apply {
            name = categoryName
            remoteBookId = bookName
            type = billType
            remoteParentId = parentId
            icon = selectedCategory?.icon // 保存选中的图标URL
        }

        lifecycleScope.launch {
            try {
                val savedId = CategoryAPI.save(currentCategoryModel)
                if (savedId > 0) {
                    ToastUtils.info(getString(R.string.save_category_success))
                    findNavController().popBackStack()
                } else {
                    ToastUtils.error(getString(R.string.save_failed))
                }
            } catch (e: Exception) {
                Logger.e("Failed to save category", e)
                ToastUtils.error(getString(R.string.save_failed))
            }
        }
    }

    /**
     * 显示删除分类确认对话框
     */
    private fun showDeleteCategoryDialog() {
        if (!isEditMode) return

        BottomSheetDialogBuilder(this)
            .setTitle(getString(R.string.delete_category))
            .setMessage(getString(R.string.delete_category_message, currentCategoryModel.name))
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                deleteCategory()
            }
            .setNegativeButton(getString(R.string.close)) { _, _ -> }
            .show()
    }

    /**
     * 删除分类
     */
    private fun deleteCategory() {
        lifecycleScope.launch {
            try {
                val deletedId = CategoryAPI.delete(currentCategoryModel.id)
                if (deletedId > 0) {
                    ToastUtils.info(getString(R.string.delete_category_success))
                    findNavController().popBackStack()
                } else {
                    ToastUtils.error(getString(R.string.delete_failed))
                }
            } catch (e: Exception) {
                Logger.e("Failed to delete category", e)
                ToastUtils.error(getString(R.string.delete_failed))
            }
        }
    }
} 