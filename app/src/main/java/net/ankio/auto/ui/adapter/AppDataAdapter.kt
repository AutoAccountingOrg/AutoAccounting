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

import android.app.Activity
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
import net.ankio.auto.R
import net.ankio.auto.app.BillUtils
import net.ankio.auto.app.js.Engine
import net.ankio.auto.constant.DataType
import net.ankio.auto.constant.toDataType
import net.ankio.auto.databinding.AdapterDataBinding
import net.ankio.auto.databinding.SettingItemInputBinding
import net.ankio.auto.ui.dialog.DataEditorDialog
import net.ankio.auto.ui.dialog.FloatEditorDialog
import net.ankio.auto.ui.scope.autoDisposeScope
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.ui.viewModes.AppDataViewModel
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.CustomTabsHelper
import net.ankio.auto.utils.DateUtils
import net.ankio.auto.utils.Github
import net.ankio.auto.utils.SpUtils
import net.ankio.auto.utils.server.model.AppDataModel

class AppDataAdapter(
    private val activity: Activity,
    private val viewModel: AppDataViewModel,
) : BaseAdapter<AdapterDataBinding,AppDataModel>(viewModel) {


    override fun onInitView(holder: BaseViewHolder<AdapterDataBinding,AppDataModel>) {
    
        val binding = holder.binding

        binding.issue.setOnClickListener {
            val item = holder.item!!
            CustomTabsHelper.launchUrl(
                AppUtils.getApplication(),
                Uri.parse(
                    if (item.match == 1) "https://github.com/AutoAccountingOrg/AutoAccounting/issues/${item.issue}" else "https://github.com/AutoAccountingOrg/AutoRule/issues/${item.issue}",
                ),
            )
        }

        binding.testRule.setOnClickListener {
            val item = holder.item!!
            holder.binding.root.autoDisposeScope.launch {
                val result = Engine.analyze(item.type, item.source, item.data, false)
                if (result == null) {
                    // 弹出悬浮窗
                    Toaster.show(R.string.no_match)
                } else {
                    val tpl = SpUtils.getString("setting_bill_remark", "【商户名称】 - 【商品名称】")
                    result.remark = BillUtils.getRemark(result, tpl)
                    BillUtils.setAccountMap(result)
                    AppUtils.getService().config().let {
                        FloatEditorDialog(activity, result, it).show(float = false)
                    }
                }
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
            val item = holder.item as AppDataModel
            val position = getHolderIndex(holder)
            if (item.issue != 0) {
                Toaster.show(activity.getString(R.string.repeater_issue))
                return@setOnClickListener
            }

            val builder =
                MaterialAlertDialogBuilder(activity)
                    .setTitle(if (item.match == 1) activity.getString(R.string.data_question) else activity.getString(R.string.upload_sure)) // 设置对话框的标题

            var settingItemInputBinding: SettingItemInputBinding? = null

            if (item.match == 0) {
                builder.setMessage(activity.getString(R.string.upload_info))
            } else {
                settingItemInputBinding = SettingItemInputBinding.inflate(LayoutInflater.from(activity))
                settingItemInputBinding.inputLayout.hint = activity.getString(R.string.data_question_info)
                builder.setView(settingItemInputBinding.root)
            }
            builder.setPositiveButton(activity.getString(R.string.ok)) { dialog, which ->
                var text = ""
                if (settingItemInputBinding != null) {
                    text = settingItemInputBinding.input.text.toString()
                }
                val uploadData = AppUtils.toPrettyFormat(item.data)
                DataEditorDialog(activity, uploadData) { data ->
                    val type =
                        when (item.type.toDataType()) {
                            DataType.App -> "App"
                            DataType.Helper -> "Helper"
                            DataType.Notice -> "Notice"
                            DataType.Sms -> "Sms"
                        }
                    val loadingUtils = LoadingUtils(activity)
                    loadingUtils.show(R.string.upload_waiting)
                    holder.binding.root.autoDisposeScope.launch {
                        runCatching {
                            val title =
                                if (item.match == 0) {
                                    "[Adaptation Request][$type]${item.source}"
                                } else {
                                    "[Bug][Rule][$type]${item.source}"
                                }
                            val msg =
                                if (item.match == 0) {
                                    """
```
                $data
```
                                            """.trimIndent()
                                } else {
                                    """
## 规则
${item.rule}
## 说明
$text
## 数据
```
$data
```
                                            """.trimIndent()
                                }
                            val issue =
                                Github.createIssue(
                                    title,
                                    msg,
                                    if (item.match == 0) "AutoRule" else "AutoAccounting",
                                )
                            item.issue = issue.toInt()
                            withContext(Dispatchers.Main) {
                                loadingUtils.close()
                                viewModel.dataList.value?.set(position,item)
                                Toaster.show(
                                    if (item.match == 0) {
                                        activity.getString(
                                            R.string.upload_success,
                                        )
                                    } else {
                                        activity.getString(R.string.question_success)
                                    },
                                )
                            }
                            AppDataModel.put(item)
                        }.onFailure {
                            withContext(Dispatchers.Main) {
                                Toaster.show(it.message)
                                CustomTabsHelper.launchUrl(
                                    activity,
                                    Uri.parse(Github.getLoginUrl()),
                                )
                                loadingUtils.close()
                            }
                        }
                    }
                    dialog.dismiss()
                }.show(false)
            }
                .setNegativeButton(R.string.close) { dialog, which ->
                    // 在取消按钮被点击时执行的操作
                    dialog.dismiss()
                }
                .show()
        }

        binding.root.setOnLongClickListener {
            val index  = getHolderIndex(holder)
            MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.delete_title)
                .setMessage(R.string.delete_data_message)
                .setPositiveButton(R.string.sure_msg) { _, _ ->
                    viewModel.dataList.value?.removeAt(index)
                }
                .setNegativeButton(R.string.cancel_msg) { _, _ -> }
                .show()
            true
        }
    }

    override fun getViewBindingClazz(): Class<AdapterDataBinding> {
        return AdapterDataBinding::class.java
    }

    override fun onBindView(
        holder: BaseViewHolder<AdapterDataBinding,AppDataModel>,
        item: Any,
    ) {
        val binding = holder.binding
        val appData = item as AppDataModel

        binding.groupCard.setCardBackgroundColor(SurfaceColors.SURFACE_1.getColor(activity))
        binding.content.setBackgroundColor(SurfaceColors.SURFACE_3.getColor(activity))
        // 格式化数据
        val prettyJson: String = AppUtils.toPrettyFormat(appData.data)

        binding.content.text = prettyJson
        when (appData.type.toDataType()) {
            DataType.Notice -> {
                binding.type.setColorFilter(ContextCompat.getColor(activity, R.color.warning))
                binding.type.setImageResource(R.drawable.data_notice)
            }

            DataType.Helper -> {
                binding.type.setColorFilter(ContextCompat.getColor(activity, R.color.danger))
                binding.type.setImageResource(R.drawable.data_helper)
            }

            DataType.Sms -> {
                binding.type.setColorFilter(ContextCompat.getColor(activity, R.color.info))
                binding.type.setImageResource(R.drawable.data_sms)
            }

            DataType.App -> {
                binding.type.setColorFilter(ContextCompat.getColor(activity, R.color.success))
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
        holder.binding.root.autoDisposeScope.launch {
            tryAdaptUnmatchedItems(holder)
        }

        val app = AppUtils.getAppInfoFromPackageName(item.source, activity)

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
                DateUtils.getTime(it )
            }
        binding.rule.visibility = View.VISIBLE
        if (item.match == 0) {
            binding.rule.visibility = View.GONE

            binding.uploadData.setIconResource(R.drawable.icon_upload)
        } else {
            binding.uploadData.setIconResource(R.drawable.icon_question)
            binding.groupCard.setCardBackgroundColor(SurfaceColors.SURFACE_5.getColor(activity))
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

    private val hashMap = HashMap<AppDataModel, Long>()

    private suspend fun tryAdaptUnmatchedItems(
        holder: BaseViewHolder<AdapterDataBinding,AppDataModel>
    ) = withContext(Dispatchers.IO) {
        val item = holder.item as AppDataModel
        if (item.match == 0) {
            val t = System.currentTimeMillis() / 1000
            if (hashMap.containsKey(item) && t - hashMap[item]!! < 30) { // 30秒内不重复匹配
                return@withContext
            }
            hashMap[item] = t
            val result = Engine.analyze(item.type, item.source, item.data,false)
            if (result != null) {
                item.rule = result.channel
                item.match = 1
                withContext(Dispatchers.Main) {
                    val index = getHolderIndex(holder)
                    viewModel.dataList.value?.set(index,item)
                }
                AppDataModel.put(item)
            }
        }
    }
}
