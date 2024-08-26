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
import net.ankio.auto.databinding.AdapterDataRuleBinding
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import org.ezbook.server.db.model.RuleModel

class DataRuleAdapter(list: MutableList<RuleModel>):BaseAdapter<AdapterDataRuleBinding,RuleModel>(AdapterDataRuleBinding::class.java,list) {
    override fun onInitViewHolder(holder: BaseViewHolder<AdapterDataRuleBinding>) {
        TODO("Not yet implemented")
    }

    override fun onBindViewHolder(holder: BaseViewHolder<AdapterDataRuleBinding>, position: Int) {
        TODO("Not yet implemented")
    }


}