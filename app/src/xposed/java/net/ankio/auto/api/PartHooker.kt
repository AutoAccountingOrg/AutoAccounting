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
import de.robv.android.xposed.XposedBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.HookMainApp

abstract class PartHooker(val hooker: Hooker) {
    abstract  val hookName: String
    //   private val loadedClazz = mutableListOf<String>()


    abstract  fun onInit(classLoader: ClassLoader, context: Context) //初始化
    suspend fun loadClass(name: String): Class<*> {
        return hooker.hookUtils.loadClass(name)
    }



    /**
     * 正常输出日志
     */
    fun log(string: String){
        val tag = HookMainApp.getTag(hooker.appName, getSimpleName())
        hooker.hookUtils.scope.launch {
           runCatching {
               hooker.hookUtils.log(tag, string)
           }.onFailure {
               if(hooker.hookUtils.startAutoApp(it, hooker.hookUtils.context))return@launch
               XposedBridge.log(it)
           }
        }
    }

    private fun getSimpleName(): String {
        val stackTrace = Thread.currentThread().stackTrace
        //XposedBridge.log(Throwable())
        val callerClassName = if (stackTrace.size >= 5) {
            stackTrace[4].className
        } else {
            "Unknown"
        }
        return callerClassName.substringAfterLast('.') // 获取简单类名
    }

    /**
     * 调试模式输出日志
     */
    fun logD(string: String){
        val tag = HookMainApp.getTag(hooker.appName, getSimpleName())
        hooker.hookUtils.scope.launch {
            runCatching {
                hooker.hookUtils.logD(tag, string)
            }.onFailure {
                if(hooker.hookUtils.startAutoApp(it, hooker.hookUtils.context))return@launch
                XposedBridge.log(it)
            }
        }

    }

    fun analyzeData(dataType: Int,  data: String,app:String? = null)
    {
        hooker.hookUtils.scope.launch {
            runCatching {
                hooker.hookUtils.analyzeData(dataType, app?:hooker.packPageName, data,hooker.appName)
            }.onFailure {
                if(hooker.hookUtils.startAutoApp(it, hooker.hookUtils.context))return@launch
                XposedBridge.log(it)
            }
        }
    }


    suspend fun isDebug():Boolean{
        runCatching {
            return hooker.hookUtils.isDebug()
        }.onFailure {
            if(hooker.hookUtils.startAutoApp(it, hooker.hookUtils.context))return false
            XposedBridge.log(it)
        }
        return false
    }


}