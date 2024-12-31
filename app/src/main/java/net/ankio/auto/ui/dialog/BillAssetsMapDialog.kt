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
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.databinding.DialogAssetsMapBinding
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.componets.IconView
import net.ankio.auto.ui.utils.AssetsUtils
import net.ankio.auto.ui.utils.ResourceUtils
import net.ankio.auto.ui.utils.ToastUtils
import org.ezbook.server.db.model.AssetsMapModel
import org.ezbook.server.db.model.AssetsModel

class BillAssetsMapDialog(
    private val context: Context,
    private val assets: MutableList<String>,
    assetsItems: List<AssetsModel>,
    private val float: Boolean,
    private val onClose: (String, String) -> Unit
) : BaseSheetDialog(context) {
    lateinit var binding: DialogAssetsMapBinding
    override fun onCreateView(inflater: LayoutInflater): View {
        binding = DialogAssetsMapBinding.inflate(inflater)
        return binding.root
    }

    private var accountFromMap = AssetsUtils.getAssetsByAlgorithm(assetsItems, assets[0])
    private var accountToMap = if (assets.size > 1) {
        AssetsUtils.getAssetsByAlgorithm(assetsItems, assets[1])
    } else {
        ""
    }


    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        // 设置第一个账户（必须存在）
        setupAccountMapping(
            rawView = binding.accountFrom.raw,
            targetView = binding.accountFrom.target,
            accountName = assets[0],
            onAssetSelected = { asset -> accountFromMap = asset.name }
        )

        // 设置第二个账户（如果存在）
        if (assets.size > 1) {
            setupAccountMapping(
                rawView = binding.accountTo.raw,
                targetView = binding.accountTo.target,
                accountName = assets[1],
                onAssetSelected = { asset -> accountToMap = asset.name }
            )
        } else {
            binding.accountTo.root.visibility = View.GONE
        }

        binding.buttonSure.setOnClickListener {
            lifecycleScope.launch {
                if (!validateAndSaveMapping()) return@launch
                onClose(accountFromMap, accountToMap)
                dismiss()
            }
        }
    }

    private fun setupAccountMapping(
        rawView: TextView,
        targetView: IconView,
        accountName: String,
        onAssetSelected: (AssetsModel) -> Unit
    ) {
        rawView.text = accountName
        setTargetAccount(targetView, "")  // 初始化为空

        targetView.setOnClickListener {
            AssetsSelectorDialog(context) { asset ->
                setTargetAccount(targetView, asset.name)
                onAssetSelected(asset)
            }.show(float = float)
        }
    }

    private suspend fun validateAndSaveMapping(): Boolean {
        // 验证源账户
        if (AssetsModel.getByName(accountFromMap) == null) {
            ToastUtils.error(R.string.map_source_not_exist)
            return false
        }

        // 验证目标账户（如果存在）
        if (assets.size > 1 && AssetsModel.getByName(accountToMap) == null) {
            ToastUtils.error(R.string.map_source_not_exist)
            return false
        }

        // 保存映射
        saveMapping(assets[0], accountFromMap)
        if (assets.size > 1) {
            saveMapping(assets[1], accountToMap)
        }

        return true
    }

    private suspend fun saveMapping(originalName: String, mappedName: String) {
        if (AssetsMapModel.getByName(mappedName) == null) {
            AssetsMapModel().apply {
                name = originalName
                mapName = mappedName
                AssetsMapModel.put(this)
            }
        }
    }

    private fun setTargetAccount(target: IconView, assetsTarget: String) {
        if (assetsTarget.isEmpty()) {
            target.setText(context.getString(R.string.map_target))
        } else {
            target.setText(assetsTarget)
        }

        lifecycleScope.launch {
            ResourceUtils.getAssetDrawableFromName(assetsTarget)
                .let { target.setIcon(it, false) }
        }
    }


}