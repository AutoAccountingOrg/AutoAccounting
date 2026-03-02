/*
 * Copyright (C) 2025 ankio(ankio@ankio.net)
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

package net.ankio.auto.ui.fragment.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentPageSignaturesBinding
import net.ankio.auto.databinding.ItemPageSignatureBinding
import net.ankio.auto.service.ocr.PageSignature
import net.ankio.auto.service.ocr.PageSignatureManager
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.dialog.BottomSheetDialogBuilder
import net.ankio.auto.utils.getAppInfoFromPackageName

/**
 * 已记住页面管理 Fragment
 *
 * 展示并管理通过手动识别成功后记住的页面特征列表。
 */
class PageSignaturesFragment : BaseFragment<FragmentPageSignaturesBinding>() {

    private lateinit var adapter: Adapter

    override fun onViewCreated(view: android.view.View, savedInstanceState: android.os.Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        adapter = Adapter(
            onDelete = { sig -> confirmDelete(sig) }
        )
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        refreshList()
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun confirmDelete(sig: PageSignature) {
        val appName = getAppInfoFromPackageName(sig.packageName)?.name ?: sig.packageName
        BaseSheetDialog.create<BottomSheetDialogBuilder>(requireContext())
            .setTitle(getString(R.string.delete_data))
            .setMessage(getString(R.string.ocr_delete_page_confirm, appName))
            .setPositiveButton(getString(R.string.sure_msg)) { _, _ ->
                PageSignatureManager.remove(sig.key())
                refreshList()
            }
            .setNegativeButton(getString(R.string.cancel_msg)) { _, _ -> }
            .show()
    }

    private fun refreshList() {
        val list = PageSignatureManager.getAll()
        adapter.submitList(list)
        binding.emptyView.visibility =
            if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }

    private class Adapter(
        private val onDelete: (PageSignature) -> Unit
    ) : RecyclerView.Adapter<Adapter.VH>() {

        private var items: List<PageSignature> = emptyList()

        fun submitList(list: List<PageSignature>) {
            items = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val b =
                ItemPageSignatureBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(b)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val sig = items[position]
            val appInfo = getAppInfoFromPackageName(sig.packageName)
            holder.binding.appIcon.setImageDrawable(appInfo?.icon)
            holder.binding.appName.text = appInfo?.name ?: sig.packageName
            holder.binding.activityName.text = sig.activityName.ifBlank { "-" }
            holder.binding.structureFingerprint.apply {
                text = sig.structureFingerprint.ifBlank { "-" }
                visibility =
                    if (sig.structureFingerprint.isBlank()) android.view.View.GONE
                    else android.view.View.VISIBLE
            }
            holder.itemView.setOnLongClickListener { onDelete(sig); true }
        }

        override fun getItemCount(): Int = items.size

        class VH(val binding: ItemPageSignatureBinding) : RecyclerView.ViewHolder(binding.root)
    }
}
