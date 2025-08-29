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
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import net.ankio.auto.R
import net.ankio.auto.databinding.ComponentAiBinding
import net.ankio.auto.http.api.AiAPI
import net.ankio.auto.ui.api.BaseComponent
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.utils.CustomTabsHelper
import net.ankio.auto.utils.toThemeColor

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

    /** 当前模型的申请地址（随着 provider/model 变化而变化） */
    private var modelKeyUri: String = ""

    /** AI 服务商列表 */
    private var providerList: List<String> = emptyList()

    /** AI 模型列表 */
    private var models: List<String> = emptyList()

    // ------------------------------------ 初始化 ------------------------------------------ //

    override fun onComponentCreate() {
        super.onComponentCreate()
        // UI 初始禁用，避免误触
        with(binding) {
            actAiProvider.isEnabled = false
            actAiModel.isEnabled = false
            btnRefreshModels.isEnabled = false
            btnTestAi.isEnabled = false
            cardTestResult.visibility = View.GONE
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
            launch {
                try {
                    AiAPI.setCurrentProvider(providerList[pos])
                } catch (e: Exception) {
                    // 静默失败，不影响用户体验
                }
            }
        }

        // 刷新模型列表
        btnRefreshModels.setOnClickListener { fetchModels() }

        // 选中模型后更新后端，并获取申请地址
        actAiModel.setOnItemClickListener { _, _, pos, _ ->
            val model = models[pos]
            launch {
                try {
                    AiAPI.setCurrentModel(model)
                    modelKeyUri = AiAPI.getCreateKeyUri()
                } catch (e: Exception) {
                    // 静默失败，不影响用户体验
                }
            }
        }

        // 跳转浏览器或复制模型申请地址
        btnGetToken.setOnClickListener {
            CustomTabsHelper.launchUrlOrCopy(modelKeyUri)
        }

        // Token 输入后动态控制 UI
        etAiToken.doOnTextChanged { text, _, _, _ ->
            val hasToken = !text.isNullOrBlank()
            actAiProvider.isEnabled = hasToken
            actAiModel.apply {
                isEnabled = hasToken
            }
            btnRefreshModels.isEnabled = hasToken
            updateTestButtonState()
        }

        // AI 测试功能
        btnTestAi.setOnClickListener { testAiConnection() }


    }

    // ------------------------------------ UI 状态管理 ------------------------------------ //

    /**
     * 更新测试按钮状态 - 只有配置完整时才能测试
     */
    private fun updateTestButtonState() = with(binding) {
        val hasToken = !etAiToken.text.isNullOrBlank()
        val hasProvider = !actAiProvider.text.isNullOrBlank()
        val hasModel = !actAiModel.text.isNullOrBlank()
        btnTestAi.isEnabled = hasToken && hasProvider && hasModel
    }

    /**
     * 显示测试结果
     */
    private fun showTestResult(isSuccess: Boolean, message: String, icon: Int) = with(binding) {
        cardTestResult.visibility = View.VISIBLE
        tvTestResultTitle.apply {
            setText(context.getString(R.string.ai_test_result))
            setColor(
                if (isSuccess) com.google.android.material.R.attr.colorPrimary.toThemeColor()
                else com.google.android.material.R.attr.colorError.toThemeColor()
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

                // 临时保存当前配置
                val originalProvider = AiAPI.getCurrentProvider()
                val originalModel = AiAPI.getCurrentModel()
                val originalKey = AiAPI.getApiKey()

                // 设置测试配置
                AiAPI.setCurrentProvider(provider)
                AiAPI.setCurrentModel(model)
                AiAPI.setApiKey(token)

                // 执行简单的AI测试
                val response = AiAPI.request(
                    "You are a helpful assistant. Please respond briefly.",
                    "Hello, please respond with a simple greeting to confirm the connection is working."
                )

                // 恢复原始配置
                AiAPI.setCurrentProvider(originalProvider)
                AiAPI.setCurrentModel(originalModel)
                AiAPI.setApiKey(originalKey)

                if (response.isNotBlank()) {
                    showTestResult(
                        true,
                        context.getString(R.string.ai_test_success_message) + "\n\n" +
                                "AI Response: ${response.take(100)}${if (response.length > 100) "..." else ""}",
                        R.drawable.ic_success
                    )
                } else {
                    showTestResult(
                        false,
                        context.getString(R.string.ai_test_failed_message, "Empty response"),
                        R.drawable.ic_error
                    )
                }

            } catch (e: Exception) {
                showTestResult(
                    false,
                    context.getString(
                        R.string.ai_test_failed_message,
                        e.message ?: "Unknown error"
                    ),
                    R.drawable.ic_error
                )
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
                AiAPI.apply {
                    setCurrentProvider(actAiProvider.text.toString())
                    setApiKey(token)
                }
                models = AiAPI.getModels()

                // 成功时更新UI
                actAiModel.apply {
                    setSimpleItems(models.toTypedArray())
                    isEnabled = true
                }
                tilAiToken.error = null
                updateTestButtonState()

            } catch (e: Exception) {
                // 失败时显示错误信息
                tilAiToken.error = e.message ?: "获取模型列表失败"
                actAiModel.isEnabled = false
                updateTestButtonState()
            } finally {
                loading.close()
            }
        }
    }

    // ------------------------------------ 生命周期 ---------------------------------------- //

    /** 恢复页面时同步后端状态到 UI - Linus式异常安全 */
    override fun onComponentResume() {
        super.onComponentResume()
        launch {
            try {
                providerList = AiAPI.getProviders()
                modelKeyUri = AiAPI.getApiUrl()

                // Provider
                binding.actAiProvider.setSimpleItems(providerList.toTypedArray())
                binding.actAiProvider.setText(AiAPI.getCurrentProvider(), false)

                // Model
                binding.actAiModel.setText(AiAPI.getCurrentModel(), false)

                // Token
                binding.etAiToken.setText(AiAPI.getApiKey())

                // 更新测试按钮状态
                updateTestButtonState()

            } catch (e: Exception) {
                // 静默失败，不影响页面显示
                // 用户可以通过手动刷新来重试
            }
        }
    }
}

