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

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.ankio.auto.database.table.Assets
import net.ankio.auto.database.table.AssetsMap
import net.ankio.auto.databinding.AdapterMapBinding

class MapAdapter(
    private val dataItems: List<AssetsMap>,
    private val onClick: (adapter: MapAdapter, item: AssetsMap, pos: Int) -> Unit,
    private val onLongClick: (adapter: MapAdapter, item: AssetsMap, pos: Int) -> Unit,
) : BaseAdapter<MapAdapter.ViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        return ViewHolder(
            AdapterMapBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            ),
            parent.context,
        )
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        val item = dataItems[position]
        holder.bind(item, position)
    }

    override fun getItemCount(): Int {
        return dataItems.size
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.cancel()
    }

    inner class ViewHolder(
        private val binding: AdapterMapBinding,
        private val context: Context,
    ) :
        RecyclerView.ViewHolder(binding.root) {
        private val job = Job()

        private val scope = CoroutineScope(Dispatchers.Main + job)

        fun cancel() {
            job.cancel()
        }

        fun bind(
            item: AssetsMap,
            pos: Int,
        ) {
            // 图片加载丢到IO线程
            scope.launch {
                Assets.getDrawable(item.mapName, context).let { drawable ->
                    binding.target.setIcon(drawable)
                }
            }

            binding.raw.text = item.name
            binding.target.setText(item.mapName)
            // 单击编辑
            binding.item.setOnClickListener {
                onClick(this@MapAdapter, item, pos)
            }
            // 长按删除
            binding.item.setOnLongClickListener {
                onLongClick(this@MapAdapter, item, pos)
                true
            }
        }
    }
}
