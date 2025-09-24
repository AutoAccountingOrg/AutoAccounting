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

import android.app.Application
import android.content.Context
import android.view.Gravity
import android.view.View
import com.hjq.toast.ToastParams
import com.hjq.toast.Toaster
import com.hjq.toast.style.CustomToastStyle
import net.ankio.auto.R
import net.ankio.auto.autoApp
import net.ankio.auto.utils.ThemeUtils


object ToastUtils {

    class CustomToastStyleAnkio(id: Int, gravity: Int) :
        CustomToastStyle(id, gravity, 0, 200, 0f, 0f) {
        override fun createView(context: Context?): View {
            return super.createView(ThemeUtils.themedCtx(context!!))
        }
    }

    fun init(application: Application) {
        Toaster.init(application)
    }

    fun info(int: Int) {
        info(autoApp.getString(int))
    }

    fun error(int: Int) {
        error(autoApp.getString(int))
    }

    fun warn(int: Int) {
        warn(autoApp.getString(int))
    }

    fun info(msg: String) {
        Toaster.show(ToastParams().apply {
            text = msg
            style = CustomToastStyleAnkio(R.layout.toast_info, Gravity.BOTTOM)

        })
    }

    fun warn(msg: String) {
        Toaster.show(ToastParams().apply {
            text = msg
            style = CustomToastStyleAnkio(R.layout.toast_warn, Gravity.BOTTOM)
            
        })
    }

    fun error(msg: String) {
        Toaster.show(ToastParams().apply {
            text = msg
            style = CustomToastStyleAnkio(R.layout.toast_error, Gravity.BOTTOM)

        })
    }
}