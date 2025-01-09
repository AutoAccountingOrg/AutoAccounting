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

package net.ankio.auto.ui.adapter

import android.view.View
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.databinding.AdapterOrderItemBinding
import net.ankio.auto.storage.ConfigUtils
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import net.ankio.auto.ui.componets.IconView
import net.ankio.auto.ui.utils.ResourceUtils
import net.ankio.auto.utils.BillTool
import net.ankio.auto.utils.DateUtils
import org.ezbook.server.constant.BillState
import org.ezbook.server.constant.BillType
import org.ezbook.server.constant.DefaultData
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.model.BillInfoModel

class OrderItemAdapter(
    private val showMore: Boolean = true
) : BaseAdapter<AdapterOrderItemBinding, BillInfoModel>(AdapterOrderItemBinding::class.java) {

    private var onItemClickListener: ((BillInfoModel, Int) -> Unit)? = null
    private var onItemLongClickListener: ((BillInfoModel, Int) -> Unit)? = null
    private var onMoreClickListener: ((BillInfoModel) -> Unit)? = null

    fun setOnItemClickListener(listener: (BillInfoModel, Int) -> Unit) {
        onItemClickListener = listener
    }

    fun setOnItemLongClickListener(listener: (BillInfoModel, Int) -> Unit) {
        onItemLongClickListener = listener
    }

    fun setOnMoreClickListener(listener: (BillInfoModel) -> Unit) {
        onMoreClickListener = listener
    }

    override fun onInitViewHolder(holder: BaseViewHolder<AdapterOrderItemBinding, BillInfoModel>) {
        val binding = holder.binding

        binding.root.setOnClickListener {
            val item = holder.item!!
            val position = indexOf(item)
            onItemClickListener?.invoke(item, position)
        }

        binding.moreBills.setOnClickListener {
            val item = holder.item!!
            onMoreClickListener?.invoke(item)
        }

        binding.root.setOnLongClickListener {
            val item = holder.item!!
            val position = indexOf(item)
            onItemLongClickListener?.invoke(item, position)
            true
        }
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder<AdapterOrderItemBinding, BillInfoModel>,
        data: BillInfoModel,
        position: Int
    ) {
        val binding = holder.binding


        val context = holder.context
        if (data.remark.isEmpty()) {
            binding.remark.text = "无备注"
        } else {
            binding.remark.text = data.remark
        }


        fun loadCategoryIcon(name: String) {
            binding.category.setText(name)
            holder.launch {
                ResourceUtils.getCategoryDrawableByName(name, holder.context).let {
                    withContext(Dispatchers.Main) {
                        binding.category.setIcon(it, true)
                    }
                }
            }
        }

        fun loadAssetIcon(view: IconView, name: String) {
            view.setText(name)
            holder.launch {
                ResourceUtils.getAssetDrawableFromName(name).let {
                    withContext(Dispatchers.Main) {
                        view.setIcon(it, false)
                    }
                }
            }
        }

        fun visibility(): Int {
            return if (ConfigUtils.getBoolean(
                    Setting.SETTING_ASSET_MANAGER,
                    DefaultData.SETTING_ASSET_MANAGER
                )
            ) View.VISIBLE else View.GONE
        }

        val visibility = visibility()

        binding.payTools1.visibility = visibility
        binding.payTools2.visibility = visibility
        binding.iconHeader.visibility = visibility

        fun notShowAccount() {
            binding.payTools2.visibility = View.GONE
            binding.iconHeader.visibility = View.GONE
        }

        when (data.type) {
            BillType.Expend -> {
                loadCategoryIcon(data.cateName)
                loadAssetIcon(binding.payTools1, data.accountNameFrom)
                notShowAccount()
            }

            BillType.ExpendReimbursement -> {
                loadCategoryIcon(data.cateName)
                binding.category.setText(
                    context.getString(
                        R.string.income_reimbursement_info,
                        data.cateName
                    )
                )
                loadAssetIcon(binding.payTools1, data.accountNameFrom)
                notShowAccount()
            }
            BillType.ExpendLending -> {
                binding.category.setText(context.getString(R.string.expend_lending))
                loadAssetIcon(binding.payTools1, data.accountNameFrom)
                loadAssetIcon(binding.payTools2, data.accountNameTo)
            }

            BillType.ExpendRepayment -> {
                binding.category.setText(context.getString(R.string.expend_repayment_info))
                loadAssetIcon(binding.payTools1, data.accountNameFrom)
                loadAssetIcon(binding.payTools2, data.accountNameTo)
            }

            BillType.Income -> {
                loadCategoryIcon(data.cateName)
                loadAssetIcon(binding.payTools1, data.accountNameFrom)
                notShowAccount()
            }
            BillType.IncomeLending -> {
                binding.category.setText(context.getString(R.string.income_lending))
                loadAssetIcon(binding.payTools1, data.accountNameFrom)
                loadAssetIcon(binding.payTools2, data.accountNameTo)
            }

            BillType.IncomeRepayment -> {
                binding.category.setText(context.getString(R.string.income_repayment_info))
                loadAssetIcon(binding.payTools1, data.accountNameFrom)
                loadAssetIcon(binding.payTools2, data.accountNameTo)
            }

            BillType.IncomeReimbursement -> {
                loadCategoryIcon(data.cateName)
                binding.category.setText(
                    context.getString(
                        R.string.income_reimbursement_info,
                        data.cateName
                    )
                )
                loadAssetIcon(binding.payTools1, data.accountNameFrom)
                notShowAccount()
            }
            BillType.Transfer -> {
                binding.category.setText(context.getText(R.string.float_transfer))
                loadAssetIcon(binding.payTools1, data.accountNameFrom)
                loadAssetIcon(binding.payTools2, data.accountNameTo)
            }

            BillType.IncomeRefund -> {
                binding.category.setText(
                    context.getString(
                        R.string.income_refund_info,
                        data.cateName
                    )
                )
                loadAssetIcon(binding.payTools1, data.accountNameFrom)
                notShowAccount()
            }
        }


        BillTool.setTextViewPrice(data.money, data.type, binding.money)
        binding.date.text = DateUtils.stampToDate(data.time, "HH:mm:ss")


        when (data.state) {
            BillState.Synced -> {
                binding.sync.setImageResource(R.drawable.ic_sync)
            }

            BillState.Wait2Edit -> {
                binding.sync.setImageResource(R.drawable.icon_edit)
            }

            else -> {
                binding.sync.setImageResource(R.drawable.ic_no_sync)
            }
        }






        if (!showMore) {
            binding.statusBar.visibility = View.GONE
        } else {
            binding.statusBar.visibility = View.VISIBLE
            if (ConfigUtils.getBoolean(Setting.AUTO_GROUP, DefaultData.AUTO_GROUP)) {
                binding.moreBills.visibility = View.VISIBLE
            } else {
                binding.moreBills.visibility = View.GONE
            }
            binding.autoRecord.visibility = if (data.auto) View.VISIBLE else View.GONE
        }


    }

    override fun areItemsSame(oldItem: BillInfoModel, newItem: BillInfoModel): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsSame(oldItem: BillInfoModel, newItem: BillInfoModel): Boolean {
        return oldItem == newItem
    }

}