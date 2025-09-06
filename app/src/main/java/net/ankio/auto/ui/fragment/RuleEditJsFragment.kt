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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
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
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.dialog.BottomSheetDialogBuilder
import net.ankio.auto.ui.utils.DisplayUtils
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.utils.ThemeUtils
import org.eclipse.tm4e.core.registry.IThemeSource
import rikka.core.util.ResourceUtils


/**
 * JS规则编辑器 - Linus品味重构版
 *
 * 核心原则：
 * 1. 数据结构优先 - 用一个JsRule类包含所有相关数据
 * 2. 消除特殊情况 - 默认模板就是空规则的正常情况
 * 3. 简化初始化 - 编辑器一次性配置完成
 */
class RuleEditJsFragment : BaseFragment<FragmentRuleJsEditBinding>() {

    companion object {
        /** Bundle key 常量 - 技术标识符不应国际化 */
        private const val KEY_NAME = "name"
        private const val KEY_JS = "js"
        private const val KEY_STRUCT = "struct"
        private const val RESULT_KEY = "js_edit_result"

        /** TextMate 配置常量 */
        private const val TEXTMATE_LANGUAGE = "source.js"
        private const val TEXTMATE_THEME_DARK = "darcula"
        private const val TEXTMATE_THEME_LIGHT = "quietlight"
        private const val TEXTMATE_LANGUAGES_CONFIG = "textmate/languages.json"

        /** JS模板常量 */
        private const val JS_TYPE_EXPEND = "Expend"
        private const val JS_CURRENCY_CNY = "CNY"
        private const val JS_FIELD_SHOP_NAME = "shopName"
        private const val JS_FIELD_SHOP_ITEM = "shopItem"
        private const val JS_FIELD_ACCOUNT_FROM = "accountNameFrom"
        private const val JS_FIELD_ACCOUNT_TO = "accountNameTo"
        private const val JS_FIELD_CHANNEL = "channel"
    }

    /**
     * JS规则数据 - 简单明了的数据结构
     */
    data class JsRule(
        val name: String,
        val content: String,
        val testData: String
    ) {
        /** 是否为空规则 */
        fun isEmpty() = content.isBlank()

        /** 获取可执行代码 - 空规则返回默认模板 */
        fun getExecutableContent() =
            if (isEmpty()) createDefaultTemplate(name, testData) else content

        companion object {
            /** 创建默认模板 - 消除特殊情况 */
            fun createDefaultTemplate(ruleName: String, testData: String): String {
                return """
            /**
            $testData
            */
            let $ruleName = {
                get(data) {
                    // ============代码从这里开始
                    // 如果无法匹配数据，请直接返回 null
                    return {
                        type: "$JS_TYPE_EXPEND", // Income, Transfer, Expend
                        money: 0.01,
                        $JS_FIELD_SHOP_NAME: "",
                        $JS_FIELD_SHOP_ITEM: "",
                        $JS_FIELD_ACCOUNT_FROM: "",
                        $JS_FIELD_ACCOUNT_TO: "",
                        fee: 0.0,
                        currency: "$JS_CURRENCY_CNY",
                        time: 0,
                        $JS_FIELD_CHANNEL: ""
                    };
                     // ============代码从这里结束
                }
            }""".trimIndent()
            }
        }
    }

    /** 核心数据 - 简单明了的规则对象 */
    private lateinit var jsRule: JsRule

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
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom

            // 键盘弹出时调整底部边距
            view.updatePadding(
                bottom = if (imeVisible) imeHeight - DisplayUtils.getNavigationBarHeight(
                    requireContext()
                ) else 0
            )

            // 通知子类键盘状态变化
            if (imeVisible) {
                Logger.d("输入法可见，高度: $imeHeight")
            }
            insets
        }
    }

    /** 初始化数据 - 消除特殊情况处理 */
    private fun initData() {
        val args = arguments ?: return
        jsRule = JsRule(
            name = args.getString(KEY_NAME) ?: "",
            content = args.getString(KEY_JS) ?: "",
            testData = args.getString(KEY_STRUCT) ?: ""
        )
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

    /** AI优化代码 - 简化错误处理 */
    private fun ai() {
        val currentJs = binding.codeEditor.text.toString()
        if (currentJs.isBlank()) {
            ToastUtils.error(getString(R.string.ai_assist_empty_code))
            return
        }

        launch {
            val loading = LoadingUtils(requireActivity())
            loading.show(getString(R.string.ai_assist_optimizing))

            try {
                val optimizedJs = JsTool.optimizeJsCode(currentJs)
                if (optimizedJs.isNullOrBlank()) {
                    ToastUtils.error(getString(R.string.ai_assist_no_result))
                } else {
                    binding.codeEditor.setText(optimizedJs)
                }
            } catch (e: Exception) {
                ToastUtils.error(
                    getString(
                        R.string.ai_assist_failed,
                        e.message ?: getString(R.string.error_text)
                    )
                )
            } finally {
                loading.close()
            }
        }
    }

    /** 设置代码编辑器 - 一次性配置完成 */
    private fun setupCodeEditor() {
        // 初始化TextMate支持
        setupTextmate()

        // 配置编辑器 - 所有配置一次完成
        binding.codeEditor.apply {
            isLineNumberEnabled = true
            isWordwrap = false
            setEditorLanguage(TextMateLanguage.create("source.js", true))
            colorScheme = createColorScheme()
            setText(jsRule.getExecutableContent())
        }

        // 配置符号输入栏
        binding.symbolInput.apply {
            bindEditor(binding.codeEditor)
            addSymbols(SYMBOLS, SYMBOL_INSERT_TEXT)
        }
    }

    /** 创建颜色主题 - 消除if判断 */
    private fun createColorScheme(): TextMateColorScheme {
        val themeName = if (ThemeUtils.isDark) "darcula" else "quietlight"
        ThemeRegistry.getInstance().setTheme(themeName)
        return TextMateColorScheme.create(ThemeRegistry.getInstance())
    }



    private fun setupTextmate() {
        // Add assets file provider so that files in assets can be loaded
        FileProviderRegistry.getInstance().addFileProvider(
            AssetsFileResolver(
                requireContext().assets // 使用应用上下文
            )
        )
        loadDefaultTextMateThemes()
        GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
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


    /** 运行JS代码 - 简化验证逻辑 */
    private fun runJs() {
        val currentJs = binding.codeEditor.text.toString()
        if (currentJs.isBlank()) {
            ToastUtils.error(getString(R.string.js_code_empty))
            return
        }
        if (jsRule.testData.isBlank()) {
            ToastUtils.error(getString(R.string.test_data_empty))
            return
        }

        launch {
            val loading = LoadingUtils(requireActivity())
            loading.show(getString(R.string.js_executing))
            val result = JsAPI.run(buildExecutableJs(currentJs))
            if (result.isNotBlank()) {
                showResult(result)
            }
            loading.close()
        }
    }

    /** 显示执行结果 - 支持JSON美化输出 */
    private fun showResult(result: String) {
        val formattedResult = formatResult(result)
        BaseSheetDialog.create<BottomSheetDialogBuilder>(requireContext())
            .setTitle(getString(R.string.js_execution_result))
            .setMessage(formattedResult)
            .setPositiveButton(getString(R.string.ok)) { _, _ -> }
            .show()
    }

    /** 格式化结果 - 简洁的JSON检测和美化 */
    private fun formatResult(result: String): String {
        return try {
            // 尝试解析为JSON并美化 - 消除特殊情况判断
            val jsonElement = JsonParser.parseString(result.trim())
            GsonBuilder().setPrettyPrinting().create().toJson(jsonElement)
        } catch (e: Exception) {
            // 不是JSON或解析失败，返回原始内容
            result
        }
    }

    /** 保存JS代码 - 简化数据传递 */
    private fun save() {
        val currentJs = binding.codeEditor.text.toString()
        val result = bundleOf("js" to currentJs, "struct" to jsRule.testData)
        parentFragmentManager.setFragmentResult("js_edit_result", result)
        ToastUtils.info(R.string.js_saved)
        findNavController().popBackStack()
    }

    /** 构建可执行的JS代码 - 简化字符串构建 */
    private fun buildExecutableJs(jsCode: String): String {
        val escapedData = Gson().toJson(jsRule.testData)
        val ruleName = jsRule.name

        return """
            $jsCode
            let window = {};
            window.data = $escapedData;
            window.rules = [{name: "$ruleName", obj: $ruleName}];
            
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



    /** 清理资源 - 现在不需要特殊处理 */
    override fun onDestroyView() {
        binding.codeEditor.release()
        super.onDestroyView()

    }
}