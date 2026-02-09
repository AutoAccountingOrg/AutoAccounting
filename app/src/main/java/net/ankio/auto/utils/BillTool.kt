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
import java.util.Locale

object BillTool {

    /**
     * 获取页面显示颜色
     */
    fun getColor(type: BillType): Int {
        val isExpenseRed = PrefManager.isExpenseRed

        return when (type) {
            BillType.Expend -> if (isExpenseRed) R.color.danger else R.color.success
            BillType.Income -> if (isExpenseRed) R.color.success else R.color.danger
            BillType.Transfer -> R.color.info
            else -> R.color.danger
        }
    }

    /**
     * 获取箭头图标
     */
    fun getIcon(type: BillType): Int {
        val isIncomeUp = PrefManager.isIncomeUp

        return when (type) {
            BillType.Income -> if (isIncomeUp) R.drawable.ic_trending_up else R.drawable.ic_trending_down
            BillType.Expend -> if (isIncomeUp) R.drawable.ic_trending_down else R.drawable.ic_trending_up
            BillType.Transfer -> R.drawable.ic_swap_horiz
            else -> R.drawable.ic_trending_down
        }
    }

    /**
     * 获取背景drawable
     */
    fun getBackground(type: BillType): Int {
        val isExpenseRed = PrefManager.isExpenseRed

        return when (type) {
            BillType.Expend -> if (isExpenseRed) R.drawable.bg_danger_icon else R.drawable.bg_success_icon
            BillType.Income -> if (isExpenseRed) R.drawable.bg_success_icon else R.drawable.bg_danger_icon

            else -> R.drawable.bg_danger_icon
        }
    }

    /**
     * 获取账单类型的完整样式信息
     * @return Triple(colorRes, iconRes, backgroundRes)
     */
    fun getStyle(type: BillType): Triple<Int, Int, Int> {
        return Triple(getColor(type), getIcon(type), getBackground(type))
    }

    /**
     * 格式化金额显示
     * 只显示数字，不带货币符号
     */
    fun formatAmount(amount: Double): String {
        return String.format(Locale.getDefault(), "%.2f", amount)
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


    fun setTextViewPrice(
        price: Double,
        type: BillType,
        view: MaterialTextView,
        currencyUnit: String? = null
    ) {
        val t = getType(type)
        val color = ContextCompat.getColor(view.context, getColor(t))
        view.setTextColor(color)
        // 有货币单位则追加到金额后面，如 "- 100.0 USD"
        val suffix = if (currencyUnit.isNullOrEmpty()) "" else " $currencyUnit"
        when (t) {
            BillType.Expend, BillType.ExpendReimbursement, BillType.ExpendLending, BillType.ExpendRepayment -> {
                view.text = "- $price$suffix"
            }

            BillType.Income, BillType.IncomeLending, BillType.IncomeRepayment, BillType.IncomeReimbursement -> {
                view.text = "+ $price$suffix"
            }

            else -> {
                view.text = "$price$suffix"
            }
        }
    }

    /**
     * 获取多币种转换提示文本
     *
     * 当启用多币种且账单币种与本位币不同时，返回 "≈ 72.30 CNY" 格式的文本；
     * 否则返回 null。
     *
     * @param bill 账单信息
     * @return 转换文本或 null
     */
    fun getConversionText(bill: BillInfoModel): String? {
        if (!PrefManager.featureMultiCurrency) return null
        val model = bill.currencyModel()
        val baseCurrency = PrefManager.baseCurrency
        if (model.code == baseCurrency || model.code.isEmpty()) return null
        if (model.rate <= 0) return null
        val converted = bill.money * model.rate
        return "≈ ${formatAmount(converted)} $baseCurrency"
    }

    fun getCateName(category1: String, category2: String? = null): String {

        if (category2 === null) {
            return category1
        }
        return "$category1 - $category2"
    }

    fun syncBills() {
        App.launch {
            // 开始同步提示
            ToastUtils.info(R.string.sync_starting)

            // 获取所有未同步的账单
            val billsToSync = BillAPI.sync()

            if (billsToSync.isEmpty()) {
                ToastUtils.info(R.string.sync_no_bills)
                return@launch
            }

            var syncedCount = 0

            // 逐个同步账单
            billsToSync.forEach { bill ->
                try {
                    syncBill(bill)
                    syncedCount++
                } catch (e: Exception) {
                    Logger.e("同步账单失败: ${bill.id}", e)
                    // 继续同步其他账单，不因单个失败而中断
                }
            }

            ToastUtils.info(autoApp.getString(R.string.sync_completed, syncedCount))


        }
    }

    suspend fun syncBill(billInfoModel: BillInfoModel) {
        AppAdapterManager.adapter().syncBill(billInfoModel)
        delay(AppAdapterManager.adapter().sleep())
    }

    fun saveBill(bill: BillInfoModel, onComplete: (() -> Unit)? = null) {
        Logger.d("保存账单: ${bill.id}")

        // 更新状态
        bill.state = BillState.Edited

        // 异步保存
        App.launchIO {
            try {
                BillAPI.put(bill)
                // 若未开启手动同步，则根据延迟同步阈值决定同步策略
                if (!PrefManager.manualSync && !bill.isChild()) {
                    val threshold = PrefManager.delayedSyncThreshold
                    if (threshold == 0) {
                        // 阈值为0表示实时同步，立即同步当前账单
                        syncBill(bill)
                    } else {
                        // 阈值大于0，检查未同步账单数量
                        val unsyncedBills = BillAPI.sync()
                        val unsyncedCount = unsyncedBills.size
                        Logger.d("未同步账单数量: $unsyncedCount, 阈值: $threshold")

                        // 如果未同步账单数量达到阈值，触发批量同步
                        if (unsyncedCount >= threshold) {
                            Logger.d("未同步账单达到阈值，触发批量同步")
                            syncBills()
                        }
                        // 否则延迟同步，不立即同步
                    }
                }
                Logger.d("账单保存成功: ${bill.id}")

                // 在UI线程执行回调
                App.launch {
                    // 显示成功提示
                    if (PrefManager.showSuccessPopup) {
                        val message = autoApp.getString(
                            R.string.auto_success,
                            bill.money.toString()
                        )
                        ToastUtils.info(message)
                    }

                    // 执行完成回调
                    onComplete?.invoke()
                }
            } catch (e: Exception) {
                Logger.e("账单保存失败: ${e.message}")
                App.launch {
                    onComplete?.invoke() // 即使失败也要执行回调
                }
            }
        }
    }

}

/** Uri.Builder 扩展：仅在 value 非空时追加参数 */
fun Uri.Builder.appendIfNotBlank(key: String, value: String) {
    if (value.isNotEmpty()) appendQueryParameter(key, value)
}
