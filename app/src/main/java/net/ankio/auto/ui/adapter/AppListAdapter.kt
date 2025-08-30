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
import com.bumptech.glide.Glide
import net.ankio.auto.databinding.AdapterAutoAppBinding
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import net.ankio.auto.adapter.IAppAdapter
import net.ankio.auto.utils.CustomTabsHelper
import net.ankio.auto.utils.isAppInstalled

class AppListAdapter(
    private val context: Context,
    private val selectApp: String, private val callback: (IAppAdapter) -> Unit
) : BaseAdapter<AdapterAutoAppBinding, IAppAdapter>() {
    override fun onInitViewHolder(holder: BaseViewHolder<AdapterAutoAppBinding, IAppAdapter>) {
        val binding = holder.binding
        binding.root.setOnClickListener {
            val app = holder.item ?: return@setOnClickListener
            val installedApp = context.isAppInstalled(app.pkg)
            if (!installedApp && app.link.isNotEmpty()) {
                CustomTabsHelper.launchUrlOrCopy(app.link)
            } else {
                binding.checkbox.isChecked = true
                callback(app)
            }
        }
    }


    override fun onBindViewHolder(
        holder: BaseViewHolder<AdapterAutoAppBinding, IAppAdapter>,
        data: IAppAdapter,
        position: Int
    ) {
        val binding = holder.binding
        val installedApp = context.isAppInstalled(data.pkg)

        // 优先使用系统图标（已安装）
        val appIconDrawable = if (installedApp) getAppIcon(data.pkg) else null
        if (appIconDrawable != null) {
            binding.appIcon.setImageDrawable(appIconDrawable)
        } else if (data.icon.startsWith("http")) {
            Glide.with(binding.appIcon).load(data.icon).into(binding.appIcon)
        } else {
            binding.appIcon.setImageDrawable(null)
        }

        // 未安装时置灰
        if (!installedApp) {
            val matrix = ColorMatrix().apply { setSaturation(0f) }
            binding.appIcon.colorFilter = ColorMatrixColorFilter(matrix)
        } else {
            binding.appIcon.clearColorFilter()
        }

        binding.appName.text = data.name
        binding.appDesc.text = data.desc
        binding.appPackageName.text = data.pkg
        binding.checkbox.isChecked = data.pkg == selectApp
    }

    private fun getAppIcon(packageName: String): Drawable? {
        return try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            Logger.e("获取应用图标失败", e)
            null
        }
    }

    override fun areItemsSame(oldItem: IAppAdapter, newItem: IAppAdapter): Boolean {
        return oldItem.pkg == newItem.pkg
    }

    override fun areContentsSame(oldItem: IAppAdapter, newItem: IAppAdapter): Boolean {
        return oldItem == newItem
    }
}