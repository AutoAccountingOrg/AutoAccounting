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
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.database.table.Assets
import net.ankio.auto.databinding.AdapterAssetsBinding
import net.ankio.auto.utils.ImageUtils
import net.ankio.auto.utils.Logger


class AssetsSelectorAdapter(
    private val dataItems: List<Assets>,
    private val onClick: (item: Assets) -> Unit
) : RecyclerView.Adapter<AssetsSelectorAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            AdapterAssetsBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ), parent.context
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataItems[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int {
        return dataItems.size
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.cancel()
    }

    inner class ViewHolder(
        private val binding: AdapterAssetsBinding,
        private val context: Context
    ) :
        RecyclerView.ViewHolder(binding.root) {


            private val job = Job()

            private val scope = CoroutineScope(Dispatchers.IO + job)

         fun cancel() {
             job.cancel()
         }
        fun bind(item: Assets) {

            //图片加载丢到IO线程

            scope.launch {
                ImageUtils.get(context, item.icon, { drawable ->
                    binding.assets.setIcon(drawable)
                }, { error ->
                    Logger.w("加载图片失败：$error")
                    binding.assets.setIcon(
                        ResourcesCompat.getDrawable(
                            context.resources,
                            R.drawable.default_cate,
                            context.theme
                        )
                    )

                })
            }



            binding.assets.setText(item.name)
            binding.assets.setOnClickListener {
                onClick(item)
            }
        }
    }
}
