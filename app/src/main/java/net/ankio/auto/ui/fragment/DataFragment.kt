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

package net.ankio.auto.ui.fragment

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tobey.dialogloading.DialogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.BuildConfig
import net.ankio.auto.R
import net.ankio.auto.app.BillInfoPopup
import net.ankio.auto.app.Engine
import net.ankio.auto.constant.DataType
import net.ankio.auto.constant.toDataType
import net.ankio.auto.database.Db
import net.ankio.auto.database.table.AppData
import net.ankio.auto.databinding.FragmentDataBinding
import net.ankio.auto.ui.adapter.DataAdapter
import net.ankio.auto.ui.adapter.DataItemListener
import net.ankio.auto.utils.ActiveUtils
import net.ankio.auto.utils.CustomTabsHelper
import net.ankio.auto.utils.Github
import net.ankio.auto.utils.HookUtils
import net.ankio.auto.utils.SpUtils
import java.io.IOException


class DataFragment : Fragment() {
    private lateinit var binding: FragmentDataBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DataAdapter
    private lateinit var layoutManager: LinearLayoutManager
    private val dataItems = mutableListOf<AppData>()
    private var currentPage = 0
    private val itemsPerPage = 10

    override fun onCreateView(   inflater: LayoutInflater,
                                 container: ViewGroup?,
                                 savedInstanceState: Bundle?
    ): View {
        binding = FragmentDataBinding.inflate(layoutInflater)
        recyclerView = binding.recyclerView
        layoutManager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = layoutManager

        adapter = DataAdapter(dataItems,object : DataItemListener {
            override fun onClickContent(string: String) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(requireContext().getString(R.string.content_title))
                    .setMessage(string)
                    .setPositiveButton(requireContext().getString(R.string.cancel_msg)) { _, _ -> }
                    .show()
            }

            override fun onClickTest(item: AppData) {
                lifecycleScope.launch {
                    val result =   Engine.runAnalyze(item.type,item.source,item.data, HookUtils(requireContext(),BuildConfig.APPLICATION_ID))
                    if(result==null){
                        //弹出悬浮窗
                        withContext(Dispatchers.Main){
                            Toast.makeText(context,getString(R.string.no_match),Toast.LENGTH_LONG).show()
                        }
                    }else{
                        withContext(Dispatchers.Main){
                            context?.let { BillInfoPopup.show(it,result) }
                        }
                    }
                }
            }

            override fun onClickDelete(item: AppData,position:Int) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(requireContext().getString(R.string.delete_data))
                    .setMessage(requireContext().getString(R.string.delete_msg))
                    .setNegativeButton(requireContext().getString(R.string.sure_msg)) { _, _ ->
                        lifecycleScope.launch {
                            Db.get().AppDataDao().del(item.id)
                            dataItems.removeAt(position)
                            withContext(Dispatchers.Main) {
                                adapter.notifyItemRemoved(position)
                            }
                        }
                    }
                    .setPositiveButton(requireContext().getString(R.string.cancel_msg)) { _, _ -> }
                    .show()
            }

            override fun onClickUploadData(item: AppData,position:Int) {
                if(item.issue!=0){
                    //之前上传过
                    Toast.makeText(context,getString(R.string.repeater_issue),Toast.LENGTH_LONG).show()
                    return
                }


                context?.let {
                    MaterialAlertDialogBuilder(it)
                        .setTitle(getString(R.string.upload_sure))  // 设置对话框的标题
                        .setMessage(getString(R.string.upload_info))  // 设置对话框的消息
                        .setPositiveButton(getString(R.string.ok)) { dialog, which ->
                            val type = when(item.type.toDataType()){
                                DataType.App->"App"
                                DataType.Helper->"Helper"
                                DataType.Notice->"Notice"
                                DataType.Sms->"Sms"
                            }
                            val dialog2 = DialogUtil.createLoadingDialog(it, getString(R.string.upload_waiting))
                            Github.createIssue("[Adaptation Request][$type]${item.source}", """
```
                ${item.data}
```
            """.trimIndent(),{ issue ->
                                item.issue = issue.toInt()
                                requireActivity().runOnUiThread {
                                    adapter.notifyItemChanged(position)
                                    Toast.makeText(it,getString(R.string.upload_success),Toast.LENGTH_LONG).show()
                                }
                                DialogUtil.closeDialog(dialog2)

                                lifecycleScope.launch {
                                    Db.get().AppDataDao().update(item)
                                }
                            },{ msg ->
                                requireActivity().runOnUiThread {
                                    Toast.makeText(it,msg,Toast.LENGTH_LONG).show()
                                    CustomTabsHelper.launchUrl(it, Uri.parse(Github.getLoginUrl()))
                                }
                                DialogUtil.closeDialog(dialog2)
                            })

                            // 可以在这里添加你的处理逻辑
                            dialog.dismiss()
                        }
                        .setNegativeButton(R.string.close) { dialog, which ->
                            // 在取消按钮被点击时执行的操作
                            dialog.dismiss()
                        }
                        .show()
                }



            }

        })

        recyclerView.adapter = adapter

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (visibleItemCount + firstVisibleItemPosition >= totalItemCount
                    && firstVisibleItemPosition >= 0
                ) {
                    loadMoreData()
                }
            }
        })





        return binding.root
    }

    private fun loadMoreData() {

        ActiveUtils.getDataList(currentPage,itemsPerPage,requireContext()){
            dataItems.addAll(it)
            adapter.notifyItemRangeInserted(currentPage * itemsPerPage +1, it.size )
            currentPage++
        }

        //下面这个放到无障碍中

       /* lifecycleScope.launch {
            val newData = Db.get().AppDataDao().loadAll(currentPage * itemsPerPage +1 ,itemsPerPage )
            val collection: Collection<AppData> = newData.toList()
            withContext(Dispatchers.Main) {
                // 在主线程更新 UI
                dataItems.addAll(collection)
                if(!collection.isEmpty()){
                    adapter.notifyItemRangeInserted(currentPage * itemsPerPage +1, itemsPerPage )
                    currentPage++
                }

            }
        }*/



    }

    override fun onResume() {
        super.onResume()
        //加载数据
        dataItems.clear()
        adapter.notifyItemRemoved(0)
        currentPage = 0
        loadMoreData()
    }



}