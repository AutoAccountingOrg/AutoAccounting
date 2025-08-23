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

package net.ankio.auto.ui.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import net.ankio.auto.R
import net.ankio.auto.databinding.ActivityErrorBinding
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseActivity
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.utils.SystemUtils

/**
 * 错误页面Activity - 提供用户友好的错误处理界面
 * 支持错误信息展示、日志复制、重启应用等功能
 */
class ErrorActivity : BaseActivity() {

    private lateinit var binding: ActivityErrorBinding
    private var fullErrorMessage: String = ""
    private var errorSummary: String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityErrorBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // 如果没有Intent，直接重启应用
        if (intent == null) {
            SystemUtils.restart()
            return
        }

        setupErrorInfo()
        setupClickListeners()
    }

    /**
     * 设置错误信息显示
     */
    private fun setupErrorInfo() {
        fullErrorMessage = intent.getStringExtra("msg") ?: getString(R.string.unknown_error)

        // 提取错误摘要（第一行或异常类型）
        errorSummary = extractErrorSummary(fullErrorMessage)

        // 设置错误摘要显示
        binding.errorSummary.text = errorSummary

        // 设置完整错误信息
        binding.errorMsg.text = fullErrorMessage

        Logger.e("ErrorActivity显示错误页面", Exception(errorSummary))
    }

    /**
     * 设置点击事件监听器
     */
    private fun setupClickListeners() {
        // 重启按钮
        binding.errorRestartButton.setOnClickListener {
            SystemUtils.restart()
        }
    }

    /**
     * 提取错误摘要信息
     */
    private fun extractErrorSummary(errorMsg: String): String {
        val lines = errorMsg.split("\n")

        // 寻找异常类型行
        for (line in lines) {
            if (line.contains("Exception") || line.contains("Error")) {
                // 提取异常类名
                val exceptionName = line.substringAfterLast(".")
                    .substringBefore(":")
                    .trim()
                if (exceptionName.isNotEmpty()) {
                    return exceptionName
                }
            }

        }

        // 如果找不到异常类型，返回第一个非空行
        return lines.firstOrNull { it.trim().isNotEmpty() }
            ?: getString(R.string.unknown_error)
    }
}

