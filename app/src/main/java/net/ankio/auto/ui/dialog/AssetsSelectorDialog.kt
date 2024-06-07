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
import com.hjq.toast.Toaster
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.databinding.DialogBookSelectBinding
import net.ankio.auto.ui.adapter.AssetsSelectorAdapter
import net.ankio.auto.utils.server.model.Assets
import net.ankio.common.constant.AssetsType

class AssetsSelectorDialog(private val context: Context,val type:AssetsType = AssetsType.NORMAL, private val callback: (Assets) -> Unit) :
    BaseSheetDialog(context) {
    private lateinit var binding: DialogBookSelectBinding
    private val dataItems = mutableListOf<Assets>()
    private val adapter =
        AssetsSelectorAdapter(dataItems) { item ->
            callback(item)
            dismiss()
        }

    override fun onCreateView(inflater: LayoutInflater): View {
        binding = DialogBookSelectBinding.inflate(inflater)
        val layoutManager = LinearLayoutManager(context)
        binding.recyclerView.layoutManager = layoutManager

        cardView = binding.cardView
        cardViewInner = binding.recyclerView

        binding.recyclerView.adapter = adapter

        return binding.root
    }

    override fun show(
        float: Boolean,
        cancel: Boolean
    ) {
        lifecycleScope.launch {
            val newData = Assets.get(500,type)

            val collection = newData.takeIf { it.isNotEmpty() } ?: listOf()

            if (collection.isEmpty()) {
                Toaster.show(R.string.no_assets)
                return@launch
            }
            super.show(float, cancel)

            dataItems.addAll(collection)

            adapter.notifyItemInserted(0)
        }
    }
}
