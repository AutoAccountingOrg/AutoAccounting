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
import net.ankio.auto.database.table.BookName
import net.ankio.auto.databinding.AdapterBookBinding
import net.ankio.auto.utils.ImageUtils

class BookSelectorAdapter(
    override val dataItems: List<BookName>,
    private val onClick: (item: BookName) -> Unit,
) : BaseAdapter(dataItems, AdapterBookBinding::class.java) {
    override fun onBindView(
        holder: BaseViewHolder,
        item: Any,
    ) {
        val it = item as BookName
        val binding = (holder.binding as AdapterBookBinding)
        holder.scope.launch {
            ImageUtils.get(holder.context, item.icon, R.drawable.default_book).let {
                binding.book.background = it
            }
        }
        binding.itemValue.text = item.name
    }

    override fun onInitView(holder: BaseViewHolder) {
        (holder.binding as AdapterBookBinding).book.setOnClickListener {
            onClick(holder.item as BookName)
        }
    }
}
