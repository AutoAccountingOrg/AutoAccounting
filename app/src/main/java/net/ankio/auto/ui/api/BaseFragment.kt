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

package net.ankio.auto.ui.api

import android.R
import android.os.Bundle
import android.view.View
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.App
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.utils.ViewUtils


/**
 * 基础的Fragment
 */
abstract class BaseFragment : Fragment() {

    abstract val binding: ViewBinding

    override fun toString(): String {
        return this.javaClass.simpleName
    }

    override fun onStop() {
        super.onStop()
        App.pageStopOrDestroy()
    }


    open fun beforeViewBindingDestroy() {

    }

    open fun navigate(@IdRes resId: Int,bundle: Bundle? = null) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val options = NavOptions.Builder()
                    .setEnterAnim(R.anim.fade_in)
                    .setExitAnim(R.anim.fade_out)
                    .setPopEnterAnim(R.anim.fade_in)
                    .setPopExitAnim(R.anim.fade_out)
                    .build()
                // 使用协程的 withContext 确保在主线程执行
                withContext(Dispatchers.Main) {
                    findNavController().navigate(resId,bundle,options)
                }
            } catch (e: Exception) {
                Logger.w("Navigation failed: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        App.pageStopOrDestroy()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.visibility = View.INVISIBLE
        view.postDelayed({
            view.visibility = View.VISIBLE
        }, 300)
        val materialToolbar = ViewUtils.findMaterialToolbar(view) ?: return
        
        // 通过tag判断是否是返回按钮
        if (materialToolbar.tag == "back_button") {
            materialToolbar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }
        }
    }

}
