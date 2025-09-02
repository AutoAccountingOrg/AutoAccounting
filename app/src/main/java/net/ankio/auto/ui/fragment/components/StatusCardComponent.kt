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
import androidx.core.net.toUri
import com.google.android.material.elevation.SurfaceColors
import net.ankio.auto.BuildConfig
import net.ankio.auto.R
import net.ankio.auto.constant.WorkMode
import net.ankio.auto.databinding.CardStatusBinding
import net.ankio.auto.http.license.AppAPI
import net.ankio.auto.service.OcrService
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseComponent
import net.ankio.auto.ui.dialog.UpdateDialog
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.utils.CoroutineUtils.withIO
import net.ankio.auto.utils.CoroutineUtils.withMain
import net.ankio.auto.utils.CustomTabsHelper
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.utils.VersionUtils
import net.ankio.auto.ui.utils.toDrawable
import net.ankio.auto.ui.utils.toThemeColor
import net.ankio.auto.xposed.XposedModule

class StatusCardComponent(binding: CardStatusBinding) :
    BaseComponent<CardStatusBinding>(binding) {

    override fun onComponentCreate() {
        super.onComponentCreate()

        // 设置点击监听器
        binding.cardContent.setOnClickListener {
            launch {
                updateApps(true)
            }
        }

        // 初始化状态显示
        updateActiveStatus()
    }

    override fun onComponentResume() {
        super.onComponentResume()
        // 每次恢复时更新状态
        updateActiveStatus()
    }

    /**
     * 检查当前工作模式下的激活状态
     */
    private fun isCurrentModeActive(): Boolean {
        return when (PrefManager.workMode) {
            WorkMode.Ocr -> OcrService.serverStarted
            else -> XposedModule.active()
        }
    }

    /**
     * 获取当前模式的标题文本
     */
    private fun getCurrentModeTitle(): String {
        return when (PrefManager.workMode) {
            WorkMode.Ocr -> context.getString(R.string.ocr_mode_title)
            else -> context.getString(R.string.xposed_mode_title)
        }
    }

    /**
     * 统一更新激活状态显示
     */
    private fun updateActiveStatus() {
        val isActive = isCurrentModeActive()
        val versionName = BuildConfig.VERSION_NAME
        val colorPrimary = com.google.android.material.R.attr.colorPrimary.toThemeColor()

        if (isActive) {
            // 激活状态：绿色背景
            setActive(
                backgroundColor = colorPrimary,
                textColor = com.google.android.material.R.attr.colorOnPrimary.toThemeColor(),
                drawable = R.drawable.home_active_success
            )
            binding.titleText.text = context.getString(R.string.active_success, versionName)
        } else {
            // 未激活状态：灰色背景
            setActive(
                backgroundColor = SurfaceColors.SURFACE_3.getColor(context),
                textColor = colorPrimary,
                drawable = R.drawable.home_active_error
            )
            binding.titleText.text = context.getString(R.string.active_error, versionName)
        }

        // 设置模式标题
        binding.subtitleText.text = getCurrentModeTitle()
    }

    override fun onComponentDestroy() {
        super.onComponentDestroy()
    }

    private fun setActive(
        @ColorInt backgroundColor: Int,
        @ColorInt textColor: Int,
        @DrawableRes drawable: Int,
    ) {
        binding.cardContent.setBackgroundColor(backgroundColor)
        binding.iconView.setImageDrawable(drawable.toDrawable())
        binding.iconView.setColorFilter(textColor)
        binding.titleText.setTextColor(textColor)
        binding.subtitleText.setTextColor(textColor)
    }

    private suspend fun updateApps(fromUser: Boolean) {
        // UI操作在主线程
        if (fromUser) {
            withMain {
                ToastUtils.info(R.string.check_update)
            }
        }

        // 网络请求在IO线程
        val update = withIO {
            val json = AppAPI.lastVer()
            VersionUtils.fromJSON(json)
        }

        if (update == null) {
            if (fromUser) {
                withMain {
                    ToastUtils.error(R.string.no_need_to_update)
                }
            }
            return
        }

        // 检查版本是否需要更新
        if (!VersionUtils.checkVersionLarge(BuildConfig.VERSION_NAME, update.version)) {
            if (fromUser) {
                withMain {
                    ToastUtils.error(R.string.no_need_to_update)
                }
            }
            return
        }

        // UI操作在主线程 - 显示更新对话框
        withMain {
            BaseSheetDialog.create<UpdateDialog>(context)
                .setUpdateModel(update)
                .setRuleTitle(context.getString(R.string.app))
                .setOnClickUpdate {
                    val url =
                        "https://cloud.ankio.net/%E8%87%AA%E5%8A%A8%E8%AE%B0%E8%B4%A6/%E8%87%AA%E5%8A%A8%E8%AE%B0%E8%B4%A6/%E7%89%88%E6%9C%AC%E6%9B%B4%E6%96%B0/${PrefManager.appChannel}/${update.version}.apk"
                    CustomTabsHelper.launchUrl(url.toUri())
                }.show()
        }
    }
}

