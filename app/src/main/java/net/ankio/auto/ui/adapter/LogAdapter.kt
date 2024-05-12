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

import android.view.View
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.databinding.AdapterLogBinding

class LogAdapter(
    override val dataItems: ArrayList<String>,
) : BaseAdapter(dataItems, AdapterLogBinding::class.java) {
    override fun onInitView(holder: BaseViewHolder) {
    }

    override fun onBindView(
        holder: BaseViewHolder,
        item: Any,
    ) {
        val binding = holder.binding as AdapterLogBinding
        val it = item as String
        // [2024-05-11 13:12:06][D][ DefaultDispatcher-worker-3 ] (RequestsUtils.kt:202) GET https://cloud.ankio.net/d/阿里云盘/自动记账/规则更新//index.json
        // 正则提取数据
        holder.scope.launch {
            val regex = Regex("""\[(.*?)]\[(.*?)]\[(.*?)] \((.*?)\) (.*)""")
            val matchResult = regex.find(it.trim())
            if (matchResult != null) {
                val groups = matchResult.groupValues
                val level = groups[2].trim()
                withContext(Dispatchers.Main) {
                    binding.logDate.text = groups[1].trim()

                    binding.logThread.text = groups[3].trim()
                    binding.logFile.text = groups[4].trim()
                    // 数据需要进行截断处理，防止过长导致绘制ANR
                    binding.log.text = groups[5].trim().substring(0, 500)
                    when (level) {
                        "D" -> binding.log.setTextColor(holder.context.getColor(R.color.success))
                        "I" -> binding.log.setTextColor(holder.context.getColor(R.color.info))
                        "V" -> binding.log.setTextColor(holder.context.getColor(R.color.info))
                        "W" -> binding.log.setTextColor(holder.context.getColor(R.color.warning))
                        "E" -> binding.log.setTextColor(holder.context.getColor(R.color.danger))
                        else -> binding.log.setTextColor(holder.context.getColor(R.color.info))
                    }
                    binding.header.visibility = View.VISIBLE
                }
            } else {
                withContext(Dispatchers.Main) {
                    binding.header.visibility = View.GONE
                    binding.log.text = it.trim().substring(0, 500)
                    binding.log.setTextColor(holder.context.getColor(R.color.info))
                }
            }
        }
    }
}
