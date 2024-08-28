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
import com.hjq.toast.Toaster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.databinding.DialogMapBinding
import net.ankio.auto.storage.ImageUtils
import net.ankio.auto.models.AssetsModel
import net.ankio.auto.models.AssetsMapModel

class MapDialog(
    private val context: Context,
    private val assetsMapModel: AssetsMapModel = AssetsMapModel(),
    val onClose: (AssetsMapModel) -> Unit,
) : BaseSheetDialog(context) {
    private lateinit var binding: DialogMapBinding

    override fun onCreateView(inflater: LayoutInflater): View {
        binding = DialogMapBinding.inflate(inflater)
        cardView = binding.cardView
        cardViewInner = binding.cardViewInner

        if (assetsMapModel.id != 0) {
            setBindingData()
        } else {
            binding.target.setText(context.getString(R.string.map_no_target))
        }
        bindingEvents()
        return binding.root
    }

    private fun setBindingData() {
        binding.raw.setText(assetsMapModel.name)
        binding.target.setText(assetsMapModel.mapName)
        binding.regex.isChecked = assetsMapModel.regex == 1
        lifecycleScope.launch {
            AssetsModel.getDrawable(assetsMapModel.mapName, context).let {
                binding.target.setIcon(it)
            }
        }
    }

    private fun bindingEvents() {
        binding.buttonCancel.setOnClickListener { dismiss() }

        binding.buttonSure.setOnClickListener {
            assetsMapModel.name = binding.raw.text.toString()
            assetsMapModel.mapName = binding.target.getText()
            assetsMapModel.regex = if(binding.regex.isChecked) 1 else 0

            if (assetsMapModel.name.isEmpty() || assetsMapModel.mapName == context.getString(R.string.map_no_target)) {
                Toaster.show(context.getString(R.string.map_empty))
                return@setOnClickListener
            }

            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    AssetsMapModel.put(assetsMapModel)
                }
                onClose(assetsMapModel)
                dismiss()
            }
        }
        binding.target.setOnClickListener {
            AssetsSelectorDialog(context) {
                assetsMapModel.mapName = it.name
                binding.target.setText(it.name)
                lifecycleScope.launch {
                    binding.target.setIcon( ImageUtils.get(context, it.icon, R.mipmap.ic_launcher_round))
                }
            }.show(cancel = true)
        }
    }
}
