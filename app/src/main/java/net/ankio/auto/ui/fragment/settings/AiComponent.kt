/*
 * Copyright (C) 2025 ankio

 * Licensed under the Apache License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-3.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.ankio.auto.ui.fragment.settings

import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import net.ankio.auto.R
import net.ankio.auto.databinding.ComponentAiBinding
import net.ankio.auto.http.api.AiAPI
import net.ankio.auto.ui.api.BaseComponent
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.utils.CustomTabsHelper
import net.ankio.auto.utils.PrefManager

/**
 * AI 接入设置组件
 *
 * 功能概览：
 * 1. 根据用户输入的 Token 动态启用 Provider、Model 下拉列表
 * 2. 支持刷新模型列表、跳转到模型 Key 申请页面
 * 3. 提供 OCR、自动识别、分类等 AI 功能开关
 *
 * ⚠️ 所有网络请求均放在 [Lifecycle.coroutineScope] 内，避免内存泄漏
 */
class AiComponent(
    binding: ComponentAiBinding,
    private val lifecycle: Lifecycle,
) : BaseComponent<ComponentAiBinding>(binding, lifecycle) {

    // ------------------------------------ 本地缓存数据 ------------------------------------ //

    /** 当前模型的申请地址（随着 provider/model 变化而变化） */
    private var modelKeyUri: String = ""

    /** AI 服务商列表 */
    private var providerList: List<String> = emptyList()

    /** AI 模型列表 */
    private var models: List<String> = emptyList()

    // ------------------------------------ 初始化 ------------------------------------------ //

    override fun init() {
        super.init()
        // UI 初始禁用，避免误触
        with(binding) {
            actAiProvider.isEnabled = false
            actAiModel.isEnabled = false
            btnRefreshModels.isEnabled = false
        }
        bindListeners()
    }

    /** 统一注册所有 UI 事件 */
    private fun bindListeners() = with(binding) {
        // Provider 选择后重置 Model
        actAiProvider.setOnItemClickListener { _, _, pos, _ ->
            actAiModel.apply {
                setText("")
                isEnabled = false
            }
            tilAiToken.error = null
            lifecycle.coroutineScope.launch { AiAPI.setCurrentProvider(providerList[pos]) }
        }

        // 刷新模型列表
        btnRefreshModels.setOnClickListener { fetchModels() }

        // 选中模型后更新后端，并获取申请地址
        actAiModel.setOnItemClickListener { _, _, pos, _ ->
            val model = models[pos]
            lifecycle.coroutineScope.launch(Dispatchers.IO) {
                AiAPI.setCurrentModel(model)
                modelKeyUri = AiAPI.getCreateKeyUri()
            }
        }

        // 跳转浏览器或复制模型申请地址
        btnGetToken.setOnClickListener {
            CustomTabsHelper.launchUrlOrCopy(context, modelKeyUri)
        }

        // Token 输入后动态控制 UI
        etAiToken.doOnTextChanged { text, _, _, _ ->
            val hasToken = !text.isNullOrBlank()
            actAiProvider.isEnabled = hasToken
            actAiModel.apply {
                setText("")
                isEnabled = hasToken
            }
            btnRefreshModels.isEnabled = hasToken
        }

        // AI 功能开关
        chipUsageOcr.setOnCheckedChangeListener { _, checked ->
            PrefManager.aiFeatureOCR = checked
        }
        chipUsageAutoDetect.setOnCheckedChangeListener { _, checked ->
            PrefManager.aiFeatureAutoDetection = checked
        }
        chipUsageAutoCategory.setOnCheckedChangeListener { _, checked ->
            PrefManager.aiFeatureCategory = checked
        }
    }

    // ------------------------------------ 网络交互 ---------------------------------------- //

    /**
     * 用户点击“刷新模型”时调用：
     * 1. 校验 Token
     * 2. 拉取模型列表（10s 超时）
     * 3. UI 反馈（Loading + 下拉列表填充）
     */
    private fun fetchModels() = with(binding) {
        val token = etAiToken.text?.toString()?.trim().orEmpty()
        if (token.isBlank()) {
            tilAiToken.error = context.getString(R.string.error_token_required)

        } else {
            val loading = LoadingUtils(activity)
            lifecycle.coroutineScope.launch {
                loading.show()
                AiAPI.apply {
                    setCurrentProvider(actAiProvider.text.toString())
                    setApiKey(token)
                }
                models = AiAPI.getModels()
                loading.close()

                actAiModel.apply {
                    setSimpleItems(models.toTypedArray())
                    isEnabled = true
                }
            }
        }
    }

    // ------------------------------------ 生命周期 ---------------------------------------- //

    /** 恢复页面时同步后端状态到 UI */
    override fun resume() {
        super.resume()
        lifecycle.coroutineScope.launch {
            providerList = AiAPI.getProviders()
            modelKeyUri = AiAPI.getApiUrl()

            // Provider
            binding.actAiProvider.setSimpleItems(providerList.toTypedArray())
            binding.actAiProvider.setText(AiAPI.getCurrentProvider(), false)

            // Model
            binding.actAiModel.setText(AiAPI.getCurrentModel(), false)

            // Token
            binding.etAiToken.setText(AiAPI.getApiKey())

            // 功能开关
            binding.chipUsageOcr.isChecked = PrefManager.aiFeatureOCR
            binding.chipUsageAutoDetect.isChecked = PrefManager.aiFeatureAutoDetection
            binding.chipUsageAutoCategory.isChecked = PrefManager.aiFeatureCategory
        }
    }
}

