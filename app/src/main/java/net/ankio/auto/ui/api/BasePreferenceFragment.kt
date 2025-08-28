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

package net.ankio.auto.ui.api

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.annotation.XmlRes
import androidx.core.view.updatePadding
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceDataStore
import androidx.preference.PreferenceFragmentCompat
import net.ankio.auto.databinding.FragmentPreferenceBaseBinding
import net.ankio.auto.ui.utils.DisplayUtils

/**
 * 基础PreferenceFragment类
 *
 * 统一处理所有设置页面的通用逻辑：
 * 1. Toolbar的创建和配置
 * 2. 返回按钮的处理
 * 3. ViewBinding的生命周期管理
 * 4. PreferenceFragment的容器管理
 *
 * 子类只需要实现：
 * - getTitleRes(): 返回标题资源ID
 * - getPreferencesRes(): 返回XML资源ID
 * - createDataStore(): 创建数据存储
 * - setupPreferences(): 设置特定的偏好行为（可选）
 */
abstract class BasePreferenceFragment : PreferenceFragmentCompat() {

    private var _binding: FragmentPreferenceBaseBinding? = null
    protected val binding get() = _binding!!

    /**
     * 获取页面标题资源ID
     */
    @StringRes
    abstract fun getTitleRes(): Int

    /**
     * 获取偏好设置XML资源ID
     */
    @XmlRes
    abstract fun getPreferencesRes(): Int

    /**
     * 创建数据存储实例
     */
    abstract fun createDataStore(): PreferenceDataStore

    /**
     * 设置特定的偏好行为（可选重写）
     */
    protected open fun setupPreferences() {
        // 子类可以重写此方法来添加特定行为
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 创建通用布局
        _binding = FragmentPreferenceBaseBinding.inflate(inflater, container, false)
        binding.root.updatePadding(
            bottom = DisplayUtils.getNavigationBarHeight(requireContext())
        )
        // 在容器中添加PreferenceFragmentCompat的视图
        val preferenceView =
            super.onCreateView(inflater, binding.preferenceContainer, savedInstanceState)
        binding.preferenceContainer.addView(preferenceView)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 统一设置标题和返回按钮
        binding.toolbar.title = getString(getTitleRes())
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // 统一设置数据存储和加载XML
        preferenceManager.preferenceDataStore = createDataStore()
        setPreferencesFromResource(getPreferencesRes(), rootKey)

        // 调用子类的特定设置
        setupPreferences()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 清理 Toolbar 的导航监听器，防止 Lambda 持有 Fragment 引用导致内存泄漏
        binding.toolbar.setNavigationOnClickListener(null)
        _binding = null
    }
}
