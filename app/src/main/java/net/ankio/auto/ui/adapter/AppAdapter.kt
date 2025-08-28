/*
 * Copyright (C) 2023 ankio(ankio@ankio.net)
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

package net.ankio.auto.ui.adapter

import android.content.pm.PackageManager
import net.ankio.auto.databinding.AdapterAppBinding
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import net.ankio.auto.ui.models.AppInfo

/**
 * 应用选择适配器
 * 提供应用列表的显示和选择功能
 *
 * 设计原则（遵循Linus好品味）：
 * 1. 简洁构造：无参数构造函数
 * 2. 链式配置：通过链式调用设置包管理器和回调
 * 3. 语义化方法：清晰表达意图的方法名
 * 4. 消除特殊情况：统一的选择处理逻辑
 */
class AppAdapter : BaseAdapter<AdapterAppBinding, AppInfo>() {

    private var packageManager: PackageManager? = null
    private var onAppSelectionChanged: ((AppInfo) -> Unit)? = null

    /**
     * 设置包管理器
     * @param packageManager Android包管理器实例
     * @return 当前适配器实例，支持链式调用
     */
    fun setPackageManager(packageManager: PackageManager) = apply {
        this.packageManager = packageManager
    }

    /**
     * 设置应用选择状态变化监听器
     * @param listener 选择状态变化回调，参数为变化的应用信息
     * @return 当前适配器实例，支持链式调用
     */
    fun setOnAppSelectionChangedListener(listener: (AppInfo) -> Unit) = apply {
        this.onAppSelectionChanged = listener
    }

    /**
     * 初始化视图持有者
     * 设置应用项的点击事件监听
     */
    override fun onInitViewHolder(holder: BaseViewHolder<AdapterAppBinding, AppInfo>) {
        val binding = holder.binding

        // 设置整行点击事件，切换选择状态
        binding.root.setOnClickListener {
            holder.item?.let { app ->
                // 切换选择状态
                app.isSelected = !app.isSelected
                binding.checkbox.isChecked = app.isSelected

                // 通知选择状态变化
                onAppSelectionChanged?.invoke(app)
            }
        }
    }


    /**
     * 绑定数据到视图
     * 显示应用图标、名称、版本和包名信息
     *
     * @param holder 视图持有者
     * @param data 应用信息数据
     * @param position 位置索引
     */
    override fun onBindViewHolder(
        holder: BaseViewHolder<AdapterAppBinding, AppInfo>,
        data: AppInfo,
        position: Int
    ) {
        val binding = holder.binding
        val pkg = packageManager ?: return
        
        try {
            // 设置应用图标
            binding.appIcon.setImageDrawable(pkg.getApplicationIcon(data.packageName))

            // 设置应用名称，优先使用自定义名称
            binding.appName.text = if (data.appName.isNotEmpty()) {
                data.appName
            } else {
                pkg.getApplicationLabel(data.pkg).toString()
            }

            // 设置版本信息
            binding.appVersionName.text = pkg.getPackageInfo(data.packageName, 0).versionName

            // 设置包名
            binding.appPackageName.text = data.packageName

            // 设置选择状态
            binding.checkbox.isChecked = data.isSelected

        } catch (e: Exception) {
            // 处理包信息获取异常，设置默认值
            binding.appName.text = data.packageName
            binding.appVersionName.text = "Unknown"
            binding.appPackageName.text = data.packageName
            binding.checkbox.isChecked = data.isSelected
        }
    }

    override fun areItemsSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
        return oldItem.packageName == newItem.packageName
    }

    override fun areContentsSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
        return oldItem == newItem
    }


}