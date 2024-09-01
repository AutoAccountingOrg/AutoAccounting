/*
 * Copyright (C) 2024 ankio(ankio@ankio.net)
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
import kotlinx.coroutines.launch
import net.ankio.auto.databinding.AdapterDataRuleBinding
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import net.ankio.auto.ui.scope.autoDisposeScope
import net.ankio.auto.ui.utils.ToastUtils
import org.ezbook.server.db.model.RuleModel

class DataRuleAdapter(private val list: MutableList<RuleModel>):BaseAdapter<AdapterDataRuleBinding,RuleModel>(AdapterDataRuleBinding::class.java,list) {
    override fun onInitViewHolder(holder: BaseViewHolder<AdapterDataRuleBinding,RuleModel>) {
        val binding = holder.binding
        binding.enable.setOnCheckedChangeListener { buttonView, isChecked ->
            val item = holder.item!!
            item.enabled = isChecked
           binding.root.autoDisposeScope.launch {
               RuleModel.update(item)
           }
        }
        binding.autoRecord.setOnCheckedChangeListener { buttonView, isChecked ->
            val item = holder.item!!
            item.autoRecord = isChecked
            binding.root.autoDisposeScope.launch {
                RuleModel.update(item)
            }
        }

        // TODO 本地规则编辑

        binding.editRule.setOnClickListener {
            val item = holder.item!!
            ToastUtils.info("敬请期待")
        }
        binding.deleteData.setOnClickListener {
            val item = holder.item!!
            ToastUtils.info("敬请期待")
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder<AdapterDataRuleBinding,RuleModel>,data:RuleModel, position: Int) {



        val system = data.creator == "system"

        holder.binding.ruleName.text = data.name
        holder.binding.deleteData.visibility = if (system) View.GONE else View.VISIBLE
        holder.binding.editRule.visibility = if (system) View.GONE else View.VISIBLE

        holder.binding.icon.visibility = if (system) View.VISIBLE else View.GONE

        holder.binding.enable.isChecked = data.enabled

        holder.binding.autoRecord.isChecked = data.autoRecord
    }


}