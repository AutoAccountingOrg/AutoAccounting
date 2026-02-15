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
import net.ankio.auto.databinding.FragmentPageSignaturesBinding
import net.ankio.auto.databinding.ItemPageSignatureBinding
import net.ankio.auto.service.ocr.PageSignature
import net.ankio.auto.service.ocr.PageSignatureManager
import net.ankio.auto.ui.api.BaseFragment

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
            onDelete = { sig -> PageSignatureManager.remove(sig.key()); refreshList() }
        )
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        refreshList()
    }

    override fun onResume() {
        super.onResume()
        refreshList()
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
            holder.binding.packageName.text = sig.packageName
            holder.binding.activityName.text = sig.activityName.ifBlank { "-" }
            holder.binding.fingerprint.text = sig.contentFingerprint.ifBlank { "-" }.take(80)
            holder.binding.deleteBtn.setOnClickListener { onDelete(sig) }
        }

        override fun getItemCount(): Int = items.size

        class VH(val binding: ItemPageSignatureBinding) : RecyclerView.ViewHolder(binding.root)
    }
}
