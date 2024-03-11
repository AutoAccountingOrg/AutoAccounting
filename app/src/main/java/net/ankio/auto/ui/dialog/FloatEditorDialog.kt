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
import com.hjq.toast.Toaster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.app.BillUtils
import net.ankio.auto.database.table.BillInfo
import net.ankio.auto.database.table.BookName
import net.ankio.auto.databinding.FloatEditorBinding
import net.ankio.auto.ui.componets.IconView
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.DateUtils
import net.ankio.auto.utils.ListPopupUtils
import net.ankio.auto.utils.Logger
import net.ankio.auto.utils.SpUtils
import net.ankio.common.config.AccountingConfig
import net.ankio.common.constant.BillType
import net.ankio.common.constant.Currency

class FloatEditorDialog(private val context: Context, private val billInfo: BillInfo, private  val autoAccountingConfig: AccountingConfig,private val float:Boolean = false) :
    BaseSheetDialog(context) {
    lateinit var binding: FloatEditorBinding
    private var billTypeLevel1 = BillType.Expend
    private var billTypeLevel2 = BillType.Expend
    private var billCategory = ""
    override fun onCreateView(inflater: LayoutInflater): View {
        binding = FloatEditorBinding.inflate(inflater)
        cardView = binding.editorCard

        Logger.d("onCreateView => $billInfo")

        billTypeLevel1 = billInfo.type
        billTypeLevel2 = billInfo.type
        binding.radioContainer.check(binding.radioNone.id)
        billCategory = billInfo.cateName


     //   billInfo.remark = BillUtils.getRemark(billInfo)

        bindUI()

        bindEvents()


        //关闭按钮
        binding.cancelButton.setOnClickListener {
            dismiss()
        }
        //确定按钮
        binding.sureButton.setOnClickListener {


            //TODO 应用UI上的更改


            lifecycleScope.launch {
                BillUtils.groupBillInfo(billInfo)
                if (!SpUtils.getBoolean("float_no_disturb")) {
                    Toaster.show(
                        context.getString(
                            R.string.auto_success,
                            billInfo.money.toString()
                        )
                    )
                }
            }
        }


        return binding.root
    }


    private fun bindingTypePopupUI(){
        binding.priceContainer.text = billInfo.money.toString()
        setPriceColor(billTypeLevel1.toInt())
    }

    private fun bindingTypePopupEvents() {

        val stringList : HashMap<String, Any> = hashMapOf(
            context.getString(R.string.float_expend) to BillType.Expend,
            context.getString(R.string.float_income) to BillType.Income,
            context.getString(R.string.float_transfer) to BillType.Transfer
        )


        val popupUtils = ListPopupUtils(context, binding.priceContainer,stringList,billTypeLevel1) { pos,key,value ->
            billInfo.type = value as BillType
            billTypeLevel1 = billInfo.type
            billTypeLevel2 = billInfo.type
            binding.radioContainer.check(binding.radioNone.id)
            bindUI()
        }

        //修改账单类型
        binding.priceContainer.setOnClickListener { popupUtils.toggle() }


    }


    private fun bindingFeeUI(){
        if(!autoAccountingConfig.fee){
            binding.fee.visibility = View.GONE
        }else{
            binding.fee.visibility = View.VISIBLE
            binding.fee.setText(billInfo.fee.toString())
        }
    }

    private fun bindingFeeEvents(){
        //TODO 手续费需要处理弹出输入框,也许不需要处理
    }


    private fun bindingBookNameUI(){
        Logger.d("bindingBookNameUI => ${billInfo.bookName}")
        lifecycleScope.launch {
            BillInfo.getBookDrawable(billInfo.bookName, context, binding.bookImage)
        }
    }

    private fun bindingBookNameEvents(){
        binding.bookImage.setOnClickListener {
            if(!autoAccountingConfig.multiBooks)return@setOnClickListener
            BookSelectorDialog(context){
                billInfo.bookName = it.name
                bindingBookNameUI()
            }.show(float)
        }

    }

    /**
     * 设置资产名称和图标
     */
    private fun setAssetItem(name:String,view:IconView){
        view.setText(name)
        lifecycleScope.launch {
            BillInfo.getAccountDrawable(name,context){
                view.setIcon(it)
            }
        }
    }

    private fun bindingPayInfoUI(){
        binding.payInfo.visibility = View.GONE
        if(!autoAccountingConfig.assetManagement ||  billTypeLevel1 ==BillType.Transfer){
            //没有资产管理
            return
        }

        if(
             billTypeLevel2 != BillType.ExpendLending //借出
            && billTypeLevel2 != BillType.IncomeLending //借入
            ){ //这三个有单独的UI
            //收入销账、收入报销
            binding.payInfo.visibility = View.VISIBLE
           setAssetItem(billInfo.accountNameFrom,binding.payFrom)
        }

        //如果是债务销账，则显示这个UI

        if(billTypeLevel2 == BillType.ExpendRepayment || billTypeLevel2 == BillType.IncomeRepayment){
            binding.payInfo.visibility = View.VISIBLE
            setAssetItem(billInfo.accountNameFrom,binding.payFrom)
        }

    }

    private fun bindingPayInfoEvents(){
        binding.payFrom.setOnClickListener {
            if(!autoAccountingConfig.assetManagement)return@setOnClickListener
            AssetsSelectorDialog(context){
                billInfo.accountNameFrom = it.name
                bindingPayInfoUI()
            }.show(float)
        }
    }

    private fun bindingTransferInfoUI(){
        binding.transferInfo.visibility = View.GONE
        if(!autoAccountingConfig.assetManagement){
            return
        }
        //只有转账生效
        if(billTypeLevel1 == BillType.Transfer){
            binding.transferInfo.visibility = View.VISIBLE
            setAssetItem(billInfo.accountNameFrom,binding.transferFrom)
            setAssetItem(billInfo.accountNameTo,binding.transferTo)
        }
    }

    private fun bindingTransferInfoEvents(){
        binding.transferFrom.setOnClickListener {
            if(!autoAccountingConfig.assetManagement)return@setOnClickListener
            AssetsSelectorDialog(context){
                billInfo.accountNameFrom = it.name
                bindingTransferInfoUI()
            }.show(float)
        }
        binding.transferTo.setOnClickListener {
            if(!autoAccountingConfig.assetManagement)return@setOnClickListener
            AssetsSelectorDialog(context){
                billInfo.accountNameTo = it.name
                bindingTransferInfoUI()
            }.show(float)
        }
    }

    //借出
    private fun bindingDebtExpendUI(){
        binding.debtExpend.visibility = View.GONE
        if(!autoAccountingConfig.assetManagement){
            return
        }

        if(billTypeLevel1 == BillType.Expend && billTypeLevel2 == BillType.ExpendLending){
            binding.debtExpend.visibility = View.VISIBLE
            setAssetItem(billInfo.accountNameFrom,binding.debtExpendFrom)
            binding.debtExpendTo.setText(billInfo.shopName)
        }

    }

    private fun bindingDebtExpendEvents(){
        binding.debtExpendFrom.setOnClickListener {
            if(!autoAccountingConfig.assetManagement)return@setOnClickListener
            AssetsSelectorDialog(context){
                billInfo.accountNameFrom = it.name
                bindingDebtExpendUI()
            }.show(float)
        }
    }

    //借入
    private fun bindingDebtIncomeUI(){
        binding.debtIncome.visibility = View.GONE
        if(!autoAccountingConfig.assetManagement ){
            return
        }

        if(billTypeLevel1 == BillType.Income && billTypeLevel2 == BillType.IncomeLending){
            binding.debtIncome.visibility = View.VISIBLE
            setAssetItem(billInfo.accountNameFrom,binding.debtIncomeTo)
            binding.debtIncomeFrom.setText(billInfo.shopName)
        }
    }

    private fun bindingDebtIncomeEvents(){
        binding.debtIncomeTo.setOnClickListener {
            if(!autoAccountingConfig.assetManagement)return@setOnClickListener
            AssetsSelectorDialog(context){
                billInfo.accountNameTo = it.name
                bindingDebtIncomeUI()
            }.show(float)
        }
    }

    private fun bindingRadioUI(){

        if(billTypeLevel1 == BillType.Transfer){
            binding.radioContainer.visibility = View.GONE
            return
        }

        if(!autoAccountingConfig.lending && !autoAccountingConfig.reimbursement){
            binding.radioContainer.visibility = View.GONE
            return
        }
        binding.radioContainer.visibility = View.VISIBLE
        if(!autoAccountingConfig.reimbursement){
            binding.radioReimbursement.visibility = View.GONE
        }else{
            binding.radioReimbursement.visibility = View.VISIBLE
        }

        if(!autoAccountingConfig.lending){
            binding.radioDebt.visibility = View.GONE
        }else{
            binding.radioDebt.visibility = View.VISIBLE
        }


    }

    //是否为支出
    private fun isPay():Boolean{
        return billTypeLevel1 == BillType.Expend
    }

    //是否为债务
    private fun isDebt():Boolean{
        return billTypeLevel2 == BillType.ExpendLending || billTypeLevel2 == BillType.IncomeLending || billTypeLevel2 == BillType.ExpendRepayment || billTypeLevel2 == BillType.IncomeRepayment
    }

    //是否为报销
    private fun isReimbursement():Boolean{
        return billTypeLevel2 == BillType.ExpendReimbursement || billTypeLevel2 == BillType.IncomeReimbursement
    }




    private fun bindingRadioEvents(){
        binding.radioNone.setOnClickListener {
            billTypeLevel2 = if(isPay()) BillType.Expend else BillType.Income
            bindUI()
        }
        binding.radioDebt.setOnClickListener {
            billTypeLevel2 = if(isPay()) BillType.ExpendLending else BillType.IncomeLending
            binding.reimbursement.isChecked = false
            bindUI()
        }
        binding.radioReimbursement.setOnClickListener {
            billTypeLevel2 = if(isPay()) BillType.ExpendReimbursement else BillType.IncomeReimbursement
            bindUI()
        }
    }

    private fun setDebt(isChecked:Boolean){
        billTypeLevel2 = if(isChecked){
            if(billTypeLevel1==BillType.Income) BillType.IncomeRepayment else BillType.ExpendRepayment
        }else{
            if(billTypeLevel1==BillType.Income) BillType.IncomeLending else BillType.ExpendLending
        }

    }

    private fun bindingDebtUI(){
        binding.debtContainer.visibility = View.GONE
        if(!autoAccountingConfig.lending
            || !isDebt()){

            return
        }
        setDebt(binding.reimbursement.isChecked)
        binding.debtContainer.visibility = View.VISIBLE




        binding.chooseDebt.visibility = if(billTypeLevel2 == BillType.IncomeRepayment || billTypeLevel2 == BillType.ExpendRepayment) View.VISIBLE else View.GONE
       // binding.reimbursement.isChecked = (billInfo.type == BillType.ExpendLending || billInfo.type == BillType.IncomeLending)

    }

    private fun bindingDebtEvents() {
        binding.reimbursement.setOnCheckedChangeListener { buttonView, isChecked ->
            //选择销账后，如果为
            setDebt(isChecked)
            bindingPayInfoUI()
            bindingDebtExpendUI()
            bindingDebtIncomeUI()
            bindingDebtUI()

        }

        binding.chooseDebt.setOnClickListener {
            //TODO 债务账单选择列表
        }
    }


    private fun bindingReimbursementUI(){
        binding.chooseReimbursement.visibility = View.GONE
        if(!autoAccountingConfig.reimbursement){
            return
        }
        if(billTypeLevel2 == BillType.IncomeReimbursement){
            binding.chooseReimbursement.visibility = View.VISIBLE
        }
    }

    private fun bindingReimbursementEvents() {


        binding.chooseReimbursement.setOnClickListener {
            //TODO 报销账单选择列表
        }
    }
   private fun bindingCategoryUI(){
        binding.category.visibility = View.VISIBLE
        lifecycleScope.launch {
            BillInfo.getCategoryDrawable(billInfo.cateName,context){
                binding.category.setIcon(it,true)
            }
        }
        binding.category.setText(billInfo.cateName)
    }

    private fun bindingCategoryEvents(){
        binding.category.setOnClickListener {
            lifecycleScope.launch {
                val book = BookName.getByName(billInfo.bookName)
                withContext(Dispatchers.Main){
                    CategorySelectorDialog(context,book.id,billTypeLevel1){ parent,child ->
                        billInfo.cateName = BillUtils.getCategory(parent?.name?:"",child?.name)
                        bindingCategoryUI()
                    }.show(float)
                }
            }

        }
    }


    private fun bindingMoneyTypeUI(){
        if(!autoAccountingConfig.multiCurrency){
            binding.moneyType.visibility = View.GONE
            return
        }
        binding.moneyType.setText(billInfo.currency.name(context))
        binding.moneyType.setIcon(billInfo.currency.icon(context))
    }


    private fun bindingMoneyTypeEvents(){
        if(!autoAccountingConfig.multiCurrency)return
        binding.moneyType.setOnClickListener {
            val hashMap = Currency.getCurrencyMap(context)
            val popupUtils = ListPopupUtils(context, binding.moneyType,hashMap,billInfo.currency) { pos,key,value ->
                billInfo.currency = value as Currency
                bindingMoneyTypeUI()
            }
            popupUtils.toggle()
        }
    }

    private fun bindingTimeUI(){
        binding.time.setText(DateUtils.getTime(billInfo.timeStamp))
    }

    private fun bindingTimeEvents(){
        binding.time.setOnClickListener {
            //TODO 时间选择器
        }
    }

    private fun bindingRemarkUI(){
        binding.remark.setText(billInfo.remark)
    }

    private fun bindingRemarkEvents(){
       // TODO
    }

    private fun bindUI(){

     //   Logger.e("bindUI => $billInfo",Throwable())

        bindingBookNameUI()
        bindingTypePopupUI()
        bindingFeeUI()
        bindingRadioUI()


        bindingPayInfoUI()
        bindingTransferInfoUI()
        bindingDebtExpendUI()
        bindingDebtIncomeUI()
        bindingDebtUI()
        bindingReimbursementUI()


        bindingCategoryUI()
        bindingMoneyTypeUI()
        bindingTimeUI()
        bindingRemarkUI()
    }

    private fun bindEvents(){
        bindingBookNameEvents()
        bindingTypePopupEvents()
        bindingFeeEvents()
        bindingRadioEvents()

        bindingPayInfoEvents()
        bindingTransferInfoEvents()
        bindingDebtExpendEvents()
        bindingDebtIncomeEvents()
        bindingDebtEvents()
        bindingReimbursementEvents()



        bindingCategoryEvents()
        bindingMoneyTypeEvents()
        bindingTimeEvents()
        bindingRemarkEvents()
    }



    private fun setPriceColor(position: Int) {
        val drawableRes = when (position) {
            0 -> R.drawable.float_minus
            1 -> R.drawable.float_add
            2 -> R.drawable.float_round
            else -> R.drawable.float_minus
        }

        val payColor = SpUtils.getInt("setting_pay_color_red",0)

        val tintRes = when (position) {
            0 -> if(payColor == 0) R.color.danger else R.color.success
            1 ->if(payColor == 1) R.color.danger else R.color.success
            2 -> R.color.info
            else -> R.color.danger
        }


        val drawable = AppCompatResources.getDrawable(context, drawableRes)
        val tint = ColorStateList.valueOf(ContextCompat.getColor(context,tintRes))
        val color = ContextCompat.getColor(context, tintRes)


        binding.priceContainer.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
        TextViewCompat.setCompoundDrawableTintList(binding.priceContainer, tint)
        binding.priceContainer.setTextColor(color)
    }



}