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

package net.ankio.auto.api

import android.content.Context
import net.ankio.auto.HookMainApp

abstract class PartHooker(val hooker: Hooker) {
    abstract fun onInit(classLoader: ClassLoader?,context: Context?)

    fun log(string: String){
        val stackTrace = Thread.currentThread().stackTrace
        val callerClassName = if (stackTrace.size >= 4) {
            // 获取调用log方法的类的全限定名
            stackTrace[3].className
        } else {
            "Unknown"
        }
        val simpleName = callerClassName.substringAfterLast('.') // 获取简单类名
        hooker.hookUtils.log(HookMainApp.getTag(hooker.appName, simpleName), string)

    }

    fun analyzeData(dataType: Int,  data: String,app:String? = null)
    {
        hooker.hookUtils.analyzeData(dataType, app?:hooker.packPageName, data)
    }
}