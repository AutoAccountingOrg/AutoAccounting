package net.ankio.auto.ui.adapter

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.databinding.AdapterBookBillBinding
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import net.ankio.auto.ui.utils.ResourceUtils
import net.ankio.auto.utils.BillTool
import net.ankio.auto.utils.DateUtils
import org.ezbook.server.constant.BillType
import org.ezbook.server.db.model.BookBillModel

class BillSelectorAdapter(
    private val selectApp: MutableList<String>,
    private val multipleSelect: Boolean
) : BaseAdapter<AdapterBookBillBinding, BookBillModel>(
    AdapterBookBillBinding::class.java
) {
    override fun onInitViewHolder(holder: BaseViewHolder<AdapterBookBillBinding, BookBillModel>) {
        val binding = holder.binding
        binding.root.setOnClickListener {
            val item = holder.item ?: return@setOnClickListener

            if (selectApp.contains(item.remoteId)) {
                selectApp.remove(item.remoteId)
                binding.checkbox.isChecked = false
            } else {
                if (!multipleSelect && selectApp.isNotEmpty()) {
                    val oldSelection = selectApp.first()
                    selectApp.clear()
                    getItems().forEachIndexed { index, bill ->
                        if (bill.remoteId == oldSelection) {
                            notifyItemChanged(index)
                        }
                    }
                }
                selectApp.add(item.remoteId)
                binding.checkbox.isChecked = true
            }
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
        holder.launch {
            ResourceUtils.getCategoryDrawableByName(data.category, holder.context).let {
                withContext(Dispatchers.Main) {
                    binding.category.setIcon(it)
                }
            }
        }
        binding.checkbox.isChecked = selectApp.contains(data.remoteId)

        binding.date.text = DateUtils.stampToDate(data.time, "yyyy-MM-dd HH:mm")

        if (data.remark.isEmpty()) {
            binding.remark.visibility = android.view.View.GONE
        } else {
            binding.remark.visibility = android.view.View.VISIBLE
            binding.remark.text = data.remark
        }

    }

    override fun areItemsSame(oldItem: BookBillModel, newItem: BookBillModel): Boolean {
        return oldItem.remoteId == newItem.remoteId
    }

    override fun areContentsSame(oldItem: BookBillModel, newItem: BookBillModel): Boolean {
        return oldItem == newItem
    }

}