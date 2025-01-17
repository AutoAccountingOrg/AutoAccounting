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

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.App
import net.ankio.auto.R
import net.ankio.auto.databinding.FloatEditorBinding
import net.ankio.auto.exceptions.BillException
import net.ankio.auto.service.FloatingWindowService
import net.ankio.auto.storage.ConfigUtils
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.componets.IconView
import net.ankio.auto.ui.utils.AssetsUtils
import net.ankio.auto.ui.utils.BookAppUtils
import net.ankio.auto.ui.utils.ListPopupUtils
import net.ankio.auto.ui.utils.ResourceUtils
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.utils.BillTool
import net.ankio.auto.utils.DateUtils
import org.ezbook.server.constant.AssetsType
import org.ezbook.server.constant.BillState
import org.ezbook.server.constant.BillType
import org.ezbook.server.constant.Currency
import org.ezbook.server.constant.DefaultData
import org.ezbook.server.constant.Setting
import org.ezbook.server.constant.SyncType
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.AssetsModel
import org.ezbook.server.db.model.BillInfoModel
import org.ezbook.server.db.model.BookNameModel
import java.util.Calendar

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
        return binding.root
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        val parent = view.parent as View
        parent.setPadding(0, 0, 0, 0)
        // 初始化账单信息
        Logger.d("Raw BillInfo => $rawBillInfo")
        billTypeLevel1 = BillTool.getType(rawBillInfo.type)
        billTypeLevel2 = rawBillInfo.type
        bindUI()
        bindEvents()
        // 处理来自记账服务的重复账单
        if (floatingWindowService != null) {
            App.launch(Dispatchers.Main) {
                runCatching {
                    val billInfo = floatingWindowService.bills.receive()
                    if (::binding.isInitialized) {
                        checkUpdateBills(billInfo)
                    }
                }
            }
        }

    }


    private suspend fun ensureAccountExists(
        accountName: String,
        assets: List<AssetsModel>,
        context: Context,
        billInfoModel: BillInfoModel,
        from: Boolean
    ) {
        if (assets.none { it.name == accountName }) {
            AssetsUtils.setMapAssets(billInfoModel)
            val account = if (from) billInfoModel.accountNameFrom else billInfoModel.accountNameTo
            if (assets.none { it.name == account }) {
                throw BillException(context.getString(R.string.expend_account_not_exist, account))
            }
        }
    }

    private fun checkAccountNotEmpty(accountName: String, errorMessage: String) {
        if (accountName.isEmpty()) {
            throw BillException(errorMessage)
        }
    }
    // 综合以上内容，应用到billInfo对象上

    private suspend fun getBillData(assets: List<AssetsModel>): BillInfoModel {
        val assetManager =
            ConfigUtils.getBoolean(Setting.SETTING_ASSET_MANAGER, DefaultData.SETTING_ASSET_MANAGER)
        val ignoreAsset = ConfigUtils.getBoolean(Setting.IGNORE_ASSET, DefaultData.IGNORE_ASSET)
    
        return billInfoModel.copy().apply {
            this.type = billTypeLevel2
            this.extendData = selectedBills.joinToString()
            val accountFrom = when (billTypeLevel2) {
                BillType.Expend, BillType.Income, BillType.ExpendReimbursement, BillType.IncomeRefund, BillType.IncomeReimbursement -> binding.payFrom.getText()
                    .toString()

                BillType.Transfer -> binding.transferFrom.getText().toString()
                BillType.ExpendLending, BillType.ExpendRepayment, BillType.IncomeLending, BillType.IncomeRepayment -> binding.debtExpendFrom.getText()
                    .toString()

                else -> ""
            }
            val accountTo = when (billTypeLevel2) {
                BillType.Transfer -> binding.transferTo.getText().toString()
                BillType.ExpendLending, BillType.ExpendRepayment, BillType.IncomeLending, BillType.IncomeRepayment -> binding.debtExpendTo.getText()
                    .toString()

                else -> ""
            }
            this.accountNameFrom = accountFrom
            this.accountNameTo = accountTo
            if (assetManager && !ignoreAsset) {
                checkAccountNotEmpty(accountFrom, context.getString(R.string.expend_account_empty))
                ensureAccountExists(accountFrom, assets, context, this@apply, true)
                if (accountTo.isNotEmpty()) {
                    ensureAccountExists(accountTo, assets, context, this@apply, false)
                }
            }
    

            this.remark = binding.remark.text.toString()
        }
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
                    if (ConfigUtils.getBoolean(
                            Setting.SHOW_SUCCESS_POPUP,
                            DefaultData.SHOW_SUCCESS_POPUP
                        )
                    ) {
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
                            DefaultData.AUTO_CREATE_CATEGORY,
                        )
                    ) {
                        // 弹出询问框
                        BillCategoryDialog(context, convertBillInfo).show(float, cancel = true)
                    }

                    BillTool.syncBills()

                }.onFailure {
                    Logger.e("Failed to record bill", it)
                }



                dismiss()
                onConfirmClick?.invoke(convertBillInfo)
            }
            //   dismiss()
        }
    }


    private fun bindingTypePopupUI() {
        binding.priceContainer.text = billInfoModel.money.toString()
    }

    private fun bindingTypePopupEvents() {
        val stringList: HashMap<String, Any> =
            if (ConfigUtils.getBoolean(
                    Setting.SETTING_ASSET_MANAGER,
                    DefaultData.SETTING_ASSET_MANAGER
                )
            )
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
                lifecycle
            ) { pos, key, value ->
                billTypeLevel1 = value as BillType
                billTypeLevel2 = billTypeLevel1
                rawBillInfo.type = billTypeLevel1
                bindUI()
            }

        // 修改账单类型
        binding.priceContainer.setOnClickListener { popupUtils.toggle() }
    }

    override fun onImeVisible() {
        //滚动到底部
        binding.scrollView.post {
            binding.scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun bindingFeeUI() {
        /*
        * 如果是转账，或者没有手续费，或者没有开启手续费，那么就不显示手续费
        * */

        if (
            !ConfigUtils.getBoolean(Setting.SETTING_FEE, DefaultData.SETTING_FEE) ||
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
            //Logger.d("BookName => ${billInfoModel.bookName}")
            val book = BookNameModel.getDefaultBook(billInfoModel.bookName)
            //Logger.d("Book => ${book.name}")
            ResourceUtils.getBookNameDrawable(book.name, context).let {
                binding.bookImage.setImageDrawable(it)
            }
        }
    }

    /**
     * 绑定账本名称事件
     */
    private fun bindingBookNameEvents() {
        binding.bookImageClick.setOnClickListener {
            if (!ConfigUtils.getBoolean(
                    Setting.SETTING_BOOK_MANAGER,
                    DefaultData.SETTING_BOOK_MANAGER
                )
            ) return@setOnClickListener
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
                view.setIcon(it, false)
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
                view.setIcon(it, false)
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
        if (!ConfigUtils.getBoolean(
                Setting.SETTING_CURRENCY_MANAGER,
                DefaultData.SETTING_CURRENCY_MANAGER
            )
        ) {
            binding.moneyType.visibility = View.GONE
            return
        }
        val currency =
            runCatching { Currency.valueOf(billInfoModel.currency) }.getOrDefault(Currency.CNY)
        binding.moneyType.setText(currency.name(context))
        binding.moneyType.setIcon(currency.icon(context), false)
    }

    private fun bindingMoneyTypeEvents() {
        if (!ConfigUtils.getBoolean(
                Setting.SETTING_CURRENCY_MANAGER,
                DefaultData.SETTING_CURRENCY_MANAGER
            )
        ) return
        binding.moneyType.setOnClickListener {
            val hashMap = Currency.getCurrencyMap(context)
            val popupUtils =
                ListPopupUtils(
                    context,
                    binding.moneyType,
                    hashMap,
                    billInfoModel.currency,
                    lifecycle
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

        val dialog = DateTimePickerDialog.withCurrentTime(
            context,
            false,
            context.getString(R.string.float_time)
        )

        val calendar =
            Calendar.getInstance().apply {
                timeInMillis = defaultTimestamp
            }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        dialog.setDateTime(year, month, day, hour, minute)

        dialog.setOnDateTimeSelectedListener { year, month, day, hour, minute ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month - 1)
            calendar.set(Calendar.DAY_OF_MONTH, day)
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            billInfoModel.time = calendar.timeInMillis
            bindingTimeUI()
        }

        dialog.bindToLifecycle(this)

        dialog.show(float, true)
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
            //报销\退款
            BillType.IncomeRefund,
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
        binding.chipRefund.visibility = View.GONE
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
                binding.chipRefund.visibility = View.VISIBLE
                binding.category.visibility = View.VISIBLE
                setBillTypeLevel2(billType)
            }

            BillType.Transfer -> {
                binding.transferInfo.visibility = View.VISIBLE
                setAssetItem(billInfoModel.accountNameFrom, binding.transferFrom)
                setAssetItem(billInfoModel.accountNameTo, binding.transferTo)
            }

            BillType.ExpendReimbursement,
            BillType.ExpendLending,
            BillType.ExpendRepayment,
            BillType.IncomeLending,
            BillType.IncomeRepayment,
            BillType.IncomeReimbursement,
            BillType.IncomeRefund -> return
        }


        if (!ConfigUtils.getBoolean(
                Setting.SETTING_ASSET_MANAGER,
                DefaultData.SETTING_ASSET_MANAGER
            )
        ) {
            binding.chipLend.visibility = View.GONE
            binding.chipBorrow.visibility = View.GONE
            binding.chipRepayment.visibility = View.GONE
            binding.payInfo.visibility = View.GONE
            binding.transferInfo.visibility = View.GONE
            binding.debtExpend.visibility = View.GONE
            binding.debtIncome.visibility = View.GONE
        }

        if (!ConfigUtils.getBoolean(Setting.SETTING_DEBT, DefaultData.SETTING_DEBT)) {
            binding.chipLend.visibility = View.GONE
            binding.chipBorrow.visibility = View.GONE
            binding.chipRepayment.visibility = View.GONE
        }

        if (!ConfigUtils.getBoolean(
                Setting.SETTING_REIMBURSEMENT,
                DefaultData.SETTING_REIMBURSEMENT
            )
        ) {
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
                R.id.chipRefund -> {
                    setBillTypeLevel2(BillType.IncomeRefund)
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
            val type = when (billTypeLevel2) {
                BillType.IncomeReimbursement -> Setting.HASH_BAOXIAO_BILL
                BillType.IncomeRefund -> Setting.HASH_BILL
                else -> Setting.HASH_BAOXIAO_BILL
            }
            // 收入对应的报销
            BillSelectorDialog(context, selectedBills, type) {
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
                    BillType.IncomeRefund -> R.id.chipRefund
                    else -> -1
                }
            )
            setBillTypeLevel2(rawBillInfo.type)
        }

        selectedBills = billInfoModel.extendData
            .split(", ")
            .map { it.trim() }
            .distinct()
            .toMutableList()


        if (ConfigUtils.getBoolean(Setting.SHOW_RULE_NAME, DefaultData.SHOW_RULE_NAME)) {
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
