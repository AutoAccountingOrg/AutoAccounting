/*
 * Copyright (C) 2025 ankio(ankio@ankio.net)
 * Licensed under the Apache License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-3.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package net.ankio.auto.ui.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import net.ankio.auto.BuildConfig
import net.ankio.auto.R
import net.ankio.auto.ai.RegexRuleTool
import net.ankio.auto.databinding.FragmentRuleV3EditBinding
import net.ankio.auto.http.api.JsAPI
import net.ankio.auto.http.api.RuleManageAPI
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.dialog.BottomSheetDialogBuilder
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.ui.utils.ToastUtils
import org.ezbook.server.db.model.AppDataModel
import org.ezbook.server.db.model.RuleModel


/**
 * WebView规则编辑器 - Linus品味重构版
 *
 * 核心原则：
 * 1. 数据结构优先 - RuleData 包含所有规则数据
 * 2. 消除特殊情况 - WebView 和 Native 通过统一的 Bridge 通信
 * 3. 简化初始化 - WebView 一次性配置完成
 */
class RuleEditV3Fragment : BaseFragment<FragmentRuleV3EditBinding>() {

    companion object {
        /** WebView URL */
        private const val WEBVIEW_URL = "file:///android_asset/rule/index.html"
    }


    /** 核心数据 - 唯一数据源 */
    private lateinit var ruleData: RuleModel

    /** 主线程Handler - 用于JS回调 */
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initData()
        setupWebView()
        setupToolbar()
    }

    /** 初始化数据 - 消除特殊情况处理 */
    private fun initData() {
        val args = arguments ?: Bundle()

        val rule = args.getString("rule")
        val data = args.getString("data")
        if (rule !== null) {
            ruleData = Gson().fromJson(rule, RuleModel::class.java)
            return
        }
        if (data !== null) {
            val appData = Gson().fromJson(data, AppDataModel::class.java)
            ruleData = RuleModel().apply {
                this.name = "规则"
                this.app = appData.app
                this.type = appData.type.name
                this.creator = "user"
                this.systemRuleName = "rule_${System.currentTimeMillis()}"
            }

            val testData = convert(appData.data)

            Logger.d("测试数据 $testData")

            ruleData.struct = Gson().toJson(
                mapOf(
                    "name" to ruleData.name,
                    "app" to ruleData.app,
                    "rule_type" to ruleData.type,
                    "creator" to ruleData.creator,
                    "systemRuleName" to ruleData.systemRuleName,
                    "testData" to testData
                )
            )

            return
        }

        findNavController().popBackStack()
    }

    /** 设置工具栏 - 只保留必要功能 */
    private fun setupToolbar() {
        binding.toolbar.apply {
            title = getString(R.string.title_rule_edit)
            setNavigationOnClickListener {
                // 点击返回按钮时保存
                triggerSave()
            }
            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_run -> {
                        triggerTest()
                        true
                    }

                    R.id.action_save -> {
                        triggerSave()
                        true
                    }

                    R.id.action_ai_assist -> {
                        triggerAi()
                        true
                    }

                    else -> false
                }
            }
        }
    }

    /** 触发AI辅助 - 根据测试数据自动生成正则规则 */
    private fun triggerAi() {
        binding.webView.evaluateJavascript("getFormData()") { result ->
            if (result == null || result == "null") {
                ToastUtils.error(getString(R.string.error_text))
                return@evaluateJavascript
            }

            val formData =
                runCatching { Gson().fromJson(result, JsonObject::class.java) }.getOrNull()
            if (formData == null) {
                ToastUtils.error(getString(R.string.rule_parse_form_failed))
                return@evaluateJavascript
            }

            val testData = formData.get("testData")?.asString?.takeIf { it.isNotBlank() }
            if (testData == null) {
                ToastUtils.error(getString(R.string.rule_test_data_required))
                return@evaluateJavascript
            }

            val existingRegex = formData.get("regex")?.asString ?: ""

            // 在协程中调用AI生成规则
            launch {
                val loadingUtils = LoadingUtils(requireActivity())
                loadingUtils.show(getString(R.string.rule_ai_generating))

                val aiResult = RegexRuleTool.generateRegexRule(testData, existingRegex)

                loadingUtils.close()

                if (aiResult == null) {
                    ToastUtils.error(getString(R.string.rule_ai_generate_failed))
                    return@launch
                }

                // 检查是否有错误
                if (aiResult.has("error")) {
                    ToastUtils.error(
                        getString(
                            R.string.rule_ai_generate_failed_with_reason,
                            aiResult.get("error").asString
                        )
                    )
                    return@launch
                }
                aiResult.addProperty("testData", testData)
                callJsFunction("webviewCallback.setData(${aiResult})")
                // 填充AI生成的规则到表单
                ToastUtils.info(getString(R.string.rule_ai_generate_success))
            }
        }
    }

    /** 触发保存 - 直接获取数据并处理 */
    private fun triggerSave() {

        binding.webView.evaluateJavascript("getFormData()") { result ->
            if (result == null || result == "null") {
                ToastUtils.error(getString(R.string.error_text))
                return@evaluateJavascript
            }

            val formData =
                runCatching { Gson().fromJson(result, JsonObject::class.java) }.getOrNull()
            if (formData == null) {
                ToastUtils.error(getString(R.string.rule_parse_form_failed))
                return@evaluateJavascript
            }
            try {

                ruleData.struct = Gson().toJson(formData)
                ruleData.name = formData.get("name").asString

                launch {
                    if (ruleData.id > 0) {
                        RuleManageAPI.update(ruleData)
                    } else {
                        RuleManageAPI.add(ruleData)
                    }
                    ToastUtils.info(R.string.js_saved)
                    findNavController().popBackStack()
                }


            } catch (e: Exception) {
                Logger.e("保存规则失败", e)
                ToastUtils.error(getString(R.string.rule_save_failed))
            }
        }

    }

    /** 触发测试 - 直接获取数据并处理 */
    private fun triggerTest() {
        binding.webView.evaluateJavascript("getFormData()") { result ->
            if (result == null || result == "null") {
                ToastUtils.error(getString(R.string.error_text))
                return@evaluateJavascript
            }

            val formData =
                runCatching { Gson().fromJson(result, JsonObject::class.java) }.getOrNull()
            if (formData == null) {
                ToastUtils.error(getString(R.string.rule_parse_form_failed))
                return@evaluateJavascript
            }

            try {

                val systemRuleName = formData.get("systemRuleName").asString
                val testData = formData.get("testData").asString

                launch {
                    val loading = LoadingUtils(requireActivity())
                    loading.show(getString(R.string.js_executing))

                    try {
                        val js = getRuleJs(formData)
                        val executableJs = buildExecutableJs(systemRuleName, testData, js)
                        val result = JsAPI.run(executableJs)
                        if (result.isNotBlank()) {
                            showResult(result)
                        } else {
                            ToastUtils.error(getString(R.string.no_rule_hint))
                        }
                    } catch (e: Exception) {
                        Logger.e(e.message ?: "", e)
                        ToastUtils.error(e.message ?: getString(R.string.error_text))
                    } finally {
                        loading.close()
                    }
                }

            } catch (e: Exception) {
                Logger.e("测试规则失败", e)
                ToastUtils.error(getString(R.string.rule_test_failed))
            }

        }
    }


    /** 设置WebView - 一次性配置完成 */
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webView.apply {
            // 配置WebSettings
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                mediaPlaybackRequiresUserGesture = false

                // 优化性能
                cacheMode = android.webkit.WebSettings.LOAD_DEFAULT

                // 启用调试（仅开发环境）
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
                }
            }

            // 设置WebView客户端
            webViewClient = createWebViewClient()
            webChromeClient = createWebChromeClient()


            Logger.d("[WebView] 开始加载规则编辑页面")
            // 加载HTML
            loadUrl(WEBVIEW_URL)
        }
    }

    /** 创建WebViewClient - 处理页面加载和错误 */
    private fun createWebViewClient() = object : WebViewClient() {
        override fun onPageStarted(
            view: WebView?,
            url: String?,
            favicon: android.graphics.Bitmap?
        ) {
            super.onPageStarted(view, url, favicon)
            Logger.d("[WebView] 页面开始加载: $url")
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            Logger.d("[WebView] 页面加载完成: $url")
            // 页面加载完成后初始化数据
            callJsFunction("webviewCallback.setData(${ruleData.struct})")
        }

    }

    /**
     * 将 JSON 扁平化展开，使用【】表示嵌套
     *
     * 转换规则：
     * - 对象嵌套: key:【subKey:value,...】
     * - 数组: key:【item1,item2,...】
     * - 基本类型: key:value
     *
     * 示例：
     * {"user":{"name":"张三","age":20},"tags":["A","B"]}
     * → user:【name:张三,age:20】,tags:【A,B】
     *
     * [{"a":1},{"b":2}]
     * → 【a:1,b:2】
     */
    fun convert(data: String): String {
        val gson = GsonBuilder().disableHtmlEscaping().create()
        // 不假设类型，让 Gson 自动判断是对象还是数组
        val json = gson.fromJson(data, com.google.gson.JsonElement::class.java)
        return convertJsonElement(json)
    }

    /**
     * 递归转换 JsonElement
     * 使用数据结构驱动，避免字符串替换的陷阱
     */
    /**
     * 递归转换 JsonElement
     * 使用数据结构驱动，避免字符串替换的陷阱
     *
     * 增强特性：
     * - 自动检测字符串中的 JSON 并递归解析
     * - 处理"双重JSON"问题（JSON 字符串化后再嵌套）
     */
    private fun convertJsonElement(element: com.google.gson.JsonElement): String {
        return when {
            // 基本类型：字符串、数字、布尔
            element.isJsonPrimitive -> {
                val primitive = element.asJsonPrimitive
                when {
                    primitive.isString -> {
                        val str = primitive.asString
                        // 尝试解析字符串中的 JSON
                        tryParseJsonString(str) ?: str
                    }

                    else -> primitive.toString()
                }
            }

            // 对象：转换为 key:value,key:value
            element.isJsonObject -> {
                element.asJsonObject.entrySet().joinToString(",") { (key, value) ->
                    val valueStr = when {
                        value.isJsonPrimitive -> convertJsonElement(value)
                        else -> "【${convertJsonElement(value)}】"
                    }
                    "$key:$valueStr"
                }
            }

            // 数组：转换为 【item1,item2】
            element.isJsonArray -> {
                element.asJsonArray.joinToString(",") { item ->
                    convertJsonElement(item)
                }
            }

            // null
            element.isJsonNull -> "null"

            else -> element.toString()
        }.replace("\\r\\n|\\r|\\n".toRegex(), "")
    }

    /**
     * 尝试将字符串解析为 JSON
     * 如果成功，返回转换后的格式；否则返回 null
     */
    private fun tryParseJsonString(str: String): String? {
        // 快速判断：不像 JSON 就直接返回
        val trimmed = str.trim()
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return null
        }

        return try {
            val gson = GsonBuilder().disableHtmlEscaping().create()
            val parsed = gson.fromJson(trimmed, com.google.gson.JsonElement::class.java)
            // 递归转换解析出的 JSON
            "【${convertJsonElement(parsed)}】"
        } catch (e: Exception) {
            // 不是有效的 JSON，返回 null
            null
        }
    }

    /** 创建WebChromeClient - 处理标题变化和控制台日志 */
    private fun createWebChromeClient() = object : WebChromeClient() {

        override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
            // 将 JavaScript 控制台消息输出到 Android Logger
            val message =
                "[WebView-JS] [${consoleMessage.sourceId()}:${consoleMessage.lineNumber()}] ${consoleMessage.message()}"

            when (consoleMessage.messageLevel()) {
                ConsoleMessage.MessageLevel.ERROR -> Logger.e(message)
                ConsoleMessage.MessageLevel.WARNING -> Logger.w(message)
                ConsoleMessage.MessageLevel.DEBUG -> Logger.d(message)
                ConsoleMessage.MessageLevel.TIP -> Logger.i(message)
                ConsoleMessage.MessageLevel.LOG -> Logger.i(message)
                else -> Logger.d(message)
            }

            return true // 返回 true 表示已处理
        }
    }

    /** 调用JS函数 - 统一的JS调用入口 */
    private fun callJsFunction(jsCode: String) {
        mainHandler.post {
            binding.webView.loadUrl("javascript:$jsCode")
            // 截断过长的日志
            val logMessage = if (jsCode.length > 200) {
                "${jsCode.take(200)}... (${jsCode.length} chars)"
            } else {
                jsCode
            }
            Logger.d("[WebView-Call] 执行JS: $logMessage")
        }
    }


    /** 构建可执行的JS代码 */
    private fun getRuleJs(jsonObject: JsonObject): String {
        val type = jsonObject.get("type").asString
        val ruleName = jsonObject.get("systemRuleName").asString
        val regex = jsonObject.get("regex").asString.replace("\\", "\\\\")
        val money = jsonObject.get("money").asString
        val shopName = jsonObject.get("shopName").asString
        val shopItem = jsonObject.get("shopItem").asString
        val accountNameFrom = jsonObject.get("accountNameFrom").asString
        val accountNameTo = jsonObject.get("accountNameTo").asString
        val fee = jsonObject.get("fee").asString
        val currency = jsonObject.get("currency").asString
        val time = jsonObject.get("time").asString

        return """
            // 规则定义
            let $ruleName = {
                get(data) {
                    const pattern = new RegExp('$regex');
                    const matches = pattern.exec(data);
                    
                    if (matches && pattern.test(data)) {
                        // 初始化字段模板
                        let type = '$type';
                        let money = '$money';
                        let shopName = '$shopName';
                        let shopItem = '$shopItem';
                        let accountNameFrom = '$accountNameFrom';
                        let accountNameTo = '$accountNameTo';
                        let fee = '$fee';
                        let currency = '$currency';
                        let time = '$time';
                        
                        // 替换正则分组：将 $1, $2... 替换为实际匹配值
                        for (let i = 1; i < matches.length; i++) {
                            const placeholder = '$' + i;
                            const value = matches[i];
                            type = type.replaceAll(placeholder, value);
                            money = money.replaceAll(placeholder, value);
                            shopName = shopName.replaceAll(placeholder, value);
                            shopItem = shopItem.replaceAll(placeholder, value);
                            accountNameFrom = accountNameFrom.replaceAll(placeholder, value);
                            accountNameTo = accountNameTo.replaceAll(placeholder, value);
                            fee = fee.replaceAll(placeholder, value);
                            currency = currency.replaceAll(placeholder, value);
                            time = time.replaceAll(placeholder, value);
                        }
                        
                        // 返回解析结果
                        return {
                            type: type,
                            money: money,
                            shopName: shopName,
                            shopItem: shopItem,
                            accountNameFrom: accountNameFrom,
                            accountNameTo: accountNameTo,
                            fee: fee,
                            currency: currency,
                            time: time,
                            channel: ""
                        };
                    }
                    
                    return null;
                }
            };
            
        """.trimIndent()
    }

    /**
     * 构建可执行的 JS 代码
     * 将用户配置的规则转换为可运行的 JavaScript
     */
    private fun buildExecutableJs(ruleName: String, testData: String, js: String): String {

        val escapedData = Gson().toJson(testData)

        return """
            
            $js
            
            // 执行环境初始化
            let window = {};
            window.data = $escapedData;
            window.rules = [{name: "$ruleName", obj: $ruleName}];
            
            // 执行规则匹配
            const data = window.data || '';
            const rules = window.rules || [];
            
            for (const rule of rules) {
                try {
                    const result = rule.obj.get(data);
                    if (result && result.money && parseFloat(result.money) > 0) {
                        result.ruleName = rule.name;
                        print(JSON.stringify(result));
                        break;
                    }
                } catch (e) {
                    print(e.message);
                }
            }
        """.trimIndent()
    }

    /** 显示执行结果 */
    private fun showResult(result: String) {
        // 尝试格式化 JSON，失败则保持原样
        val formattedResult = try {
            val jsonElement = com.google.gson.JsonParser.parseString(result)
            GsonBuilder()
                .setPrettyPrinting()
                .create()
                .toJson(jsonElement)
        } catch (e: Exception) {
            result // 不是有效 JSON，保持原样
        }

        BaseSheetDialog.create<BottomSheetDialogBuilder>(requireContext())
            .setTitle(getString(R.string.js_execution_result))
            .setMessage(formattedResult)
            .setPositiveButton(getString(R.string.ok)) { _, _ -> }
            .show()
    }

    /** 清理资源 */
    override fun onDestroyView() {
        binding.webView.apply {
            loadUrl("about:blank")
            clearHistory()
            clearCache(true)
            destroy()
        }
        super.onDestroyView()
    }
}
