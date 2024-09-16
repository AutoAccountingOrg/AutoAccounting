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

import android.content.Intent
import android.net.Uri
import android.view.View
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.elevation.SurfaceColors
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.App
import net.ankio.auto.R
import net.ankio.auto.databinding.AdapterDataBinding
import net.ankio.auto.service.FloatingWindowService
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseActivity
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import net.ankio.auto.ui.dialog.DataEditorDialog
import net.ankio.auto.ui.dialog.DataIssueDialog
import net.ankio.auto.ui.scope.autoDisposeScope
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.utils.CustomTabsHelper
import net.ankio.auto.utils.DateUtils
import net.ankio.auto.utils.Github
import org.ezbook.server.Server
import org.ezbook.server.db.model.AppDataModel
import org.ezbook.server.db.model.BillInfoModel

class AppDataAdapter(
    private val list: MutableList<AppDataModel>,
    private val activity: BaseActivity
) : BaseAdapter<AdapterDataBinding, AppDataModel>(AdapterDataBinding::class.java, list) {


    private val hashMap = HashMap<AppDataModel, Long>()

    private suspend fun tryAdaptUnmatchedItems(
        holder: BaseViewHolder<AdapterDataBinding, AppDataModel>
    ) = withContext(Dispatchers.IO) {
        val item = holder.item!!

        Logger.d("tryAdaptUnmatchedItems: $item")

        val t = System.currentTimeMillis() / 1000
        if (hashMap.containsKey(item) && t - hashMap[item]!! < 60) { // 30秒内不重复匹配
            return@withContext
        }
        hashMap[item] = t

        val billModel = testRule(item) ?: return@withContext

        item.match = true
        item.rule = billModel.ruleName

        Logger.d("tryAdaptUnmatchedItems Update: $item")
        item.issue = 0

        val position = indexOf(item)

        AppDataModel.put(item)
        withContext(Dispatchers.Main) {
            list[position] = item
            notifyItemChanged(position)
        }
    }

    private suspend fun testRule(item: AppDataModel): BillInfoModel? = withContext(Dispatchers.IO) {
        val result = Server.request(
            "js/analysis?type=${item.type.name}&app=${item.app}&fromAppData=true",
            item.data
        )
            ?: return@withContext null
        Logger.d("testRule: $result")
        val data = Gson().fromJson(result, JsonObject::class.java)
        if (data.get("code").asInt != 200) {
            Logger.e("testRule: ${data.get("msg").asString}")
            return@withContext null
        }
        return@withContext Gson().fromJson(data.getAsJsonObject("data"), BillInfoModel::class.java)
    }


    private fun buildUploadDialog(holder: BaseViewHolder<AdapterDataBinding, AppDataModel>) {
        val item = holder.item!!
        DataEditorDialog(activity, item.data) {
            val loading = LoadingUtils(activity)
            loading.show(R.string.upload_waiting)
            holder.binding.root.autoDisposeScope.launch {
                val type = item.type.name
                val title = "[Adaptation Request][$type]${item.app}"
                val body = """
```
${item.data}
```
                """.trimIndent()


                runCatching {
                    item.issue = Github.createIssue(
                        title,
                        body,
                        "AutoRule",
                    )
                }.onFailure {
                    withContext(Dispatchers.Main) {
                        ToastUtils.error(it.message!!)
                        CustomTabsHelper.launchUrl(
                            activity,
                            Uri.parse(Github.getLoginUrl()),
                        )
                        loading.close()
                    }
                }.onSuccess {
                    withContext(Dispatchers.Main) {
                        loading.close()
                        val position = indexOf(item)
                        list[position] = item
                        AppDataModel.put(item)
                        notifyItemChanged(position)
                        ToastUtils.info(R.string.upload_success)
                    }
                }


            }
        }.show(float = false)
    }

    private fun buildIssueDialog(holder: BaseViewHolder<AdapterDataBinding, AppDataModel>) {
        val item = holder.item!!

        DataIssueDialog(activity) { issue ->
            DataEditorDialog(activity, item.data) {
                val loading = LoadingUtils(activity)
                loading.show(R.string.upload_waiting)
                holder.binding.root.autoDisposeScope.launch {
                    val type = item.type.name
                    val title = "[Bug][Rule][$type]${item.app}"
                    val body = """
## 规则
${item.rule}
## 说明
$issue
## 数据
```
${item.data}
```
                         
                                            """.trimIndent()


                    runCatching {
                        item.issue = Github.createIssue(
                            title,
                            body,
                            "AutoAccounting",
                        )
                    }.onFailure {
                        withContext(Dispatchers.Main) {
                            ToastUtils.error(it.message!!)
                            CustomTabsHelper.launchUrl(
                                activity,
                                Uri.parse(Github.getLoginUrl()),
                            )
                            loading.close()
                        }
                        return@launch
                    }.onSuccess {
                        withContext(Dispatchers.Main) {
                            loading.close()
                            val position = indexOf(item)
                            list[position] = item
                            AppDataModel.put(item)
                            notifyItemChanged(position)
                            ToastUtils.info(R.string.upload_success_issue)
                        }
                    }




                }
            }.show(float = false)

        }.show(float = false)


    }

    override fun onInitViewHolder(holder: BaseViewHolder<AdapterDataBinding, AppDataModel>) {
        val binding = holder.binding
        binding.issue.setOnClickListener {
            val item = holder.item!!
            CustomTabsHelper.launchUrl(
                activity,
                Uri.parse(
                    if (item.match) "https://github.com/AutoAccountingOrg/AutoAccounting/issues/${item.issue}" else "https://github.com/AutoAccountingOrg/AutoRule/issues/${item.issue}",

                    )
            )
        }


        binding.testRule.setOnClickListener {
            val item = holder.item!!
            holder.binding.root.autoDisposeScope.launch {
                val billModel = testRule(item)
                if (billModel == null) {
                    ToastUtils.error(R.string.no_rule_hint)
                } else {
                    val serviceIntent =
                        Intent(activity, FloatingWindowService::class.java).apply {
                            putExtra("parent", "")
                            putExtra("billInfo", Gson().toJson(billModel))
                            putExtra("showWaitTip", false)
                        }
                    activity.startService(serviceIntent)
                }
            }

        }

        binding.content.setOnClickListener {
            MaterialAlertDialogBuilder(activity)
                .setTitle(activity.getString(R.string.content_title))
                .setMessage(binding.content.text as String)
                .setPositiveButton(activity.getString(R.string.cancel_msg)) { _, _ -> }
                .setNegativeButton(activity.getString(R.string.copy)) { _, _ ->
                    App.copyToClipboard(binding.content.text as String)
                    ToastUtils.error(R.string.copy_command_success)
                }
                .show()
        }

        binding.uploadData.setOnClickListener {
            val item = holder.item!!
            if (item.issue != 0) {
                ToastUtils.error(holder.context.getString(R.string.repeater_issue))
                return@setOnClickListener
            }

            if (!item.match) {
                buildUploadDialog(holder)
                return@setOnClickListener
            }
            buildIssueDialog(holder)

        }

        binding.root.setOnLongClickListener {
            MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.delete_title)
                .setMessage(R.string.delete_data_message)
                .setPositiveButton(R.string.sure_msg) { _, _ ->
                    val item = holder.item!!
                    val position = indexOf(item)
                    list.removeAt(position)
                    notifyItemRemoved(position)
                    binding.root.autoDisposeScope.launch {
                        AppDataModel.delete(item.id)
                    }
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

        if (!data.match || data.rule.isEmpty()) {
            holder.binding.root.autoDisposeScope.launch {
                tryAdaptUnmatchedItems(holder)
            }
        }


        binding.time.setText(DateUtils.getTime(data.time))

        if (!data.match || data.rule.isEmpty()) {
            binding.rule.visibility = View.GONE
            binding.uploadData.setIconResource(R.drawable.icon_upload)
        } else {
            binding.rule.visibility = View.VISIBLE
            binding.uploadData.setIconResource(R.drawable.icon_question)
        }
        val rule = data.rule

        val regex = "\\[(.*?)]".toRegex()
        val matchResult = regex.find(rule)
        if (matchResult != null) {
            val (value) = matchResult.destructured
            binding.rule.setText(value)
        } else {
            binding.rule.setText(data.rule)
        }
    }
}
