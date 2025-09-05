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
 * 规则编辑页面 - 简洁版本
 *
 * 好品味原则：
 * - 单一数据源，消除复杂状态管理
 * - 直接的数据流动，没有特殊情况
 * - 简单的页面导航，不需要复杂回调
 */
class RuleEditFragment : BaseFragment<FragmentRuleEditBinding>() {

    /** 当前编辑的规则 - 唯一数据源 */
    private var rule = RuleModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 简单的数据初始化 - 消除特殊情况
        initRule()
        bindViews()
        setupEvents()
        setupFragmentResultListener()
    }

    /** 初始化规则数据 - 统一处理，没有特殊情况 */
    private fun initRule() {
        // 从参数获取规则数据
        val ruleJson = arguments?.getString("rule")
        val dataJson = arguments?.getString("data")

        Logger.d("ruleJson-> $ruleJson")
        Logger.d("dataJson-> $dataJson")

        if (!ruleJson.isNullOrEmpty()) {
            // 编辑现有规则
            rule = Gson().fromJson(ruleJson, RuleModel::class.java)
        } else if (!dataJson.isNullOrEmpty()) {
            // 从应用数据创建新规则
            val appData = Gson().fromJson(dataJson, AppDataModel::class.java)
            rule.app = appData.app
            rule.type = appData.type.toString()
            rule.name = "用户创建_" + MD5HashTable.md5(appData.data)
            rule.systemRuleName = "rule_${System.currentTimeMillis()}"
            rule.creator = "user"
            rule.struct = appData.data
        } else {
            findNavController().popBackStack()
        }

        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
    }

    /** 绑定数据到视图 - 简单直接 */
    private fun bindViews() = with(binding) {
        nameEdit.setText(rule.name)
        appEdit.setText(rule.app)
        typeEdit.setText(rule.type)
        // JS预览 - 消除复杂的长度判断
        jsEditInput.setText(
            rule.js.ifBlank { getString(R.string.rule_tap_to_edit) }
        )
    }

    /** 绑定事件 - 简单直接 */
    private fun setupEvents() = with(binding) {
        jsEditInput.setOnClickListener { editJs() }
        saveButton.setOnClickListener { save() }
    }

    /** 编辑JS - 直接导航，不需要复杂的数据同步 */
    private fun editJs() {
        collectData()
        val args = bundleOf(
            "js" to rule.js,
            "struct" to rule.struct,
            "name" to rule.systemRuleName
        )
        findNavController().navigate(R.id.ruleEditJsFragment, args)
    }

    /** 收集界面数据 - 简单直接 */
    private fun collectData() = with(binding) {
        rule.name = nameEdit.text.toString().trim()
        rule.app = appEdit.text.toString().trim()
        rule.type = typeEdit.text.toString().trim()
    }

    /** 保存规则 - 简单验证和保存 */
    private fun save() {
        collectData()

        if (rule.name.isBlank()) {
            ToastUtils.error(getString(R.string.rule_name_required))
            return
        }

        launch {
            if (rule.id > 0) {
                RuleManageAPI.update(rule)
            } else {
                RuleManageAPI.add(rule)
            }
            ToastUtils.info(R.string.btn_save)
            findNavController().popBackStack()
        }
    }

    /** 监听JS编辑页面返回的数据 - 修复数据丢失问题 */
    private fun setupFragmentResultListener() {
        parentFragmentManager.setFragmentResultListener("js_edit_result", this) { _, bundle ->
            // 获取JS编辑页面返回的数据
            val js = bundle.getString("js") ?: ""
            // 更新规则数据
            rule.js = js
            // 刷新界面显示
            bindViews()

            Logger.d("接收到JS编辑结果: js长度=${js.length}")
        }
    }
    

}