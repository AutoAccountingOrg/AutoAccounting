/*
 * Copyright (C) 2025 ankio(ankio@ankio.net)
 * Licensed under the Apache License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
           http://www.apache.org/licenses/LICENSE-3.0
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
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.addCallback
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentRuleEditBinding
import net.ankio.auto.http.api.JsAPI
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.dialog.BottomSheetDialogBuilder
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.ui.utils.ToastUtils
import org.ezbook.server.db.model.AppDataModel

/**
 * JS 规则编辑页面
 *
 * 简单的 WebView 包装，加载 JS 编辑器，提供运行和保存功能。
 * 不要过度设计，不要无意义的抽象。
 */
class RuleEditJsFragment : BaseFragment<FragmentRuleEditBinding>() {

    // 核心数据，使用灵活的 HashMap
    private var jsContent: String = ""
    private var structData = mutableMapOf<String, String>()
    private var isWebViewReady = false

    // 简单的回调存储
    private val callbacks = mutableMapOf<String, (String) -> Unit>()

    // 常用字段的便捷访问
    private val testData: String get() = structData["testData"] ?: ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 直接初始化，不要愚蠢的包装函数
        initializeData()
        initializeWebView()
        initializeToolbar()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            saveJsCode()
        }

        binding.webView.visibility = View.INVISIBLE
        binding.webView.loadUrl("file:///android_asset/edit/js.html")
    }

    /**
     * 直接初始化数据，使用灵活的 HashMap
     */
    private fun initializeData() {
        val args = arguments ?: return
        val js = args.getString("js") ?: ""
        val structJson = args.getString("struct") ?: ""
        val name = args.getString("name") ?: ""
        val dataJson = args.getString("data")

        // 解析应用数据
        val appData = dataJson?.let {
            runCatching {
                com.google.gson.Gson().fromJson(it, AppDataModel::class.java)
            }.getOrNull()
        }

        // 解析 struct：支持 JSON 和字符串格式
        structData = parseStruct(structJson, appData?.data)

        // 设置 JS 内容
        jsContent = js.ifBlank { generateTemplate(name, testData) }

        Logger.d("数据初始化完成：name=$name, jsLength=${jsContent.length}, hasTestData=${testData.isNotEmpty()}")
    }

    /**
     * 解析 struct，返回灵活的 HashMap
     */
    private fun parseStruct(structJson: String, fallbackData: String?): MutableMap<String, String> {
        val result = mutableMapOf<String, String>()

        if (structJson.isBlank()) {
            if (!fallbackData.isNullOrBlank()) {
                result["testData"] = fallbackData
            }
            return result
        }

        try {
            if (structJson.startsWith("{")) {
                // JSON 格式：解析为 Map
                val gson = com.google.gson.Gson()
                val mapType =
                    object : com.google.gson.reflect.TypeToken<Map<String, String>>() {}.type
                val parsed: Map<String, String> = gson.fromJson(structJson, mapType)
                result.putAll(parsed)
            } else {
                // 旧格式：直接作为测试数据
                result["testData"] = structJson
            }
        } catch (e: Exception) {
            Logger.w("解析 struct 失败，使用兜底数据: ${e.message}")
            result["testData"] = structJson
        }

        return result
    }


    /**
     * 生成 JS 模板，合并两个愚蠢的函数
     */
    private fun generateTemplate(name: String, data: String?): String {
        if (name.isBlank()) return "// 请编写您的 JS 代码"

        val dataComment = data ?: "// 请在此处添加测试数据"
        return """/*
$dataComment
*/

let $name = {
    get(data){
        //--------请从这里开始编写代码，其他部分不要修改--------------
        // return null; 表示没有匹配到数据
        return {
            type : "Expend", //收入是 Income
            money : 0.01, // 金额
            shopName : '',  // 商户名称
            shopItem : '', //商品名称
            accountNameFrom : '', // 支出账户、收入账户
            accountNameTo : '', // 转账收入账户
            fee : 0.0, //手续费
            currency : "CNY", //币种
            time : 0, //时间戳
            channel : '' //渠道信息，例如OCR 账单页面
        };
        //------------代码结束位置-------------
    }
}"""
    }

    /**
     * 初始化工具栏，不要愚蠢的包装
     */
    private fun initializeToolbar() {
        binding.toolbar.apply {
            title = getString(R.string.title_rule_edit)
            setNavigationOnClickListener { saveJsCode() }
            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_run -> {
                        runJsCode(); true
                    }

                    R.id.action_save -> {
                        saveJsCode(); true
                    }

                    else -> false
                }
            }
        }
    }

    /**
     * 初始化 WebView，去掉无意义的包装
     */
    @SuppressLint("SetJavaScriptEnabled")
    private fun initializeWebView() {
        binding.webView.apply {
            // 基础设置，不要过度封装
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                cacheMode = WebSettings.LOAD_NO_CACHE
                setSupportZoom(true)
                builtInZoomControls = false
                displayZoomControls = false
            }

            addJavascriptInterface(JsBridge(), "Android")
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    if (!isWebViewReady) {
                        isWebViewReady = true
                        setupWebViewContent()
                    }
                }
            }
            webChromeClient = WebChromeClient()
        }
    }

    /**
     * 设置 WebView 内容，去掉无意义的延迟和包装
     */
    private fun setupWebViewContent() {
        injectMaterialColors()

        if (jsContent.isNotEmpty()) {
            setJsContent(jsContent)
        }
        binding.webView.visibility = View.VISIBLE
    }

    /**
     * 运行 JS 代码，去掉愚蠢的包装
     */
    private fun runJsCode() {
        getCurrentJsContent { js ->
            when {
                js.isBlank() -> ToastUtils.error(getString(R.string.js_content_empty))
                testData.isBlank() -> ToastUtils.error(getString(R.string.no_test_data_create_from_data_page))
                else -> executeJs(js, testData)
            }
        }
    }

    /**
     * 保存 JS 代码并完成，消除愚蠢的函数重复
     */
    private fun saveJsCode() {
        getCurrentJsContent { js ->
            sendResult(js)
            ToastUtils.info(getString(R.string.btn_save))
        }
    }

    /**
     * 发送结果并返回
     */
    private fun sendResult(jsContent: String) {
        // 序列化灵活的 HashMap
        val structJson = com.google.gson.Gson().toJson(structData)

        val result = bundleOf(
            "js" to jsContent,
            "struct" to structJson
        )

        parentFragmentManager.setFragmentResult("js_edit_result", result)
        Logger.d("返回结果：JS长度=${jsContent.length}，struct=${structJson}")
        findNavController().popBackStack()
    }

    /**
     * 获取当前 JS 内容，去掉过度复杂的线程安全伪装
     */
    private fun getCurrentJsContent(callback: (String) -> Unit) {
        if (!isWebViewReady) {
            callback("")
            return
        }

        val callbackName = "getJs_${System.currentTimeMillis()}"
        callbacks[callbackName] = callback

        val js = """
            (function() {
                try {
                    var content = (typeof getJs === 'function') ? getJs() : '';
                    Android.onJsContentReceived('$callbackName', content || '');
                } catch(e) {
                    Android.reportError('获取JS内容失败: ' + e.message);
                    Android.onJsContentReceived('$callbackName', '');
                }
            })();
        """.trimIndent()

        binding.webView.evaluateJavascript(js, null)
    }

    /**
     * 执行 JS 代码，去掉无意义的注释和包装
     */
    private fun executeJs(jsContent: String, testDataString: String) {
        if (jsContent.isBlank()) {
            ToastUtils.error(getString(R.string.js_code_empty_cannot_execute))
            return
        }

        lifecycleScope.launch {
            val loading = LoadingUtils(requireActivity())
            loading.show(getString(R.string.executing_js_code))
            val completeJs = buildExecutableJs(jsContent, testDataString)
            val result = JsAPI.run(completeJs)
            loading.close()

            if (result.isNotBlank()) {
                val displayResult = extractAndFormatData(result)
                BottomSheetDialogBuilder(requireActivity())
                    .setTitle(getString(R.string.execution_result))
                    .setMessage(displayResult)
                    .setNegativeButton(getString(R.string.confirm)) { _, _ ->
                        // 对话框会自动关闭，无需手动调用 dismiss()
                    }
                    .show()
            } else {
                ToastUtils.info(getString(R.string.execution_completed_no_result))
            }
        }
    }

    /**
     * 构造可执行的 JS 代码
     * 按照用户提供的模式：先设置 window 数据，再添加规则，最后执行测试逻辑
     */
    private fun buildExecutableJs(userJs: String, testData: String): String {
        // 转义 JSON 字符串，确保数据安全
        val escapedData = com.google.gson.Gson().toJson(testData)

        // 从用户 JS 中提取规则名称（简单匹配 let ruleName = 格式）
        val ruleName = extractRuleName(userJs) ?: "rule_1"

        return buildString {
            // 用户编写的规则在前
            appendLine(userJs)
            appendLine()
            appendLine("let window = {};")

            // 设置 window 数据
            appendLine("window.data = $escapedData;")
            appendLine()

            // 设置 window.rules 数组
            appendLine("window.rules = [{name: \"$ruleName\", obj: $ruleName}];")
            appendLine()

            // 执行测试逻辑
            appendLine(getTestExecutionScript())
        }
    }

    /**
     * 从用户 JS 代码中提取规则名称
     */
    private fun extractRuleName(jsContent: String): String? {
        val pattern = """let\s+(\w+)\s*=\s*\{""".toRegex()
        return pattern.find(jsContent)?.groupValues?.get(1)
    }

    /**
     * 获取测试执行脚本
     * 这是固定的执行逻辑，不需要动态生成
     */
    private fun getTestExecutionScript(): String {
        return """
            const data = window.data || '';
            const rules = window.rules || [];
            
            for (const rule of rules) {
              let result = null;
              try {
                result = rule.obj.get(data);
                if (
                  result !== undefined && result !== null &&
                  result.money !== null &&
                  parseFloat(result.money) > 0
                ) {
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

    /**
     * 从响应中提取并格式化 data 字段
     *
     * 期望的响应格式：{"code": 200, "data": "..."}
     * 如果 data 是 JSON 字符串，尝试格式化显示
     * 如果不是或解析失败，直接显示原始内容
     */
    private fun extractAndFormatData(response: String): String {
        return try {
            // 解析外层 JSON
            val gson = com.google.gson.Gson()
            val jsonElement = gson.fromJson(response, com.google.gson.JsonElement::class.java)

            if (jsonElement.isJsonObject) {
                val jsonObject = jsonElement.asJsonObject

                // 提取 data 字段
                val dataElement = jsonObject.get("data")
                if (dataElement != null && !dataElement.isJsonNull) {
                    val dataString = if (dataElement.isJsonPrimitive) {
                        dataElement.asString
                    } else {
                        // data 本身就是 JSON 对象，直接序列化
                        gson.toJson(dataElement)
                    }

                    // 尝试格式化 data 内容
                    formatJsonString(dataString)
                } else {
                    "data 字段为空或不存在"
                }
            } else {
                // 不是 JSON 对象，可能是纯字符串结果
                formatJsonString(response)
            }
        } catch (e: Exception) {
            // JSON 解析失败，返回原始内容
            Logger.d("响应解析失败，显示原始内容: ${e.message}")
            response
        }
    }

    /**
     * 尝试格式化 JSON 字符串
     * 如果不是有效 JSON，返回原始字符串
     */
    private fun formatJsonString(text: String): String {
        return try {
            val gson = com.google.gson.GsonBuilder()
                .setPrettyPrinting()
                .create()

            // 先解析确认是有效 JSON
            val element = gson.fromJson(text, com.google.gson.JsonElement::class.java)

            // 重新格式化输出
            gson.toJson(element)
        } catch (e: Exception) {
            // 不是有效 JSON，返回原始字符串
            text
        }
    }


    /**
     * 注入主题颜色，合并所有愚蠢的包装函数
     */
    private fun injectMaterialColors() {
        val colors = mapOf(
            "primary" to MaterialColors.getColor(
                binding.root,
                com.google.android.material.R.attr.colorPrimary
            ).toHex(),
            "onPrimary" to MaterialColors.getColor(
                binding.root,
                com.google.android.material.R.attr.colorOnPrimary
            ).toHex(),
            "primaryContainer" to MaterialColors.getColor(
                binding.root,
                com.google.android.material.R.attr.colorPrimaryContainer
            ).toHex(),
            "onPrimaryContainer" to MaterialColors.getColor(
                binding.root,
                com.google.android.material.R.attr.colorOnPrimaryContainer
            ).toHex(),
            "secondary" to MaterialColors.getColor(
                binding.root,
                com.google.android.material.R.attr.colorSecondary
            ).toHex(),
            "onSecondary" to MaterialColors.getColor(
                binding.root,
                com.google.android.material.R.attr.colorOnSecondary
            ).toHex(),
            "surface" to MaterialColors.getColor(
                binding.root,
                com.google.android.material.R.attr.colorSurface
            ).toHex(),
            "onSurface" to MaterialColors.getColor(
                binding.root,
                com.google.android.material.R.attr.colorOnSurface
            ).toHex(),
            "error" to MaterialColors.getColor(
                binding.root,
                com.google.android.material.R.attr.colorError
            ).toHex(),
            "onError" to MaterialColors.getColor(
                binding.root,
                com.google.android.material.R.attr.colorOnError
            ).toHex()
        )

        val colorsJson =
            colors.entries.joinToString(",", "{", "}") { "\"${it.key}\":\"${it.value}\"" }
        setThemeColors(colorsJson)
    }

    /** 简单的颜色转换，不要包装函数 */
    private fun Int.toHex(): String = String.format("#%06X", 0xFFFFFF and this)

    /**
     * 设置 JS 内容，去掉愚蠢的包装类
     */
    private fun setJsContent(content: String) {
        val safeContent = content
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")

        val js = """
            (function(){
                try {
                    if (typeof setJs === 'function') {
                        setJs("$safeContent");
                    }
                } catch(e) {
                    Android.reportError('设置JS内容失败: ' + e.message);
                }
            })();
        """.trimIndent()

        binding.webView.evaluateJavascript(js, null)
    }

    /**
     * 设置主题颜色，去掉愚蠢的包装类
     */
    private fun setThemeColors(colorsJson: String) {
        val js = """
            (function(){
                try {
                    var colors = JSON.parse('$colorsJson');
                    window.__MATERIAL_COLORS = colors;
                    
                    var root = document.documentElement;
                    if (root && root.style) {
                        Object.keys(colors).forEach(function(key) {
                            root.style.setProperty('--md-color-' + key, colors[key]);
                        });
                    }
                    
                    if (typeof window.applyAppTheme === 'function') {
                        window.applyAppTheme(colors);
                    }
                } catch(e) {
                    Android.reportError('设置主题颜色失败: ' + e.message);
                }
            })();
        """.trimIndent()

        binding.webView.evaluateJavascript(js, null)
    }

    /**
     * JS 桥接，只保留必要的接口
     */
    private inner class JsBridge {
        @JavascriptInterface
        fun onJsContentReceived(callbackName: String, content: String) {
            // JavaScript回调在WebView线程，切换到主线程执行UI操作
            requireActivity().runOnUiThread {
                callbacks.remove(callbackName)?.invoke(content)
            }
        }


        @JavascriptInterface
        fun log(message: String) {
            Logger.d("JS: $message")
        }

        @JavascriptInterface
        fun reportError(error: String) {
            Logger.e("JS错误: $error")
        }
    }

    /**
     * 清理资源，合并所有愚蠢的包装函数
     */
    override fun onDestroyView() {
        callbacks.clear()
        isWebViewReady = false

        binding.webView.apply {
            stopLoading()
            webViewClient = WebViewClient()
            webChromeClient = null
            removeJavascriptInterface("Android")
            loadUrl("about:blank")
            clearHistory()
            clearCache(true)
            removeAllViews()
            destroy()
        }

        super.onDestroyView()
    }

    override fun onPause() {
        super.onPause()
        if (isWebViewReady) binding.webView.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (isWebViewReady) binding.webView.onResume()
    }
}