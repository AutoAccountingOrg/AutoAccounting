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
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.hjq.toast.Toaster
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.app.BillUtils
import net.ankio.common.constant.BillType
import net.ankio.auto.database.table.BillInfo
import net.ankio.auto.databinding.FloatEditorBinding
import net.ankio.auto.ui.componets.IconView
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.DateUtils
import net.ankio.auto.utils.ImageUtils
import net.ankio.auto.utils.SpUtils

class FloatEditorDialog(context: Context,val billInfo: BillInfo) : BaseSheetDialog(context) {
     lateinit var binding: FloatEditorBinding
    override fun onCreateView(inflater: LayoutInflater): View {
         this.binding = FloatEditorBinding.inflate(inflater)
        this.cardView = binding.editorCard
        //金额类型
         bindingTypePopup()
        //TODO 币种 手续费
        //TODO 报销 分类
        //修改账本
        bindingChangeBookName()
        //关闭按钮
        binding.cancelButton.setOnClickListener {
            dismiss()
        }
        //确定按钮
        binding.sureButton.setOnClickListener {


            //TODO 应用UI上的更改


            lifecycleScope.launch {
                BillUtils.groupBillInfo(billInfo)
                if(!SpUtils.getBoolean("float_no_disturb")){
                    Toaster.show(context.getString(R.string.auto_success,billInfo.money.toString()))
                }
            }
        }

        binding.reimbursement.setOnCheckedChangeListener { buttonView, isChecked ->
            binding.chooseDebt.visibility = if(isChecked) View.VISIBLE else View.GONE

            if(!isChecked){
                changeInput()
            }else{
                binding.payInfo.visibility = View.VISIBLE
                binding.debtExpend.visibility = View.GONE
                binding.debtIncome.visibility = View.GONE
            }
        }


        binding.radioNone.setOnClickListener {
            binding.debtContainer.visibility = View.GONE
            binding.reimbursementContainer.visibility = View.GONE
            binding.reimbursement.isChecked = false
            binding.payInfo.visibility = View.VISIBLE
            binding.debtExpend.visibility = View.GONE
            binding.debtIncome.visibility = View.GONE
        }


        binding.radioDebt.setOnClickListener {
            binding.debtContainer.visibility = View.VISIBLE
            binding.reimbursementContainer.visibility = View.GONE
            binding.reimbursement.isChecked = false
            changeInput()

        }


        binding.radioReimbursement.setOnClickListener {
            binding.debtContainer.visibility = View.GONE
            binding.reimbursementContainer.visibility = if(billInfo.type=== net.ankio.common.constant.BillType.Income) View.VISIBLE else View.GONE
            binding.payInfo.visibility = View.VISIBLE
            binding.debtExpend.visibility = View.GONE
            binding.debtIncome.visibility = View.GONE
        }

        setEditorUI()

        setPayInfo()


        binding.payFrom.setOnClickListener {
            selectPayInfo(it as IconView)
        }

        binding.transferFrom.setOnClickListener {
            selectPayInfo(it as IconView)
        }
        binding.transferTo.setOnClickListener {
            selectPayInfo(it as IconView)
        }

        binding.debtExpendFrom.setOnClickListener {
            selectPayInfo(it as IconView)
        }

        binding.debtIncomeTo.setOnClickListener {
            selectPayInfo(it as IconView)
        }


        return binding.root
    }

    private fun selectPayInfo(view: IconView){
        //TODO 查找可用资产
    }
    private fun setPayInfo(){
        lifecycleScope.launch {
            BillInfo.getAccountDrawable(billInfo.accountNameTo,context){
                binding.payFrom.setIcon(it)
                binding.payFrom.setText(billInfo.accountNameTo)

                binding.transferFrom.setIcon(it)
                binding.transferFrom.setText(billInfo.accountNameTo)

                binding.debtExpendFrom.setIcon(it)
                binding.debtExpendFrom.setText(billInfo.accountNameTo)

                binding.debtIncomeTo.setIcon(it)
                binding.debtIncomeTo.setText(billInfo.accountNameTo)
            }
            if(billInfo.accountNameFrom!==""){
                BillInfo.getAccountDrawable(billInfo.accountNameFrom,context){
                    binding.transferTo.setIcon(it)
                    binding.transferTo.setText(billInfo.accountNameFrom)
                }
            }


            BillInfo.getCategoryDrawable(billInfo.cateName,context){
                binding.category.setIcon(it,true)
                binding.category.setText(billInfo.cateName)
            }
        }
        binding.moneyType.setText(billInfo.currency.name(AppUtils.getApplication()))
        binding.fee.setText(billInfo.fee.toString())
        binding.time.setText(DateUtils.getTime(billInfo.timeStamp))
        binding.remark.setText(billInfo.remark)
    }
    private fun changeInput(){
        binding.payInfo.visibility = View.GONE
        binding.debtExpend.visibility = View.GONE
        binding.debtIncome.visibility = View.GONE
        if(billInfo.type === net.ankio.common.constant.BillType.Income){
            binding.debtIncome.visibility = View.VISIBLE
            binding.debtIncomeFrom.setText(billInfo.shopName)
        }else if (billInfo.type === net.ankio.common.constant.BillType.Expend){
            binding.debtExpend.visibility = View.VISIBLE
            binding.debtExpendTo.setText(billInfo.shopName)
        }else{
            binding.payInfo.visibility = View.VISIBLE
        }
    }

    private fun bindingChangeBookName(){
        lifecycleScope.launch{
            BillInfo.getBookDrawable(billInfo.bookName,context,binding.bookImage)
        }
        binding.bookImage.setOnClickListener {
         /*   BookSelectorDialog(context).show(true) {
                billInfo.bookName = it.name?:""
                ImageUtils.get(context, it.icon?:"", { drawable ->
                    binding.bookImage.setImageDrawable(drawable)
                }, {
                    binding.bookImage.setImageDrawable(ResourcesCompat.getDrawable(context.resources,R.drawable.default_book,context.theme))
                })
            }*/
        }
    }
    private fun bindingTypePopup(){

        binding.price.text = billInfo.money.toString()
        setPriceColor(billInfo.type.toInt())


        val stringList = arrayListOf(context.getString(R.string.float_expend),context.getString(R.string.float_income),context.getString(
            R.string.float_transfer))
        val listPopupThemeWindow = ListPopupWindow(context, null)

        listPopupThemeWindow.anchorView =  binding.priceContainer

        listPopupThemeWindow.setAdapter(ArrayAdapter(context, R.layout.list_popup_window_item,  stringList))
        listPopupThemeWindow.width = WindowManager.LayoutParams.WRAP_CONTENT

        listPopupThemeWindow.setOnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
            billInfo.type = net.ankio.common.constant.BillType.values()[position]
            //设置UI
            setEditorUI()
            //设置顶部样式
            setPriceColor(position)
            listPopupThemeWindow.dismiss()
        }
        //修改账单类型
        binding.priceContainer.setOnClickListener{ listPopupThemeWindow.show() }


    }


    //设置不同类型的UI
    private fun setEditorUI(){

        when(billInfo.type){
            net.ankio.common.constant.BillType.Transfer->setAsTransfer()
            net.ankio.common.constant.BillType.Expend-> {
                setAsExpend()
                binding.radioNone.callOnClick()
            }
            net.ankio.common.constant.BillType.Income-> {
                setAsIncome()
                binding.radioNone.callOnClick()
            }

            net.ankio.common.constant.BillType.ExpendReimbursement -> TODO()
            net.ankio.common.constant.BillType.ExpendLending -> TODO()
            net.ankio.common.constant.BillType.ExpendRepayment -> TODO()
            net.ankio.common.constant.BillType.IncomeLending -> TODO()
            net.ankio.common.constant.BillType.IncomeRepayment -> TODO()
        }
        binding.debtContainer.visibility = View.GONE
        binding.reimbursementContainer.visibility = View.GONE

    }

    private fun setPriceColor(position:Int){
        var drawable = AppCompatResources.getDrawable(context,R.drawable.float_minus)
        var tint = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.danger))
        var color = ContextCompat.getColor(context, R.color.danger)
        when(position){
            1 -> {
                drawable = AppCompatResources.getDrawable(context,R.drawable.float_add)
                tint = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.success))
                color = ContextCompat.getColor(context, R.color.success)
            }
            2 -> {
                drawable = AppCompatResources.getDrawable(context,R.drawable.float_round)
                tint = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.info))
                color = ContextCompat.getColor(context, R.color.info)
            }
        }

        binding.typeIcon.setImageDrawable(drawable)
        ImageViewCompat.setImageTintList(binding.typeIcon,tint)
        binding.price.setTextColor(color)
    }

    //UI设置为支出
    private fun setAsExpend(){

        binding.tagInfo.visibility  = View.VISIBLE
        binding.payInfo.visibility = View.VISIBLE
        binding.transferInfo.visibility = View.GONE
        binding.debtExpend.visibility = View.GONE
        binding.debtIncome.visibility = View.GONE
        binding.category.visibility = View.VISIBLE
        //设置来自账户，分类信息

        //只有收入才需要选择报销容器

        //币种不管他，反正就在那

        //手续费似乎也不用管？

        //时间和备注也不用管，反正都在


    }
    //UI设置为收入
    private fun setAsIncome(){
        binding.debtContainer.visibility = View.GONE
        binding.reimbursementContainer.visibility = View.GONE

        binding.tagInfo.visibility  = View.VISIBLE
        binding.payInfo.visibility = View.VISIBLE
        binding.transferInfo.visibility = View.GONE
        binding.debtExpend.visibility = View.GONE
        binding.debtIncome.visibility = View.GONE
        binding.category.visibility = View.VISIBLE



    }

    //UI设置为转账
    private fun setAsTransfer(){
        binding.tagInfo.visibility  = View.GONE
        //转账也使用自己的
        binding.payInfo.visibility = View.GONE
        binding.transferInfo.visibility = View.VISIBLE
        binding.debtExpend.visibility = View.GONE
        binding.debtIncome.visibility = View.GONE
        //转账没有分类
        binding.category.visibility = View.GONE


        //只有收入才需要选择报销容器


    }
    //UI设置为债务




}