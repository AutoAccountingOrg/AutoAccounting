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
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.google.android.material.elevation.SurfaceColors
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import net.ankio.auto.App
import net.ankio.auto.R
import net.ankio.auto.databinding.AdapterDataBinding
import net.ankio.auto.http.Pastebin
import net.ankio.auto.http.api.JsAPI
import net.ankio.auto.http.license.RuleAPI
import net.ankio.auto.intent.IntentType
import net.ankio.auto.service.CoreService
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseActivity
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import net.ankio.auto.ui.dialog.BottomSheetDialogBuilder
import net.ankio.auto.ui.dialog.DataEditorDialog
import net.ankio.auto.ui.dialog.EditorDialogBuilder
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.utils.CustomTabsHelper
import net.ankio.auto.utils.DateUtils
import net.ankio.auto.utils.PrefManager
import org.ezbook.server.db.model.AppDataModel

class AppDataAdapter(
    private val activity: BaseActivity
) : BaseAdapter<AdapterDataBinding, AppDataModel>() {

    override fun onInitViewHolder(holder: BaseViewHolder<AdapterDataBinding, AppDataModel>) {
        val binding = holder.binding


        binding.testRule.setOnClickListener {
            activity.lifecycleScope.launch {
                onTestRuleClick(it, holder.item)
            }
        }

        binding.content.setOnClickListener {
            onContentClick(it, holder.item!!)
        }

        binding.createRule.setOnClickListener {
            onCreateRule(it, holder.item!!)
        }


        binding.uploadData.setOnClickListener {
            val item = holder.item!!
            // AI生成的不支持上传
            if (item.rule.endsWith("生成")) {
                ToastUtils.error(R.string.ai_not_support)
                return@setOnClickListener
            }
            DataEditorDialog(activity, item.data) { result ->

                activity.lifecycleScope.launch {
                    val loading = LoadingUtils(activity)
                    loading.show(R.string.upload_waiting)
                    if (!item.match) {
                        requestRuleHelp(item, result)
                    } else {
                        EditorDialogBuilder(activity)
                            .setTitleInt(R.string.rule_issue)
                            .setMessage("")
                            .setEditorPositiveButton(R.string.btn_confirm) {
                                activity.lifecycleScope.launch {
                                    requestRuleBug(item, result, it)
                                }
                            }.setPositiveButton(R.string.btn_cancel) { _, _ ->

                            }.show()

                    }
                    loading.close()
                }

            }.show()
        }

        binding.root.setOnLongClickListener {
            val item = holder.item!!
            BottomSheetDialogBuilder(activity)
                .setTitle(activity.getString(R.string.delete_title))
                .setMessage(activity.getString(R.string.delete_data_message))
                .setPositiveButton(activity.getString(R.string.btn_confirm)) { _, _ ->
                    removeItem(item)
                    activity.lifecycleScope.launch {
                        AppDataModel.delete(item.id)
                    }
                }
                .setNegativeButton(activity.getString(R.string.btn_cancel)) { _, _ -> }
                .show()
            true
        }

        binding.groupCard.setCardBackgroundColor(SurfaceColors.SURFACE_1.getColor(activity))
        // binding.content.setBackgroundColor(SurfaceColors.SURFACE_3.getColor(activity))

    }

    private fun onCreateRule(it: View?, item: AppDataModel) {
        //TODO 携带数据到规则创建页面
    }


    private fun onContentClick(view: View, item: AppDataModel) {
        BottomSheetDialogBuilder(activity)
            .setTitle(activity.getString(R.string.content_title))
            .setMessage(item.data)
            .setNegativeButton(activity.getString(R.string.cancel_msg)) { _, _ -> }
            .setPositiveButton(activity.getString(R.string.copy)) { _, _ ->
                App.copyToClipboard(item.data)
                ToastUtils.info(R.string.copy_command_success)
            }
            .show()
    }


    private suspend fun onTestRuleClick(view: View, item: AppDataModel?) {

        if (item == null) return

        val loadingUtils = LoadingUtils(activity)
        loadingUtils.show(
            activity.getString(
                R.string.ai_loading
            )
        )

        val billResultModel = JsAPI.analysis(item.type, item.data, item.app, true)

        if (billResultModel?.billInfoModel == null) {
            ToastUtils.error(R.string.no_rule_hint)
            loadingUtils.close()
            return
        }

        val targetIntent = Intent(activity, CoreService::class.java).apply {
            putExtra("parent", "")
            putExtra("billInfo", Gson().toJson(billResultModel!!.billInfoModel))
            putExtra("showWaitTip", false)
            putExtra("from", "AppData")
            putExtra("intentType", IntentType.FloatingIntent.name)
        }
        try {
            activity.startForegroundService(targetIntent)
        } catch (e: Exception) {
            Logger.e("Failed to start service: ${e.message}", e)
        } finally {
            loadingUtils.close()
        }
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder<AdapterDataBinding, AppDataModel>,
        data: AppDataModel,
        position: Int
    ) {
        val binding = holder.binding
        binding.content.text = data.data
        binding.uploadData.visibility = View.VISIBLE

        binding.time.setText(DateUtils.stampToDate(data.time))

        if (!data.match || data.rule.isEmpty()) {
            binding.ruleName.visibility = View.INVISIBLE
            binding.uploadData.setIconResource(R.drawable.icon_upload)
            binding.createRule.visibility = View.VISIBLE
        } else {
            binding.ruleName.visibility = View.VISIBLE
            binding.uploadData.setIconResource(R.drawable.icon_question)
            binding.createRule.visibility = if (data.rule.contains("生成")) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
        val rule = data.rule

        val regex = "\\[(.*?)]".toRegex()
        val matchResult = regex.find(rule)
        if (matchResult != null) {
            val (value) = matchResult.destructured
            binding.ruleName.setText(value)
        } else {
            binding.ruleName.setText(data.rule)
        }

    }


    override fun areItemsSame(oldItem: AppDataModel, newItem: AppDataModel): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsSame(oldItem: AppDataModel, newItem: AppDataModel): Boolean {
        return oldItem == newItem
    }


    suspend fun requestRuleHelp(item: AppDataModel, result: String) {
        val type = item.type.name
        val title = "[Adaptation Request][$type]${item.app}"
        runCatching {
            val (url, timeout) = Pastebin.add(result)
            val body = """
<!------ 
 1. 请不要手动复制数据，下面的链接中已包含数据；
 2. 您可以新增信息，但是不要删除本页任何内容；
 3. 一般情况下，您直接划到底部点击submit即可。
 ------>                        
## 数据链接                        
[数据过期时间：${timeout}](${url})
## 其他信息
<!------ 
 1. 您可以在下面添加说明信息。
 ------>  

                """.trimIndent()

            //上传Github Or 使用API
            if (PrefManager.githubConnectivity) {
                submitGithub(title, body)
            } else {
                submitCloud(title, body)
            }

        }.onFailure {
            Logger.e(it.message ?: "", it)
            ToastUtils.error(it.message ?: "Error in post issue")
        }
    }

    suspend fun requestRuleBug(item: AppDataModel, result: String, desc: String) {
        val type = item.type.name
        val title = "[Bug][Rule][$type]${item.app}"
        runCatching {
            val (url, timeout) = Pastebin.add(result)
            val body = """
<!------ 
 1. 请不要手动复制数据，下面的链接中已包含数据；
 2. 该功能是反馈规则识别错误的，请勿写其他无关内容；
 ------>  
## 规则
${item.rule}
## 数据
[数据过期时间：${timeout}](${url})
## 说明
${desc}

                         
                                            """.trimIndent()

            //上传Github Or 使用API
            if (PrefManager.githubConnectivity) {
                submitGithub(title, body)
            } else {
                submitCloud(title, body)
            }

        }.onFailure {
            Logger.e(it.message ?: "", it)
            ToastUtils.error(it.message ?: "Error in post issue")
        }
    }

    // Github可访问的话，直接通过Github提交

    fun submitGithub(title: String, body: String) {
        val uri = "https://github.com/AutoAccountingOrg/AutoRuleSubmit/issues"
        CustomTabsHelper.launchUrl(
            activity,
            "$uri/new?title=${Uri.encode(title)}&body=${Uri.encode(body)}".toUri(),
        )
    }


    // Github不可访问的话，直接通过云端代理提交

    suspend fun submitCloud(title: String, body: String) {
        val result = RuleAPI.submit(title, body)
        val data = Gson().fromJson(result, JsonObject::class.java)
        if (data.get("code").asInt != 200) {
            throw RuntimeException(data.get("msg").asString)
        }
    }
}
