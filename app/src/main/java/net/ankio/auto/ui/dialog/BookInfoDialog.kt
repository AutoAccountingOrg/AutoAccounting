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

package net.ankio.auto.ui.dialog

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.database.table.BookName
import net.ankio.auto.databinding.DialogBookInfoBinding
import net.ankio.auto.utils.ImageUtils
import net.ankio.common.constant.BillType

class BookInfoDialog(private val context: Context, private val book: BookName, private val callback: (BillType) -> Unit) :
    BaseSheetDialog(context) {
    lateinit var binding: DialogBookInfoBinding
    override fun onCreateView(inflater: LayoutInflater): View {
        binding = DialogBookInfoBinding.inflate(inflater)

        cardView = binding.cardView
        cardViewInner = binding.cardViewInner

        lifecycleScope.launch {
            binding.adapterBook.book.setImageDrawable(
                ImageUtils.get(context, book.icon,R.drawable.default_book)
            )
        }

        binding.adapterBook.itemValue.text = book.name

        binding.expend.setOnClickListener {
            callback(BillType.Expend)
        }

        binding.income.setOnClickListener {
            callback(BillType.Income)
        }

        return binding.root
    }
}