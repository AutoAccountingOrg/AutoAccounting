/*
 * Copyright (C) 2023 ankio(ankio@ankio.net)
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

package net.ankio.auto.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.App
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentBillBinding
import net.ankio.auto.ui.adapter.OrderAdapter
import net.ankio.auto.ui.api.BasePageFragment
import net.ankio.auto.ui.dialog.BottomSheetDialogBuilder
import net.ankio.auto.ui.models.OrderGroup
import net.ankio.auto.ui.utils.viewBinding
import net.ankio.auto.utils.DateUtils
import org.ezbook.server.db.model.BillInfoModel


open class OrderFragment : BasePageFragment<OrderGroup>() {
    override val binding: FragmentBillBinding by viewBinding(FragmentBillBinding::inflate)

    override suspend fun loadData(callback: (resultData: List<OrderGroup>) -> Unit) {
        val list = BillInfoModel.list(page, pageSize)

        val groupedData = list.groupBy {
            DateUtils.stampToDate(it.time, "yyyy-MM-dd")
        }.map { (date, bills) ->
            OrderGroup(date, bills)
        }

        val oldSize = pageData.size
        pageData.addAll(groupedData)

        withContext(Dispatchers.Main) {
            if (oldSize == 0) {
                statusPage.contentView?.adapter?.notifyDataSetChanged()
            } else {
                statusPage.contentView?.adapter?.notifyItemRangeInserted(oldSize, groupedData.size)
            }

            callback.invoke(if (list.isEmpty()) emptyList() else pageData)
        }
    }

    override fun onCreateAdapter() {
        val recyclerView = binding.statusPage.contentView!!
        val layoutManager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = OrderAdapter(pageData)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = binding.root


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.topAppBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.item_sync -> {
                    App.startBookApp()
                    true
                }

                R.id.item_clear -> {
                    BottomSheetDialogBuilder(requireActivity())
                        .setTitle(requireActivity().getString(R.string.delete_data))
                        .setMessage(requireActivity().getString(R.string.delete_msg))
                        .setPositiveButton(requireActivity().getString(R.string.sure_msg)) { _, _ ->
                            lifecycleScope.launch {
                                BillInfoModel.clear()
                                page = 1
                                loadDataInside()
                            }
                        }
                        .setNegativeButton(requireActivity().getString(R.string.cancel_msg)) { _, _ -> }
                        .showInFragment(this, false, true)
                    true
                }

                else -> false
            }
        }
    }

}
