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

import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.elevation.SurfaceColors
import com.hjq.toast.Toaster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.App
import net.ankio.auto.R
import net.ankio.auto.app.BillUtils
import net.ankio.auto.app.js.Engine
import net.ankio.auto.constant.DataType
import net.ankio.auto.constant.toDataType
import net.ankio.auto.databinding.AdapterDataBinding
import net.ankio.auto.databinding.SettingItemInputBinding
import net.ankio.auto.storage.SpUtils
import net.ankio.auto.ui.api.BaseActivity
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import net.ankio.auto.ui.dialog.DataEditorDialog
import net.ankio.auto.ui.scope.autoDisposeScope
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.CustomTabsHelper
import net.ankio.auto.utils.DateUtils
import net.ankio.auto.utils.Github
import org.ezbook.server.db.model.AppDataModel
import org.ezbook.server.db.model.LogModel

class AppDataAdapter(private val list: MutableList<AppDataModel>,private val activity: BaseActivity): BaseAdapter<AdapterDataBinding,AppDataModel>(AdapterDataBinding::class.java, list) {



    private val hashMap = HashMap<AppDataModel, Long>()

    private suspend fun tryAdaptUnmatchedItems(
        holder: BaseViewHolder<AdapterDataBinding, AppDataModel>
    ) = withContext(Dispatchers.IO) {
        val item = holder.item as AppDataModel
        if (!item.match) {
            val t = System.currentTimeMillis() / 1000
            if (hashMap.containsKey(item) && t - hashMap[item]!! < 60) { // 30秒内不重复匹配
                return@withContext
            }
            hashMap[item] = t

            // TODO 匹配规则
        }
    }

    override fun onInitViewHolder(holder: BaseViewHolder<AdapterDataBinding, AppDataModel>) {
        val binding = holder.binding
        binding.issue.setOnClickListener {
            val item = holder.item!!
            CustomTabsHelper.launchUrl(
                AppUtils.getApplication(),
                Uri.parse(
                    if (item.match) "https://github.com/AutoAccountingOrg/AutoAccounting/issues/${item.issue}" else "https://github.com/AutoAccountingOrg/AutoRule/issues/${item.issue}",

           ))
        }


        binding.testRule.setOnClickListener {
            val item = holder.item!!
            holder.binding.root.autoDisposeScope.launch {
                // TODO 测试规则
            }

        }

        binding.content.setOnClickListener {
            MaterialAlertDialogBuilder(activity)
                .setTitle(activity.getString(R.string.content_title))
                .setMessage(binding.content.text as String)
                .setPositiveButton(activity.getString(R.string.cancel_msg)) { _, _ -> }
                .setNegativeButton(activity.getString(R.string.copy)) { _, _ ->
                    AppUtils.copyToClipboard(binding.content.text as String)
                    Toaster.show(R.string.copy_command_success)
                }
                .show()
        }

        binding.uploadData.setOnClickListener {

        }

        binding.root.setOnLongClickListener {
            MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.delete_title)
                .setMessage(R.string.delete_data_message)
                .setPositiveButton(R.string.sure_msg) { _, _ ->
                    list.removeAt(holder.positionIndex)
                    notifyItemRemoved(holder.positionIndex)
                }
                .setNegativeButton(R.string.cancel_msg) { _, _ -> }
                .show()
            true
        }

        binding.groupCard.setCardBackgroundColor(SurfaceColors.SURFACE_1.getColor(activity))
        binding.content.setBackgroundColor(SurfaceColors.SURFACE_3.getColor(activity))

    }

    override fun onBindViewHolder(
        holder: BaseViewHolder<AdapterDataBinding, AppDataModel>,
        data: AppDataModel,
        position: Int
    ) {
        val binding = holder.binding
        binding.content.text = data.data
        binding.issue.visibility = View.VISIBLE
        binding.uploadData.visibility = View.VISIBLE

        if (data.issue == 0) {
            binding.issue.visibility = View.GONE
        } else {
            binding.uploadData.visibility = View.GONE
            binding.issue.text = "# ${data.issue}"
        }


        holder.binding.root.autoDisposeScope.launch {
            tryAdaptUnmatchedItems(holder)
        }

        val app = App.getAppInfoFromPackageName(data.app)

        if (app != null) {
            binding.app.text = app[0] as String
            binding.image.setImageDrawable(app[1] as Drawable)
        }else{
            binding.app.text = ""
            binding.image.setImageResource(R.drawable.data_sms)
        }

        binding.time.text = DateUtils.getTime(data.time)
        binding.rule.visibility = View.VISIBLE
        if (!data.match) {
            binding.rule.visibility = View.GONE
            binding.uploadData.setIconResource(R.drawable.icon_upload)
        } else {
            binding.uploadData.setIconResource(R.drawable.icon_question)
            binding.groupCard.setCardBackgroundColor(SurfaceColors.SURFACE_5.getColor(activity))
        }
        val rule = data.rule
        val regex = "\\[(.*?)]".toRegex()
        val matchResult = regex.find(rule)
        if (matchResult != null) {
            val (value) = matchResult.destructured
            binding.rule.text = value
        } else {
            binding.rule.text = data.rule
        }
    }
}
