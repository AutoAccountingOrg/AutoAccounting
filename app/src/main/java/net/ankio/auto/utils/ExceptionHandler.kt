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

package net.ankio.auto.utils

import android.content.Context
import android.content.Intent
import com.bugsnag.android.Bugsnag
import net.ankio.auto.BuildConfig
import net.ankio.auto.exceptions.AutoServiceException


class ExceptionHandler : Thread.UncaughtExceptionHandler {
    private var mDefaultHandler: Thread.UncaughtExceptionHandler? = null

    private var context: Context? = null
    /**
     * 初始化默认异常捕获
     */
    fun init(context: Context?) {
        // 获取默认异常处理器
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        // 将当前类设为默认异常处理器
        Thread.setDefaultUncaughtExceptionHandler(this)
        if(SpUtils.getBoolean("sendToAppCenter",true)){
            Bugsnag.start(context!!)
        }
        this.context = context
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        if (!handleException(getRootCause(e))) {
            // 如果不处理,则调用系统默认处理异常,弹出系统强制关闭的对话框
            if (mDefaultHandler != null) {
                mDefaultHandler!!.uncaughtException(t, e)
            }
        }
    }

    private fun handleException(e: Throwable?): Boolean {
        if (e == null) {
            return false
        }
        Logger.e("未处理异常",e)
        if(e is AutoServiceException){
        /*    context?.let {
                val intent = Intent(it, ServiceActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                it.startActivity(intent)
            }*/
        }else{
            if(SpUtils.getBoolean("sendToAppCenter",true) && !BuildConfig.DEBUG){
                Bugsnag.notify(e)
            }
            //TODO 跳转错误页面，在错误页面重新启动
        }
        return true
    }

    private fun getRootCause(e: Throwable): Throwable {
        var cause = e.cause
        while (cause?.cause != null) {
            cause = cause.cause
        }
        return cause ?: e
    }


    companion object {
        fun init(context: Context) {
              ExceptionHandler().init(context)
        }
    }
}