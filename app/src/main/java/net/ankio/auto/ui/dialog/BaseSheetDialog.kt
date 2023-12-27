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

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import net.ankio.auto.R


abstract class BaseSheetDialog(context: Context) :
    BottomSheetDialog(context, R.style.BottomSheetDialog) {
    private val job = Job()
    val coroutineScope = CoroutineScope(Dispatchers.Main + job)
    abstract fun onCreateView(inflater: LayoutInflater): View

    fun show(float: Boolean = false) {
        val inflater = LayoutInflater.from(context)
        val root = this.onCreateView(inflater)
        this.setContentView(root)
        this.setCancelable(false);
        if (float) {
            this.window?.setType((WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY));
        }
        val bottomSheet: View = root.parent as View


        val bottomSheetBehavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)

        // 禁用向下滑动关闭行为
        bottomSheetBehavior.isDraggable = false
        // 设置BottomSheetDialog展开到全屏高度
        bottomSheetBehavior.skipCollapsed = true
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED;

        this.show()
    }

    override fun dismiss() {
        super.dismiss()
        job.cancel()
    }

}