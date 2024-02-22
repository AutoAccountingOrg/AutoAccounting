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
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.database.Db
import net.ankio.auto.database.table.AssetsMap
import net.ankio.auto.databinding.DialogMapBinding
import net.ankio.auto.utils.Logger

class MapDialog(private val context: Context,
                private val assetsMap: AssetsMap = AssetsMap(), val onClose:(AssetsMap)->Unit):BaseSheetDialog(context) {
    private lateinit var binding:DialogMapBinding
    override fun onCreateView(inflater: LayoutInflater): View {
        binding = DialogMapBinding.inflate(inflater)
        cardView = binding.cardView
        cardViewInner = binding.cardViewInner

        if(assetsMap.id!=0){
            setBindingData()
        }else{
            binding.target.setText(context.getString(R.string.map_no_target))
        }
        bindingEvents()
        return binding.root
    }

    private fun setBindingData(){
       binding.raw.setText(assetsMap.name)
        binding.target.setText(assetsMap.mapName)
        binding.regex.isChecked = assetsMap.regex
        lifecycleScope.launch {
            Db.get().AssetsDao().getAccountDrawable(assetsMap.mapName,context) {
                binding.target.setIcon(it)
            }
        }
    }

    private fun bindingEvents(){
        binding.buttonCancel.setOnClickListener { dismiss() }

        binding.buttonSure.setOnClickListener {
            assetsMap.name = binding.raw.text.toString()
            assetsMap.mapName = binding.target.getText()
            assetsMap.regex = binding.regex.isChecked

            if(assetsMap.name.isEmpty()||assetsMap.mapName == context.getString(R.string.map_no_target)){
                Toast.makeText(context,context.getString(R.string.map_empty),Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                withContext(Dispatchers.IO){
                    if(assetsMap.id!=0) {
                        Db.get().AssetsMapDao().update(assetsMap)
                    }else{
                        assetsMap.id = Db.get().AssetsMapDao().insert(assetsMap).toInt()
                    }
                }
                onClose(assetsMap)
                dismiss()
            }
        }
        binding.target.setOnClickListener {
            AssetsSelectorDialog(context){
                assetsMap.mapName = it.name
                binding.target.setText(it.name)
                lifecycleScope.launch {
                    Db.get().AssetsDao().getDrawable(it.icon,context) { drawable ->
                        binding.target.setIcon(drawable)
                    }
                }
            }.show(cancel = true)
        }
    }

}