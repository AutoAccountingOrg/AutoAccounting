/*
 * Copyright (C) 2023 ankio(ankio@ankio.net)
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

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.App
import net.ankio.auto.R
import net.ankio.auto.broadcast.LocalBroadcastHelper
import net.ankio.auto.databinding.FloatEditorBinding
import net.ankio.auto.exceptions.BillException
import net.ankio.auto.service.FloatingWindowService
import net.ankio.auto.storage.ConfigUtils
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.componets.IconView
import net.ankio.auto.ui.utils.ListPopupUtils
import net.ankio.auto.ui.utils.ResourceUtils
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.utils.BillTool
import net.ankio.auto.utils.DateUtils
import org.ezbook.server.constant.*
import org.ezbook.server.constant.Currency
import org.ezbook.server.db.model.AssetsMapModel
import org.ezbook.server.db.model.AssetsModel
import org.ezbook.server.db.model.BillInfoModel
import org.ezbook.server.db.model.BookNameModel
import java.util.*

class FloatEditorDialog(
    private val context: Context,
    private var billInfoModel: BillInfoModel,
    private val float: Boolean = false,
    private val onCancelClick: ((billInfoModel: BillInfoModel) -> Unit)? = null,
    private val onConfirmClick: ((billInfoModel: BillInfoModel) -> Unit)? = null,
    private val floatingWindowService: FloatingWindowService? = null,
) :
    BaseSheetDialog(context) {
    lateinit var binding: FloatEditorBinding
    private var billTypeLevel1 = BillType.Expend
    private var billTypeLevel2 = BillType.Expend

    // 原始的账单数据
    private var rawBillInfo = billInfoModel.copy()

    private var convertBillInfo = billInfoModel.copy()

    // 选择的账单ID
    private var selectedBills = mutableListOf<String>()


    private fun checkUpdateBills(bill: BillInfoModel): Boolean {
        billInfoModel = bill.copy()
        rawBillInfo = bill.copy()
        convertBillInfo = bill.copy()
        billTypeLevel1 = BillTool.getType(rawBillInfo.type)
        billTypeLevel2 = rawBillInfo.type
        bindUI()
        return true
    }

    override fun onCreateView(inflater: LayoutInflater): View {


        binding = FloatEditorBinding.inflate(inflater)
        cardView = binding.editorCard

        Logger.d("Raw BillInfo => $rawBillInfo")
        billTypeLevel1 = BillTool.getType(rawBillInfo.type)
        billTypeLevel2 = rawBillInfo.type
        bindUI()
        bindEvents()

        if (floatingWindowService != null) {
            App.launch(Dispatchers.Main) {
                val billInfo = floatingWindowService.bills.receive()
                if (::binding.isInitialized) {
                    checkUpdateBills(billInfo)
                }
            }
        }

        return binding.root
    }

    // 综合以上内容，应用到billInfo对象上

    private fun getBillData(assets: List<AssetsModel>): BillInfoModel {
        val assetManager = ConfigUtils.getBoolean(Setting.SETTING_ASSET_MANAGER, true)
        Logger.d("Get Bill Data, type=> $billTypeLevel2, type1=> $billTypeLevel1")
        return billInfoModel.copy().apply {
            this.type = billTypeLevel2
            when (billTypeLevel2) {
                BillType.Expend -> {
                    this.accountNameFrom = binding.payFrom.getText()
                    this.accountNameTo = ""
                    if (assetManager) {
                        if (this.accountNameFrom.isEmpty()) {
                            throw BillException(context.getString(R.string.expend_account_empty))
                        }

                        if (assets.find { it.name == this.accountNameFrom } == null) {
                            throw BillException(
                                context.getString(
                                    R.string.expend_account_not_exist,
                                    this.accountNameFrom
                                )
                            )
                        }
                    }

                }

                BillType.Income -> {
                    this.accountNameFrom = binding.payFrom.getText()
                    this.accountNameTo = ""
                    if (assetManager) {
                        if (this.accountNameFrom.isEmpty()) {
                            throw BillException(context.getString(R.string.income_account_empty))
                        }
                        if (assets.find { it.name == this.accountNameFrom } == null) {
                            throw BillException(
                                context.getString(
                                    R.string.expend_account_not_exist,
                                    this.accountNameFrom
                                )
                            )
                        }
                    }

                }

                BillType.Transfer -> {
                    this.accountNameFrom = binding.transferFrom.getText()
                    this.accountNameTo = binding.transferTo.getText()
                    if (assetManager) {
                        if (this.accountNameFrom.isEmpty()) {
                            throw BillException(context.getString(R.string.transfer_from_empty))
                        }
                        if (this.accountNameTo.isEmpty()) {
                            throw BillException(context.getString(R.string.transfer_to_empty))
                        }

                        if (this.accountNameFrom == this.accountNameTo) {
                            throw BillException(context.getString(R.string.transfer_same_account))
                        }

                        if (assets.find { it.name == this.accountNameFrom } == null) {
                            throw BillException(
                                context.getString(
                                    R.string.expend_account_not_exist,
                                    this.accountNameFrom
                                )
                            )
                        }

                        if (assets.find { it.name == this.accountNameTo } == null) {
                            throw BillException(
                                context.getString(
                                    R.string.expend_account_not_exist,
                                    this.accountNameTo
                                )
                            )
                        }

                    }
                }

                BillType.ExpendReimbursement -> {
                    this.accountNameFrom = binding.payFrom.getText()
                    this.accountNameTo = ""
                    if (assetManager) {
                        if (this.accountNameFrom.isEmpty()) {
                            throw BillException(context.getString(R.string.reimbursement_account_empty))
                        }
                        if (assets.find { it.name == this.accountNameFrom } == null) {
                            throw BillException(
                                context.getString(
                                    R.string.expend_account_not_exist,
                                    this.accountNameFrom
                                )
                            )
                        }

                    }

                }

                BillType.IncomeReimbursement -> {
                    this.accountNameFrom = binding.payFrom.getText()
                    this.extendData = selectedBills.joinToString { it }
                    if (assetManager) {
                        if (this.accountNameFrom.isEmpty()) {
                            throw BillException(context.getString(R.string.reimbursement_income_account_empty))
                        }
                        if (selectedBills.isEmpty()) {
                            throw BillException(context.getString(R.string.reimbursement_bill_empty))
                        }
                        if (assets.find { it.name == this.accountNameFrom } == null) {
                            throw BillException(
                                context.getString(
                                    R.string.expend_account_not_exist,
                                    this.accountNameFrom
                                )
                            )
                        }

                    }

                }
                // 借出,还款
                BillType.ExpendLending, BillType.ExpendRepayment -> {
                    this.accountNameFrom = binding.debtExpendFrom.getText()
                    this.accountNameTo = binding.debtExpendTo.getText().toString()
                    if (assetManager) {
                        if (this.accountNameFrom.isEmpty()) {
                            throw BillException(
                                if (BillType.ExpendLending == billTypeLevel2) context.getString(R.string.expend_debt_empty) else context.getString(
                                    R.string.repayment_account_empty
                                )
                            )
                        }
                        if (this.accountNameTo.isEmpty()) {
                            throw BillException(
                                if (BillType.ExpendLending == billTypeLevel2) context.getString(R.string.debt_account_empty) else context.getString(
                                    R.string.repayment_account2_empty
                                )
                            )
                        }
                        if (this.accountNameFrom == this.accountNameTo) {
                            throw BillException(context.getString(R.string.lending_same_account))
                        }


                    }

                }

                // 借入,收款
                BillType.IncomeLending, BillType.IncomeRepayment -> {
                    this.accountNameFrom = binding.debtIncomeFrom.getText().toString()
                    this.accountNameTo = binding.debtIncomeTo.getText()
                    if (assetManager) {
                        if (this.accountNameFrom.isEmpty()) {
                            throw BillException(
                                if (BillType.IncomeLending == billTypeLevel2) context.getString(R.string.income_debt_empty) else context.getString(
                                    R.string.income_lending_account_empty
                                )
                            )
                        }
                        if (this.accountNameTo.isEmpty()) {
                            throw BillException(
                                if (BillType.IncomeLending == billTypeLevel2) context.getString(R.string.income_lending_account2_empty) else context.getString(
                                    R.string.income_repayment_account_empty
                                )
                            )
                        }
                        if (this.accountNameFrom == this.accountNameTo) {
                            throw BillException(context.getString(R.string.lending_same_account))
                        }

                    }

                }

            }
            this.remark = binding.remark.text.toString()
        }
    }

    override fun dismiss() {
        super.dismiss()
    }

    private fun bindingButtonsEvents() {
        // 关闭按钮
        binding.cancelButton.setOnClickListener {
            dismiss()
            onCancelClick?.invoke(convertBillInfo)
        }
        // 确定按钮
        binding.sureButton.setOnClickListener {


            lifecycleScope.launch {
                val assets = AssetsModel.list()
                try {
                    convertBillInfo = getBillData(assets)
                } catch (e: BillException) {
                    ToastUtils.error(e.message ?: "未知错误")
                    Logger.e("Failed to get bill data", e)
                    return@launch
                }

                convertBillInfo.state = BillState.Edited
                Logger.d("Save Bill => $convertBillInfo")
                runCatching {
                    BillInfoModel.put(convertBillInfo)
                    if (ConfigUtils.getBoolean(Setting.SHOW_SUCCESS_POPUP, true)) {
                        ToastUtils.info(
                            context.getString(
                                R.string.auto_success,
                                billInfoModel.money.toString(),
                            ),
                        )
                    }

                    if (rawBillInfo.cateName != convertBillInfo.cateName &&
                        ConfigUtils.getBoolean(
                            Setting.AUTO_CREATE_CATEGORY,
                            false,
                        )
                    ) {
                        // 弹出询问框
                        BillCategoryDialog(context, convertBillInfo).show(float, cancel = true)
                    }

                    if (ConfigUtils.getBoolean(Setting.AUTO_ASSET, false)) {

                        setAccountMap(assets, rawBillInfo.accountNameFrom, convertBillInfo.accountNameFrom)
                        setAccountMap(assets, rawBillInfo.accountNameTo, convertBillInfo.accountNameTo)
                    }

                    val syncType = ConfigUtils.getString(Setting.SYNC_TYPE, SyncType.WhenOpenApp.name)
                    if (syncType != SyncType.WhenOpenApp.name) {
                        val bills = BillInfoModel.sync()
                        if ((syncType == SyncType.BillsLimit10.name && bills.size >= 10) || (syncType == SyncType.BillsLimit5.name && bills.size >= 5)) {
                            App.startBookApp()
                        }
                    }

                }.onFailure {
                    Logger.e("Failed to record bill", it)
                }



                dismiss()
                onConfirmClick?.invoke(convertBillInfo)
            }
            //   dismiss()
        }
    }


    private suspend fun setAccountMap(assets: List<AssetsModel>, accountName: String, eqAccountName: String) =
        withContext(Dispatchers.IO) {

            if (accountName.isEmpty()) return@withContext
            if (accountName == eqAccountName) return@withContext
            //非标准资产需要映射
            val find = assets.find { it.name == accountName }
            if (find == null) return@withContext
            Logger.d("Create new asset map => $accountName -> $eqAccountName")
            AssetsMapModel.put(AssetsMapModel().apply {
                this.name = accountName
                this.mapName = eqAccountName
            })


        }

    private fun bindingTypePopupUI() {
        binding.priceContainer.text = billInfoModel.money.toString()
    }

    private fun bindingTypePopupEvents() {
        val stringList: HashMap<String, Any> =
            if (ConfigUtils.getBoolean(Setting.SETTING_ASSET_MANAGER, true))
                hashMapOf(
                    context.getString(R.string.float_expend) to BillType.Expend,
                    context.getString(R.string.float_income) to BillType.Income,
                    context.getString(R.string.float_transfer) to BillType.Transfer,
                ) else hashMapOf(
                context.getString(R.string.float_expend) to BillType.Expend,
                context.getString(R.string.float_income) to BillType.Income,
            )

        val popupUtils =
            ListPopupUtils(
                context,
                binding.priceContainer,
                stringList,
                billTypeLevel1,
            ) { pos, key, value ->
                billTypeLevel1 = value as BillType
                billTypeLevel2 = billTypeLevel1
                rawBillInfo.type = billTypeLevel1
                bindUI()
            }

        // 修改账单类型
        binding.priceContainer.setOnClickListener { popupUtils.toggle() }
    }

    private fun bindingFeeUI() {
        /*
        * 如果是转账，或者没有手续费，或者没有开启手续费，那么就不显示手续费
        * */

        if (
            !ConfigUtils.getBoolean(Setting.SETTING_FEE, false) ||
            billTypeLevel1 != BillType.Transfer ||
            billInfoModel.fee == 0.0
        ) {
            binding.fee.visibility = View.GONE
            return
        }
        binding.fee.visibility = View.VISIBLE
        binding.fee.setText(billInfoModel.fee.toString())
    }

    private fun bindingFeeEvents() {
        // TODO 手续费需要处理弹出输入框,也许不需要处理，等一个有手续费的数据作为参考
    }

    /**
     * 设置账本名称和图标
     */
    private fun bindingBookNameUI() {
        lifecycleScope.launch {
            ResourceUtils.getBookNameDrawable(billInfoModel.bookName, context).let {
                binding.bookImage.setImageDrawable(it)
            }
        }
    }

    /**
     * 绑定账本名称事件
     */
    private fun bindingBookNameEvents() {
        binding.bookImageClick.setOnClickListener {
            if (!ConfigUtils.getBoolean(Setting.SETTING_BOOK_MANAGER, true)) return@setOnClickListener
            BookSelectorDialog(context) { book, _ ->
                billInfoModel.bookName = book.name
                bindingBookNameUI()
            }.show(float, cancel = true)
        }
    }

    /**
     * 设置资产名称和图标
     */
    private fun setAssetItem(
        name: String,
        view: IconView,
    ) {
        view.setText(name)
        lifecycleScope.launch {
            ResourceUtils.getAssetDrawableFromName(name).let {
                view.setIcon(it)
            }
        }
    }

    /**
     * 设置资产名称和图标
     */
    private fun setAssetItem(
        name: String,
        icon: String,
        view: IconView,
    ) {
        view.setText(name)
        lifecycleScope.launch {
            ResourceUtils.getAssetDrawable(icon).let {
                view.setIcon(it)
            }
        }
    }

    private fun bindingCategoryUI() {
        lifecycleScope.launch {
            val book = BookNameModel.getDefaultBook(billInfoModel.bookName)
            ResourceUtils.getCategoryDrawableByName(
                billInfoModel.cateName,
                context,
                book.remoteId,
                billTypeLevel2.name
            ).let {
                binding.category.setIcon(it, true)
            }
        }
        binding.category.setText(billInfoModel.cateName)
    }

    private fun bindingCategoryEvents() {
        binding.category.setOnClickListener {
            lifecycleScope.launch {
                val book = BookNameModel.getDefaultBook(billInfoModel.bookName)
                withContext(Dispatchers.Main) {
                    CategorySelectorDialog(
                        context,
                        book.remoteId,
                        billTypeLevel1
                    ) { parent, child ->
                        if (parent == null) return@CategorySelectorDialog
                        billInfoModel.cateName =
                            BillTool.getCateName(parent.name ?: "", child?.name)

                        bindingCategoryUI()
                    }.show(float, cancel = true)
                }
            }
        }
    }

    private fun bindingMoneyTypeUI() {
        if (!ConfigUtils.getBoolean(Setting.SETTING_CURRENCY_MANAGER, false)) {
            binding.moneyType.visibility = View.GONE
            return
        }
        val currency = runCatching { Currency.valueOf(billInfoModel.currency) }.getOrDefault(Currency.CNY)
        binding.moneyType.setText(currency.name(context))
        binding.moneyType.setIcon(currency.icon(context))
    }

    private fun bindingMoneyTypeEvents() {
        if (!ConfigUtils.getBoolean(Setting.SETTING_CURRENCY_MANAGER, false)) return
        binding.moneyType.setOnClickListener {
            val hashMap = Currency.getCurrencyMap(context)
            val popupUtils =
                ListPopupUtils(
                    context,
                    binding.moneyType,
                    hashMap,
                    billInfoModel.currency,
                ) { pos, key, value ->
                    billInfoModel.currency = (value as Currency).name
                    bindingMoneyTypeUI()
                }
            popupUtils.toggle()
        }
    }

    private fun bindingTimeUI() {
        binding.time.setText(DateUtils.stampToDate(billInfoModel.time))
    }

    private fun showDateTimePicker(defaultTimestamp: Long) {
        val calendar =
            Calendar.getInstance().apply {
                timeInMillis = defaultTimestamp
            }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)


        // 创建并显示日期选择器
        val datePickerDialog =
            DatePickerDialog(
                context,//R.style.CustomDatePickerDialogTheme,
                { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDay: Int ->
                    // 更新日历对象的日期
                    calendar.set(Calendar.YEAR, selectedYear)
                    calendar.set(Calendar.MONTH, selectedMonth)
                    calendar.set(Calendar.DAY_OF_MONTH, selectedDay)

                    // 创建并显示时间选择器
                    val timePickerDialog =
                        TimePickerDialog(
                            context,
                            { _: TimePicker, selectedHour: Int, selectedMinute: Int ->
                                // 更新日历对象的时间
                                calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                                calendar.set(Calendar.MINUTE, selectedMinute)

                                // 最终的日期和时间结果
                                billInfoModel.time = calendar.timeInMillis
                                bindingTimeUI()
                                // 处理最终的时间戳
                            },
                            hour,
                            minute,
                            true,
                        )
                    if (float) {
                        timePickerDialog.window!!.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
                    }

                    timePickerDialog.show()
                },
                year,
                month,
                day,
            )
        if (float) {
            datePickerDialog.window!!.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        }
        //  datePickerDialog.datePicker.setBackgroundColor(surfaceColor)
        datePickerDialog.show()
    }

    private fun bindingTimeEvents() {
        binding.time.setOnClickListener {
            // 弹出时间选择器 时间选择器
            showDateTimePicker(billInfoModel.time)
        }
    }

    private fun bindingRemarkUI() {
        binding.remark.setText(billInfoModel.remark)
    }

    private fun setBillTypeLevel2(billType: BillType) {
        if (billType == BillType.Transfer) return
        binding.payInfo.visibility = View.GONE
        binding.debtExpend.visibility = View.GONE
        binding.debtIncome.visibility = View.GONE
        binding.chooseBill.visibility = View.GONE
        binding.category.visibility = View.GONE
        billTypeLevel2 = billType

        when (billType) {
            BillType.Expend -> {
                binding.payInfo.visibility = View.VISIBLE
                setAssetItem(billInfoModel.accountNameFrom, binding.payFrom)
                binding.category.visibility = View.VISIBLE
            }
            //报销
            BillType.ExpendReimbursement -> {
                binding.payInfo.visibility = View.VISIBLE
                setAssetItem(billInfoModel.accountNameFrom, binding.payFrom)
                binding.category.visibility = View.VISIBLE
            }
            //借出
            BillType.ExpendLending -> {
                binding.debtExpend.visibility = View.VISIBLE
                binding.debtExpendToLayout.setHint(R.string.float_expend_debt)
                setAssetItem(billInfoModel.accountNameFrom, binding.debtExpendFrom)
                binding.debtExpendTo.setText(billInfoModel.accountNameTo.ifEmpty { billInfoModel.shopName })
                lifecycleScope.launch {
                    AssetsModel.list().filter { it.type == AssetsType.BORROWER }.let { assets ->
                        withContext(Dispatchers.Main) {
                            binding.debtExpendTo.setSimpleItems(assets.map { it.name }
                                .toTypedArray())
                        }
                    }
                }
            }
            //还款
            BillType.ExpendRepayment -> {
                binding.debtExpend.visibility = View.VISIBLE
                //binding.chooseBill.visibility = View.VISIBLE
                binding.debtExpendToLayout.setHint(R.string.float_income_debt)
                setAssetItem(billInfoModel.accountNameFrom, binding.debtExpendFrom)
                binding.debtExpendTo.setText(billInfoModel.accountNameTo.ifEmpty { billInfoModel.shopName })
                lifecycleScope.launch {
                    AssetsModel.list().filter { it.type == AssetsType.CREDITOR }.let { assets ->
                        withContext(Dispatchers.Main) {
                            binding.debtExpendTo.setSimpleItems(assets.map { it.name }
                                .toTypedArray())
                        }
                    }
                }
            }

            BillType.Income -> {
                binding.payInfo.visibility = View.VISIBLE
                setAssetItem(billInfoModel.accountNameFrom, binding.payFrom)
                binding.category.visibility = View.VISIBLE
            }
            //借入
            BillType.IncomeLending -> {
                binding.debtIncome.visibility = View.VISIBLE
                binding.debtIncomeFromLayout.setHint(R.string.float_income_debt)
                binding.debtIncomeFrom.setText(billInfoModel.accountNameFrom.ifEmpty { billInfoModel.shopName })
                setAssetItem(billInfoModel.accountNameTo, binding.debtIncomeTo)
                lifecycleScope.launch {
                    AssetsModel.list().filter { it.type == AssetsType.CREDITOR }.let { assets ->
                        withContext(Dispatchers.Main) {
                            binding.debtIncomeFrom.setSimpleItems(assets.map { it.name }
                                .toTypedArray())
                        }
                    }
                }
            }
            //收款
            BillType.IncomeRepayment -> {
                binding.debtIncome.visibility = View.VISIBLE
                //binding.chooseBill.visibility = View.VISIBLE
                binding.debtIncomeFromLayout.setHint(R.string.float_expend_debt)
                binding.debtIncomeFrom.setText(billInfoModel.accountNameFrom.ifEmpty { billInfoModel.shopName })
                setAssetItem(billInfoModel.accountNameTo, binding.debtIncomeTo)
                lifecycleScope.launch {
                    AssetsModel.list().filter { it.type == AssetsType.BORROWER }.let { assets ->
                        withContext(Dispatchers.Main) {
                            binding.debtIncomeFrom.setSimpleItems(assets.map { it.name }
                                .toTypedArray())
                        }
                    }
                }
            }
            //报销
            BillType.IncomeReimbursement -> {
                binding.chooseBill.visibility = View.VISIBLE
                binding.payInfo.visibility = View.VISIBLE
                setAssetItem(billInfoModel.accountNameFrom, binding.payFrom)
                binding.category.visibility = View.VISIBLE
                selectedBills.clear()
                bindingSelectBillsUi()
            }

            BillType.Transfer -> return
        }
    }

    private fun setBillType(billType: BillType) {
        setPriceColor(billType)
        billTypeLevel1 = billType
        billTypeLevel2 = billType

        binding.chipGroup.visibility = View.GONE
        binding.payInfo.visibility = View.GONE
        binding.transferInfo.visibility = View.GONE
        binding.debtExpend.visibility = View.GONE
        binding.debtIncome.visibility = View.GONE
        binding.chooseBill.visibility = View.GONE

        binding.chipLend.visibility = View.GONE
        binding.chipBorrow.visibility = View.GONE

        binding.chipGroup.clearCheck()


        binding.fee.visibility = View.GONE

        setAssetItem("", binding.payFrom)
        setAssetItem("", binding.transferFrom)
        setAssetItem("", binding.transferTo)
        setAssetItem("", binding.debtExpendFrom)
        binding.debtExpendTo.setText("")
        binding.debtIncomeFrom.setText("")
        setAssetItem("", binding.debtIncomeTo)
        selectedBills.clear()
        binding.category.visibility = View.GONE
        when (billType) {
            BillType.Expend -> {
                binding.chipRepayment.text = context.getString(R.string.expend_repayment)
                binding.chipGroup.visibility = View.VISIBLE
                binding.chipLend.visibility = View.VISIBLE
                binding.category.visibility = View.VISIBLE
                setBillTypeLevel2(billType)
            }

            BillType.Income -> {
                binding.chipRepayment.text = context.getString(R.string.income_repayment)
                binding.chipGroup.visibility = View.VISIBLE
                binding.chipBorrow.visibility = View.VISIBLE
                binding.category.visibility = View.VISIBLE
                setBillTypeLevel2(billType)
            }

            BillType.Transfer -> {
                binding.transferInfo.visibility = View.VISIBLE
                setAssetItem(billInfoModel.accountNameFrom, binding.transferFrom)
                setAssetItem(billInfoModel.accountNameTo, binding.transferTo)
            }

            BillType.ExpendReimbursement -> return
            BillType.ExpendLending -> return
            BillType.ExpendRepayment -> return
            BillType.IncomeLending -> return
            BillType.IncomeRepayment -> return
            BillType.IncomeReimbursement -> return
        }


        if (!ConfigUtils.getBoolean(Setting.SETTING_ASSET_MANAGER, true)) {
            binding.chipLend.visibility = View.GONE
            binding.chipBorrow.visibility = View.GONE
            binding.chipRepayment.visibility = View.GONE
            binding.payInfo.visibility = View.GONE
            binding.transferInfo.visibility = View.GONE
            binding.debtExpend.visibility = View.GONE
            binding.debtIncome.visibility = View.GONE
        }

        if (!ConfigUtils.getBoolean(Setting.SETTING_DEBT, true)) {
            binding.chipLend.visibility = View.GONE
            binding.chipBorrow.visibility = View.GONE
            binding.chipRepayment.visibility = View.GONE
        }

        if (!ConfigUtils.getBoolean(Setting.SETTING_REIMBURSEMENT, true)) {
            binding.chipReimbursement.visibility = View.GONE
        }
    }

    private fun bindingChipGroupEvents() {
        binding.chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            val check = checkedIds.firstOrNull()
            if (check == null) {
                setBillTypeLevel2(billTypeLevel1)
                return@setOnCheckedStateChangeListener
            }
            when (check) {
                R.id.chipReimbursement -> {
                    if (billTypeLevel1 == BillType.Expend) {
                        setBillTypeLevel2(BillType.ExpendReimbursement)
                    } else {
                        setBillTypeLevel2(BillType.IncomeReimbursement)
                    }

                }

                R.id.chipLend -> {
                    setBillTypeLevel2(BillType.ExpendLending)
                }

                R.id.chipBorrow -> {
                    setBillTypeLevel2(BillType.IncomeLending)
                }

                R.id.chipRepayment -> {
                    if (billTypeLevel1 == BillType.Expend) {
                        setBillTypeLevel2(BillType.ExpendRepayment)
                    } else {
                        setBillTypeLevel2(BillType.IncomeRepayment)
                    }
                }
            }
        }
    }

    private fun bindingSelectBillsUi() {
        if (selectedBills.isEmpty()) {
            binding.chooseBill.setText(R.string.float_choose_bill)
        } else {
            binding.chooseBill.text =
                context.getString(R.string.float_choose_bills, selectedBills.size)
        }
    }

    private fun bindingSelectBillsEvents() {
        binding.chooseBill.setOnClickListener {

            // 收入对应的报销

            BillSelectorDialog(context, selectedBills) {
                bindingSelectBillsUi()
            }.show(float, cancel = true)
        }
    }

    private fun bindUI() {

        setBillType(billTypeLevel1)
        binding.chipGroup.clearCheck()
        if (billTypeLevel1 != BillType.Transfer && billTypeLevel1 != rawBillInfo.type) {
            binding.chipGroup.check(
                when (rawBillInfo.type) {
                    BillType.ExpendReimbursement -> R.id.chipReimbursement
                    BillType.ExpendLending -> R.id.chipLend
                    BillType.ExpendRepayment -> R.id.chipRepayment
                    BillType.IncomeLending -> R.id.chipBorrow
                    BillType.IncomeRepayment -> R.id.chipRepayment
                    BillType.IncomeReimbursement -> R.id.chipReimbursement
                    else -> -1
                }
            )
            setBillTypeLevel2(rawBillInfo.type)
        }

        selectedBills = billInfoModel.extendData.split(",").toMutableList()

        if (ConfigUtils.getBoolean(Setting.SHOW_RULE_NAME, true)) {
            binding.ruleName.visibility = View.VISIBLE
            binding.ruleName.setText(billInfoModel.ruleName)
        } else {
            binding.ruleName.visibility = View.GONE
        }

        bindingBookNameUI()
        bindingTypePopupUI()
        bindingFeeUI()
        bindingCategoryUI()
        bindingMoneyTypeUI()
        bindingTimeUI()
        bindingRemarkUI()


        bindChangeAssets()
        bindingSelectBillsUi()

    }

    private fun bindChangeAssets() {
        binding.payFrom.setOnClickListener {
            AssetsSelectorDialog(context) { model ->
                setAssetItem(model.name, model.icon, binding.payFrom)
            }.show(float = float, cancel = true)
        }
        binding.transferFrom.setOnClickListener {
            AssetsSelectorDialog(context) { model ->
                setAssetItem(model.name, model.icon, binding.transferFrom)
            }.show(float = float, cancel = true)
        }

        binding.transferTo.setOnClickListener {
            AssetsSelectorDialog(context) { model ->
                setAssetItem(model.name, model.icon, binding.transferTo)
            }.show(float = float, cancel = true)
        }

        binding.debtExpendFrom.setOnClickListener {
            AssetsSelectorDialog(context) { model ->
                setAssetItem(model.name, model.icon, binding.debtExpendFrom)
            }.show(float = float, cancel = true)
        }

        binding.debtIncomeTo.setOnClickListener {
            AssetsSelectorDialog(context) { model ->
                setAssetItem(model.name, model.icon, binding.debtIncomeTo)
            }.show(float = float, cancel = true)
        }
    }

    /**
     * 绑定事件
     */
    private fun bindEvents() {
        bindingBookNameEvents()
        bindingTypePopupEvents()
        bindingFeeEvents()

        bindingCategoryEvents()
        bindingMoneyTypeEvents()
        bindingTimeEvents()
        bindingButtonsEvents()
        bindingChipGroupEvents()

        bindingSelectBillsEvents()
    }

    /**
     * 设置价格颜色
     */
    private fun setPriceColor(billType: BillType) {
        val drawableRes =
            when (billType) {
                BillType.Expend -> R.drawable.float_minus
                BillType.Income -> R.drawable.float_add
                BillType.Transfer -> R.drawable.float_round
                else -> R.drawable.float_minus
            }

        val tintRes = BillTool.getColor(billType)

        val drawable = AppCompatResources.getDrawable(context, drawableRes)
        val tint = ColorStateList.valueOf(ContextCompat.getColor(context, tintRes))
        val color = ContextCompat.getColor(context, tintRes)

        binding.priceContainer.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
        TextViewCompat.setCompoundDrawableTintList(binding.priceContainer, tint)
        binding.priceContainer.setTextColor(color)
    }
}
