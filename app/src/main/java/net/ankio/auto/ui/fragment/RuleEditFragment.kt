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
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
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
 * 规则编辑页面 - 简洁版
 *
 * 好品味原则：消除所有不必要的复杂性
 */
class RuleEditFragment : BaseFragment<FragmentRuleEditBinding>() {

    /** 当前编辑的规则 - 唯一数据源 */
    private var rule = RuleModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initRule()
        bindViews()
        setupEvents()
        setupResultListener()
    }


    /** 初始化规则 - 统一处理所有情况 */
    private fun initRule() {
        // 恢复状态 > 编辑规则 > 新建规则 > 退出
        rule = when {

            arguments?.getString("rule") != null -> {
                Logger.d("编辑规则")
                Gson().fromJson(arguments?.getString("rule"), RuleModel::class.java)
            }

            arguments?.getString("data") != null -> {
                Logger.d("新建规则")
                val appData =
                    Gson().fromJson(arguments?.getString("data"), AppDataModel::class.java)
                RuleModel().apply {
                    app = appData.app
                    type = appData.type.toString()
                    name = "用户创建_" + MD5HashTable.md5(appData.data)

                    creator = "user"
                    struct = appData.data
                }
            }

            else -> {
                findNavController().popBackStack(); return
            }
        }
        Logger.d("初始化规则：$rule")
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
    }

    private fun bindViews() = with(binding) {
        nameEdit.setText(rule.name)
        appEdit.setText(rule.app)
        typeEdit.setText(rule.type)
        updateJsDisplay()
    }

    private fun setupEvents() = with(binding) {
        jsEditInput.setOnClickListener { editJs() }
        saveButton.setOnClickListener { save() }
    }

    private fun updateJsDisplay() {
        binding.jsEditInput.setText(
            rule.js.ifBlank { getString(R.string.rule_tap_to_edit) }
        )
    }

    private fun editJs() {
        collectData()
        if (rule.systemRuleName.isEmpty()) {
            rule.systemRuleName = "rule_${System.currentTimeMillis()}"
        }
        findNavController().navigate(
            R.id.ruleEditJsFragment, bundleOf(
            "js" to rule.js,
            "struct" to rule.struct,
            "name" to rule.systemRuleName
            )
        )
    }

    private fun collectData() = with(binding) {
        rule.name = nameEdit.text.toString().trim()
        rule.app = appEdit.text.toString().trim()
        rule.type = typeEdit.text.toString().trim()
    }

    private fun save() {
        collectData()
        if (rule.name.isBlank()) {
            ToastUtils.error(getString(R.string.rule_name_required))
            return
        }

        launch {
            if (rule.id > 0) RuleManageAPI.update(rule) else RuleManageAPI.add(rule)
            ToastUtils.info(R.string.btn_save)
            findNavController().popBackStack()
        }
    }

    private fun setupResultListener() {
        parentFragmentManager.setFragmentResultListener("js_edit_result", this) { _, bundle ->
            rule.js = bundle.getString("js") ?: ""
            rule.systemRuleName = bundle.getString("name") ?: ""
            updateJsDisplay()
            Logger.d("恢复数据：${rule.systemRuleName}")
        }
    }

    override fun onDestroyView() {
        parentFragmentManager.clearFragmentResultListener("js_edit_result")
        super.onDestroyView()
    }
}