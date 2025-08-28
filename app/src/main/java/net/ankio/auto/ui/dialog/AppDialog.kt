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

package net.ankio.auto.ui.dialog

import android.app.Activity
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import androidx.recyclerview.widget.RecyclerView
import net.ankio.auto.adapter.AppAdapterManager
import net.ankio.auto.adapter.IAppAdapter
import net.ankio.auto.databinding.DialogAppBinding
import net.ankio.auto.ui.adapter.AppListAdapter
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.components.WrapContentLinearLayoutManager
import net.ankio.auto.utils.PrefManager

/**
 * 记账软件选择对话框
 *
 * 功能：
 * - 展示支持的记账软件列表
 * - 点击已安装的目标项即可选择并保存
 * - 未安装则跳转到官网/应用页面
 * - 支持在 Activity / Fragment / Service(悬浮窗) 环境中使用
 *
 * 使用方式：
 * ```kotlin
 * BaseSheetDialog.create<AppDialog>(context)
 *     .setOnClose { refreshUI() }
 *     .show()
 * ```
 */
class AppDialog internal constructor(
    context: android.content.Context
) : BaseSheetDialog<DialogAppBinding>(context) {

    /**
     * 关闭回调。
     *
     * 当对话框关闭（dismiss）后触发，用于通知外部刷新界面等。
     */
    private var onClose: (() -> Unit)? = null

    /**
     * 设置关闭回调
     *
     * @param callback 对话框关闭后要执行的回调
     * @return 返回自身以便链式调用
     */
    fun setOnClose(callback: () -> Unit) = apply {
        this.onClose = callback
    }

    override fun onViewCreated(view: View?) {
        super.onViewCreated(view)
        setupRecycler(binding.recyclerView)
    }

    /**
     * 初始化并绑定列表
     */
    private fun setupRecycler(recyclerView: RecyclerView) {
        recyclerView.layoutManager = WrapContentLinearLayoutManager(ctx)

        // 直接使用 IAppAdapter 列表作为数据源
        val apps: List<IAppAdapter> = AppAdapterManager.adapterList()

        // 创建并绑定适配器
        val adapter = AppListAdapter(ctx, PrefManager.bookApp) { selected ->
            // 保存选择的记账软件
            PrefManager.bookApp = selected.pkg
            // 关闭弹窗
            dismiss()
        }
        recyclerView.adapter = adapter
        // 使用全量提交，避免依赖 Diff 回调
        adapter.submitItems(apps)
    }

    override fun dismiss() {
        // 先关闭对话框，再回调通知外部刷新
        super.dismiss()
        onClose?.invoke()
    }

    /**
     * 弹窗销毁时清理回调引用，防止内存泄漏
     */
    override fun onDialogDestroy() {
        super.onDialogDestroy()
        // 清理回调引用，防止持有 Fragment 引用导致内存泄漏
        onClose = null
    }
}