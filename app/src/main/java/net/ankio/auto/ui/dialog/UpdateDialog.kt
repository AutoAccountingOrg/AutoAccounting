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

package net.ankio.auto.ui.dialog

import android.app.Activity
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import net.ankio.auto.databinding.DialogUpdateBinding
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.utils.UpdateModel
import rikka.html.text.toHtml

/**
 * 更新对话框
 *
 * 用于显示应用或规则的更新信息，包括版本号、更新日志和更新操作
 *
 * 使用方式：
 * ```kotlin
 * UpdateDialog.create(activity)
 *     .setUpdateModel(updateModel)
 *     .setRuleTitle("规则更新")
 *     .setOnClickUpdate {
 *         // 执行更新操作
 *     }
 *     .show()
 * ```
 */
class UpdateDialog internal constructor(
    context: android.content.Context
) : BaseSheetDialog<DialogUpdateBinding>(context) {

    private var updateModel: UpdateModel? = null
    private var onClickUpdate: (() -> Unit)? = null
    private var ruleTitle: String? = null

    /**
     * 设置更新模型
     * @param model 更新信息模型
     * @return 当前对话框实例，支持链式调用
     */
    fun setUpdateModel(model: UpdateModel) = apply {
        this.updateModel = model
        updateUI()
    }

    /**
     * 设置更新点击回调
     * @param callback 点击更新按钮的回调函数
     * @return 当前对话框实例，支持链式调用
     */
    fun setOnClickUpdate(callback: () -> Unit) = apply {
        this.onClickUpdate = callback
    }

    /**
     * 设置规则标题
     * @param title 规则标题
     * @return 当前对话框实例，支持链式调用
     */
    fun setRuleTitle(title: String) = apply {
        this.ruleTitle = title
        binding.name.text = title
    }

    override fun onViewCreated(view: View?) {
        super.onViewCreated(view)

        updateUI()
        
        binding.update.setOnClickListener {
            onClickUpdate?.invoke()
            dismiss()
        }

        Logger.d("UpdateDialog created")
    }

    /**
     * 更新UI显示
     */
    private fun updateUI() {
        updateModel?.let { model ->
            binding.version.text = model.version
            binding.updateInfo.text = model.log.toHtml()
            binding.date.text = model.date
        }

        ruleTitle?.let { title ->
            binding.name.text = title
        }
    }

    override fun onDialogDestroy() {
        onClickUpdate = null
        super.onDialogDestroy()
    }

}
