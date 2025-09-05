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

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import net.ankio.auto.R
import net.ankio.auto.ai.JsTool
import net.ankio.auto.databinding.FragmentRuleJsEditBinding
import net.ankio.auto.http.api.JsAPI
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.dialog.BottomSheetDialogBuilder
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.ui.utils.ToastUtils
import org.eclipse.tm4e.core.registry.IThemeSource
import rikka.core.util.ResourceUtils
import android.view.WindowManager
import com.google.android.material.chip.Chip
import io.github.rosemoe.sora.widget.subscribeEvent

/**
 * JS 规则编辑页面 - 简洁版本
 *
 * 好品味原则：
 * - 单一职责：只负责JS代码编辑
 * - 简单数据：只存储必要的JS内容
 * - 直接交互：不需要复杂的回调机制
 */
class RuleEditJsFragment : BaseFragment<FragmentRuleJsEditBinding>() {

    /** 唯一的数据 - JS代码内容 */
    private var jsContent = ""

    /** 测试数据 - 简单字符串 */
    private var testData = ""

    private var name = ""

    /**
     * Symbols to be displayed in symbol input view
     */
    val SYMBOLS = arrayOf(
        "->", "{", "}", "(", ")",
        ",", ".", ";", "\"", "?",
        "+", "-", "*", "/", "<",
        ">", "[", "]", ":"
    )

    /**
     * Texts to be committed to editor for symbols above
     */
    val SYMBOL_INSERT_TEXT = arrayOf(
        "\t", "{}", "}", "(", ")",
        ",", ".", ";", "\"", "?",
        "+", "-", "*", "/", "<",
        ">", "[", "]", ":"
    )
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initData()
        setupCodeEditor()
        setupToolbar()
    }

    /** 初始化数据 - 简单直接 */
    private fun initData() {
        val args = arguments ?: return
        jsContent = args.getString("js") ?: ""
        testData = args.getString("struct") ?: ""
        name = args.getString("name") ?: ""

        // 如果没有JS内容，生成简单模板
        if (jsContent.isBlank()) {
            jsContent = generateSimpleTemplate()
        }
    }

    /** 生成简单模板 - 不要复杂的数据注入 */
    private fun generateSimpleTemplate(): String {
        return """
            /**
            $testData
            */
            let $name = {
    get(data){
        // 请在这里编写您的解析逻辑
        return {
            type: "Expend", // Income, Transfer, Expend
            money: 0.01,
            shopName: "",
            shopItem: "",
            accountNameFrom: "",
            accountNameTo: "",
            fee: 0.0,
            currency: "CNY",
            time: 0,
            channel: ""
        };
    }
}"""
    }

    /** 设置工具栏 - 只保留必要功能 */
    private fun setupToolbar() {
        binding.toolbar.apply {
            title = getString(R.string.title_rule_edit)
            setNavigationOnClickListener { save() }
            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_run -> {
                        runJs(); true
                    }

                    R.id.action_save -> {
                        save(); true
                    }

                    R.id.action_ai_assist -> {
                        ai(); true
                    }
                    else -> false
                }
            }
        }
    }

    /** AI优化JavaScript代码 - 简洁直接 */
    private fun ai() {
        val currentJs = binding.codeEditor.text.toString()
        if (currentJs.isBlank()) {
            ToastUtils.error(getString(R.string.ai_assist_empty_code))
            return
        }

        // 启动协程进行AI优化
        launch {
            val loading = LoadingUtils(requireActivity())
            loading.show(getString(R.string.ai_assist_optimizing))

            try {
                // 调用JsTool优化代码
                val optimizedJs = JsTool.optimizeJsCode(currentJs)
                loading.close()

                if (optimizedJs.isNullOrBlank()) {
                    ToastUtils.error(getString(R.string.ai_assist_no_result))
                    return@launch
                }

                // 直接设置到编辑器
                jsContent = optimizedJs
                binding.codeEditor.setText(jsContent)
                
            } catch (e: Exception) {
                loading.close()
                ToastUtils.error(getString(R.string.ai_assist_failed, e.message ?: "未知错误"))
            }
        }
    }

    /** 设置代码编辑器 - 正确的初始化顺序 */
    private fun setupCodeEditor() {
        // 1. 先初始化 TextMate 支持
        setupTextmate()

        // 2. 配置编辑器基本属性
        binding.codeEditor.apply {
            isLineNumberEnabled = true
            isWordwrap = false
        }


        // Configure symbol input view
        val inputView = binding.symbolInput
        inputView.bindEditor(binding.codeEditor)
        inputView.addSymbols(SYMBOLS, SYMBOL_INSERT_TEXT)

        // 3. 设置主题（根据PrefManager自适应）
        setupAdaptiveTheme()

        // 4. 设置语言支持
        val language = TextMateLanguage.create("source.js", true)
        binding.codeEditor.setEditorLanguage(language)

        // 5. 设置自定义补全
        //setupCustomAutoCompletion()

        // 6. 最后设置内容
        binding.codeEditor.setText(jsContent)
    }

    private fun ensureTextmateTheme() {
        val editor = binding.codeEditor
        var editorColorScheme = editor.colorScheme
        if (editorColorScheme !is TextMateColorScheme) {
            editorColorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
            editor.colorScheme = editorColorScheme
        }
    }


    private fun setupTextmate() {
        // Add assets file provider so that files in assets can be loaded
        FileProviderRegistry.getInstance().addFileProvider(
            AssetsFileResolver(
                requireContext().assets // 使用应用上下文
            )
        )
        loadDefaultTextMateThemes()
        loadDefaultTextMateLanguages()
    }
    /**
     * Load default textmate themes
     */
    private /*suspend*/ fun loadDefaultTextMateThemes() /*= withContext(Dispatchers.IO)*/ {
        val themes = arrayOf("darcula", "abyss", "quietlight", "solarized_dark")
        val themeRegistry = ThemeRegistry.getInstance()
        themes.forEach { name ->
            val path = "textmate/$name.json"
            themeRegistry.loadTheme(
                ThemeModel(
                    IThemeSource.fromInputStream(
                        FileProviderRegistry.getInstance().tryGetInputStream(path), path, null
                    ), name
                ).apply {
                    if (name != "quietlight") {
                        isDark = true
                    }
                }
            )
        }

        themeRegistry.setTheme("quietlight")
    }

    private /*suspend*/ fun loadDefaultTextMateLanguages() /*= withContext(Dispatchers.Main)*/ {
        GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
    }


    /** 运行JS代码 - 简单直接 */
    private fun runJs() {
        if (jsContent.isBlank()) {
            ToastUtils.error("JS代码为空")
            return
        }
        if (testData.isBlank()) {
            ToastUtils.error("测试数据为空")
            return
        }

        launch {
            val loading = LoadingUtils(requireActivity())
            loading.show("执行中...")
            val result = JsAPI.run(buildExecutableJs())
            loading.close()

            if (result.isNotBlank()) {
                BaseSheetDialog.create<BottomSheetDialogBuilder>(requireContext())
                    .setTitle("执行结果")
                    .setMessage(result)
                    .setPositiveButton("确定") { _, _ -> }
                    .show()
            }
        }
    }

    /** 保存JS代码 */
    private fun save() {
        jsContent = binding.codeEditor.text.toString()
        val result = bundleOf("js" to jsContent, "struct" to testData)
        parentFragmentManager.setFragmentResult("js_edit_result", result)
        ToastUtils.info("已保存")
        findNavController().popBackStack()
    }

    /** 构建可执行的JS代码 */
    private fun buildExecutableJs(): String {
        val escapedData = Gson().toJson(testData)
        val ruleName = name
        
        return buildString {
            appendLine(jsContent)
            appendLine("let window = {};")
            appendLine("window.data = $escapedData;")
            appendLine("window.rules = [{name: \"$ruleName\", obj: $ruleName}];")
            appendLine(
                """
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
            )
        }
    }


    /** 设置自适应主题 - 根据PrefManager.darkTheme自动切换 */
    private fun setupAdaptiveTheme() {
        val isDarkMode = ResourceUtils.isNightMode(requireContext().resources.configuration)
        val themeName = if (isDarkMode) "darcula" else "quietlight"

        // 设置TextMate主题
        ensureTextmateTheme()
        ThemeRegistry.getInstance().setTheme(themeName)

        // 强制刷新编辑器主题
        binding.codeEditor.colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
    }

    /** 清理资源 - 现在不需要特殊处理 */
    override fun onDestroyView() {
        binding.codeEditor.release()
        super.onDestroyView()

    }
}