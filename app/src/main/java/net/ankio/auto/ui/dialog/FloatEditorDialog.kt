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
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.ImageViewCompat
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.constant.BillType
import net.ankio.auto.database.table.BillInfo
import net.ankio.auto.databinding.FloatEditorBinding
import net.ankio.auto.utils.ImageUtils

class FloatEditorDialog(context: Context,val billInfo: BillInfo) : BaseSheetDialog(context) {
     lateinit var binding: FloatEditorBinding
    override fun onCreateView(inflater: LayoutInflater): View {
         this.binding = FloatEditorBinding.inflate(inflater)
        //金额类型
         bindingTypePopup()
        //TODO 币种 手续费
        //TODO 报销 分类
        //修改账本
        bindingChangeBookName()

        return binding.root
    }


    private fun bindingChangeBookName(){
        coroutineScope.launch{
            BillInfo.getBookDrawable(billInfo.bookName,context,binding.bookImage)
        }
        binding.bookImage.setOnClickListener {
            BookSelectorDialog(context).show(true) {
                billInfo.bookName = it.name?:""
                ImageUtils.get(context, it.icon?:"", { drawable ->
                    binding.bookImage.setImageDrawable(drawable)
                }, {
                    binding.bookImage.setImageDrawable(ResourcesCompat.getDrawable(context.resources,R.drawable.default_book,context.theme))
                })
            }
        }
    }
    private fun bindingTypePopup(){

        binding.price.text = billInfo.money.toString()
        setPriceColor(billInfo.type.type)


        val stringList = arrayListOf(context.getString(R.string.float_expend),context.getString(R.string.float_income),context.getString(
            R.string.float_transfer),context.getString(R.string.float_debt))
        val listPopupThemeWindow = ListPopupWindow(context, null)

        listPopupThemeWindow.anchorView =  binding.priceContainer

        listPopupThemeWindow.setAdapter(ArrayAdapter(context, R.layout.list_popup_window_item,  stringList))
        listPopupThemeWindow.width = WindowManager.LayoutParams.WRAP_CONTENT

        listPopupThemeWindow.setOnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
            billInfo.type = BillType.values()[position]
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
            BillType.Transfer->setAsTransfer(billInfo,binding)
            BillType.Expend->setAsExpend(billInfo,binding)
            BillType.Income->setAsIncome(billInfo,binding)
            else -> setAsDebt(billInfo,binding)
        }
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
            3 -> {
                drawable = AppCompatResources.getDrawable(context,R.drawable.float_check)
                tint = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.warning))
                color = ContextCompat.getColor(context, R.color.warning)
            }
        }

        binding.typeIcon.setImageDrawable(drawable)
        ImageViewCompat.setImageTintList(binding.typeIcon,tint)
        binding.price.setTextColor(color)
    }

    //UI设置为支出
    private fun setAsExpend(billInfo: BillInfo,binding: FloatEditorBinding){


    }
    //UI设置为收入
    private fun setAsIncome(billInfo: BillInfo,binding: FloatEditorBinding){

    }

    //UI设置为转账
    private fun setAsTransfer(billInfo: BillInfo,binding: FloatEditorBinding){

    }
    //UI设置为债务
    private fun setAsDebt(billInfo: BillInfo,binding: FloatEditorBinding){

    }




}