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

import android.net.Uri
import androidx.core.content.ContextCompat
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import net.ankio.auto.App
import net.ankio.auto.BuildConfig
import net.ankio.auto.R
import net.ankio.auto.adapter.AppAdapterManager
import net.ankio.auto.autoApp
import net.ankio.auto.http.api.BillAPI
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.utils.ToastUtils
import org.ezbook.server.constant.BillState
import org.ezbook.server.constant.BillType
import org.ezbook.server.db.model.BillInfoModel

object BillTool {

    /**
     * 获取页面显示颜色
     */
    fun getColor(type: BillType): Int {
        val payColor = PrefManager.expenseColorRed

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
            BillType.IncomeRefund -> BillType.Income
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

        if (category2 === null) {
            return category1
        }
        return "$category1 - $category2"
    }

    suspend fun syncBills() {
        //获取所有未同步的账单
        BillAPI.sync().forEach {
            App.launch(Dispatchers.IO) {
                syncBill(it)
            }
        }

    }

    suspend fun syncBill(billInfoModel: BillInfoModel) {
        AppAdapterManager.adapter().syncBill(billInfoModel)
        delay(AppAdapterManager.adapter().sleep())
    }
    fun saveBill(bill: BillInfoModel) {
        Logger.d("保存账单: ${bill.id}")

        // 更新状态
        bill.state = BillState.Edited

        // 异步保存
        App.launchIO {
            BillAPI.put(bill)
            // 若未开启手动同步，则保存后立即同步；否则跳过
            if (!PrefManager.manualSync) {
                syncBill(bill)
            }
            Logger.d("账单保存成功: ${bill.id}")
        }

        // 显示成功提示
        if (PrefManager.showSuccessPopup && AppAdapterManager.adapter().pkg == BuildConfig.APPLICATION_ID) {
            val message = autoApp.getString(
                R.string.auto_success,
                bill.money.toString()
            )
            ToastUtils.info(message)
        }
    }

}

/** Uri.Builder 扩展：仅在 value 非空时追加参数 */
fun Uri.Builder.appendIfNotBlank(key: String, value: String) {
    if (value.isNotEmpty()) appendQueryParameter(key, value)
}
