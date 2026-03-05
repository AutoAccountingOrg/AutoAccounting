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

package net.ankio.auto.ui.fragment.components

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.lifecycle.findViewTreeLifecycleOwner
import net.ankio.auto.R
import net.ankio.auto.databinding.CardStatusBinding
import net.ankio.auto.ui.api.BaseComponent
import net.ankio.auto.ui.theme.DynamicColors
import net.ankio.auto.ui.utils.toDrawable
import net.ankio.auto.ui.vm.HomeActivityVm
import net.ankio.auto.ui.vm.StatusData
import net.ankio.auto.utils.PrefManager
import java.util.Locale

/**
 * 状态卡组件，展示工作状态、规则/应用版本，点击检查更新。
 * 需通过 [setVm] 注入 [HomeActivityVm]，UI 数据与点击逻辑均走 VM。
 */
class StatusCardComponent(binding: CardStatusBinding) :
    BaseComponent<CardStatusBinding>(binding) {

    private var vm: HomeActivityVm? = null

    /** 注入 ViewModel，必须在 [onComponentCreate] 之后由宿主调用 */
    fun setVm(vm: HomeActivityVm) {
        this.vm = vm
        setupVm()
    }

    override fun onComponentCreate() {
        super.onComponentCreate()
        binding.cardContentRule.setBackgroundColor(DynamicColors.SurfaceColor3)
        binding.cardContentApp.setBackgroundColor(DynamicColors.SurfaceColor3)

        // 点击：检查更新（走 VM）
        binding.cardContentRule.setOnClickListener {
            vm?.checkRuleUpdate(fromUser = true)
        }
        binding.cardContentApp.setOnClickListener {
            vm?.checkAppUpdate(fromUser = true)
        }

        // 长按：强制规则更新后检查
        binding.cardContentRule.setOnLongClickListener {
            PrefManager.ruleVersion = ""
            vm?.checkRuleUpdate(fromUser = true)
            true
        }
        binding.cardContentApp.setOnLongClickListener {
            vm?.checkAppUpdate(fromUser = true)
            true
        }
    }

    override fun onComponentResume() {
        super.onComponentResume()
        vm?.refreshStatus(context)
    }

    /** 绑定 VM 后调用：注册观察、首次刷新 */
    private fun setupVm() {
        val v = vm ?: return
        val owner = binding.root.findViewTreeLifecycleOwner() ?: return
        v.refreshStatus(context)
        v.statusData.observe(owner) { data ->
            applyStatusData(data)
        }
    }

    /** 根据 [StatusData] 更新 UI */
    private fun applyStatusData(data: StatusData) {
        binding.titleText.text =
            if (data.isActive) context.getString(R.string.status_working)
            else context.getString(R.string.status_inactive)
        binding.modeText.text = data.workMode.name.uppercase(Locale.getDefault())
        binding.debugTag.visibility =
            if (data.debugMode) android.view.View.VISIBLE else android.view.View.GONE

        if (data.isActive) {
            setActive(
                backgroundColor = DynamicColors.SecondaryContainer,
                textColor = DynamicColors.OnPrimaryContainer,
                drawable = R.drawable.home_active_success
            )
        } else {
            setActive(
                backgroundColor = DynamicColors.SurfaceColor3,
                textColor = DynamicColors.Primary,
                drawable = R.drawable.home_active_error
            )
        }
        binding.subtitleText.text = "v${data.versionName}"
        binding.ruleVersionText.text = data.ruleVersion
        binding.ruleUpdateText.text = data.ruleUpdate

        val values = context.resources.getStringArray(R.array.update_channel_values)
        val texts = context.resources.getStringArray(R.array.update_channel_texts)
        val channelIndex = values.indexOf(data.channelValue).coerceAtLeast(0)
        binding.channelText.text =
            texts.getOrElse(channelIndex) { texts.firstOrNull() ?: data.channelValue }
    }

    private fun setActive(
        @ColorInt backgroundColor: Int,
        @ColorInt textColor: Int,
        @DrawableRes drawable: Int,
    ) {
        binding.cardContentStatus.setBackgroundColor(backgroundColor)
        //   binding.iconView.setImageDrawable(drawable.toDrawable())
        //   binding.iconView.setColorFilter(textColor)
        binding.titleText.setTextColor(textColor)
        binding.modeText.setTextColor(DynamicColors.OnPrimary)
        binding.subtitleText.setTextColor(textColor)
        binding.ruleVersionText.setTextColor(textColor)
        binding.ruleUpdateText.setTextColor(textColor)
        // 同步更新右下角装饰图标（保持 XML 中的 alpha/tint，仅更新图形）
        binding.statusBgIcon.setImageDrawable(drawable.toDrawable())

    }

}

