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

import android.content.pm.PackageManager
import net.ankio.auto.databinding.AdapterAppBinding
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import net.ankio.auto.ui.models.AppInfo

class AppAdapter(
    private val list: MutableList<AppInfo>,
    private val pkg: PackageManager,
    private val callback: (AppInfo) -> Unit
) : BaseAdapter<AdapterAppBinding, AppInfo>(AdapterAppBinding::class.java, list) {
    override fun onInitViewHolder(holder: BaseViewHolder<AdapterAppBinding, AppInfo>) {
        val binding = holder.binding
        binding.root.setOnClickListener {
            holder.item!!.isSelected = !binding.checkbox.isChecked
            callback(holder.item!!)
            binding.checkbox.isChecked = holder.item!!.isSelected
        }
    }


    override fun onBindViewHolder(
        holder: BaseViewHolder<AdapterAppBinding, AppInfo>,
        data: AppInfo,
        position: Int
    ) {
        if (position < 0 || position >= list.size) {
            return
        }
        
        val binding = holder.binding
        try {
            binding.appIcon.setImageDrawable(pkg.getApplicationIcon(data.packageName))
            if (data.appName.isNotEmpty()) {
                binding.appName.text = data.appName
            } else {
                binding.appName.text = pkg.getApplicationLabel(data.pkg).toString()
            }
            binding.appVersionName.text = pkg.getPackageInfo(data.packageName, 0).versionName
            binding.appPackageName.text = data.packageName
            binding.checkbox.isChecked = data.isSelected
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateData(filtered: MutableList<AppInfo>) {
        list.clear()
        list.addAll(filtered)
        notifyDataSetChanged()
    }


}