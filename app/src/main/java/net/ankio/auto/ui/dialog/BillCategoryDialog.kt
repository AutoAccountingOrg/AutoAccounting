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

package net.ankio.auto.ui.dialog

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.databinding.DialogBillCategoryBinding
import net.ankio.auto.utils.Logger
import net.ankio.auto.utils.server.model.BillInfo
import net.ankio.auto.utils.server.model.BookNameModel
import net.ankio.auto.utils.server.model.CustomRuleModel

class BillCategoryDialog(
    private val context: Context,
    private val billInfo: BillInfo,
) :
    BaseSheetDialog(context) {
    private lateinit var binding: DialogBillCategoryBinding

    override fun onCreateView(inflater: LayoutInflater): View {
        binding = DialogBillCategoryBinding.inflate(inflater)

        cardView = binding.cardView
        cardViewInner = binding.innerView

        binding.cancelButton.setOnClickListener {
            dismiss()
        }

        binding.sureButton.setOnClickListener {
            // 自动生成只关注 shopName和shopItem

            var shopName = billInfo.shopName
            var shopItem = billInfo.shopItem

            // 如果包含数字，就是无效数据

            if (shopName.contains(Regex("\\d"))) {
                shopName = ""
            }

            if (shopItem.contains(Regex("\\d"))) {
                shopItem = ""
            }

            if (shopItem.isEmpty() && shopName.isEmpty()) {
                dismiss()
                return@setOnClickListener
            }
            val list: MutableList<HashMap<String, Any>> = mutableListOf()
            var text = "若满足"
            var condition = ""
            if (shopName.isNotEmpty()) {
                val select = 0
                val content = shopName
                val type = "shopName"
                val js = "$type.indexOf(\"$content\")!==-1 "
                val msg =
                    context.getString(
                        R.string.shop_name_contains,
                        context.getString(R.string.shop_name),
                        content,
                    )

                val data: HashMap<String, Any> =
                    hashMapOf(
                        "select" to select,
                        "content" to content,
                        "type" to type,
                        "js" to js,
                        "text" to msg,
                    )
                text += msg
                condition += js
                list.add(data)
            }

            if (shopItem.isNotEmpty()) {
                if (shopName.isNotEmpty()) {
                    val innerData: HashMap<String, Any> =
                        hashMapOf(
                            "jsPre" to "and",
                            "text" to " 且 ",
                            "js" to " && ",
                        )
                    condition += " && "
                    text += " 且 "
                    list.add(innerData)
                }
                val select = 0
                val content = shopItem
                val type = "shopItem"
                val js = "$type.indexOf(\"$content\")!==-1 "
                val msg =
                    context.getString(
                        R.string.shop_name_contains,
                        context.getString(R.string.shop_item_name),
                        content,
                    )
                val data: HashMap<String, Any> =
                    hashMapOf(
                        "select" to select,
                        "content" to content,
                        "type" to type,
                        "js" to js,
                        "text" to msg,
                    )
                condition += js
                text += msg
                list.add(data)
            }

            text += "，则账本为【${billInfo.bookName}】，分类为【${billInfo.cateName}】。"

            lifecycleScope.launch {
                Logger.i("condition:$condition")
                val book = BookNameModel.getByName(billInfo.bookName)
                val id = book.id
                val otherData =
                    hashMapOf<String, Any>(
                        "book" to billInfo.bookName,
                        "category" to billInfo.cateName,
                        "id" to id,
                    )
                list.add(otherData)
                condition += ""
                val js =
                    "if($condition){ return { book:'${billInfo.bookName}',category:'${billInfo.cateName}'} }"
                val customRuleModel = CustomRuleModel()
                customRuleModel.js = js
                customRuleModel.text = text
                customRuleModel.element = Gson().toJson(list)
                customRuleModel.auto = true
                customRuleModel.use = true
                CustomRuleModel.put(customRuleModel)
                dismiss()
            }
        }

        return binding.root
    }
}
