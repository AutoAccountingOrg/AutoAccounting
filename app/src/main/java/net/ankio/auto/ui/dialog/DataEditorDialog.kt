/*
 * Copyright (C) 2024 ankio(ankio@ankio.net)
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
import androidx.lifecycle.LifecycleService
import net.ankio.auto.R
import net.ankio.auto.databinding.DialogDataEditorBinding
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.utils.Desensitizer
import net.ankio.auto.ui.utils.ToastUtils

class DataEditorDialog private constructor(
    context: android.content.Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner?,
    isOverlay: Boolean
) : BaseSheetDialog<DialogDataEditorBinding>(context, lifecycleOwner, isOverlay) {

    private var data: String = ""
    private var callback: ((result: String) -> Unit)? = null

    /**
     * 设置初始数据
     * @param data 要编辑的数据
     * @return 当前对话框实例，支持链式调用
     */
    fun setData(data: String) = apply {
        this.data = data
        // 如果view已创建，立即更新UI
        binding.etContent.setText(data)
    }

    /**
     * 设置回调函数
     * @param callback 数据编辑完成后的回调
     * @return 当前对话框实例，支持链式调用
     */
    fun setOnConfirm(callback: (result: String) -> Unit) = apply {
        this.callback = callback
    }

    override fun onViewCreated(view: View?) {
        super.onViewCreated(view)
        binding.btnConfirm.setOnClickListener {
            callback?.invoke(binding.etContent.text.toString())
            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        // 设置初始数据
        binding.etContent.setText(data)

        binding.btnReplace.setOnClickListener {
            val keyword = binding.etRaw.text.toString()
            val replaceData = binding.etTarget.text.toString()
            val editorData = binding.etContent.text.toString()

            if (keyword.isEmpty() || replaceData.isEmpty()) {
                ToastUtils.error(R.string.no_empty)
                return@setOnClickListener
            }

            if (!editorData.contains(keyword)) {
                ToastUtils.error(R.string.no_replace)
                return@setOnClickListener
            }
            binding.etContent.setText(editorData.replace(keyword, replaceData))
        }

        binding.btnMaskAll.setOnClickListener {
            val result = Desensitizer.maskAll(binding.etContent.text.toString())
            BottomSheetDialogBuilder.create(context as Activity)
                .setTitle(context.getString(R.string.replace_result))
                .setMessage(result.changes.joinToString(separator = "\n") { (from, to) -> "\"$from\" → \"$to\"" })
                .setPositiveButton(R.string.btn_confirm) { _, _ ->
                    binding.etContent.setText(result.masked)
                }.setNegativeButton(R.string.btn_cancel) { _, _ ->

                }.show()

        }

    }

    companion object {
        /**
         * 从Activity创建数据编辑对话框
         * @param activity 宿主Activity
         * @return 对话框实例，支持链式调用
         */
        fun create(activity: Activity): DataEditorDialog {
            return DataEditorDialog(activity, activity as androidx.lifecycle.LifecycleOwner, false)
        }

        /**
         * 从Fragment创建数据编辑对话框
         * @param fragment 宿主Fragment
         * @return 对话框实例，支持链式调用
         */
        fun create(fragment: Fragment): DataEditorDialog {
            return DataEditorDialog(fragment.requireContext(), fragment.viewLifecycleOwner, false)
        }

        /**
         * 从Service创建数据编辑对话框（悬浮窗模式）
         * @param service 宿主Service
         * @return 对话框实例，支持链式调用
         */
        fun create(service: LifecycleService): DataEditorDialog {
            return DataEditorDialog(service, service, true)
        }
    }
}

