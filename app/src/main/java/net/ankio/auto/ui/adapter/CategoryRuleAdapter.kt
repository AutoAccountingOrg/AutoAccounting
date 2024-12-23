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


package net.ankio.auto.ui.adapter

import android.app.Activity
import android.view.View
import androidx.core.view.size
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.elevation.SurfaceColors
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.databinding.AdapterRuleBinding
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import net.ankio.auto.ui.dialog.BottomSheetDialogBuilder
import org.ezbook.server.db.model.CategoryRuleModel

class CategoryRuleAdapter(
    val dataItems: MutableList<CategoryRuleModel>,
    val activity: Activity,
    val onClickEdit: (CategoryRuleModel, Int) -> Unit = { _, _ -> }
) : BaseAdapter<AdapterRuleBinding, CategoryRuleModel>(AdapterRuleBinding::class.java, dataItems) {


    override fun onInitViewHolder(holder: BaseViewHolder<AdapterRuleBinding, CategoryRuleModel>) {
        val binding = holder.binding

        binding.groupCard.setCardBackgroundColor(SurfaceColors.SURFACE_1.getColor(activity))
        binding.deleteData.setOnClickListener {
            val item = holder.item!!

            BottomSheetDialogBuilder(activity)
                .setTitle(activity.getString(R.string.delete_data))
                .setMessage(activity.getString(R.string.delete_msg))
                .setPositiveButton(activity.getString(R.string.sure_msg)) { _, _ ->
                    holder.launch {
                        withContext(Dispatchers.IO) {
                            CategoryRuleModel.remove(item.id)
                        }
                        val position = indexOf(item)
                        if (position == -1) return@launch
                        dataItems.removeAt(position)
                        withContext(Dispatchers.Main) {
                            notifyItemRemoved(position)
                        }
                    }
                }
                .setNegativeButton(activity.getString(R.string.cancel_msg)) { _, _ -> }
                .show()

        }
        binding.editRule.setOnClickListener {
            val item = holder.item!!
            val position = indexOf(item)
            onClickEdit(item, position)
        }
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder<AdapterRuleBinding, CategoryRuleModel>,
        data: CategoryRuleModel,
        position: Int
    ) {
        val binding = holder.binding

        binding.autoCreate.visibility = if (data.creator == "user") {
            View.GONE
        } else {
            View.VISIBLE
        }
        val listType = object : TypeToken<MutableList<HashMap<String, Any>>>() {}.type
        val list: MutableList<HashMap<String, Any>> =
            Gson().fromJson(data.element, listType) ?: return

        val lastElement = list.removeAt(list.lastIndex)
        val flexboxLayout = binding.flexboxLayout
        flexboxLayout.removedAllElement()
        flexboxLayout.textAppearance =
            com.google.android.material.R.style.TextAppearance_Material3_BodyLarge
        flexboxLayout.appendTextView(activity.getString(R.string.if_condition_true))
        flexboxLayout.firstWaveTextViewPosition = flexboxLayout.size - 1
        for (hashMap in list) {
            if (hashMap.containsKey("jsPre")) {
                flexboxLayout.appendButton(
                    if ((hashMap["jsPre"] as String).contains("&&")) {
                        activity.getString(
                            R.string.and,
                        )
                    } else {
                        activity.getString(R.string.or)
                    },
                )
            }
            flexboxLayout.appendWaveTextview(
                hashMap["text"] as String,
                connector = false,
            ) { _, _ -> }
        }
        flexboxLayout.appendTextView(activity.getString(R.string.condition_result_book))
        flexboxLayout.appendWaveTextview(lastElement["book"] as String) { _, _ -> }
        flexboxLayout.appendTextView(activity.getString(R.string.condition_result_category))
        flexboxLayout.appendWaveTextview(lastElement["category"] as String) { _, _ -> }
    }
}

