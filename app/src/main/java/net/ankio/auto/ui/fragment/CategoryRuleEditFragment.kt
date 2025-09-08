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
import com.google.gson.Gson
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentCategoryRuleEditBinding
import net.ankio.auto.http.api.CategoryRuleAPI
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.api.bindAs
import net.ankio.auto.ui.fragment.components.CategoryRuleEditComponent
import net.ankio.auto.ui.utils.ToastUtils
import org.ezbook.server.db.model.CategoryRuleModel

/**
 * 分类规则编辑Fragment - 重构后的简洁版本
 *
 * 重构亮点（体现Linus好品味）：
 * 1. 单一职责：Fragment只负责生命周期管理和导航
 * 2. 消除复杂性：UI逻辑完全委托给CategoryComponent
 * 3. 保持兼容：API接口和参数传递方式保持不变
 * 4. 简洁实现：从1000+行代码简化到100行左右
 *
 * 设计原则：
 * - Fragment作为"使用者"而不是"实现者"
 * - 组件化思维：复用CategoryComponent的强大功能
 * - 错误处理：统一的异常处理和用户反馈
 * - 向后兼容：不破坏现有的调用方式
 */
class CategoryRuleEditFragment : BaseFragment<FragmentCategoryRuleEditBinding>() {

    /** 分类规则编辑组件 */
    private lateinit var categoryRuleEditComponent: CategoryRuleEditComponent


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            // 初始化Fragment
            binding.toolbar.setNavigationOnClickListener {
                findNavController().popBackStack()
            }

            categoryRuleEditComponent = binding.categoryComponent.bindAs()

            // 设置事件监听器
            setupEventListeners()

            // 解析参数并设置数据
            parseArgumentsAndSetupComponent()

        } catch (e: Exception) {
            Logger.e("Fragment初始化失败: ${e.message}")
            handleInitializationError(e)
        }
    }


    /**
     * 设置事件监听器
     */
    private fun setupEventListeners() {
        // 保存按钮点击事件
        binding.saveItem.setOnClickListener {
            saveRule()
        }
    }

    /**
     * 解析传入参数并设置组件数据
     */
    private fun parseArgumentsAndSetupComponent() {
        val bundle = arguments
        if (bundle == null) {
            Logger.d("没有传入参数，创建新规则")
            setupForNewRule()
            return
        }

        try {
            val dataJson = bundle.getString("data")
            if (dataJson.isNullOrEmpty()) {
                Logger.d("参数为空，创建新规则")
                setupForNewRule()
                return
            }

            // 解析现有规则数据
            val categoryRuleModel = Gson().fromJson(dataJson, CategoryRuleModel::class.java)
                ?: CategoryRuleModel()

            setupForExistingRule(categoryRuleModel)

            Logger.d("解析规则数据成功: id=${categoryRuleModel.id}")
        } catch (e: Exception) {
            Logger.w("解析规则数据失败，使用默认值: ${e.message}")
            setupForNewRule()
        }
    }

    /**
     * 设置新规则创建模式
     */
    private fun setupForNewRule() {

        // 设置组件为编辑模式，传入空模型
        categoryRuleEditComponent.setRuleModel(null, readOnly = false)

        Logger.d("设置为新规则创建模式")
    }

    /**
     * 设置现有规则编辑模式
     */
    private fun setupForExistingRule(ruleModel: CategoryRuleModel) {
        // 设置组件为编辑模式，传入现有模型
        categoryRuleEditComponent.setRuleModel(ruleModel, readOnly = false)

        Logger.d("设置为现有规则编辑模式: ${ruleModel.id}")
    }

    /**
     * 保存规则 - 简化后的实现
     */
    private fun saveRule() {
        val categoryRuleModel = categoryRuleEditComponent.getRule() ?: return
        launch {
            // 调用API保存规则
            CategoryRuleAPI.put(categoryRuleModel)

            Logger.i("规则保存成功: ${categoryRuleModel.js}")

            // 显示成功提示
            ToastUtils.info(R.string.save_rule_success)

            // 返回上一页面
            findNavController().popBackStack()
        }
    }

    /**
     * 处理初始化错误
     */
    private fun handleInitializationError(error: Throwable) {
        Logger.e("Fragment初始化失败: ${error.message}")

        // 显示错误提示
        ToastUtils.error("初始化失败，请重试")

        // 返回上一页面
        findNavController().popBackStack()
    }

    /**
     * Fragment销毁时的清理工作
     */
    override fun onDestroyView() {
        super.onDestroyView()

        // CategoryComponent会通过BaseComponent自动清理
        // 这里只需要清理Fragment级别的资源

        // CategoryRuleEditComponent会通过BaseComponent自动清理，无需手动调用

        Logger.d("CategoryRuleEditFragment销毁完成")
    }
}