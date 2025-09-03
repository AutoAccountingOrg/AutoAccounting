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
import com.google.gson.Gson
import net.ankio.auto.R
import net.ankio.auto.databinding.AdapterDataRuleBinding
import net.ankio.auto.http.api.RuleManageAPI
import net.ankio.auto.storage.Logger
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

        // 编辑规则按钮 - 跳转到规则编辑页面
        binding.editRule.setOnClickListener {
            val item = holder.item!!
            navigateToRuleEdit(it, item)
        }

        // 删除规则按钮 - 显示确认对话框
        binding.deleteData.setOnClickListener {
            val item = holder.item!!
            showDeleteConfirmDialog(it, item)
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
        view.findNavController().navigate(R.id.ruleEditFragment, bundle)
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

        // 根据规则类型设置图标
        binding.icon.visibility = View.VISIBLE
        if (isSystemRule) {
            // 系统规则使用云端图标
            binding.icon.setImageResource(R.drawable.ic_cloud)
            binding.icon.contentDescription = "云端规则"
        } else {
            // 用户规则使用本地图标（如果没有合适的图标，先用一个通用图标）
            binding.icon.setImageResource(R.drawable.setting2_icon_from_local)
            binding.icon.contentDescription = "本地规则"
        }

        // 操作按钮可见性：系统规则不允许编辑和删除
        binding.editRule.visibility = if (isSystemRule) View.GONE else View.VISIBLE
        binding.deleteData.visibility = if (isSystemRule) View.GONE else View.VISIBLE

        // 临时移除监听器，设置状态后再恢复
        // 这样避免在数据绑定时触发监听器回调
        binding.enable.setOnCheckedChangeListener(null)
        binding.autoRecord.setOnCheckedChangeListener(null)

        // 设置开关状态（此时不会触发监听器）
        binding.enable.isChecked = data.enabled
        binding.autoRecord.isChecked = data.autoRecord

        // 恢复监听器 - 重新设置原来的监听器逻辑
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
    }

    override fun areItemsSame(oldItem: RuleModel, newItem: RuleModel): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsSame(oldItem: RuleModel, newItem: RuleModel): Boolean {
        return oldItem == newItem
    }


}