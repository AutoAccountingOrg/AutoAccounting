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

import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.databinding.AdapterAssetsBinding
import net.ankio.auto.ui.viewModes.BaseViewModel
import net.ankio.auto.utils.ImageUtils
import net.ankio.auto.utils.server.model.Assets

class AssetsSelectorAdapter(
    viewModel: BaseViewModel
    private val onClick: (item: Assets) -> Unit,
) : BaseAdapter<AdapterAssetsBinding>() {
    override fun onInitView(holder: BaseViewHolder) {
        (holder.binding as AdapterAssetsBinding).assets.setOnClickListener {
            onClick(holder.item as Assets)
        }
    }

    override fun onBindView(
        holder: BaseViewHolder,
        item: Any,
    ) {
        val it = item as Assets
        val binding = (holder.binding as AdapterAssetsBinding)
        holder.scope.launch {
            ImageUtils.get(holder.context, it.icon, R.drawable.default_cate).let {
                binding.assets.setIcon(it)
            }
        }

        binding.assets.setText(it.name)
    }
}
