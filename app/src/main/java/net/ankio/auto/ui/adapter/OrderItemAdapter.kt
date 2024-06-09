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
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.app.BillUtils
import net.ankio.auto.databinding.AdapterOrderItemBinding
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.DateUtils
import net.ankio.auto.utils.server.model.Assets
import net.ankio.auto.utils.server.model.BillInfo
import net.ankio.auto.utils.server.model.BookName
import net.ankio.auto.utils.server.model.Category
import net.ankio.common.config.AccountingConfig
import net.ankio.common.constant.BillType

class OrderItemAdapter(
    override val dataItems: List<BillInfo>,
    private val onItemChildClick: ((item: BillInfo) -> Unit)?,
    private val onItemChildMoreClick: ((item: BillInfo) -> Unit)?,
) : BaseAdapter(dataItems, AdapterOrderItemBinding::class.java) {
    override fun onInitView(holder: BaseViewHolder) {
        val binding = holder.binding as AdapterOrderItemBinding
        binding.root.setOnClickListener {
            onItemChildClick?.invoke(holder.item as BillInfo)
        }
        binding.moreBills.setOnClickListener {
            onItemChildMoreClick?.invoke(holder.item as BillInfo)
        }

        binding.payTools.visibility = if (config.assetManagement) View.VISIBLE else View.GONE
    }

    private lateinit var config: AccountingConfig

    fun notifyConfig(autoAccountingConfig: AccountingConfig) {
        config = autoAccountingConfig
    }

    override fun onBindView(
        holder: BaseViewHolder,
        item: Any,
    ) {
        val binding = holder.binding as AdapterOrderItemBinding
        val billInfo = item as BillInfo
        val scope = holder.scope
        val context = holder.context
        binding.category.setText(billInfo.cateName)
        scope.launch {
            val book = BookName.getDefaultBook(billInfo.bookName)
            Category.getDrawable(billInfo.cateName, book.id, context).let {
                withContext(Dispatchers.Main) {
                    binding.category.setIcon(it, true)
                }
            }
        }

        binding.date.text = DateUtils.getTime("HH:mm:ss", billInfo.timeStamp)

        val type =
            when (BillType.fromInt(billInfo.type)) {
                BillType.Expend -> BillType.Expend
                BillType.ExpendReimbursement -> BillType.Expend
                BillType.ExpendLending -> BillType.Expend
                BillType.ExpendRepayment -> BillType.Expend
                BillType.Income -> BillType.Income
                BillType.IncomeLending -> BillType.Income
                BillType.IncomeRepayment -> BillType.Income
                BillType.IncomeReimbursement -> BillType.Income
                BillType.Transfer -> BillType.Transfer
            }

        val symbols =
            when (type.toInt()) {
                0 -> "- "
                1 -> "+ "
                2 -> "→ "
                else -> "- "
            }

        val tintRes = BillUtils.getColor(type.toInt())

        val color = ContextCompat.getColor(context, tintRes)
        binding.money.setColor(color)

        binding.money.setText(symbols + billInfo.money.toString())

        binding.remark.text = billInfo.remark

        binding.payTools.setText(billInfo.accountNameFrom)

        scope.launch {
            Assets.getDrawable(billInfo.accountNameFrom, context).let {
                binding.payTools.setIcon(it, false)
            }
            AppUtils.getAppInfoFromPackageName(item.from, context)?.let {
                binding.fromApp.setImageDrawable(it.icon)
            }
        }

        val rule = billInfo.channel
        val regex = "\\[(.*?)]".toRegex()
        val matchResult = regex.find(rule)
        if (matchResult != null) {
            val (value) = matchResult.destructured
            binding.channel.text = value
        } else {
            binding.channel.text = billInfo.channel
        }

        //   binding.fromApp.setIcon()

        if (BillUtils.noNeedFilter(item)) {
            binding.moreBills.visibility = View.GONE
        }

        if (onItemChildMoreClick == null) {
            binding.moreBills.visibility = View.GONE
        }
    }
}
