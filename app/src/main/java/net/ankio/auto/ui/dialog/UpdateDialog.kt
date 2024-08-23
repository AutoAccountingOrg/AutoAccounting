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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.databinding.DialogUpdateBinding
import net.ankio.auto.events.AutoServiceErrorEvent
import net.ankio.auto.events.UpdateFinishEvent
import net.ankio.auto.exceptions.AutoServiceException
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.utils.Logger
import net.ankio.auto.utils.SpUtils
import net.ankio.auto.utils.event.EventBus
import net.ankio.auto.utils.request.RequestsUtils
import net.ankio.auto.models.SettingModel
import rikka.html.text.toHtml

class UpdateDialog(
    private val context: Context,
    private val download: HashMap<String, String>, // 下载地址
    val log: String, // 更新日志,html
    val version: String, // 版本号
    private val date: String, // 更新日期
    val type: Int = 0, // 更新类型,0 为App, 1 为规则
    private val code: Int = 0,
) : BaseSheetDialog(context) {
    private lateinit var binding: DialogUpdateBinding

    private lateinit var loadingUtils: LoadingUtils
    private val listener = { event: UpdateFinishEvent ->
        if (::loadingUtils.isInitialized) {
            loadingUtils.close()
        }
        dismiss()
        false
    }

    override fun onCreateView(inflater: LayoutInflater): View {
        binding = DialogUpdateBinding.inflate(inflater)

        cardView = binding.cardView
        cardViewInner = binding.cardViewInner

        binding.version.text = version
        binding.updateInfo.text = log.toHtml()
        binding.date.text = date
        binding.name.text =
            if (type == 1) context.getString(R.string.rule) else context.getString(R.string.app)
        binding.update.setOnClickListener {
            if (type == 1) {
                loadingUtils = LoadingUtils(context as Activity)
                loadingUtils.show(context.getString(R.string.update_process))
                // 使用顶级协程作用域
                lifecycleScope.launch {
                    updateRule(download["category"] ?: "", download["rule"] ?: "")
                }
            } else {
                updateApp(download["url"] ?: "")
                dismiss()
            }
        }

        EventBus.register(UpdateFinishEvent::class.java, listener)

        return binding.root
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        EventBus.unregister(UpdateFinishEvent::class.java, listener)
    }

    private suspend fun updateLoadingUtils(int: Int) =
        withContext(Dispatchers.Main) {
            loadingUtils.setText(int)
        }

    private suspend fun updateRule(
        category: String,
        rule: String,
    ) = withContext(Dispatchers.IO) {
        runCatching {
            val requestUtils = RequestsUtils(context)
            updateLoadingUtils(R.string.update_rule)
            val result = requestUtils.get(rule, cacheTime = 0)
            String(result.byteArray).let {
                SettingModel.set(
                    SettingModel().apply {
                        app = "server"
                        key = "rule_js"
                        value = it
                    },
                )
            }
            updateLoadingUtils(R.string.update_category)
            // 分类更新
            val result2 = requestUtils.get(category, cacheTime = 0)
            // 规则更新
            String(result2.byteArray).let {
                SettingModel.set(
                    SettingModel().apply {
                        app = "server"
                        key = "official_cate_js"
                        value = it
                    },
                )
            }
        }.onFailure {
            if (it is AutoServiceException) {
                withContext(Dispatchers.Main) {
                    EventBus.post(AutoServiceErrorEvent(it))
                }
            }
            Logger.e("更新出错", it)
            withContext(Dispatchers.Main) {
                EventBus.post(UpdateFinishEvent())
            }
        }.onSuccess {
            SpUtils.putString("ruleVersionName", version)
            SpUtils.putInt("ruleVersion", code)
            SettingModel.set(
                SettingModel().apply {
                    app = "server"
                    key = "ruleVersion"
                    value = code.toString()
                },
            )
            withContext(Dispatchers.Main) {
                EventBus.post(UpdateFinishEvent())
            }
        }
    }

    // URL由外部构造可能出错
    private fun updateApp(url: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        }.onFailure {
            Logger.e("更新出错", it)
        }
    }
}
