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
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.databinding.DialogAssetsMapBinding
import net.ankio.auto.ui.adapter.BillAssetsMapAdapter
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.componets.WrapContentLinearLayoutManager
import net.ankio.auto.ui.utils.AssetsUtils
import net.ankio.auto.ui.utils.ToastUtils
import org.ezbook.server.db.model.AssetsMapModel
import org.ezbook.server.db.model.AssetsModel

class BillAssetsMapDialog(
    private val context: Context,
    private val float: Boolean,
    private val items: MutableList<AssetsMapModel>,
    private val assetsItems: List<AssetsModel>,
    private val onClose: (MutableList<AssetsMapModel>) -> Unit
) : BaseSheetDialog(context) {
    lateinit var binding: DialogAssetsMapBinding
    override fun onCreateView(inflater: LayoutInflater): View {
        binding = DialogAssetsMapBinding.inflate(inflater)
        return binding.root
    }



    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        items.forEach {
            it.mapName = AssetsUtils.getAssetsByAlgorithm(assetsItems, it.name)
        }

        val adapter = BillAssetsMapAdapter(context)
        adapter.updateItems(items)
        adapter.setOnClickListener {
            AssetsSelectorDialog(context) { asset ->
                val position = items.indexOf(it)
                it.mapName = asset.name
                adapter.updateItem(position, it)
            }.show(float = float)
        }

        binding.statusPage.contentView!!.layoutManager = WrapContentLinearLayoutManager(context)
        binding.statusPage.contentView!!.adapter = adapter
        binding.statusPage.showContent()
        binding.buttonSure.setOnClickListener {
            lifecycleScope.launch {
                if (!validateAndSaveMapping()) return@launch
                onClose(items)
                dismiss()
            }
        }
        binding.buttonCancel.setOnClickListener {
            dismiss()
        }
    }


    private suspend fun validateAndSaveMapping(): Boolean {
        items.forEach {
            if (AssetsModel.getByName(it.mapName) == null) {
                ToastUtils.error(R.string.map_source_not_exist)
                return false
            }
            AssetsMapModel.put(it)
        }

        return true
    }



}