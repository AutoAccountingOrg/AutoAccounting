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

package net.ankio.auto.ui.adapter

import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat
import net.ankio.auto.App
import net.ankio.auto.databinding.AdapterAutoAppBinding
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import net.ankio.auto.ui.models.AutoApp
import net.ankio.auto.utils.CustomTabsHelper

class AppListAdapter(
    private val context: Context, private val list: MutableList<AutoApp>,
    private val selectApp: String, private val callback: (AutoApp) -> Unit
) : BaseAdapter<AdapterAutoAppBinding, AutoApp>(AdapterAutoAppBinding::class.java, list) {
    override fun onInitViewHolder(holder: BaseViewHolder<AdapterAutoAppBinding, AutoApp>) {
        val binding = holder.binding
        binding.root.setOnClickListener {
            val app = holder.item ?: return@setOnClickListener
            val installedApp = App.isAppInstalled(app.packageName)
            if (!installedApp) {
                CustomTabsHelper.launchUrlOrCopy(context, app.url)
            } else {
                binding.checkbox.isChecked = true
                callback(app)
            }
        }
    }


    override fun onBindViewHolder(
        holder: BaseViewHolder<AdapterAutoAppBinding, AutoApp>,
        data: AutoApp,
        position: Int
    ) {
        val binding = holder.binding
        val drawable = ResourcesCompat.getDrawable(context.resources, data.icon, null)
        val installedApp = App.isAppInstalled(data.packageName)
        // 获取应用图标
        val appIcon = if (installedApp) {
            getAppIcon(data.packageName) ?: ResourcesCompat.getDrawable(
                context.resources,
                data.icon,
                null
            )
        } else {
            drawable
        }
        binding.appIcon.setImageDrawable(if (installedApp) appIcon else toGrayscale(appIcon!!))
        binding.appName.text = data.name
        binding.appDesc.text = data.desc
        binding.appPackageName.text = data.packageName
        binding.checkbox.isChecked = data.packageName == selectApp

        binding.checkbox.isEnabled = installedApp

    }

    private fun toGrayscale(drawable: Drawable): Drawable {
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)   // 将饱和度设为0，即灰度化

        val filter = ColorMatrixColorFilter(colorMatrix)
        drawable.colorFilter = filter

        return drawable
    }

    private fun getAppIcon(packageName: String): Drawable? {
        return try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            Logger.e("获取应用图标失败", e)
            null
        }
    }
}