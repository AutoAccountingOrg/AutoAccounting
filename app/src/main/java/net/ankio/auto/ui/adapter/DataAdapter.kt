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

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.elevation.SurfaceColors
import net.ankio.auto.R
import net.ankio.auto.constant.DataType
import net.ankio.auto.database.table.AppData
import net.ankio.auto.databinding.AdapterDataBinding
import net.ankio.auto.utils.AppInfoUtils
import net.ankio.auto.utils.DateUtils
import net.ankio.auto.utils.ThemeUtils
import org.json.JSONObject


class DataAdapter(private val dataItems: List<AppData>,private val listener: ItemListener) : RecyclerView.Adapter<DataAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(AdapterDataBinding.inflate(LayoutInflater.from(parent.context),parent,false),parent.context)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataItems[position]
        holder.bind(item,position)
    }

    override fun getItemCount(): Int {
        return dataItems.size
    }

    inner class ViewHolder(private val binding: AdapterDataBinding,private val context:Context) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AppData,position: Int) {
            binding.groupCard.setCardBackgroundColor(SurfaceColors.SURFACE_1.getColor(context))
            binding.content.setBackgroundColor(SurfaceColors.SURFACE_3.getColor(context))

            val jsonObject = JSONObject(item.data)
            val prettyJson: String = jsonObject.toString(4) // 使用4个空格缩进

            binding.content.text = prettyJson
            when(item.type){
                DataType.Notice -> {
                    binding.type.setColorFilter(ContextCompat.getColor(context,R.color.warning))
                    binding.type.setImageResource(R.drawable.data_notice)
                }
                DataType.Helper ->{
                    binding.type.setColorFilter(ContextCompat.getColor(context,R.color.danger))
                    binding.type.setImageResource(R.drawable.data_helper)
                }
                DataType.Sms -> {
                    binding.type.setColorFilter(ContextCompat.getColor(context,R.color.info))
                    binding.type.setImageResource(R.drawable.data_sms)
                }
                DataType.App -> {
                    binding.type.setColorFilter(ContextCompat.getColor(context,R.color.success))
                    binding.type.setImageResource(R.drawable.data_app)
                }
            }
            val app = AppInfoUtils(context).getAppInfoFromPackageName(item.from)

            binding.app.text = item.from.let {
                if (item.type !== DataType.Sms){
                     app?.name
                }else{
                    it
                }
            }
            if (app != null) {
                binding.image.setImageDrawable(app.icon)
            }else{
                binding.image.setImageResource(R.drawable.data_sms)
            }

            binding.time.text = item.time.let {
                DateUtils.getTime(it)
            }

            if(!item.match){
                binding.rule.visibility = View.GONE
            }else{
                binding.uploadData.visibility = View.GONE
                binding.groupCard.setCardBackgroundColor(SurfaceColors.SURFACE_5.getColor(context))
            }
            binding.rule.text = item.rule

            binding.testRule.setOnClickListener {
                listener.onClickTest(item);
            }
            binding.content.setOnClickListener{
                listener.onClickContent(binding.content.text as String);

            }
            binding.deleteData.setOnClickListener {
                listener.onClickDelete(item,position)

            }
            binding.uploadData.setOnClickListener{
                listener.onClickUploadData(item)
            }
        }
    }
}

interface ItemListener{
    fun onClickContent(string: String)
    fun onClickTest(item: AppData)
    fun onClickDelete(item: AppData,position: Int)

    fun onClickUploadData(item: AppData)
}