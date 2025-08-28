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
import net.ankio.auto.utils.CustomTabsHelper
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.utils.Throttle
import net.ankio.auto.utils.VersionUtils
import net.ankio.auto.utils.toDrawable
import net.ankio.auto.utils.toThemeColor
import net.ankio.auto.xposed.common.ActiveInfo

class StatusCardComponent(binding: CardStatusBinding) :
    BaseComponent<CardStatusBinding>(binding) {
    private val throttle = Throttle.asFunction<Boolean>(5000) { fromUser ->
        launch {
            try {
                updateApps(fromUser)
            } catch (e: Exception) {
                Logger.e(e.message ?: "", e)
            }
        }
    }

    override fun onComponentCreate() {
        super.onComponentCreate()


        val colorPrimary = com.google.android.material.R.attr.colorPrimary.toThemeColor()

        if (!ActiveInfo.isModuleActive()) {
            setActive(
                SurfaceColors.SURFACE_3.getColor(context),
                colorPrimary,
                R.drawable.home_active_error,
            )
        } else {
            setActive(
                colorPrimary,
                com.google.android.material.R.attr.colorOnPrimary.toThemeColor(),
                R.drawable.home_active_success,
            )
        }

        binding.cardContent.setOnClickListener { throttle(true) }

        if (PrefManager.autoCheckAppUpdate) throttle(false)

    }


    private fun checkXposedActive(): Boolean {
        return ActiveInfo.isModuleActive()
    }

    private fun checkOcrActive(): Boolean {
        return OcrService.serverStarted
    }

    override fun onComponentResume() {
        super.onComponentResume()

        val active = if (PrefManager.workMode === WorkMode.Ocr) {
            checkOcrActive()
        } else {
            checkXposedActive()
        }
        val colorPrimary = com.google.android.material.R.attr.colorPrimary.toThemeColor()
        val versionName = BuildConfig.VERSION_NAME
        if (!active) {
            setActive(
                SurfaceColors.SURFACE_3.getColor(context),
                colorPrimary,
                R.drawable.home_active_error,
            )
            binding.titleText.text = context.getString(R.string.active_error, versionName)
        } else {
            setActive(
                colorPrimary,
                com.google.android.material.R.attr.colorOnPrimary.toThemeColor(),
                R.drawable.home_active_success,
            )
            binding.titleText.text = context.getString(R.string.active_success, versionName)
        }

        binding.subtitleText.text = if (PrefManager.workMode === WorkMode.Ocr) {
            context.getString(R.string.ocr_mode_title)
        } else {
            context.getString(R.string.xposed_mode_title)
        }


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
        if (fromUser) {
            ToastUtils.info(R.string.check_update)
        }
        try {
            val json = AppAPI.lastVer()
            val update = VersionUtils.fromJSON(json)
            if (update == null) {
                if (fromUser) {
                    ToastUtils.error(R.string.no_need_to_update)
                }
                return
            }

            // 检查版本是否需要更新
            if (!VersionUtils.checkVersionLarge(BuildConfig.VERSION_NAME, update.version)) {
                if (fromUser) {
                    ToastUtils.error(R.string.no_need_to_update)
                }
                return
            }

            // 显示更新对话框
            BaseSheetDialog.create<UpdateDialog>(context)
                .setUpdateModel(update)
                .setRuleTitle(context.getString(R.string.app))
                .setOnClickUpdate {
                    val url =
                        "https://cloud.ankio.net/%E8%87%AA%E5%8A%A8%E8%AE%B0%E8%B4%A6/%E8%87%AA%E5%8A%A8%E8%AE%B0%E8%B4%A6/%E7%89%88%E6%9C%AC%E6%9B%B4%E6%96%B0/${PrefManager.appChannel}/${update.version}.apk"
                    CustomTabsHelper.launchUrl(url.toUri())
                }.show()
        } catch (e: Exception) {
            Logger.e(e.message ?: "", e)
            if (fromUser) {
                ToastUtils.error(R.string.no_need_to_update)
            }
        }
    }
}

