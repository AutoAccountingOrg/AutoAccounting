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

import net.ankio.auto.App
import net.ankio.auto.R
import net.ankio.auto.databinding.AdapterLogBinding
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import net.ankio.auto.utils.DateUtils
import org.ezbook.server.constant.LogLevel
import org.ezbook.server.db.model.LogModel

class LogAdapter(list: MutableList<LogModel>) :
    BaseAdapter<AdapterLogBinding, LogModel>(AdapterLogBinding::class.java, list) {
    override fun onInitViewHolder(holder: BaseViewHolder<AdapterLogBinding, LogModel>) {
        holder.binding.root.setOnClickListener {
            val item = holder.item!!
            App.copyToClipboard(item.message)
        }
    }

    private val cachedApp = HashMap<String, String>()

    override fun onBindViewHolder(
        holder: BaseViewHolder<AdapterLogBinding, LogModel>,
        data: LogModel,
        position: Int
    ) {
        val binding = holder.binding
        val level = data.level

        val sb = StringBuilder()
        sb.append("[ ")
        sb.append(DateUtils.getTime(data.time))
        sb.append(" ] [ ")


        var appName = data.app

        if (cachedApp.containsKey(data.app)) {
            appName = cachedApp[data.app]!!
        } else {
            val array = App.getAppInfoFromPackageName(data.app)
            if (array !== null) {
                cachedApp[data.app] = array[0] as String
                appName = cachedApp[data.app]!!
            }
        }

        sb.append(appName)
        sb.append(" ] [ ")
        sb.append(data.location)
        sb.append(" ] ")
        sb.append(data.message)
        binding.log.text = sb.toString()
        when (level) {
            LogLevel.DEBUG -> binding.log.setTextColor(holder.context.getColor(R.color.success))
            LogLevel.INFO -> binding.log.setTextColor(holder.context.getColor(R.color.info))
            LogLevel.WARN -> binding.log.setTextColor(holder.context.getColor(R.color.warning))
            LogLevel.ERROR -> binding.log.setTextColor(holder.context.getColor(R.color.danger))
            else -> binding.log.setTextColor(holder.context.getColor(R.color.info))
        }
    }

}