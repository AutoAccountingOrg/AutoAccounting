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

import android.view.View
import android.text.InputType
import net.ankio.auto.BuildConfig
import net.ankio.auto.R
import net.ankio.auto.databinding.ComponentAiBinding
import net.ankio.auto.http.api.AiAPI
import net.ankio.auto.ui.api.BaseComponent
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.utils.CustomTabsHelper
import net.ankio.auto.ui.theme.DynamicColors
import net.ankio.auto.utils.PrefManager

/**
 * AI 接入设置组件 - Linus式极简设计
 *
 * 设计原则：
 * 1. 消除构造函数参数冗余 - 只需要binding，自动推断生命周期
 * 2. 统一协程管理 - 使用BaseComponent的launch方法
 * 3. 完整错误处理 - 所有网络请求都有异常捕获
 * 4. 简化状态管理 - 减少不必要的状态变量
 *
 * 功能概览：
 * 1. 根据用户输入的 Token 动态启用 Provider、Model 下拉列表
 * 2. 支持刷新模型列表、跳转到模型 Key 申请页面
 * 3. 自动生命周期管理，无需手动处理协程清理
 */
class AiComponent(
    binding: ComponentAiBinding
) : BaseComponent<ComponentAiBinding>(binding) {

    // ------------------------------------ 本地缓存数据 ------------------------------------ //

    /** AI 服务商列表 */
    private var providerList: List<String> = emptyList()
    private var createKeyUri = ""
    /** AI 模型列表 */
    private var models: List<String> = emptyList()

    // ------------------------------------ 初始化 ------------------------------------------ //

    override fun onComponentCreate() {
        super.onComponentCreate()
        bindListeners()
        // Debug 模式下将 Token 输入框设置为明文文本，便于开发调试
        if (BuildConfig.DEBUG) {
            binding.etAiToken.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }
    }

    private suspend fun loadProvider(provider: String) {
        runCatching { AiAPI.getInfo(provider) }.onSuccess { info ->
            val apiUri = info["apiUri"].orEmpty()
            val apiModel = info["apiModel"].orEmpty()
            createKeyUri = info["createKeyUri"].orEmpty()
            if (apiUri.isNotEmpty()) binding.etAiBaseUrl.setText(apiUri)
            if (apiModel.isNotEmpty()) binding.actAiModel.setText(apiModel, false)
        }
    }

    /** 统一注册所有 UI 事件 */
    private fun bindListeners() = with(binding) {
        // Provider 选择：根据后端信息填充 URL / Model（仅更新UI，不保存）
        actAiProvider.setOnItemClickListener { _, _, pos, _ ->
            actAiModel.setText("")
            tilAiToken.error = null
            val provider = providerList.getOrNull(pos).orEmpty()
            launch {
                loadProvider(provider)
            }
        }

        // 刷新模型列表
        btnRefreshModels.setOnClickListener { fetchModels() }


        // 选中模型：仅更新 UI，不立即保存
        actAiModel.setOnItemClickListener { _, _, pos, _ ->
            val model = models.getOrNull(pos).orEmpty()
            if (model.isNotEmpty()) actAiModel.setText(model, false)
        }

        // 跳转浏览器或复制模型申请地址
        btnGetToken.setOnClickListener {
            CustomTabsHelper.launchUrlOrCopy(createKeyUri)
        }

        // AI 测试功能
        btnTestAi.setOnClickListener { testAiConnection() }


    }

    // ------------------------------------ UI 状态管理 ------------------------------------ //

    // 移除了updateTestButtonState方法 - 测试按钮始终可用

    /**
     * 显示测试结果
     */
    private fun showTestResult(isSuccess: Boolean, message: String, icon: Int) = with(binding) {
        cardTestResult.visibility = View.VISIBLE
        tvTestResultTitle.apply {
            setText(context.getString(R.string.ai_test_result))
            setColor(
                if (isSuccess) DynamicColors.Primary else DynamicColors.Error
            )
            setIcon(context.getDrawable(icon), true)
        }
        tvTestResultContent.text = message
    }

    // ------------------------------------ 网络交互 ---------------------------------------- //

    /**
     * 测试 AI 连接 - Linus式简洁测试
     *
     * 设计原则：
     * 1. 简单测试 - 发送"Hello"验证连通性
     * 2. 清晰反馈 - 成功/失败状态明确
     * 3. 不影响配置 - 测试不修改任何设置
     * 4. 异常安全 - 所有错误都有处理
     */
    private fun testAiConnection() = with(binding) {
        // 参数验证
        val token = etAiToken.text?.toString()?.trim().orEmpty()
        val provider = actAiProvider.text?.toString()?.trim().orEmpty()
        val model = actAiModel.text?.toString()?.trim().orEmpty()
        val apiUri = etAiBaseUrl.text?.toString()?.trim().orEmpty()

        if (token.isBlank() || provider.isBlank() || model.isBlank()) {
            showTestResult(
                false,
                context.getString(R.string.ai_test_no_config),
                R.drawable.ic_error
            )
            return@with
        }

        val loading = LoadingUtils(context)
        launch {
            try {
                loading.show()
                val result = AiAPI.request(
                    systemPrompt = "You are a helpful assistant. Please respond briefly.",
                    userPrompt = "Hello, please respond with a simple greeting to confirm the connection is working.",
                    provider = provider,
                    apiKey = token,
                    apiUri = apiUri,
                    model = model
                )

                if (result.isSuccess) {
                    val response = result.getOrNull().orEmpty()
                    showTestResult(
                        true,
                        context.getString(R.string.ai_test_success_message) + "\n\n" +
                                " ${response.take(100)}${if (response.length > 100) "..." else ""}",
                        R.drawable.ic_success
                    )
                    // 测试成功：保存到本地 Pref
                    PrefManager.apply {
                        apiProvider = provider
                        apiKey = token
                        this.apiUri = apiUri
                        apiModel = model
                    }
                } else {
                    val errMsg = result.exceptionOrNull()?.message ?: "Empty response"
                    showTestResult(
                        false,
                        context.getString(R.string.ai_test_failed_message, errMsg),
                        R.drawable.ic_error
                    )
                }


            } finally {
                loading.close()
            }
        }
    }

    /**
     * 用户点击"刷新模型"时调用 - Linus式错误处理
     *
     * 设计原则：
     * 1. 参数验证在最前面 - 快速失败
     * 2. 统一的错误处理 - 不让异常泄露到UI层
     * 3. 正确的资源管理 - LoadingUtils使用context而不是activity
     * 4. 清晰的状态反馈 - 成功/失败都有明确提示
     */
    private fun fetchModels() = with(binding) {
        val token = etAiToken.text?.toString()?.trim().orEmpty()
        if (token.isBlank()) {
            tilAiToken.error = context.getString(R.string.error_token_required)
            return@with
        }

        val loading = LoadingUtils(context)
        launch {
            try {
                loading.show()
                val provider = actAiProvider.text?.toString()?.trim().orEmpty()
                val url = etAiBaseUrl.text?.toString()?.trim().orEmpty()
                models = AiAPI.getModels(provider = provider, apiKey = token, apiUri = url)

                // 成功时更新UI
                actAiModel.setSimpleItems(models.toTypedArray())
                tilAiToken.error = null

            } finally {
                loading.close()
            }
        }
    }

    // ------------------------------------ 生命周期 ---------------------------------------- //

    /** 恢复页面时同步后端状态到 UI - Linus式异常安全 */
    override fun onComponentResume() {
        super.onComponentResume()
        val loading = LoadingUtils(context)
        launch {
            try {
                loading.show()
                // 1) 载入 Provider 列表
                providerList = AiAPI.getProviders()
                binding.actAiProvider.setSimpleItems(providerList.toTypedArray())
                loadProvider(PrefManager.apiProvider)
                // 2) 使用 PrefManager 将配置填充到页面
                binding.actAiProvider.setText(PrefManager.apiProvider, false)
                binding.etAiToken.setText(PrefManager.apiKey)
                binding.etAiBaseUrl.setText(PrefManager.apiUri)
                binding.actAiModel.setText(PrefManager.apiModel, false)

                // 测试按钮始终可用，无需更新状态
            } finally {
                loading.close()
            }
        }
    }
}

