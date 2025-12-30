/*
 * Copyright (C) 2024 ankio(ankio@ankio.net)
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

package net.ankio.auto.ui.adapter

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.google.android.material.color.MaterialColors
import com.google.gson.Gson
import net.ankio.auto.R
import net.ankio.auto.databinding.AdapterDataRuleBinding
import net.ankio.auto.http.api.RuleManageAPI
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.api.BaseViewHolder
import net.ankio.auto.ui.dialog.BottomSheetDialogBuilder
import net.ankio.auto.ui.utils.ToastUtils
import org.ezbook.server.db.model.RuleModel

/**
 * 数据规则适配器
 * @param fragment 宿主Fragment，用于显示对话框和导航
 */
class DataRuleAdapter(
    private val fragment: Fragment
) : BaseAdapter<AdapterDataRuleBinding, RuleModel>() {
    override fun onInitViewHolder(holder: BaseViewHolder<AdapterDataRuleBinding, RuleModel>) {
        val binding = holder.binding

        // 编辑按钮
        binding.editButton.setOnClickListener {
            navigateToRuleEdit(it, holder.item!!)
        }

        // 删除按钮
        binding.deleteButton.setOnClickListener {
            showDeleteConfirmDialog(it, holder.item!!)
        }
    }

    /**
     * 导航到规则编辑页面
     * 传递规则数据给编辑页面进行修改
     */
    private fun navigateToRuleEdit(view: View, rule: RuleModel) {
        val bundle = Bundle().apply {
            putString("rule", Gson().toJson(rule))
        }
        // 使用目的地 ID 导航，避免当前目的地为 NavGraph 时解析不到 action
        view.findNavController().navigate(R.id.RuleEditV3Fragment, bundle)
    }

    /**
     * 显示删除确认对话框
     * 确认后删除规则并刷新列表
     */
    private fun showDeleteConfirmDialog(view: View, rule: RuleModel) {
        BaseSheetDialog.create<BottomSheetDialogBuilder>(fragment.requireContext())
            .setTitle(fragment.getString(R.string.delete_rule_title))
            .setMessage(fragment.getString(R.string.delete_rule_message, rule.name))
            .setNegativeButton(fragment.getString(R.string.cancel)) { _, _ -> }
            .setPositiveButton(fragment.getString(R.string.delete)) { _, _ ->
                deleteRule(rule)
            }
            .show()
    }

    /**
     * 执行规则删除操作
     */
    private fun deleteRule(rule: RuleModel) {
        launchInAdapter {
            RuleManageAPI.delete(rule.id)
            ToastUtils.info(R.string.rule_deleted_successfully)
        }
        removeItem(rule)
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder<AdapterDataRuleBinding, RuleModel>,
        data: RuleModel,
        position: Int
    ) {
        val binding = holder.binding

        // 判断规则类型：系统规则（云端）vs 用户规则（本地）
        val isSystemRule = data.creator == "system"

        // 设置规则名称
        binding.ruleName.text = data.name

        // 设置规则类型标签 - 简单的TextView，清晰地告诉用户这是什么类型的规则
        binding.ruleType.setText(if (isSystemRule) R.string.rule_type_cloud else R.string.rule_type_local)

        // 设置数据类型标签 - 区分 APP/通知/OCR
        setDataTypeTag(binding, data.type)

        // 设置规则描述 - 优先使用数据库中的description字段，否则智能生成
        binding.ruleDescription.text = generateRuleDescription(data)

        // 设置启用开关
        binding.enable.setOnCheckedChangeListener(null) // 先移除监听器避免触发
        binding.enable.isChecked = data.enabled
        binding.enable.setOnCheckedChangeListener { _, isChecked ->
            val item = holder.item!!
            item.enabled = isChecked
            launchInAdapter {
                RuleManageAPI.update(item)
                val statusText =
                    if (isChecked) R.string.rule_enabled_successfully else R.string.rule_disabled_successfully
                ToastUtils.info(statusText)
            }
        }

        // 设置自动记账Chip - 使用更清晰的文字
        binding.autoRecord.setOnCheckedChangeListener(null)
        binding.autoRecord.isChecked = data.autoRecord
        binding.autoRecord.setOnCheckedChangeListener { _, isChecked ->
            val item = holder.item!!
            item.autoRecord = isChecked
            launchInAdapter {
                RuleManageAPI.update(item)
                val statusText =
                    if (isChecked) R.string.auto_record_enabled_successfully else R.string.auto_record_disabled_successfully
                ToastUtils.info(statusText)
            }
        }

        // 设置操作按钮 - 系统规则不提供操作，用户规则可编辑/删除
        binding.actionButtons.visibility = if (isSystemRule) View.GONE else View.VISIBLE
    }

    override fun areItemsSame(oldItem: RuleModel, newItem: RuleModel): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsSame(oldItem: RuleModel, newItem: RuleModel): Boolean {
        return oldItem == newItem
    }

    /**
     * 设置数据类型标签
     * 根据规则类型设置不同的文本和颜色，提升视觉识别度
     *
     * 数据结构驱动：rule.type → 标签文本 + Material You 主题色
     * 消除特殊情况：使用统一的颜色解析方法
     *
     * @param binding ViewBinding
     * @param type 数据类型（app/notice/ocr）
     */
    private fun setDataTypeTag(binding: AdapterDataRuleBinding, type: String) {

        when (type.lowercase()) {
            "data" -> {
                binding.dataType.text = fragment.getString(R.string.data_type_app)
            }

            "notice" -> {
                binding.dataType.text = fragment.getString(R.string.data_type_notice)
            }

            "ocr" -> {
                binding.dataType.text = fragment.getString(R.string.data_type_ocr)
            }

            else -> {
                // 未知类型，使用默认样式
                binding.dataType.text = ""
            }
        }
    }

    /**
     * 生成规则描述文本
     * 根据规则类型和应用包名智能生成带有使用提示的描述
     *
     * 数据结构驱动：无论是系统规则还是用户规则，都使用相同的生成逻辑
     * 消除特殊情况：本地规则和云规则享受相同的描述生成能力
     *
     * @param rule 规则模型
     * @return 描述文本
     */
    private fun generateRuleDescription(rule: RuleModel): String {
        val ruleType = rule.type.lowercase()
        val appPackage = rule.app


        val description = when {
            // 微信相关规则 - 提示关注公众号
            appPackage.contains("com.tencent.mm") -> {
                fragment.getString(R.string.rule_desc_wechat_notice, rule.name)
            }

            // 支付宝规则 - 提示保持后台运行
            appPackage.contains("com.eg.android.AlipayGphone") -> {
                fragment.getString(R.string.rule_desc_alipay_app, rule.name)
            }

            appPackage.contains("com.android.phone") -> {
                fragment.getString(R.string.rule_desc_sms_hint, rule.name)
            }
            // 其他通知类规则 - 提示授权通知权限
            ruleType == "notice" -> {
                fragment.getString(R.string.rule_desc_other_notice, rule.name)
            }
            // 默认描述
            else -> ""
        }

        return description
    }

}