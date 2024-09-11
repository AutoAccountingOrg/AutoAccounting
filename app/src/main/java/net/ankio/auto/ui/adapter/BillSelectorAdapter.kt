package net.ankio.auto.ui.adapter

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.databinding.AdapterBookBillBinding
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import net.ankio.auto.ui.scope.autoDisposeScope
import net.ankio.auto.ui.utils.ResourceUtils
import net.ankio.auto.utils.BillTool
import net.ankio.auto.utils.DateUtils
import org.ezbook.server.constant.BillType
import org.ezbook.server.db.model.BookBillModel

class BillSelectorAdapter(
    list: MutableList<BookBillModel>,
    private val selectApp: MutableList<String>
) : BaseAdapter<AdapterBookBillBinding, BookBillModel>(
    AdapterBookBillBinding::class.java, list
) {
    override fun onInitViewHolder(holder: BaseViewHolder<AdapterBookBillBinding, BookBillModel>) {
        val binding = holder.binding
        binding.root.setOnClickListener {
            val item = holder.item ?: return@setOnClickListener

            if (selectApp.contains(item.remoteId)) {
                selectApp.remove(item.remoteId)
                binding.checkbox.isChecked = false
            } else {
                selectApp.add(item.remoteId)
                binding.checkbox.isChecked = true
            }
            Log.d("BillSelectorDialog", "selectedBills: $selectApp")
        }
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder<AdapterBookBillBinding, BookBillModel>,
        data: BookBillModel,
        position: Int
    ) {
        val binding = holder.binding
        binding.category.setText(data.category)
        BillTool.setTextViewPrice(data.money, BillType.Expend, binding.money)
        holder.binding.root.autoDisposeScope.launch {
            ResourceUtils.getCategoryDrawableByName(data.category, holder.context).let {
                withContext(Dispatchers.Main) {
                    binding.category.setIcon(it)
                }
            }
        }
        binding.checkbox.isChecked = selectApp.contains(data.remoteId)

        binding.date.text = DateUtils.stampToDate(data.time, "yyyy-MM-dd HH:mm")


    }

}