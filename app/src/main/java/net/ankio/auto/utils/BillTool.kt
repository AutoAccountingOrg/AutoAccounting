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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.storage.SpUtils
import org.ezbook.server.constant.BillType
import org.ezbook.server.db.model.CategoryModel
import org.ezbook.server.db.model.SettingModel

object BillTool {

    /**
     * 获取页面显示颜色
     */
    fun getColor(type: BillType): Int {
        val payColor = SpUtils.getInt("setting_pay_color_red", 0)

        return when (type) {
            BillType.Expend -> if (payColor == 0) R.color.danger else R.color.success
            BillType.Income -> if (payColor == 1) R.color.danger else R.color.success
            BillType.Transfer -> R.color.info
            else -> R.color.danger
        }
    }


    fun setTextViewPrice(price: Double, type: BillType,view:MaterialTextView) {
        val color = ContextCompat.getColor(view.context, getColor(type))
        view.setTextColor(color)
        when (type) {
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

        val showParent = SpUtils.getBoolean("category_show_parent",false)
        if (category2 === null) {
            return category1
        }
        if (showParent) {
            return "$category1 - $category2"
        }
        return category2
    }
}