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

//import net.ankio.auto.ui.adapter.BillSelectorAdapter
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.hjq.toast.Toaster
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.databinding.DialogBillSelectBinding
import net.ankio.auto.exceptions.AutoServiceException
import net.ankio.auto.models.BookBillModel
import net.ankio.auto.storage.Logger
import net.ankio.common.constant.BillType

class BillSelectorDialog(
    private val context: Context,
    private val billType: BillType,
    private val selectedBills: ArrayList<String> = ArrayList(),
    private val callback: () -> Unit,
) :
    BaseSheetDialog(context) {
    private lateinit var binding: DialogBillSelectBinding
    private val dataItems = mutableListOf<BookBillModel>()
  //  private val adapter = BillSelectorAdapter(dataItems, selectedBills)

    override fun onCreateView(inflater: LayoutInflater): View {
        binding = DialogBillSelectBinding.inflate(inflater)
        val layoutManager = LinearLayoutManager(context)
        binding.recyclerView.layoutManager = layoutManager

        cardView = binding.cardView
        cardViewInner = binding.innerView

        //binding.recyclerView.adapter = adapter

        binding.btn.setOnClickListener {
            callback.invoke()
            dismiss()
        }

        return binding.root
    }

    override fun show(
        float: Boolean,
        cancel: Boolean,
    ) {
        lifecycleScope.launch {
            runCatching {

             /*   val data = BookBillModel.get(500,billType)
                if (data.isNullOrEmpty()) {
                    dismiss()
                    Toaster.show(R.string.no_bills)
                    return@runCatching
                }
                super.show(float, cancel)
                dataItems.addAll(data)
*/
             //   adapter.notifyDataSetChanged()
            }.onFailure {
                dismiss()
                Toaster.show(R.string.no_bills)
                Logger.e("get auto_bills ${billType.name} error", it)
                if (it is AutoServiceException) {
                  //  EventBus.post(AutoServiceErrorEvent(it))
                }
            }
        }
    }
}
