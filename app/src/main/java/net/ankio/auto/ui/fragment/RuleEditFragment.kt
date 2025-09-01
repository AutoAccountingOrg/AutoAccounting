/*
 * Copyright (C) 2025 ankio(ankio@ankio.net)
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

package net.ankio.auto.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentRuleEditBinding
import net.ankio.auto.http.api.RuleManageAPI
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.utils.ToastUtils
import org.ezbook.server.db.model.AppDataModel
import org.ezbook.server.db.model.RuleModel
import org.ezbook.server.tools.MD5HashTable

/**
 * 规则编辑页面（编辑 RuleModel 基础信息，不包含 JS 内容）
 *
 * 设计原则：
 * - 数据单向流动，避免复杂的状态同步
 * - 使用 Bundle 传递页面间数据，不依赖全局缓存
 * - 新建规则的默认值在模型层处理
 */
class RuleEditFragment : BaseFragment<FragmentRuleEditBinding>() {

    /** 当前编辑的规则模型 */
    private var currentRule: RuleModel = RuleModel()

    /** 关联的应用数据（用于新建规则时的上下文） */
    private var appData: AppDataModel? = null

    /** 保存临时状态的键名 */
    private val stateKey = "rule_edit_state_json"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()

        // 优先从状态恢复
        val restored = restoreState(savedInstanceState)
        if (!restored) {
            val json = arguments?.getString("rule")
            if (!json.isNullOrEmpty()) {
                runCatching { Gson().fromJson(json, RuleModel::class.java) }
                    .onSuccess { currentRule = it }
                    .onFailure { Logger.e("Failed to parse rule json", it) }
            }
        }

        // 初始化新建规则的默认值
        initializeNewRuleDefaults()

        // 判断是否传入AppData
        val data = arguments?.getString("data")
        if (!data.isNullOrEmpty()) {
            runCatching { Gson().fromJson(data, AppDataModel::class.java) }
                .onSuccess {
                    appData = it
                    currentRule.app = it.app
                    currentRule.type = it.type.toString()
                    currentRule.name = "用户创建_" + MD5HashTable.md5(it.data)
                }
                .onFailure { Logger.e("Failed to parse appData json", it) }
        }
        bindRuleToViews()
        setupEvents()
        handleJsEditResult()
    }

    /** 顶部工具栏 */
    private fun setupToolbar() = with(binding) {
        toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
    }


    /** 绑定数据到视图 */
    private fun bindRuleToViews() = with(binding) {
        nameEdit.setText(currentRule.name)
        appEdit.setText(currentRule.app)
        typeEdit.setText(currentRule.type)
        systemRuleNameEdit.setText(currentRule.systemRuleName)
        enabledSwitch.isChecked = currentRule.enabled
        autoRecordSwitch.isChecked = currentRule.autoRecord

        // 显示JS内容预览（只读）
        updateJsPreview()
    }

    /** 更新JS内容预览 */
    private fun updateJsPreview() = with(binding) {
        val jsContent = currentRule.js
        jsEditInput.setText(
            when {
                jsContent.isNullOrBlank() -> getString(R.string.rule_tap_to_edit)
                jsContent.length <= 100 -> jsContent // 短内容直接显示
                else -> {
                    // 长内容显示前100个字符并添加省略号
                    val preview = jsContent.take(100).lines().take(3).joinToString("\n")
                    "$preview\n..."
                }
            }
        )
    }

    /** 绑定事件 */
    private fun setupEvents() = with(binding) {
        // 编辑JS规则
        jsEditInput.setOnClickListener {
            //  collectViewsToRule()
            navigateToJsEditor()
        }

        // 保存按钮事件
        saveButton.setOnClickListener {
            saveRule()
        }
    }

    /** 收集视图数据回写到模型 */
    private fun collectViewsToRule() = with(binding) {
        currentRule.name = nameEdit.text?.toString()?.trim() ?: ""
        currentRule.app = appEdit.text?.toString()?.trim() ?: ""
        currentRule.type = typeEdit.text?.toString()?.trim() ?: ""
        currentRule.systemRuleName = systemRuleNameEdit.text?.toString()?.trim() ?: ""
        currentRule.enabled = enabledSwitch.isChecked
        currentRule.autoRecord = autoRecordSwitch.isChecked
        // JS内容由JS编辑页面直接修改currentRule，不需要从缓存读取
    }

    /** 初始化新建规则的默认值 */
    private fun initializeNewRuleDefaults() {
        if (currentRule.id == 0) {
            currentRule.systemRuleName = "rule_${System.currentTimeMillis()}"
            currentRule.creator = "user"
        }
    }

    /** 导航到JS编辑器 */
    private fun navigateToJsEditor() {
        val args = bundleOf(
            "js" to currentRule.js,
            "struct" to currentRule.struct,
            "name" to currentRule.systemRuleName,
            "data" to appData?.let { Gson().toJson(it) }
        )
        findNavController().navigate(R.id.ruleEditJsFragment, args)
    }

    /** 保存规则（新增或更新） */
    private fun saveRule() {
        collectViewsToRule()

        // 基本验证
        if (currentRule.name.isBlank()) {
            ToastUtils.error(getString(R.string.rule_name_required))
            return
        }
        if (currentRule.systemRuleName.isBlank()) {
            ToastUtils.error(getString(R.string.rule_system_name_required))
            return
        }

        launch {
            if (currentRule.id > 0) {
                RuleManageAPI.update(currentRule)
            } else {
                RuleManageAPI.add(currentRule)
            }
            ToastUtils.info(R.string.btn_save)
            findNavController().popBackStack()
        }
    }

    /** 恢复本地状态 */
    private fun restoreState(savedInstanceState: Bundle?): Boolean {
        val json = savedInstanceState?.getString(stateKey) ?: return false
        return runCatching {
            currentRule = Gson().fromJson(json, RuleModel::class.java)
            true
        }.getOrDefault(false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        collectViewsToRule()
        outState.putString(stateKey, Gson().toJson(currentRule))
    }

    /**
     * 处理从JS编辑页面返回的结果
     * 当JS编辑页面完成编辑后，会通过Fragment Result API返回结果
     */
    private fun handleJsEditResult() {
        // 监听来自JS编辑页面的结果
        parentFragmentManager.setFragmentResultListener("js_edit_result", this) { _, bundle ->
            val updatedJs = bundle.getString("js")
            val updatedStruct = bundle.getString("struct")

            if (updatedJs != null) {
                currentRule.js = updatedJs
            }
            if (updatedStruct != null) {
                currentRule.struct = updatedStruct
            }

            // 更新UI显示
            bindRuleToViews()
        }
    }

}