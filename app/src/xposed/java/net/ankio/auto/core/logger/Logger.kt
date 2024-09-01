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

package net.ankio.auto.core.logger

import android.util.Log
import de.robv.android.xposed.XposedBridge
import kotlinx.coroutines.launch
import net.ankio.auto.core.App
import org.ezbook.server.constant.LogLevel
import org.ezbook.server.db.model.LogModel

/**
 * Xposed日志工具
 *
 */
object Logger {


    /**
     * 是否为调试模式
     */
    var debug = false

    private fun getTag(): String {
        return Throwable().stackTrace[2].className.substringBefore('$').substringAfterLast(".")
    }
    /**
     * 打印日志
     */
    fun log(app:String,msg: String) {
       XposedBridge.log(msg)
        Log.i("自动记账", msg)
       //写入自动记账日志
        App.scope.launch {
           runCatching {
               LogModel.add(LogLevel.INFO,app, getTag(),msg)
           }.onFailure {
               Log.d("自动记账", msg)
           }
        }

    }

    /**
     * 只在调试模式输出日志
     */
    fun logD(app:String,msg: String) {
        if (debug) {
           this.log(app,msg)
        }
    }

    /**
     * 打印错误日志
     */
    fun logE(app:String,e: Throwable) {
        XposedBridge.log(e)
        Log.e("自动记账", e.message?:"")
        App.scope.launch {
            LogModel.add(LogLevel.ERROR,app, getTag(),e.message?:"")
        }
    }
}