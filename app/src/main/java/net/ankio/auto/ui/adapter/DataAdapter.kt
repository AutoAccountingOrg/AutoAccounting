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

import android.net.Uri
import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.elevation.SurfaceColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.app.js.Engine
import net.ankio.auto.constant.DataType
import net.ankio.auto.constant.toDataType
import net.ankio.auto.database.Db
import net.ankio.auto.database.table.AppData
import net.ankio.auto.databinding.AdapterDataBinding
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.CustomTabsHelper
import net.ankio.auto.utils.DateUtils

class DataAdapter(
    override val dataItems: MutableList<AppData>,
    private val onClickContent: (string: String) -> Unit,
    private val onClickTest: (item: AppData) -> Unit,
    private val onClickUploadData: (item: AppData, pos: Int) -> Unit,
) : BaseAdapter(dataItems, AdapterDataBinding::class.java) {
    override fun onInitView(holder: BaseViewHolder) {
        val binding = holder.binding as AdapterDataBinding
        val context = holder.context

        binding.issue.setOnClickListener {
            val item = holder.item as AppData
            CustomTabsHelper.launchUrl(
                context,
                Uri.parse(
                    if (item.match) "https://github.com/AutoAccountingOrg/AutoAccounting/issues/${item.issue}" else "https://github.com/AutoAccountingOrg/AutoRule/issues/${item.issue}",
                ),
            )
        }

        binding.testRule.setOnClickListener {
            val item = holder.item as AppData
            onClickTest(item)
        }
        binding.content.setOnClickListener {
            onClickContent(binding.content.text as String)
        }

        binding.uploadData.setOnClickListener {
            val item = holder.item as AppData
            onClickUploadData(item, getHolderIndex(holder))
        }
    }

    override fun onBindView(
        holder: BaseViewHolder,
        item: Any,
    ) {
        val binding = holder.binding as AdapterDataBinding
        val appData = item as AppData
        val context = holder.context

        binding.groupCard.setCardBackgroundColor(SurfaceColors.SURFACE_1.getColor(context))
        binding.content.setBackgroundColor(SurfaceColors.SURFACE_3.getColor(context))
        // 格式化数据
        val prettyJson: String = AppUtils.toPrettyFormat(appData.data)

        binding.content.text = prettyJson
        when (appData.type.toDataType()) {
            DataType.Notice -> {
                binding.type.setColorFilter(ContextCompat.getColor(context, R.color.warning))
                binding.type.setImageResource(R.drawable.data_notice)
            }

            DataType.Helper -> {
                binding.type.setColorFilter(ContextCompat.getColor(context, R.color.danger))
                binding.type.setImageResource(R.drawable.data_helper)
            }

            DataType.Sms -> {
                binding.type.setColorFilter(ContextCompat.getColor(context, R.color.info))
                binding.type.setImageResource(R.drawable.data_sms)
            }

            DataType.App -> {
                binding.type.setColorFilter(ContextCompat.getColor(context, R.color.success))
                binding.type.setImageResource(R.drawable.data_app)
            }
        }
        binding.issue.visibility = View.VISIBLE
        binding.uploadData.visibility = View.VISIBLE

        if (appData.issue == 0) {
            binding.issue.visibility = View.GONE
        } else {
            binding.uploadData.visibility = View.GONE
            binding.issue.text = "# ${appData.issue}"
        }
        holder.scope.launch {
            tryAdaptUnmatchedItems(holder, this@DataAdapter)
        }
        val app = AppUtils.getAppInfoFromPackageName(item.source, context)

        binding.app.text =
            item.source.let {
                if (item.type.toDataType() != DataType.Sms) {
                    app?.name
                } else {
                    it
                }
            }
        if (app != null) {
            binding.image.setImageDrawable(app.icon)
        } else {
            binding.image.setImageResource(R.drawable.data_sms)
        }

        binding.time.text =
            item.time.let {
                DateUtils.getTime(it)
            }
        binding.rule.visibility = View.VISIBLE
        if (!item.match) {
            binding.rule.visibility = View.GONE

            binding.uploadData.setIconResource(R.drawable.icon_upload)
        } else {
            binding.uploadData.setIconResource(R.drawable.icon_question)
            binding.groupCard.setCardBackgroundColor(SurfaceColors.SURFACE_5.getColor(context))
        }

        // 使用正则提取 \[(.*?)\]

        val rule = item.rule
        val regex = "\\[(.*?)]".toRegex()
        val matchResult = regex.find(rule)
        if (matchResult != null) {
            val (value) = matchResult.destructured
            binding.rule.text = value
        } else {
            binding.rule.text = item.rule
        }

        //    binding.rule.text = item.rule
    }

    private val hashMap = HashMap<AppData, Long>()

    private suspend fun tryAdaptUnmatchedItems(
        holder: BaseViewHolder,
        adapter: DataAdapter,
    ) = withContext(Dispatchers.IO) {
        val item = holder.item as AppData
        if (!item.match) {
            val t = System.currentTimeMillis() / 1000
            if (hashMap.containsKey(item) && t - hashMap[item]!! < 30) { // 30秒内不重复匹配
                return@withContext
            }
            hashMap[item] = t
            val result = Engine.analyze(item.type, item.source, item.data)
            if (result != null) {
                item.rule = result.channel
                item.match = true
                withContext(Dispatchers.Main) {
                    val index = getHolderIndex(holder)
                    dataItems[index] = item
                    adapter.notifyItemChanged(index)
                }
                Db.get().AppDataDao().update(item)
            }
        }
    }
}
