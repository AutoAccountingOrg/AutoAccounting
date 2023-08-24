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

package net.ankio.auto.ui.fragment.rule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.elevation.SurfaceColors
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentEditBinding
import net.ankio.auto.databinding.InputDialogBinding
import net.ankio.auto.databinding.MoneyDialogBinding
import net.ankio.auto.ui.componets.FlowElement
import net.ankio.auto.ui.componets.FlowLayoutManager
import net.ankio.auto.ui.dialog.BookSelectorDialog
import net.ankio.auto.ui.dialog.CategorySelectorDialog
import java.util.Calendar


class EditFragment : Fragment() {
    private lateinit var binding: FragmentEditBinding


    private var book = 1
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditBinding.inflate(layoutInflater)

        binding.ruleCard.setCardBackgroundColor(SurfaceColors.SURFACE_1.getColor(requireContext()))


        val flexboxLayout = binding.flexboxLayout

        flexboxLayout.appendTextView(getString(R.string.if_condition_true))

        val buttonElem = flexboxLayout.appendAddButton(callback = { it, _ ->
           flexboxLayout.appendWaveTextview(getString(R.string.condition), connector = true, elem = it){ it2,view ->
            showSelectType(flexboxLayout,view,it2)
           }
        })


        val buttonView =  buttonElem.getFirstView()
        flexboxLayout.firstWaveTextViewPosition = flexboxLayout.indexOfChild(buttonView)
        buttonView?.callOnClick()


        flexboxLayout.appendTextView(getString(R.string.condition_result_book))



        flexboxLayout.appendWaveTextview(getString(R.string.rule_book)){ it2,view ->
            BookSelectorDialog().show(requireActivity(),false) {
                it2.removed().setAsWaveTextview(it.name?:"",it2.connector, callback = it2.waveCallback)
                it2.data["js"] = it.name?:""
                book = it.id
            }
        }

        flexboxLayout.appendTextView(getString(R.string.condition_result_category))


        flexboxLayout.appendWaveTextview(getString(R.string.category)){ it2,view ->
            CategorySelectorDialog().show(requireActivity(),book,false){ parent,child->
                var string  = ""
                string = if(parent==null){
                    "其他"
                }else{
                    if(child==null){
                        parent.name.toString()
                    }else{
                        parent.name.toString() + " - "+child.name.toString()
                    }
                }
                it2.removed().setAsWaveTextview(string,it2.connector, callback = it2.waveCallback)
                it2.data["js"] = string

            }
        }


        return binding.root
    }
    private fun showSelectType(flexboxLayout:FlowLayoutManager,view:View,element: FlowElement){
        val popup = PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.type_menu, popup.menu)
        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
            selectType(view,element,menuItem,flexboxLayout)
            true
        }
        popup.show()
    }

    /**
     * 选择分类
     */
    private fun selectType(view:View,element: FlowElement,menuItem: MenuItem,flexboxLayout:FlowLayoutManager){
        when(menuItem.itemId){
            R.id.type_money->{
               inputMoneyRange(flexboxLayout,element)
            }
            R.id.type_time->{
                inputTimeRange(flexboxLayout,element)
            }

            R.id.type_shop->{
                inputShop(flexboxLayout,element)
            }

            R.id.type_item->{
                inputShopItem(flexboxLayout, element)
            }

            R.id.type_type->{
                val popup = PopupMenu(requireContext(), view)
                popup.menuInflater.inflate(R.menu.type_type, popup.menu)
                popup.setOnMenuItemClickListener { m: MenuItem ->

                    var msg = ""
                    var js = ""
                    msg = getString(R.string.type_pay,m.title)
                    when (m.itemId){
                        R.id.type_for_pay->{
                            js = "type === 0"

                        }
                        R.id.type_for_income->{
                            js = "type === 1"
                        }
                        R.id.type_for_transfer->{
                            js = "type === 2"
                        }
                    }

                    element.data["js"] = js
                    element.removed().setAsWaveTextview(msg,element.connector, callback = element.waveCallback)
                    true
                }
                popup.show()
            }
        }
    }

    private fun inputShop(flexboxLayout:FlowLayoutManager, view: FlowElement) {
        showInput(flexboxLayout,view,R.string.shop_input,"shopName",getString(R.string.shop_name))
    }
    private fun inputShopItem(flexboxLayout:FlowLayoutManager, view: FlowElement) {
        showInput(flexboxLayout,view,R.string.shop_item_input,"shopItem",getString(R.string.shop_item_name))
    }

    private fun showInput(flexboxLayout:FlowLayoutManager, element: FlowElement,title: Int,item:String,name:String){
        val input_binding = InputDialogBinding.inflate(LayoutInflater.from(requireContext()))
        var select = element.data.getOrDefault("select",0) as Int
        var content = element.data.getOrDefault("content","") as String
        val options: Array<String> = arrayOf(getString(R.string.input_contains), getString(R.string.input_regex))
        val adapter: ArrayAdapter<String> = ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item,options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        input_binding.spinner.adapter = adapter
        input_binding.spinner.setSelection(select)
        input_binding.content.setText(content)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(input_binding.root)
            .setPositiveButton(R.string.sure_msg) { dialog, which ->
                select = options.indexOf(input_binding.spinner.selectedItem)
                content = input_binding.content.text.toString()
                element.data["select"] = select
                element.data["content"] = content
                var msg = ""
                if(select==0){
                    element.data["js"] = "$item.indexOf(\"$content\")!==-1 "

                    msg = getString(R.string.shop_name_contains,name,content)
                }else{
                    element.data["js"] = "$item.match(/$content/)"
                    msg = getString(R.string.shop_name_regex,name,content)
                }

                element.removed().setAsWaveTextview(msg,element.connector){ it,view->
                    inputShop(flexboxLayout,it)
                }
            }
            .setNegativeButton(R.string.cancel_msg, null)
            .show()
    }
    private fun showTimer(time:String,title: String, callback: (String) -> Unit) {
        val result = time.split(":")
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(result[0].toInt())
            .setMinute(result[1].toInt())
            .setTitleText(title)
            .build()
        picker.show(requireActivity().supportFragmentManager, "time_picker")
        picker.addOnPositiveButtonClickListener {
            val selectedTime = "${picker.hour}:${picker.minute}"
            callback(selectedTime)
        }
    }
    private fun inputTimeRange(flexboxLayout:FlowLayoutManager, element: FlowElement) {
        val currentTime = Calendar.getInstance()
        val hour = currentTime.get(Calendar.HOUR_OF_DAY)
        val minute = currentTime.get(Calendar.MINUTE)
        var minTime = element.data.getOrDefault("minTime","$hour:$minute").toString()
        var maxTime = element.data.getOrDefault("maxTime","$hour:$minute").toString()


        showTimer(minTime,getString(R.string.select_time_lower)){ it1 ->
            minTime = it1
            showTimer(maxTime,getString(R.string.select_time_higher)){
                maxTime = it
                val js = "timeRange('${minTime}','${maxTime}')"

                val input = getString(R.string.time_range,minTime,maxTime)


                element.data["js"] = js
                element.data["minTime"] = minTime
                element.data["maxTime"] = maxTime
                element.removed().setAsWaveTextview(input,element.connector){ it,view->
                    inputTimeRange(flexboxLayout,it)
                }
            }
        }




    }


    private fun inputMoneyRange(flexboxLayout:FlowLayoutManager, element: FlowElement){
        val money_range_binding = MoneyDialogBinding.inflate(LayoutInflater.from(requireContext()))
        money_range_binding.lower.setText(element.data.getOrDefault("minAmount","").toString())
        money_range_binding.higher.setText(element.data.getOrDefault("maxAmount","").toString())
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.money_range)
            .setView(money_range_binding.root)
            .setPositiveButton(R.string.sure_msg) { dialog, which ->
                // 处理用户输入的金额范围
                // 从 dialogView 中获取用户输入的数据


                val maxAmount =   runCatching {
                    money_range_binding.higher.text.toString().toFloat()
                }.getOrDefault(0).toFloat()

                val minAmount =   runCatching {
                    money_range_binding.lower.text.toString().toFloat()
                }.getOrDefault(0).toFloat()

                var js = ""
                var input = ""
                if(maxAmount.toInt() == 0 && minAmount >0){
                    js = "money > $minAmount"
                    input = getString(R.string.money_max_info,minAmount.toString())
                }

                if(minAmount.toInt() == 0 && maxAmount >0){
                    js = "money < $maxAmount"
                    input = getString(R.string.money_min_info,maxAmount.toString())
                }

                if(minAmount>0 && maxAmount>0 && maxAmount>minAmount){
                    js = "money < $maxAmount and money > $minAmount"
                    input = getString(R.string.money_range_info,minAmount.toString(),maxAmount.toString())
                }

                if(minAmount>0 && maxAmount>0 && maxAmount == minAmount){
                    js = "money == $minAmount"
                    input = getString(R.string.money_equal_info,minAmount.toString())
                }
                // 在此处处理用户输入的金额范围
                if(js===""){
                    Toast.makeText(requireContext(),R.string.money_error,Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                element.data["js"] = js
                element.data["minAmount"] = minAmount
                element.data["maxAmount"] = maxAmount
                element.removed().setAsWaveTextview(input,element.connector){ it,view->
                    inputMoneyRange(flexboxLayout,it)
                }

            }
            .setNegativeButton(R.string.cancel_msg, null)
            .show()
    }


    //TODO 保存
    //TODo 读取 重新渲染
}