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

package net.ankio.auto.ui.fragment.home.rule

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.size
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.elevation.SurfaceColors
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hjq.toast.Toaster
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.app.BillUtils
import net.ankio.auto.databinding.DialogRegexInputBinding
import net.ankio.auto.databinding.DialogRegexMoneyBinding
import net.ankio.auto.databinding.FragmentEditBinding
import net.ankio.auto.ui.componets.FlowElement
import net.ankio.auto.ui.componets.FlowLayoutManager
import net.ankio.auto.ui.dialog.BookInfoDialog
import net.ankio.auto.ui.dialog.BookSelectorDialog
import net.ankio.auto.ui.dialog.CategorySelectorDialog
import net.ankio.auto.ui.fragment.BaseFragment
import net.ankio.auto.utils.ListPopupUtils
import net.ankio.auto.utils.server.model.BookName
import net.ankio.auto.utils.server.model.CustomRuleModel
import java.util.Calendar

class EditFragment : BaseFragment() {
    private lateinit var binding: FragmentEditBinding
    private var customRuleModel: CustomRuleModel = CustomRuleModel()
    private var book: Int = 0
    private var bookName: String = ""
    private var category: String = ""
    private var list: MutableList<HashMap<String, Any>>? = mutableListOf()

    private fun buildUI() {
        val flexboxLayout = binding.flexboxLayout
        // 监听器取消
        for (i in 0 until binding.flexboxLayout.childCount) {
            val child = binding.flexboxLayout.getChildAt(i)
            child.setOnClickListener(null)
            child.setOnLongClickListener(null)
        }
        // 清除所有的UI
        flexboxLayout.removeAllViews()
        flexboxLayout.appendTextView(getString(R.string.if_condition_true))

        val listType = object : TypeToken<MutableList<HashMap<String, Any>>>() {}.type
        val list: MutableList<HashMap<String, Any>>? = Gson().fromJson(customRuleModel.element, listType)
        // 依次排列
        if (list.isNullOrEmpty()) {
            val buttonElem =
                flexboxLayout.appendAddButton(callback = { it, _ ->
                    flexboxLayout.appendWaveTextview(
                        getString(R.string.condition),
                        connector = true,
                        elem = it,
                    ) { it2, view ->
                        showSelectType(flexboxLayout, view, it2)
                    }
                })

            val buttonView = buttonElem.getFirstView()
            flexboxLayout.firstWaveTextViewPosition = flexboxLayout.indexOfChild(buttonView)
            buttonView?.callOnClick()
            flexboxLayout.appendTextView(getString(R.string.condition_result_book))
            flexboxLayout.appendWaveTextview(getString(R.string.rule_book)) { it2, _ ->
                onClickBook(it2)
            }
            flexboxLayout.appendTextView(getString(R.string.condition_result_category))
            flexboxLayout.appendWaveTextview(getString(R.string.category)) { it2, _ ->
                onClickCategory(it2)
            }
            // 列表为空
            return
        }
        // 最后一个是数据
        val lastElement = list!!.removeLast()
        // fix #7 因为存储的时候使用的是hashmap<String,Any>，反向识别的时候可能会将Int类型识别为Double
        book =
            if (lastElement["id"] is Int) {
                lastElement["id"] as Int
            } else if (lastElement["id"] is Double) {
                (lastElement["id"] as Double).toInt()
            } else {
                0
            }
        bookName = lastElement["book"] as String
        category = lastElement["category"] as String

        // 添加到页面来
        flexboxLayout.firstWaveTextViewPosition = flexboxLayout.size - 1
        val buttonElem =
            flexboxLayout.appendAddButton(callback = { it, _ ->
                flexboxLayout.appendWaveTextview(
                    getString(R.string.condition),
                    connector = true,
                    elem = it,
                ) { it2, view ->
                    showSelectType(flexboxLayout, view, it2)
                }
            })
        for (hashMap in list!!) {
            flexboxLayout.appendWaveTextview(
                hashMap["text"] as String,
                connector = hashMap.containsKey("jsPre"),
                elem = buttonElem,
                data = hashMap,
            ) { it2, view ->
                val type = it2.data["type"]
                if (type != null) {
                    when (type as String) {
                        "type" -> inputType(it2, view)
                        "shopName" -> inputShop(flexboxLayout, it2)
                        "shopItem" -> inputShopItem(flexboxLayout, it2)
                        "timeRange" -> inputTimeRange(flexboxLayout, it2)
                        "moneyRange" -> inputMoneyRange(flexboxLayout, it2)
                    }
                }
            }
        }

        flexboxLayout.appendTextView(getString(R.string.condition_result_book))

        flexboxLayout.appendWaveTextview(lastElement["book"] as String) { it2, _ ->
            onClickBook(it2)
        }
        flexboxLayout.appendTextView(getString(R.string.condition_result_category))

        flexboxLayout.appendWaveTextview(lastElement["category"] as String) { it2, _ ->

            onClickCategory(it2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentEditBinding.inflate(layoutInflater)

        binding.ruleCard.setCardBackgroundColor(SurfaceColors.SURFACE_1.getColor(requireContext()))

        binding.saveItem.setOnClickListener {
            saveItem()
        }

        arguments?.apply {
            customRuleModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getSerializable("regular", CustomRuleModel::class.java)
            } else {
                getSerializable("regular") as? CustomRuleModel
            } ?: CustomRuleModel()
        }
        buildUI()

        return binding.root
    }

    private fun onClickBook(it2: FlowElement) {
        BookSelectorDialog(requireContext()) {
            it2.removed().setAsWaveTextview(it.name, it2.connector, callback = it2.waveCallback)
            bookName = it.name
            book = it.id
        }.show(cancel = true)
    }

    private fun onClickCategory(it2: FlowElement) {
        lifecycleScope.launch {
            var book = BookName.getByName(bookName)

            BookInfoDialog(requireActivity(), book) { type ->
                CategorySelectorDialog(requireActivity(), book.id, type) { parent, child ->
                    val string: String =
                        if (parent == null) {
                            "其他"
                        } else {
                            if (child == null) {
                                BillUtils.getCategory(parent.name.toString())
                            } else {
                                BillUtils.getCategory(
                                    parent.name.toString(),
                                    child.name.toString(),
                                )
                            }
                        }
                    it2.removed().setAsWaveTextview(
                        string,
                        it2.connector,
                        callback = it2.waveCallback,
                    )
                    category = string
                }.show(cancel = true)
            }.show(cancel = true)
        }
    }

    private fun showSelectType(
        flexboxLayout: FlowLayoutManager,
        view: View,
        element: FlowElement,
    ) {
        val menuItems: HashMap<String, Any> =
            hashMapOf(
                getString(R.string.type_money) to 0,
                getString(R.string.type_time) to 1,
                getString(R.string.type_shop) to 2,
                getString(R.string.type_item) to 3,
                getString(R.string.type_type) to 4,
            )
        val listPopupUtils =
            ListPopupUtils(requireContext(), view, menuItems, 0) { pos, key, value ->
                when (value) {
                    0 -> inputMoneyRange(flexboxLayout, element)
                    1 -> inputTimeRange(flexboxLayout, element)
                    2 -> inputShop(flexboxLayout, element)
                    3 -> inputShopItem(flexboxLayout, element)
                    4 -> inputType(element, view)
                }
            }
        listPopupUtils.toggle()
    }

    private fun inputType(
        element: FlowElement,
        view: View,
    ) {
        val menuItems: HashMap<String, Any> =
            hashMapOf(
                getString(R.string.type_for_pay) to 0,
                getString(R.string.type_for_income) to 1,
            )
        var msg = ""
        var js = ""

        val listPopupUtils =
            ListPopupUtils(requireContext(), view, menuItems, 0) { pos, key, value ->
                msg = getString(R.string.type_pay, key)
                when (value) {
                    0 -> js = "type === 0"
                    1 -> js = "type === 1"
                }
                element.data["js"] = js
                element.data["type"] = "type"
                element.data["text"] = msg
                element.removed()
                    .setAsWaveTextview(msg, element.connector, callback = element.waveCallback)
            }

        listPopupUtils.toggle()
    }

    private fun inputShop(
        flexboxLayout: FlowLayoutManager,
        view: FlowElement,
    ) {
        showInput(
            flexboxLayout,
            view,
            R.string.shop_input,
            "shopName",
            getString(R.string.shop_name),
        )
    }

    private fun inputShopItem(
        flexboxLayout: FlowLayoutManager,
        view: FlowElement,
    ) {
        showInput(
            flexboxLayout,
            view,
            R.string.shop_item_input,
            "shopItem",
            getString(R.string.shop_item_name),
        )
    }

    private fun showInput(
        flexboxLayout: FlowLayoutManager,
        element: FlowElement,
        title: Int,
        item: String,
        name: String,
    ) {
        val inputBinding = DialogRegexInputBinding.inflate(LayoutInflater.from(requireContext()))
        var select: Int =
            when (val selectValue = element.data.getOrDefault("select", 0)) {
                is Int -> selectValue
                is Double -> selectValue.toInt()
                else -> 0 // 或者你可以选择其他默认值
            }
        var content = element.data.getOrDefault("content", "") as String
        val options: Array<String> =
            arrayOf(getString(R.string.input_contains), getString(R.string.input_regex))
        val adapter: ArrayAdapter<String> =
            ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        inputBinding.spinner.adapter = adapter
        inputBinding.spinner.setSelection(select)
        inputBinding.content.setText(content)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(inputBinding.root)
            .setPositiveButton(R.string.sure_msg) { dialog, which ->
                select = options.indexOf(inputBinding.spinner.selectedItem)
                content = inputBinding.content.text.toString()
                element.data["select"] = select
                element.data["content"] = content
                element.data["type"] = item
                var msg = ""
                if (select == 0) {
                    element.data["js"] = "$item.indexOf(\"$content\")!==-1 "

                    msg = getString(R.string.shop_name_contains, name, content)
                } else {
                    element.data["js"] = "$item.match(/$content/)"
                    msg = getString(R.string.shop_name_regex, name, content)
                }
                element.data["text"] = msg
                element.removed().setAsWaveTextview(msg, element.connector) { it, view ->
                    inputShop(flexboxLayout, it)
                }
            }
            .setNegativeButton(R.string.cancel_msg, null)
            .show()
    }

    private fun showTimer(
        time: String,
        title: String,
        callback: (String) -> Unit,
    ) {
        val result = time.split(":")
        val picker =
            MaterialTimePicker.Builder()
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

    private fun inputTimeRange(
        flexboxLayout: FlowLayoutManager,
        element: FlowElement,
    ) {
        val currentTime = Calendar.getInstance()
        val hour = currentTime.get(Calendar.HOUR_OF_DAY)
        val minute = currentTime.get(Calendar.MINUTE)
        var minTime = element.data.getOrDefault("minTime", "$hour:$minute").toString()
        var maxTime = element.data.getOrDefault("maxTime", "$hour:$minute").toString()
        showTimer(minTime, getString(R.string.select_time_lower)) { it1 ->
            minTime = it1
            showTimer(maxTime, getString(R.string.select_time_higher)) {
                maxTime = it
                val js = "timeRange('$minTime','$maxTime')"
                val input = getString(R.string.time_range, minTime, maxTime)
                element.data["js"] = js
                element.data["minTime"] = minTime
                element.data["maxTime"] = maxTime
                element.data["text"] = input
                element.data["type"] = "timeRange"
                element.removed().setAsWaveTextview(input, element.connector) { it, view ->
                    inputTimeRange(flexboxLayout, it)
                }
            }
        }
    }

    private fun inputMoneyRange(
        flexboxLayout: FlowLayoutManager,
        element: FlowElement,
    ) {
        val moneyRangeBinding =
            DialogRegexMoneyBinding.inflate(LayoutInflater.from(requireContext()))
        moneyRangeBinding.lower.setText(element.data.getOrDefault("minAmount", "").toString())
        moneyRangeBinding.higher.setText(element.data.getOrDefault("maxAmount", "").toString())
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.money_range)
            .setView(moneyRangeBinding.root)
            .setPositiveButton(R.string.sure_msg) { _, _ ->
                // 处理用户输入的金额范围
                // 从 dialogView 中获取用户输入的数据

                val maxAmount =
                    runCatching {
                        moneyRangeBinding.higher.text.toString().toFloat()
                    }.getOrDefault(0).toFloat()

                val minAmount =
                    runCatching {
                        moneyRangeBinding.lower.text.toString().toFloat()
                    }.getOrDefault(0).toFloat()

                var js = ""
                var input = ""
                if (maxAmount.toInt() == 0 && minAmount > 0) {
                    js = "money > $minAmount"
                    input = getString(R.string.money_max_info, minAmount.toString())
                }

                if (minAmount.toInt() == 0 && maxAmount > 0) {
                    js = "money < $maxAmount"
                    input = getString(R.string.money_min_info, maxAmount.toString())
                }

                if (minAmount > 0 && maxAmount > 0 && maxAmount > minAmount) {
                    js = "money < $maxAmount and money > $minAmount"
                    input =
                        getString(
                            R.string.money_range_info,
                            minAmount.toString(),
                            maxAmount.toString(),
                        )
                }

                if (minAmount > 0 && maxAmount > 0 && maxAmount == minAmount) {
                    js = "money == $minAmount"
                    input = getString(R.string.money_equal_info, minAmount.toString())
                }
                // 在此处处理用户输入的金额范围
                if (js === "") {
                    Toaster.show(R.string.money_error)
                    return@setPositiveButton
                }
                element.data["js"] = js
                element.data["minAmount"] = minAmount
                element.data["maxAmount"] = maxAmount
                element.data["text"] = input
                element.data["type"] = "moneyRange"
                element.removed().setAsWaveTextview(input, element.connector) { it, view ->
                    inputMoneyRange(flexboxLayout, it)
                }
            }
            .setNegativeButton(R.string.cancel_msg, null)
            .show()
    }

    private fun saveItem() {
        val map = binding.flexboxLayout.getViewMap()
        var condition = ""
        var text = "若满足"
        list = mutableListOf()

        for (flowElement in map) {
            if (flowElement.data.containsKey("js")) {
                list!!.add(flowElement.data)
                val t = flowElement.data["text"] as String

                if (flowElement.data.containsKey("jsPre")) {
                    val pre = flowElement.data["jsPre"]
                    condition += pre
                    text += if (pre == "or") " 或 " else " 且 "
                }
                condition += flowElement.data["js"]
                text += t
            }
        }
        text += "，则账本为【$bookName】，分类为【$category】。"
        val otherData =
            hashMapOf<String, Any>(
                "book" to bookName,
                "category" to category,
                "id" to book,
            )
        list!!.add(otherData)
        condition += ""
        val js = "if($condition){ return { book:'$bookName',category:'$category'} }"

        customRuleModel.js = js
        customRuleModel.text = text
        customRuleModel.element = Gson().toJson(list)

        customRuleModel.use = true

        if (customRuleModel.js.contains("if()")) {
            Toaster.show(R.string.useless_condition)
            return
        }
        if (customRuleModel.js.contains("book:''")) {
            Toaster.show(getString(R.string.useless_book))
            return
        }
        if (customRuleModel.js.contains("category:''")) {
            Toaster.show(getString(R.string.useless_category))
            return
        }

        CustomRuleModel.put(customRuleModel)

        lifecycleScope.launch {
            findNavController().popBackStack() // 返回上一个页面
        }
    }
}
