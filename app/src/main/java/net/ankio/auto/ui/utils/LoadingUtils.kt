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

package net.ankio.auto.ui.utils

import android.app.Activity
import android.app.Dialog
import android.content.Context
import net.ankio.auto.utils.CoroutineUtils
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.StringRes
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import net.ankio.auto.R
import net.ankio.auto.databinding.DialogLoadingBinding
import net.ankio.auto.utils.SystemUtils.findLifecycleOwner

/**
 * 加载对话框工具类
 *
 * 设计原则（遵循Linus好品味）：
 * 1. 简化构造：支持Context，自动推断LifecycleOwner
 * 2. 生命周期管理：自动在生命周期结束时清理资源
 * 3. 向后兼容：保留Activity构造函数，支持渐进式迁移
 * 4. 消除特殊情况：统一的UI线程处理
 *
 * 使用方式：
 * ```kotlin
 * // 推荐方式：自动生命周期管理
 * val loading = LoadingUtils(context)
 * loading.show("加载中...")
 *
 * // 兼容方式：手动传入Activity
 * val loading = LoadingUtils(requireActivity())
 * loading.show("加载中...")
 * ```
 */
class LoadingUtils : DefaultLifecycleObserver {

    private val context: Context
    private val lifecycleOwner: LifecycleOwner?


    private var dialog: Dialog? = null
    private var binding: DialogLoadingBinding? = null

    /**
     * 推荐构造函数：使用Context，自动推断生命周期
     * @param context 上下文，可以是Activity、Fragment等
     */
    constructor(context: Context) {
        this.context = context
        this.lifecycleOwner = context.findLifecycleOwner()

        // 注册生命周期观察者，自动清理资源
        lifecycleOwner.lifecycle.addObserver(this)
    }

    /**
     * 兼容构造函数：保持向后兼容性
     * @param activity Activity实例
     */
    constructor(activity: Activity) : this(activity as Context)

    fun show(@StringRes text: Int) {
        show(context.getString(text))
    }

    fun show(text: String? = null) {
        CoroutineUtils.runOnUiThread {
            // 如果生命周期已结束，不显示对话框
            if (lifecycleOwner?.lifecycle?.currentState?.isAtLeast(androidx.lifecycle.Lifecycle.State.INITIALIZED) == false) {
                return@runOnUiThread
            }

            if (dialog?.isShowing == true) {
                return@runOnUiThread
            }

            dialog = Dialog(context, R.style.CustomDialogTheme).apply {
                setCancelable(false)
            }

            binding = DialogLoadingBinding.inflate(LayoutInflater.from(context)).apply {
                if (text.isNullOrEmpty()) {
                    loadingText.visibility = View.GONE
                } else {
                    loadingText.text = text
                }
            }

            dialog?.setContentView(binding!!.root)
            dialog?.show()
        }
    }

    fun setText(@StringRes text: Int) {
        setText(context.getString(text))
    }

    fun setText(text: String?) {
        CoroutineUtils.runOnUiThread {
            if (dialog?.isShowing == true && binding != null) {
                binding!!.loadingText.text = text
                binding!!.loadingText.visibility = View.VISIBLE
            }
        }
    }

    fun close() {
        CoroutineUtils.runOnUiThread {
            dialog?.takeIf { it.isShowing }?.dismiss()
            dialog = null
            binding = null
        }
    }

    /**
     * 生命周期回调：Activity/Fragment销毁时自动清理
     */
    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        close()
        lifecycleOwner?.lifecycle?.removeObserver(this)
    }
    

}
