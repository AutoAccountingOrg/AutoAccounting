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
import net.ankio.auto.databinding.DialogMapBinding
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.utils.ResourceUtils
import net.ankio.auto.ui.utils.ToastUtils
import org.ezbook.server.db.model.AssetsMapModel

class AssetsMapDialog(
    private val context: Context,
    private val assetsMapModel: AssetsMapModel = AssetsMapModel(),
    private val onClose: (AssetsMapModel) -> Unit,
) : BaseSheetDialog(context) {
    private lateinit var binding: DialogMapBinding

    override fun onCreateView(inflater: LayoutInflater): View {
        binding = DialogMapBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        initializeView()
        setupEventListeners()
    }

    private fun initializeView() {
        if (assetsMapModel.id != 0L) {
            setBindingData()
        }
    }

    private fun setBindingData() = with(binding) {
        raw.setText(assetsMapModel.name)
        if(assetsMapModel.mapName.isEmpty()) {
            target.setText(context.getString(R.string.map_target))
        } else {
            target.setText(assetsMapModel.mapName)
        }
        regex.isChecked = assetsMapModel.regex
        
        lifecycleScope.launch {
            ResourceUtils.getAssetDrawableFromName(assetsMapModel.mapName)
                .let { target.setIcon(it,false) }
        }
    }

    private fun setupEventListeners() = with(binding) {
        buttonCancel.setOnClickListener { dismiss() }
        buttonSure.setOnClickListener { handleSaveAction() }
        target.setOnClickListener { showAssetSelector() }
    }

    private fun handleSaveAction() = with(binding) {
        val name = raw.text.toString()
        val mapName = target.getText()
        
        if (name.isEmpty() || mapName == context.getString(R.string.map_target)) {
            ToastUtils.error(context.getString(R.string.map_empty))
        } else{
            assetsMapModel.apply {
                this.name = name
                this.mapName = mapName
                this.regex = binding.regex.isChecked
            }
            lifecycleScope.launch {
                onClose(assetsMapModel)
                dismiss()
            }
        }

    }

    private fun showAssetSelector() {
       val dialog = AssetsSelectorDialog(context) { asset ->
            assetsMapModel.mapName = asset.name
            binding.target.setText(asset.name)

            lifecycleScope.launch {
                ResourceUtils.getAssetDrawable(asset.icon)
                    .let { binding.target.setIcon(it,false) }
            }
        }
        if (lifecycleOwner != null) {
            dialog.bindToLifecycle(lifecycleOwner!!)
        }
        dialog.show()
    }
}
