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
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.elevation.SurfaceColors
import net.ankio.auto.R
import net.ankio.auto.database.table.AppData
import net.ankio.auto.constant.DataType
import net.ankio.auto.constant.toDataType
import net.ankio.auto.databinding.AdapterDataBinding
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.CustomTabsHelper
import net.ankio.auto.utils.DateUtils
import org.json.JSONArray
import org.json.JSONObject


class DataAdapter(
    private val dataItems: List<AppData>,
    private val onClickContent: (string: String)->Unit,
    private val onClickTest: (item: AppData)->Unit,
    private val onClickUploadData: (item: AppData, position: Int)->Unit
) : BaseAdapter<DataAdapter.ViewHolder>() {

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
        fun bind(item: AppData, position: Int) {
            binding.groupCard.setCardBackgroundColor(SurfaceColors.SURFACE_1.getColor(context))
            binding.content.setBackgroundColor(SurfaceColors.SURFACE_3.getColor(context))
            //格式化数据
            val prettyJson: String = AppUtils.toPrettyFormat(item.data)

            binding.content.text = prettyJson
            when(item.type.toDataType()){
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
            if(item.issue==0){
                binding.issue.visibility = View.GONE
            }else{
                binding.uploadData.visibility = View.GONE
                binding.issue.text = "# ${item.issue}"
                binding.issue.setOnClickListener {
                        CustomTabsHelper.launchUrl(context, Uri.parse("https://github.com/AutoAccountingOrg/AutoRule/issues/${item.issue}"))
                }
            }
            val app = AppUtils.getAppInfoFromPackageName(item.source,context)

            binding.app.text = item.source.let {
                if (item.type.toDataType() !== DataType.Sms){
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
                onClickTest(item);
            }
            binding.content.setOnClickListener{
                onClickContent(binding.content.text as String);

            }

            binding.uploadData.setOnClickListener{
                onClickUploadData(item,position)
            }
        }
    }
}

