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
 *//*


package net.ankio.auto.ui.adapter

import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.databinding.AdapterMapBinding
import net.ankio.auto.storage.ImageUtils
import net.ankio.auto.utils.server.model.Assets
import net.ankio.auto.utils.server.model.AssetsMap

class MapAdapter(
    override val dataItems: List<AssetsMap>,
    private val onClick: (adapter: MapAdapter, item: AssetsMap, pos: Int) -> Unit,
    private val onLongClick: (adapter: MapAdapter, item: AssetsMap, pos: Int) -> Unit,
) : BaseAdapter(dataItems, AdapterMapBinding::class.java) {
    override fun onBindView(
        holder: BaseViewHolder,
        item: Any,
    ) {
        val binding = holder.binding as AdapterMapBinding
        val it = item as AssetsMap
        val context = holder.context
        // 图片加载丢到IO线程
        holder.scope.launch {
            Assets.getDrawable(it.mapName, context).let { drawable ->
                binding.target.setIcon(drawable)
            }
        }

        binding.raw.text = item.name
        binding.target.setText(item.mapName)
    }

    override fun onInitView(holder: BaseViewHolder) {
        val binding = holder.binding as AdapterMapBinding

        // 单击编辑
        binding.item.setOnClickListener {
            val item = holder.item as AssetsMap
            onClick(this@MapAdapter, item, getHolderIndex(holder))
        }
        // 长按删除
        binding.item.setOnLongClickListener {
            val item = holder.item as AssetsMap
            onLongClick(this@MapAdapter, item, getHolderIndex(holder))
            true
        }
    }
}
*/
