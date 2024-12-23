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
import com.google.android.material.elevation.SurfaceColors
import net.ankio.auto.R
import net.ankio.auto.databinding.AdapterDataBinding
import net.ankio.auto.storage.ConfigUtils
import net.ankio.auto.ui.api.BaseActivity
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import net.ankio.auto.utils.DateUtils
import org.ezbook.server.constant.DefaultData
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.model.AppDataModel

class AppDataAdapter(
    private val list: MutableList<AppDataModel>,
    private val activity: BaseActivity
) : BaseAdapter<AdapterDataBinding, AppDataModel>(AdapterDataBinding::class.java, list) {


    private val version = ConfigUtils.getString(Setting.RULE_VERSION, "0")


    override fun onInitViewHolder(holder: BaseViewHolder<AdapterDataBinding, AppDataModel>) {
        val binding = holder.binding

        binding.testRuleAi.setOnClickListener {
            onItemTestRuleAiClick(holder.item!!)
        }

        binding.testRule.setOnClickListener {
            onItemTestRuleClick(holder.item!!)

        }

        binding.content.setOnClickListener {
            onContentClick(holder.item!!)
        }

        binding.uploadData.setOnClickListener {
            onItemUploadClick(holder.item!!)
        }

        binding.root.setOnLongClickListener {
            onItemLongClick(holder.item!!)
            true
        }

        binding.groupCard.setCardBackgroundColor(SurfaceColors.SURFACE_1.getColor(activity))
        binding.content.setBackgroundColor(SurfaceColors.SURFACE_3.getColor(activity))
        binding.edit.setOnClickListener {
            onItemEditClick(holder.item!!)
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

        binding.testRuleAi.visibility =
            if (ConfigUtils.getBoolean(
                    Setting.USE_AI,
                    DefaultData.USE_AI
                )
            ) View.VISIBLE else View.GONE


        binding.time.setText(DateUtils.stampToDate(data.time))

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

        binding.edit.visibility = View.GONE
    }

    private var onItemLongClick: (AppDataModel) -> Unit = {}
    fun setOnLongClick(onLongClick: (AppDataModel) -> Unit): AppDataAdapter {
        onItemLongClick = onLongClick
        return this
    }

    private var onItemEditClick: (AppDataModel) -> Unit = {}
    fun setOnEditClick(onEditClick: (AppDataModel) -> Unit): AppDataAdapter {
        onItemEditClick = onEditClick
        return this
    }

    private var onItemUploadClick: (AppDataModel) -> Unit = {}
    fun setOnUploadClick(onUploadClick: (AppDataModel) -> Unit): AppDataAdapter {
        onItemUploadClick = onUploadClick
        return this
    }

    private var onItemTestRuleClick: (AppDataModel) -> Unit = {}
    fun setOnTestRuleClick(onTestRuleClick: (AppDataModel) -> Unit): AppDataAdapter {
        onItemTestRuleClick = onTestRuleClick
        return this
    }

    private var onItemTestRuleAiClick: (AppDataModel) -> Unit = {}
    fun setOnTestRuleAiClick(onTestRuleAiClick: (AppDataModel) -> Unit): AppDataAdapter {
        onItemTestRuleAiClick = onTestRuleAiClick
        return this
    }

    private var onContentClick: (AppDataModel) -> Unit = {}
    fun setOnContentClick(onContentClick: (AppDataModel) -> Unit): AppDataAdapter {
        this.onContentClick = onContentClick
        return this
    }
}
