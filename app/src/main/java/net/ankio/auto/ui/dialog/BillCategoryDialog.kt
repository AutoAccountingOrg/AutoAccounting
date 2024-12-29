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
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseSheetDialog
import org.ezbook.server.db.model.BillInfoModel
import org.ezbook.server.db.model.BookNameModel
import org.ezbook.server.db.model.CategoryRuleModel


class BillCategoryDialog(
    private val context: Context,
    private val billInfoModel: BillInfoModel,
) :
    BaseSheetDialog(context) {
    private lateinit var binding: DialogBillCategoryBinding

    override fun onCreateView(inflater: LayoutInflater): View {
        binding = DialogBillCategoryBinding.inflate(inflater)

        //cardView = binding.cardView
        // cardViewInner = binding.innerView

        binding.cancelButton.setOnClickListener {
            dismiss()
        }

        binding.sureButton.setOnClickListener {
            // 自动生成只关注 shopName和shopItem

            var shopName = billInfoModel.shopName
            var shopItem = billInfoModel.shopItem

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
            //  var text = "若满足"
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
                // text += msg
                condition += js
                list.add(data)
            }

            if (shopItem.isNotEmpty()) {
                var jsPre = false
                if (shopName.isNotEmpty()) {
                    jsPre = true
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
                if (jsPre) {
                    condition += " && "
                    data["jsPre"] = " && "
                }
                condition += js
                //  text += msg
                list.add(data)
            }

            Logger.i("list=$list")

            //  text += "，则账本为【${billInfoModel.bookName}】，分类为【${billInfoModel.cateName}】。"

            lifecycleScope.launch {
                val book = BookNameModel.getByName(billInfoModel.bookName)
                val id = book.id
                val otherData =
                    hashMapOf<String, Any>(
                        "book" to billInfoModel.bookName,
                        "category" to billInfoModel.cateName,
                        "id" to id,
                    )
                list.add(otherData)
                condition += ""
                val js =
                    "if($condition){ return { book:'${billInfoModel.bookName}',category:'${billInfoModel.cateName}'} }"
                val categoryRuleModel = CategoryRuleModel()
                categoryRuleModel.js = js
                // categoryRuleModel.text = text
                categoryRuleModel.element = Gson().toJson(list)
                categoryRuleModel.creator = "system"
                /*   customRuleModel.auto = true
                   customRuleModel.use = true*/
                CategoryRuleModel.put(categoryRuleModel)
                Logger.d("Create Category Rule: ${categoryRuleModel.js}")
                dismiss()
            }
        }

        return binding.root
    }
}
