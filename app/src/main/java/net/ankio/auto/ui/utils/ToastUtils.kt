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
import com.hjq.toast.ToastParams
import com.hjq.toast.Toaster
import com.hjq.toast.style.CustomToastStyle
import net.ankio.auto.App
import net.ankio.auto.R


object ToastUtils {
    fun init(application: Application){
        //这里进行主题包装
        Toaster.init(application)
    }

    fun info(int: Int){
        info(App.app.getString(int))
    }

    fun error(int: Int){
        error(App.app.getString(int))
    }

    fun info(msg: String){
        val params = ToastParams()
        params.text = msg
        params.style = CustomToastStyle(R.layout.toast_info)
        Toaster.show(params)
    }

    fun error(msg: String){
        val params = ToastParams()
        params.text = msg
        params.style = CustomToastStyle(R.layout.toast_error)
        Toaster.show(params)
    }
}