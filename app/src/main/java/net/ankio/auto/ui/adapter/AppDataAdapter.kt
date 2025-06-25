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
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.google.android.material.elevation.SurfaceColors
import com.google.gson.Gson
import kotlinx.coroutines.launch
import net.ankio.auto.App
import net.ankio.auto.R
import net.ankio.auto.databinding.AdapterDataBinding
import net.ankio.auto.http.api.JsAPI
import net.ankio.auto.intent.IntentType
import net.ankio.auto.service.CoreService
import net.ankio.auto.storage.ConfigUtils
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseActivity
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import net.ankio.auto.ui.dialog.BottomSheetDialogBuilder
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.utils.DateUtils
import org.ezbook.server.constant.DefaultData
import org.ezbook.server.constant.Setting
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

        // TODO 上传

        binding.uploadData.setOnClickListener {

        }

        binding.root.setOnLongClickListener {

            true
        }

        binding.groupCard.setCardBackgroundColor(SurfaceColors.SURFACE_1.getColor(activity))
        // binding.content.setBackgroundColor(SurfaceColors.SURFACE_3.getColor(activity))

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
                R.string.ai_loading,
                ConfigUtils.getString(Setting.AI_MODEL)
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
        } else {
            binding.ruleName.visibility = View.VISIBLE
            binding.uploadData.setIconResource(R.drawable.icon_question)
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
}
