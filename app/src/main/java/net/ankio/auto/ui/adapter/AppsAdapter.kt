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

package net.ankio.auto.ui.adapter;

import net.ankio.auto.databinding.AdapterAppsBinding;
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import net.ankio.auto.ui.models.AppInfo

class AppsAdapter(
    private val list: MutableList<AppInfo>,
    private val callback: (AppInfo) -> Unit
) : BaseAdapter<AdapterAppsBinding, AppInfo>(AdapterAppsBinding::class.java, list) {
    override fun onInitViewHolder(holder: BaseViewHolder<AdapterAppsBinding, AppInfo>) {
        val binding = holder.binding
        binding.root.setOnClickListener {
            callback(holder.item!!)
        }
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder<AdapterAppsBinding, AppInfo>,
        data: AppInfo,
        position: Int
    ) {
        if (position < 0 || position >= list.size) {
            return
        }

        val binding = holder.binding
        try {
            binding.appIcon.setImageDrawable(data.icon)
            binding.appName.text = data.appName
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
