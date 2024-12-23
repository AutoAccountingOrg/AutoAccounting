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

package net.ankio.auto.utils

import androidx.core.content.ContextCompat
import com.google.android.material.textview.MaterialTextView
import net.ankio.auto.R
import net.ankio.auto.storage.ConfigUtils
import org.ezbook.server.constant.BillType
import org.ezbook.server.constant.DefaultData
import org.ezbook.server.constant.Setting

object BillTool {

    /**
     * 获取页面显示颜色
     */
    fun getColor(type: BillType): Int {
        val payColor = ConfigUtils.getInt(Setting.EXPENSE_COLOR_RED, DefaultData.EXPENSE_COLOR_RED)

        return when (type) {
            BillType.Expend -> if (payColor == 0) R.color.danger else R.color.success
            BillType.Income -> if (payColor == 1) R.color.danger else R.color.success
            BillType.Transfer -> R.color.info
            else -> R.color.danger
        }
    }

    fun getType(type: BillType): BillType {
        return when (type) {
            BillType.ExpendReimbursement -> BillType.Expend
            BillType.ExpendLending -> BillType.Expend
            BillType.ExpendRepayment -> BillType.Expend
            BillType.IncomeLending -> BillType.Income
            BillType.IncomeRepayment -> BillType.Income
            BillType.IncomeReimbursement -> BillType.Income
            else -> type
        }
    }


    fun setTextViewPrice(price: Double, type: BillType, view: MaterialTextView) {
        val t = getType(type)
        val color = ContextCompat.getColor(view.context, getColor(t))
        view.setTextColor(color)
        when (t) {
            BillType.Expend, BillType.ExpendReimbursement, BillType.ExpendLending, BillType.ExpendRepayment -> {
                view.text = "- $price"
            }

            BillType.Income, BillType.IncomeLending, BillType.IncomeRepayment, BillType.IncomeReimbursement -> {
                view.text = "+ $price"
            }

            else -> {
                view.text = "$price"
            }
        }
    }

    fun getCateName(category1: String, category2: String? = null): String {

        val showParent =
            ConfigUtils.getBoolean(Setting.CATEGORY_SHOW_PARENT, DefaultData.CATEGORY_SHOW_PARENT)
        if (category2 === null) {
            return category1
        }
        if (showParent) {
            return "$category1 - $category2"
        }
        return category2
    }
}