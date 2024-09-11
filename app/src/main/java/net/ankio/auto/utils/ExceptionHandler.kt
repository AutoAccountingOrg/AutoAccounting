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
import com.bugsnag.android.Event
import net.ankio.auto.App
import net.ankio.auto.BuildConfig
import net.ankio.auto.exceptions.AutoServiceException
import net.ankio.auto.storage.ConfigUtils
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.activity.ErrorActivity
import org.ezbook.server.constant.Setting
import kotlin.system.exitProcess

class ExceptionHandler(private val context: Context) : Thread.UncaughtExceptionHandler {
    private var mDefaultHandler: Thread.UncaughtExceptionHandler? = null


    init {
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        // 将当前类设为默认异常处理器
        Thread.setDefaultUncaughtExceptionHandler(this)

        Bugsnag.start(context)
        Bugsnag.addOnError { event ->
            val result = handleException(event) // 发送此异常
            Logger.i("是否发送异常到AppCenter => $result")
            result
        }
    }


    private fun handleException(events: Event): Boolean {
        val error = events.originalError
        if (error == null) {
            val msg = events.errors[0].errorMessage ?: ""
            Logger.e("发生内容为NULL的异常 => $msg")
            return false
        }
        val root = getRootCause(error)
        if (root is AutoServiceException) {
            return false
        }
        // return true
        // 调试模式不上传错误数据
        return !App.debug && ConfigUtils.getBoolean(Setting.SEND_ERROR_REPORT, true)
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
            ExceptionHandler(context)
        }
    }

    override fun uncaughtException(
        t: Thread,
        e: Throwable,
    ) {
        val root = getRootCause(e)
        Logger.e("发生未处理的异常", root)

        // 将异常拼成字符串
        val sb = StringBuilder()
        sb.append("版本: ${BuildConfig.VERSION_NAME}\n")
        sb.append("版本号: ${BuildConfig.VERSION_CODE}\n")
        sb.append("异常信息: ${root.message}\n")
        sb.append("异常堆栈: \n")
        root.stackTrace.forEach {
            sb.append(it.toString())
            sb.append("\n")
        }
        // 打开ErrorActivity
        val intent = Intent(context, ErrorActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra("msg", sb.toString())
        context.startActivity(intent)
        exitProcess(0) // 关闭已奔溃的app进程
    }
}
