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

import android.content.res.ColorStateList
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import com.google.android.material.color.MaterialColors
import com.google.android.material.textview.MaterialTextView
import net.ankio.auto.R
import net.ankio.auto.databinding.AdapterOrderItemBinding
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import net.ankio.auto.ui.components.IconView
import net.ankio.auto.ui.utils.PaletteManager
import net.ankio.auto.ui.utils.setAssetIconByName
import net.ankio.auto.ui.utils.setCategoryIcon
import net.ankio.auto.utils.BillTool
import net.ankio.auto.utils.DateUtils
import org.ezbook.server.constant.BillState
import org.ezbook.server.constant.BillType
import org.ezbook.server.db.model.BillInfoModel

class BillItemAdapter(
    private val showMore: Boolean = true
) : BaseAdapter<AdapterOrderItemBinding, BillInfoModel>() {

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
            binding.remark.visibility = View.GONE
        } else {
            binding.remark.visibility = View.VISIBLE
            binding.remark.text = data.remark
        }

        // 标签渲染：单独封装，避免在主逻辑里塞细节
        renderTags(binding, data)


        fun loadCategoryIcon(name: String) {
            binding.category.setText(name)
            launchInAdapter {
                binding.category.imageView().setCategoryIcon(name)
            }
        }

        fun loadAssetIcon(view: IconView, name: String) {
            view.setText(name)
            launchInAdapter {
                view.imageView().setAssetIconByName(name)
            }
        }


        binding.payTools1.visibility =
            if (PrefManager.featureAssetManage) View.VISIBLE else View.GONE
        binding.payTools2.visibility =
            if (PrefManager.featureAssetManage) View.VISIBLE else View.GONE
        binding.iconHeader.visibility =
            if (PrefManager.featureAssetManage) View.VISIBLE else View.GONE

        /**
         * 根据账户信息是否展示，动态切换第三行的约束，避免“GONE 导致不右对齐/挤没/顶出”的特殊情况。
         *
         * 约束策略（只做一件事）：
         * - 只显示 account1 时：payTools1 的 end 直接约束到 parent end，确保靠右。
         * - 显示转账关系时：payTools1 的 end 约束到箭头（iconHeader），保持结构一致。
         *
         * 注意：RecyclerView 复用会保留旧约束，所以必须在每次 bind 时显式恢复/设置。
         */
        fun updateAccountLineConstraints(showArrowAndAccount2: Boolean) {
            val layout =
                binding.root.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.account_line)
            val set = ConstraintSet()
            set.clone(layout)

            set.clear(R.id.payTools1, ConstraintSet.END)
            if (showArrowAndAccount2) {
                set.connect(
                    R.id.payTools1,
                    ConstraintSet.END,
                    R.id.icon_header,
                    ConstraintSet.START
                )
            } else {
                set.connect(
                    R.id.payTools1,
                    ConstraintSet.END,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.END
                )
            }

            set.applyTo(layout)
        }

        fun notShowAccount() {
            binding.payTools2.visibility = View.GONE
            binding.iconHeader.visibility = View.GONE
            updateAccountLineConstraints(showArrowAndAccount2 = false)
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
                updateAccountLineConstraints(showArrowAndAccount2 = true)
            }

            BillType.ExpendRepayment -> {
                binding.category.setText(context.getString(R.string.expend_repayment_info))
                loadAssetIcon(binding.payTools1, data.accountNameFrom)
                loadAssetIcon(binding.payTools2, data.accountNameTo)
                updateAccountLineConstraints(showArrowAndAccount2 = true)
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
                updateAccountLineConstraints(showArrowAndAccount2 = true)
            }

            BillType.IncomeRepayment -> {
                binding.category.setText(context.getString(R.string.income_repayment_info))
                loadAssetIcon(binding.payTools1, data.accountNameFrom)
                loadAssetIcon(binding.payTools2, data.accountNameTo)
                updateAccountLineConstraints(showArrowAndAccount2 = true)
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
                updateAccountLineConstraints(showArrowAndAccount2 = true)
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


        binding.bookName.text = data.bookName
        val defaultBookName = context.getString(R.string.setting_default_book)
        val shouldShowBook = data.bookName.isNotEmpty() && data.bookName != defaultBookName

        // 手续费/优惠（仅设置文本和样式，visibility在后面统一控制）
        val shouldShowFee = data.fee != 0.0
        if (shouldShowFee) {
            if (data.fee < 0) {
                // 手续费：警告信息（红色）
                val feeAmount = BillTool.formatAmount(kotlin.math.abs(data.fee))
                binding.fee.text = context.getString(R.string.bill_fee_label, feeAmount)
                val bgColor = MaterialColors.getColor(
                    binding.fee,
                    com.google.android.material.R.attr.colorErrorContainer
                )
                val textColor = MaterialColors.getColor(
                    binding.fee,
                    com.google.android.material.R.attr.colorOnErrorContainer
                )
                binding.fee.backgroundTintList = ColorStateList.valueOf(bgColor)
                binding.fee.setTextColor(textColor)
            } else {
                // 优惠：正面信息（紫色/橙色系）
                val discountAmount = BillTool.formatAmount(data.fee)
                binding.fee.text = context.getString(R.string.bill_discount_label, discountAmount)
                val bgColor = MaterialColors.getColor(
                    binding.fee,
                    com.google.android.material.R.attr.colorTertiaryContainer
                )
                val textColor = MaterialColors.getColor(
                    binding.fee,
                    com.google.android.material.R.attr.colorOnTertiaryContainer
                )
                binding.fee.backgroundTintList = ColorStateList.valueOf(bgColor)
                binding.fee.setTextColor(textColor)
            }
        }


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


        binding.date.visibility = View.VISIBLE
        binding.bookName.visibility = if (shouldShowBook) View.VISIBLE else View.GONE
        binding.fee.visibility = if (shouldShowFee) View.VISIBLE else View.GONE
        binding.autoRecord.visibility = if (data.auto) View.VISIBLE else View.GONE
        binding.sync.visibility = View.VISIBLE
        if (PrefManager.autoGroup) {
            binding.moreBills.visibility = View.VISIBLE
        } else {
            binding.moreBills.visibility = View.GONE
        }

    }

    /**
     * 渲染标签列表到 ChipGroup
     *
     * 规则：
     * - 空标签：隐藏整行并清空内容
     * - 有标签：按名称创建静态 Chip
     *
     * @param binding 账单条目绑定
     * @param data 账单数据
     */
    private fun renderTags(binding: AdapterOrderItemBinding, data: BillInfoModel) {
        val tagNames = data.getTagList()
        if (tagNames.isEmpty()) {
            binding.tagLabels.visibility = View.GONE
            binding.tagLabels.removeAllViews()
            return
        }

        binding.tagLabels.visibility = View.VISIBLE
        binding.tagLabels.removeAllViews()

        // 使用中性配色作为兜底，避免和主信息冲突
        val defaultBackgroundColor = MaterialColors.getColor(
            binding.tagLabels,
            com.google.android.material.R.attr.colorSurfaceContainerHighest
        )
        val defaultTextColor = MaterialColors.getColor(
            binding.tagLabels,
            com.google.android.material.R.attr.colorOnSurfaceVariant
        )
        val paddingHorizontal = dpToPx(binding.tagLabels, 8)
        val paddingVertical = dpToPx(binding.tagLabels, 3)
        var marginEnd = dpToPx(binding.tagLabels, 4)
        val marginBottom = dpToPx(binding.tagLabels, 4)

        tagNames.forEach { name ->
            // 标签以 label 样式呈现，保持与其它元数据一致
            val (textColor, backgroundColor) = PaletteManager.getLabelColors(
                binding.root.context,
                name,
                defaultTextColor,
                defaultBackgroundColor
            )
            val label = MaterialTextView(binding.root.context).apply {
                text = "$name"
                setTextColor(textColor)
                textSize = 12f
                setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical)
                background = binding.root.context.getDrawable(R.drawable.currency_label_background)
                background?.setTint(backgroundColor)
                setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_tag, 0, 0, 0)
                compoundDrawablePadding = dpToPx(binding.tagLabels, 6)
            }

            val params = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = marginEnd
                bottomMargin = marginBottom
            }
            binding.tagLabels.addView(label, params)
        }
    }

    /**
     * dp 转 px，避免硬编码
     */
    private fun dpToPx(view: View, dp: Int): Int {
        return (dp * view.resources.displayMetrics.density).toInt()
    }


    override fun areItemsSame(oldItem: BillInfoModel, newItem: BillInfoModel): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsSame(oldItem: BillInfoModel, newItem: BillInfoModel): Boolean {
        return oldItem == newItem
    }

}