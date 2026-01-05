/*
 * Copyright (C) 2023 ankio(ankio@ankio.net)
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

package net.ankio.auto.ui.dialog.components

import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.databinding.ComponentRuleInfoBinding
import net.ankio.auto.http.api.RuleManageAPI
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseComponent
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.dialog.BottomSheetDialogBuilder
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.utils.SystemUtils.findLifecycleOwner
import org.ezbook.server.constant.BillType
import org.ezbook.server.constant.DefaultData
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.model.BillInfoModel
import org.ezbook.server.db.model.RuleModel

/**
 * 规则信息组件 - 专用于账单编辑对话框
 *
 * 职责：
 * - 显示账单匹配的规则名称
 * - 提供规则禁用功能（点击关闭按钮）
 * - 根据配置控制显示/隐藏
 *
 * 使用方式：
 * ```kotlin
 * val ruleInfo: RuleInfoComponent = binding.ruleInfo.bindAs()
 * ruleInfo.setBillInfo(billInfoModel)
 * ```
 */
class RuleInfoComponent(
    binding: ComponentRuleInfoBinding
) : BaseComponent<ComponentRuleInfoBinding>(binding) {

    private lateinit var billInfoModel: BillInfoModel

    /**
     * 设置账单信息
     */
    fun setBillInfo(billInfoModel: BillInfoModel) {
        this.billInfoModel = billInfoModel
        refresh()
    }

    /**
     * 刷新显示 - 根据当前账单信息更新UI
     */
    fun refresh() {
        if (!::billInfoModel.isInitialized) return

        binding.root.isVisible = PrefManager.showRuleName

        binding.ruleName.setText(billInfoModel.ruleName)

        if (billInfoModel.generateByAi()) {
            binding.close.visibility = View.INVISIBLE
        }

    }

    override fun onComponentCreate() {
        super.onComponentCreate()
        setupCloseButtonListener()
    }

    /**
     * 设置关闭按钮监听器
     */
    private fun setupCloseButtonListener() {
        binding.close.setOnClickListener {
            showDisableRuleConfirmation()
        }
    }

    /**
     * 显示禁用规则确认对话框
     */
    private fun showDisableRuleConfirmation() {
        BaseSheetDialog.create<BottomSheetDialogBuilder>(context)
            .setTitleInt(R.string.disable_rule_title)
            .setMessage(context.getString(R.string.disable_rule_message, billInfoModel.ruleName))
            .setPositiveButton(R.string.sure_msg) { _, _ ->
                disableRule(billInfoModel.ruleName)
            }
            .setNegativeButton(R.string.cancel_msg, null)
            .show()
    }

    /**
     * 禁用规则
     * @param ruleName 规则名称
     */
    private fun disableRule(ruleName: String) {
        // 提取实际规则名（去除类型前缀）
        val ruleData = ruleName
            .removePrefix("通知·")
            .removePrefix("数据·")
            .removePrefix("OCR·")
            .trim()

        Logger.d("查找的规则：$ruleData")

        launch {
            try {
                // 搜索匹配的规则
                val rule = RuleManageAPI.list(
                    app = "",
                    type = "",
                    creator = "",
                    page = 1,
                    limit = 1,
                    search = ruleData
                ).firstOrNull()

                if (rule == null) {
                    ToastUtils.error(R.string.rule_not_found)
                    return@launch
                }

                // 禁用规则
                rule.enabled = false
                RuleManageAPI.put(rule)
                Logger.d("禁用规则: ${rule.name} (ID: ${rule.id})")

                ToastUtils.info(R.string.rule_disabled_successfully)
                binding.close.visibility = View.GONE
            } catch (e: Exception) {
                Logger.e("禁用规则异常", e)
                ToastUtils.error(R.string.rule_disable_failed)
            }
        }
    }
}
