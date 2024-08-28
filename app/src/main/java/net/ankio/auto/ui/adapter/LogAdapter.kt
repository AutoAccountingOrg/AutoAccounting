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
import net.ankio.auto.R
import net.ankio.auto.databinding.AdapterLogBinding
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import net.ankio.auto.utils.DateUtils
import org.ezbook.server.constant.LogLevel
import org.ezbook.server.db.model.LogModel

class LogAdapter(private val list: MutableList<LogModel>): BaseAdapter<AdapterLogBinding, LogModel>(AdapterLogBinding::class.java, list) {
    override fun onInitViewHolder(holder: BaseViewHolder<AdapterLogBinding>) {

    }

    override fun onBindViewHolder(holder: BaseViewHolder<AdapterLogBinding>, position: Int) {
        val binding = holder.binding
        val it = list[position]
        val level = it.level
        binding.logDate.text = DateUtils.getTime(it.time)
        binding.app.text = it.app
        binding.logFile.text = it.location
        binding.log.text = it.message
        when (level) {
            LogLevel.DEBUG -> binding.log.setTextColor(holder.context.getColor(R.color.success))
            LogLevel.INFO -> binding.log.setTextColor(holder.context.getColor(R.color.info))
            LogLevel.WARN -> binding.log.setTextColor(holder.context.getColor(R.color.warning))
            LogLevel.ERROR -> binding.log.setTextColor(holder.context.getColor(R.color.danger))
            else -> binding.log.setTextColor(holder.context.getColor(R.color.info))
        }
    }

}