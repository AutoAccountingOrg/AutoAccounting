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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import net.ankio.auto.database.Db
import net.ankio.auto.database.table.BillInfo
import net.ankio.auto.databinding.DialogBillMoreBinding
import net.ankio.auto.databinding.DialogFilterBinding
import net.ankio.auto.ui.adapter.OrderItemAdapter

class FilterDialog(
    context: Context,
    private val callback: (keyword:String)->Unit) :
    BaseSheetDialog(context) {
    private lateinit var binding: DialogFilterBinding


    override fun onCreateView(inflater: LayoutInflater): View {
        binding = DialogFilterBinding.inflate(inflater)
        cardView = binding.cardView
        cardViewInner = binding.cardViewInner
        binding.buttonSure.setOnClickListener {
            callback(binding.raw.text.toString())
            dismiss()
        }

        binding.buttonCancel.setOnClickListener {
            dismiss()
        }

        return binding.root
    }



}