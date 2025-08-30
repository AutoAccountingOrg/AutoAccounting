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
import net.ankio.auto.utils.SystemUtils
import net.ankio.auto.utils.getAppInfoFromPackageName
import org.ezbook.server.constant.LogLevel
import org.ezbook.server.db.model.LogModel

class LogAdapter :
    BaseAdapter<AdapterLogBinding, LogModel>() {

    companion object {
        private const val DATE_FORMAT = "yyyy-MM-dd\nHH:mm:ss"

        // 将日志级别与颜色资源ID映射
        private val LOG_LEVEL_COLORS = mapOf(
            LogLevel.DEBUG to R.color.log_debug,
            LogLevel.INFO to R.color.log_info,
            LogLevel.WARN to R.color.log_warning,
            LogLevel.ERROR to R.color.log_error
        )
    }

    private val cachedApp = mutableMapOf<String, String>()

    override fun onInitViewHolder(holder: BaseViewHolder<AdapterLogBinding, LogModel>) {
        holder.binding.root.setOnLongClickListener {
            val item = holder.item!!
            SystemUtils.copyToClipboard(item.message)
            true
        }
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder<AdapterLogBinding, LogModel>,
        data: LogModel,
        position: Int
    ) {
        val binding = holder.binding

        binding.date.text = DateUtils.stampToDate(data.time, DATE_FORMAT)

        val appName = cachedApp.getOrPut(data.app) {
            getAppInfoFromPackageName(data.app)?.name ?: data.app
        }

        binding.app.text = if (data.location.isNotEmpty()) {
            "$appName ${data.location}"
        } else {
            appName
        }

        val maxLength = 1024
        val message = data.message
        val displayedMessage = if (message.codePointCount(0, message.length) > maxLength) {
            val cutPoint = message.offsetByCodePoints(0, maxLength)
            message.substring(0, cutPoint) + "..."
        } else {
            message
        }

        binding.log.text = displayedMessage

        // 使用映射获取颜色
        val colorResId = LOG_LEVEL_COLORS[data.level] ?: R.color.log_info
        binding.log.setTextColor(holder.context.getColor(colorResId))
    }

    override fun areItemsSame(oldItem: LogModel, newItem: LogModel): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsSame(oldItem: LogModel, newItem: LogModel): Boolean {
        return oldItem == newItem
    }
}