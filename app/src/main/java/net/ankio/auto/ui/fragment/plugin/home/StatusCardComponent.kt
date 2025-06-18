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

package net.ankio.auto.ui.fragment.plugin.home

import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.lifecycle.Lifecycle
import com.google.android.material.elevation.SurfaceColors
import net.ankio.auto.BuildConfig
import net.ankio.auto.R
import net.ankio.auto.constant.WorkMode
import net.ankio.auto.databinding.CardStatusBinding
import net.ankio.auto.service.OcrService
import net.ankio.auto.ui.api.BaseComponent
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.utils.toDrawable
import net.ankio.auto.utils.toThemeColor
import net.ankio.auto.xposed.common.ActiveInfo

class StatusCardComponent(binding: CardStatusBinding, private val lifecycle: Lifecycle) :
    BaseComponent<CardStatusBinding>(binding, lifecycle) {

    override fun init() {
        super.init()


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


    }


    private fun checkXposedActive(): Boolean {
        return ActiveInfo.isModuleActive()
    }

    private fun checkOcrActive(): Boolean {
        return OcrService.serverStarted
    }

    override fun resume() {
        super.resume()

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

    override fun cleanup() {
        super.cleanup()
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

}

